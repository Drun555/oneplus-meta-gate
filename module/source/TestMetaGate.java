import android.content.Context;
import android.os.*;
import android.view.KeyEvent;
import java.lang.reflect.*;
import local.codex.metagate.MetaGate;
public class TestMetaGate {
 public static class Info { public int windowOwnerUid; Info(int uid){windowOwnerUid=uid;} }
 public static class WM { public Info info; WM(int uid){info=new Info(uid);} public Info get(IBinder token){return info;} }
 static int count;
 static void eq(String name,int expected,int actual){if(expected!=actual)throw new AssertionError(name+": "+actual+" != "+expected);count++;System.out.println("PASS "+name);}
 static KeyEvent e(int action,int code,int meta){return new KeyEvent(1,2,action,code,0,meta,9,0);}
 public static void main(String[] a)throws Exception {
  Looper.prepareMainLooper(); Class<?> c=Class.forName("android.app.ActivityThread"); Object at=c.getMethod("systemMain").invoke(null); Context ctx=(Context)c.getMethod("getSystemContext").invoke(at);
  Field m=MetaGate.class.getDeclaredField("getInfo");m.setAccessible(true);m.set(null,WM.class.getMethod("get",IBinder.class));
  Field f=MetaGate.class.getDeclaredField("ownerUid");f.setAccessible(true);f.set(null,Info.class.getField("windowOwnerUid"));
  WM remote=new WM(ctx.getPackageManager().getApplicationInfo("com.limelight.noir",0).uid);WM other=new WM(1000);IBinder token=new Binder();
  int interactive=0x20000000, meta=KeyEvent.META_META_ON|KeyEvent.META_META_LEFT_ON;
  eq("ordinary A unchanged",-2,MetaGate.queue(e(0,29,0),interactive));
  eq("Search down blocked",0,MetaGate.queue(e(0,84,0),interactive));eq("Search up blocked",0,MetaGate.queue(e(1,84,0),interactive));
  eq("Meta queues without policy",1,MetaGate.queue(e(0,117,meta),interactive));eq("Meta release",1,MetaGate.queue(e(1,117,0),interactive));
  eq("screen-off Meta blocked",0,MetaGate.queue(e(0,118,meta),0));MetaGate.queue(e(1,118,0),0);
  eq("Meta+L bypasses queue policy",1,MetaGate.queue(e(0,40,meta),interactive));eq("L up after Meta released still tracked",1,MetaGate.queue(e(1,40,0),interactive));
  eq("Meta blocked outside allowlist",-1,MetaGate.dispatch(ctx,other,token,e(0,117,meta)));eq("Meta up blocked outside",-1,MetaGate.dispatch(ctx,other,token,e(1,117,0)));
  eq("Meta passes remote directly",0,MetaGate.dispatch(ctx,remote,token,e(0,117,meta)));eq("Meta+L passes remote",0,MetaGate.dispatch(ctx,remote,token,e(0,40,meta)));eq("Meta up remote",0,MetaGate.dispatch(ctx,remote,token,e(1,117,0)));eq("L up after Meta passes remote",0,MetaGate.dispatch(ctx,remote,token,e(1,40,0)));
  eq("Meta+L blocked outside",-1,MetaGate.dispatch(ctx,other,token,e(0,40,meta)));eq("no orphan release outside",-1,MetaGate.dispatch(ctx,other,token,e(1,40,0)));
  eq("Search blocked even remote",-1,MetaGate.dispatch(ctx,remote,token,e(0,84,0)));
  eq("no focused window fails closed",-1,MetaGate.dispatch(ctx,remote,null,e(0,118,meta)));MetaGate.dispatch(ctx,remote,null,e(1,118,0));
  eq("ordinary keys keep original policy",-2,MetaGate.dispatch(ctx,remote,token,e(0,29,0)));
  eq("Meta fallback suppressed",1,MetaGate.suppressFallback(e(0,117,0))?1:0);
  eq("Escape queues without system policy",1,MetaGate.queue(e(0,111,0),interactive));
  eq("Escape queue release",1,MetaGate.queue(e(1,111,0),interactive));
  eq("Escape delivered to ordinary app",0,MetaGate.dispatch(ctx,other,token,e(0,111,0)));
  eq("Escape release delivered to ordinary app",0,MetaGate.dispatch(ctx,other,token,e(1,111,0)));
  eq("Escape delivered to remote app",0,MetaGate.dispatch(ctx,remote,token,e(0,111,0)));
  eq("Escape release delivered to remote app",0,MetaGate.dispatch(ctx,remote,token,e(1,111,0)));
  eq("Escape never falls back to Back",1,MetaGate.suppressFallback(e(0,111,0))?1:0);
  eq("Back key retains original behavior",-2,MetaGate.queue(e(0,4,0),interactive));
  eq("Meta+Escape blocked outside",-1,MetaGate.dispatch(ctx,other,token,e(0,111,meta)));
  eq("Meta+Escape release paired outside",-1,MetaGate.dispatch(ctx,other,token,e(1,111,0)));
  System.out.println("PASSED "+count+" checks");System.exit(0);
 }
}
