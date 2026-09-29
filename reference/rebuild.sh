#!/usr/bin/env bash
set -euo pipefail
JDK=/nix/store/0fwxx5ym8irn3ik35ibpf2x7881p9q8v-openjdk-headless-17.0.20.1+1
SDK=/home/drun/Documents/Codex/2026-09-26/x20/work/android-build-tools
PY=/nix/store/0if41r2dp11y0v833p5yrpgr8mdanqjk-python3-3.13.15-env/bin/python3
"$JDK/bin/javac" -source 8 -target 8 -cp "$SDK/android.jar" -d work/helper-classes work/module/source/MetaGate.java
"$JDK/bin/java" -cp "$SDK/r8.jar" com.android.tools.r8.D8 --min-api 36 --output work/helper-dex work/helper-classes/local/codex/metagate/MetaGate.class
"$JDK/bin/java" -cp 'work/tools/*' org.jf.baksmali.Main d work/helper-dex/classes.dex -o work/helper-smali
cp work/InputManagerService.original.smali work/smali2/com/android/server/input/InputManagerService.smali
"$PY" work/patch.py
"$JDK/bin/java" -Xmx2g -cp 'work/tools/*' org.jf.smali.Main a work/smali2 --api 35 -o work/patched-classes2.dex
"$PY" work/package-jar.py
"$JDK/bin/javac" -source 8 -target 8 -cp "work/helper-classes:$SDK/android.jar" -d work/test-classes work/TestMetaGate.java
"$JDK/bin/java" -cp "$SDK/r8.jar" com.android.tools.r8.D8 --min-api 36 --output work/test-dex work/test-classes/*.class work/helper-classes/local/codex/metagate/MetaGate.class
adb push work/test-dex/classes.dex /data/local/tmp/metagate-test.dex >/dev/null
adb shell 'su -c "CLASSPATH=/data/local/tmp/metagate-test.dex app_process /system/bin TestMetaGate"'
