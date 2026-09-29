#!/system/bin/sh
MODDIR=${0%/*}
echo 'Meta Gate: Search always blocked; Meta routed only to allowed focused apps.'
echo 'Escape: raw KEYCODE_ESCAPE to all apps; no system Back fallback.'
echo 'Configuration: /data/system/meta_gate/apps.conf'
cat /data/system/meta_gate/apps.conf 2>/dev/null
echo ''
echo 'Disable/uninstall in KernelSU, then reboot to restore original behavior.'
tail -n 8 "$MODDIR/boot.log" 2>/dev/null
