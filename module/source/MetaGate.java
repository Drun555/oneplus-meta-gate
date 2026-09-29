package local.codex.metagate;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/** Executes inside system_server, before Android's keyboard gesture policy. */
public final class MetaGate {
    private static final String CONFIG = "/data/system/meta_gate/apps.conf";
    private static final Set<Long> queued = new HashSet<>();
    private static final Map<Long, IBinder> routed = new HashMap<>();
    private static Set<String> apps = new HashSet<>(Arrays.asList(
        "com.limelight", "com.limelight.noir", "com.microsoft.rdc.androidx", "com.microsoft.rdc.android"));
    private static long lastRead = -3000;
    private static Method getInfo;
    private static Field ownerUid;
    private static boolean reported;

    public static boolean relevant(KeyEvent e) {
        int k = e.getKeyCode();
        return k == KeyEvent.KEYCODE_SEARCH || k == KeyEvent.KEYCODE_ESCAPE || k == KeyEvent.KEYCODE_META_LEFT
            || k == KeyEvent.KEYCODE_META_RIGHT || e.isMetaPressed();
    }
    private static long id(KeyEvent e) {
        return ((long)e.getDeviceId() << 32) | (e.getKeyCode() & 0xffffffffL);
    }
    // -2 = original policy; 0 = drop; 1 = pass to dispatcher, bypass gestures.
    public static synchronized int queue(KeyEvent e, int flags) {
        long id = id(e);
        boolean special = relevant(e) || queued.contains(id);
        if (!special) return -2;
        if (e.getAction() == KeyEvent.ACTION_UP) queued.remove(id);
        else queued.add(id);
        if (e.getKeyCode() == KeyEvent.KEYCODE_SEARCH) return 0;
        return (flags & 0x20000000) != 0 ? 1 : 0; // POLICY_FLAG_INTERACTIVE
    }
    private static void reload() {
        long now = SystemClock.uptimeMillis();
        if (now-lastRead < 2000) return;
        lastRead = now;
        try (BufferedReader r = new BufferedReader(new FileReader(CONFIG))) {
            Set<String> next = new HashSet<>();
            String line;
            while ((line=r.readLine()) != null) {
                line=line.trim();
                if (line.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+")) next.add(line);
            }
            apps=next;
        } catch (IOException ex) { /* Keep last valid config; defaults on first boot. */ }
    }
    private static boolean allowed(Context context, Object wm, IBinder token) {
        if (context == null || wm == null || token == null) return false;
        try {
            KeyguardManager km = (KeyguardManager)context.getSystemService(Context.KEYGUARD_SERVICE);
            if (km == null || km.isKeyguardLocked()) return false;
            if (getInfo == null) {
                getInfo=Class.forName("com.android.server.wm.WindowManagerInternal")
                    .getMethod("getKeyInterceptionInfoFromToken",IBinder.class);
                ownerUid=Class.forName("com.android.internal.policy.KeyInterceptionInfo")
                    .getField("windowOwnerUid");
            }
            Object info=getInfo.invoke(wm,token);
            if (info == null) return false;
            String[] packages=context.getPackageManager().getPackagesForUid(ownerUid.getInt(info));
            reload();
            if (packages != null) for (String p:packages) if(apps.contains(p)) return true;
        } catch (Throwable ex) {
            if (!reported) { reported=true; Log.e("MetaGate","Focus lookup failed; Meta blocked",ex); }
        }
        return false;
    }
    // -2 = original policy; -1 = consume; 0 = deliver directly to focused window.
    public static synchronized int dispatch(Context context, Object wm, IBinder token, KeyEvent e) {
        long id=id(e);
        boolean tracked=routed.containsKey(id);
        if (!relevant(e) && !tracked) return -2;
        if (e.getKeyCode() == KeyEvent.KEYCODE_SEARCH) return -1;
        boolean plainEscape = e.getKeyCode() == KeyEvent.KEYCODE_ESCAPE && !e.isMetaPressed();
        boolean permit;
        if (tracked) permit = token != null && token.equals(routed.get(id))
            && (plainEscape || allowed(context,wm,token));
        else permit=token != null && (plainEscape || allowed(context,wm,token))
            && e.getAction()==KeyEvent.ACTION_DOWN;
        if (e.getAction()==KeyEvent.ACTION_UP) routed.remove(id);
        else if (!tracked) routed.put(id,permit?token:null);
        return permit?0:-1;
    }
    public static synchronized boolean suppressFallback(KeyEvent e) {
        return relevant(e) || routed.containsKey(id(e));
    }
}
