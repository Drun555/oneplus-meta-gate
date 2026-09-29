from pathlib import Path
import shutil,re
p=Path('work/smali2/com/android/server/input/InputManagerService.smali')
s=p.read_text();Path('work/InputManagerService.original.smali').write_text(s)
def insert(signature,code,registers=None):
 global s
 pattern=r'('+re.escape(signature)+r'\n    \.registers )(\d+)(\n)'
 def sub(m):return m[1]+str(registers if registers is not None else m[2])+m[3]+code+'\n'
 s,n=re.subn(pattern,sub,s);assert n==1,(signature,n)
insert('.method public interceptKeyBeforeQueueing(Landroid/view/KeyEvent;I)I','''
    invoke-static {p1, p2}, Llocal/codex/metagate/MetaGate;->queue(Landroid/view/KeyEvent;I)I
    move-result v0
    const/4 v1, -0x2
    if-eq v0, v1, :metagate_original_queue
    return v0
    :metagate_original_queue
''')
insert('.method public interceptKeyBeforeDispatching(Landroid/os/IBinder;Landroid/view/KeyEvent;I)J','''
    iget-object v0, p0, Lcom/android/server/input/InputManagerService;->mContext:Landroid/content/Context;
    iget-object v1, p0, Lcom/android/server/input/InputManagerService;->mKeyGestureController:Lcom/android/server/input/KeyGestureController;
    iget-object v1, v1, Lcom/android/server/input/KeyGestureController;->mWindowManagerInternal:Lcom/android/server/wm/WindowManagerInternal;
    invoke-static {v0, v1, p1, p2}, Llocal/codex/metagate/MetaGate;->dispatch(Landroid/content/Context;Ljava/lang/Object;Landroid/os/IBinder;Landroid/view/KeyEvent;)I
    move-result v2
    const/4 v3, -0x2
    if-eq v2, v3, :metagate_original_dispatch
    int-to-long v0, v2
    return-wide v0
    :metagate_original_dispatch
''',8)
insert('.method public final dispatchUnhandledKey(Landroid/os/IBinder;Landroid/view/KeyEvent;I)Landroid/view/KeyEvent;','''
    invoke-static/range {p2 .. p2}, Llocal/codex/metagate/MetaGate;->suppressFallback(Landroid/view/KeyEvent;)Z
    move-result v0
    if-eqz v0, :metagate_original_fallback
    const/4 v0, 0x0
    return-object v0
    :metagate_original_fallback
''')
p.write_text(s)
shutil.copytree('work/helper-smali/local','work/smali2/local',dirs_exist_ok=True)
