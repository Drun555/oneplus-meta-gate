#!/system/bin/sh
MODDIR=${0%/*}
BB=/data/adb/ksu/bin/busybox
exec >>"$MODDIR/boot.log" 2>&1
echo "Meta Gate boot: $(date)"
if [ -e "$MODDIR/boot-pending" ]; then
    echo 'Previous boot did not complete. Disabling Meta Gate for recovery.'
    touch "$MODDIR/disable"
    exit 0
fi
EXPECTED=$(cat "$MODDIR/original.sha256")
ACTUAL=$($BB sha256sum /system/framework/services.jar | $BB cut -d ' ' -f 1)
if [ "$ACTUAL" != "$EXPECTED" ]; then
    echo 'Firmware hash differs. No mount performed. Module disabled.'
    touch "$MODDIR/disable"
    exit 0
fi
KL=/odm/usr/keylayout/Vendor_22d9_Product_3869.kl
ACTUAL=$($BB sha256sum "$KL" | $BB cut -d ' ' -f 1)
if [ "$ACTUAL" != "$(cat "$MODDIR/keyboard-original.sha256")" ]; then
    echo 'Keyboard layout differs. Module disabled before mounting.'
    touch "$MODDIR/disable"
    exit 0
fi
chmod 0644 "$MODDIR/keyboard.kl"
chcon u:object_r:system_file:s0 "$MODDIR/keyboard.kl" || exit 1
mkdir -p /data/system/meta_gate
chown 1000:1000 /data/system/meta_gate
chmod 0750 /data/system/meta_gate
if [ ! -f /data/system/meta_gate/apps.conf ]; then
    cp "$MODDIR/apps.conf" /data/system/meta_gate/apps.conf
fi
chown 1000:1000 /data/system/meta_gate/apps.conf
chmod 0640 /data/system/meta_gate/apps.conf
chcon u:object_r:system_data_file:s0 /data/system/meta_gate /data/system/meta_gate/apps.conf
chmod 0644 "$MODDIR/services.jar"
chcon u:object_r:system_file:s0 "$MODDIR/services.jar" || exit 1
touch "$MODDIR/boot-pending"
if "$BB" nsenter -t 1 -m "$BB" mount -o bind "$MODDIR/services.jar" /system/framework/services.jar; then
    if "$BB" nsenter -t 1 -m "$BB" mount -o bind "$MODDIR/keyboard.kl" "$KL"; then
        echo 'Patched services.jar and OnePlus keyboard layout mounted.'
    else
        "$BB" nsenter -t 1 -m "$BB" umount -l /system/framework/services.jar
        rm -f "$MODDIR/boot-pending"
        touch "$MODDIR/disable"
        echo 'Keyboard mount failed; framework mount rolled back.'
    fi
else
    echo 'Mount failed; original framework retained.'
    rm -f "$MODDIR/boot-pending"
    touch "$MODDIR/disable"
fi
