#!/system/bin/sh
MODDIR=${0%/*}
[ -e "$MODDIR/disable" ] && exit 0
# KernelSU starts this asynchronously. A failed boot retains the guard marker.
until [ "$(getprop sys.boot_completed)" = 1 ]; do sleep 3; done
sleep 15
rm -f "$MODDIR/boot-pending"
echo "Boot completed: $(date)" >> "$MODDIR/boot.log"
