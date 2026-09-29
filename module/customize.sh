#!/system/bin/sh
[ "$KSU" = true ] || abort 'KernelSU / KernelSU Next is required.'
EXPECTED=$(cat "$MODPATH/original.sha256")
ACTUAL=$(sha256sum /system/framework/services.jar | cut -d ' ' -f 1)
[ "$ACTUAL" = "$EXPECTED" ] || [ "$ACTUAL" = "$(cat "$MODPATH/services-patched.sha256")" ] || abort 'Different services.jar detected. Rebuild this module for your firmware before installing.'
KL=/odm/usr/keylayout/Vendor_22d9_Product_3869.kl
ACTUAL=$(sha256sum "$KL" | cut -d ' ' -f 1)
[ "$ACTUAL" = "$(cat "$MODPATH/keyboard-original.sha256")" ] || [ "$ACTUAL" = "$(cat "$MODPATH/keyboard-patched.sha256")" ] || abort 'Keyboard layout differs; rebuild required.'
ui_print 'Physical OnePlus Escape: scan code 158 -> ESCAPE'
ui_print 'Meta Gate: crDroid 12.12 / OnePlus Pad 3'
ui_print 'Search: blocked everywhere'
ui_print 'Meta: remote app window only, bypassing Android shortcuts'
ui_print 'Escape: raw key to every app, no Android Back fallback'
ui_print 'Activation requires reboot. No system partition is written.'
set_perm_recursive "$MODPATH" 0 0 0755 0644
set_perm "$MODPATH/post-fs-data.sh" 0 0 0755
set_perm "$MODPATH/service.sh" 0 0 0755
set_perm "$MODPATH/action.sh" 0 0 0755
set_perm "$MODPATH/uninstall.sh" 0 0 0755
