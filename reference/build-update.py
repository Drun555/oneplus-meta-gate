from pathlib import Path
import shutil,hashlib,zipfile
p=Path('work/module-v1.2');shutil.copytree('work/module',p,dirs_exist_ok=True)
original=Path('work/escape-diagnosis/keyboard-original.kl').read_bytes()
assert original.count(b'key 158   BACK\n')==1
patched=original.replace(b'key 158   BACK\n',b'key 158   ESCAPE\n')
(p/'keyboard.kl').write_bytes(patched)
(p/'keyboard-original.sha256').write_text(hashlib.sha256(original).hexdigest()+'\n')
(p/'keyboard-patched.sha256').write_text(hashlib.sha256(patched).hexdigest()+'\n')
(p/'services-patched.sha256').write_text(hashlib.sha256((p/'services.jar').read_bytes()).hexdigest()+'\n')
s=(p/'module.prop').read_text().replace('version=1.1.0','version=1.2.0').replace('versionCode=110','versionCode=120').replace('Blocks Search,','Fixes physical OnePlus Escape mapping, blocks Search,');(p/'module.prop').write_text(s)
s=(p/'customize.sh').read_text().replace('[ "$ACTUAL" = "$EXPECTED" ] || abort', '[ "$ACTUAL" = "$EXPECTED" ] || [ "$ACTUAL" = "$(cat "$MODPATH/services-patched.sha256")" ] || abort')
s=s.replace("ui_print 'Meta Gate:",'''KL=/odm/usr/keylayout/Vendor_22d9_Product_3869.kl
ACTUAL=$(sha256sum "$KL" | cut -d ' ' -f 1)
[ "$ACTUAL" = "$(cat "$MODPATH/keyboard-original.sha256")" ] || [ "$ACTUAL" = "$(cat "$MODPATH/keyboard-patched.sha256")" ] || abort 'Keyboard layout differs; rebuild required.'
ui_print 'Physical OnePlus Escape: scan code 158 -> ESCAPE'
ui_print 'Meta Gate:''')
(p/'customize.sh').write_text(s)
s=(p/'post-fs-data.sh').read_text()
s=s.replace('mkdir -p /data/system/meta_gate','''KL=/odm/usr/keylayout/Vendor_22d9_Product_3869.kl
ACTUAL=$($BB sha256sum "$KL" | $BB cut -d ' ' -f 1)
if [ "$ACTUAL" != "$(cat "$MODDIR/keyboard-original.sha256")" ]; then
    echo 'Keyboard layout differs. Module disabled before mounting.'
    touch "$MODDIR/disable"
    exit 0
fi
chmod 0644 "$MODDIR/keyboard.kl"
chcon u:object_r:system_file:s0 "$MODDIR/keyboard.kl" || exit 1
mkdir -p /data/system/meta_gate''')
s=s.replace("    echo 'Patched services.jar mounted in init mount namespace.'",'''    if "$BB" nsenter -t 1 -m "$BB" mount -o bind "$MODDIR/keyboard.kl" "$KL"; then
        echo 'Patched services.jar and OnePlus keyboard layout mounted.'
    else
        "$BB" nsenter -t 1 -m "$BB" umount -l /system/framework/services.jar
        rm -f "$MODDIR/boot-pending"
        touch "$MODDIR/disable"
        echo 'Keyboard mount failed; framework mount rolled back.'
    fi''')
(p/'post-fs-data.sh').write_text(s)
s=(p/'README.md').read_text().replace('1.1.0','1.2.0')
s=s.replace('## Поведение','''## Изменение 1.2

Физическая Escape на клавиатуре OnePlus Pad 3 посылает Linux `KEY_BACK`
(scan code 158). В её аппаратной раскладке исправлена одна строка:
`key 158 BACK` → `key 158 ESCAPE`. После этого применяется существующий
пропуск Escape в приложения. Кнопка/жест «Назад» на экране и другие клавиатуры
не изменяются. Artemis не модифицируется.

Подменяется также `/odm/usr/keylayout/Vendor_22d9_Product_3869.kl`.
Его исходный SHA-256 проверяется перед включением. Для чтения раскладки
драйвером ввода после установки нужна перезагрузка.

## Поведение''')
s=s.replace('Установка поверх уже активного патча требует\nпредварительного отключения модуля и перезагрузки.','Установщик допускает обновление поверх активной версии 1.1 с тем же services.jar.')
(p/'README.md').write_text(s)
out=Path('outputs/OnePlus-Meta-Gate-1.2.0-crDroid-12.12.zip')
with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
 for f in sorted(p.rglob('*')):
  if f.is_file():z.write(f,str(f.relative_to(p)))
shutil.copy(p/'README.md','outputs/README-Meta-Gate.md')
Path('outputs/SHA256SUMS.txt').write_text(''.join(hashlib.sha256(f.read_bytes()).hexdigest()+'  '+f.name+'\n' for f in sorted(Path('outputs').glob('*.zip'))))
print('Only layout change:', [line for line in patched.decode().splitlines() if line.startswith('key 158')])
print(out,hashlib.sha256(out.read_bytes()).hexdigest())
