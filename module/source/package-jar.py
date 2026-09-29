from pathlib import Path
import zipfile,hashlib
src=Path('work/services.jar');dst=Path('work/module/services.jar')
with zipfile.ZipFile(src) as z, zipfile.ZipFile(dst,'w',compression=zipfile.ZIP_STORED) as o:
 for entry in z.infolist():
  o.writestr(entry.filename,Path('work/patched-classes2.dex').read_bytes() if entry.filename=='classes2.dex' else z.read(entry))
Path('work/module/original.sha256').write_text(hashlib.sha256(src.read_bytes()).hexdigest()+'\n')
print('original',hashlib.sha256(src.read_bytes()).hexdigest());print('patched',hashlib.sha256(dst.read_bytes()).hexdigest())
