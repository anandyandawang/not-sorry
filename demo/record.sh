#!/usr/bin/env bash
set -x
mkdir -p out/frames

adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell settings put secure enabled_accessibility_services com.notsorry/com.notsorry.ReplaceService
adb shell settings put secure accessibility_enabled 1
sleep 3
adb shell settings get secure enabled_accessibility_services | tee out/services.txt

adb shell am start -a android.intent.action.INSERT -t vnd.android.cursor.dir/contact
sleep 6

dump() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null
  adb pull /sdcard/ui.xml "$1" >/dev/null
}

foreground() {
  adb shell dumpsys activity activities | grep -E "topResumedActivity|mResumedActivity" | tee -a out/foreground.txt
}

adb logcat -c
foreground

center() {
  python3 - "$1" "$2" <<'PY'
import re, sys
xml = open(sys.argv[1]).read()
for node in re.findall(r'<node [^>]*>', xml):
    if re.search(sys.argv[2], node):
        b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', node)
        x1, y1, x2, y2 = map(int, b.groups())
        print((x1 + x2) // 2, (y1 + y2) // 2)
        break
PY
}

dump out/ui1.xml
btn=$(center out/ui1.xml 'class="android.widget.Button"[^>]*text="(?i)(keep|ok|skip|no thanks|got it|cancel)[^"]*"')
if [ -n "$btn" ]; then
  adb shell input tap $btn
  sleep 2
  dump out/ui2.xml
fi
field=""
[ -f out/ui2.xml ] && field=$(center out/ui2.xml 'class="android.widget.EditText"')
[ -n "$field" ] || field=$(center out/ui1.xml 'class="android.widget.EditText"')
adb shell input tap $field
sleep 1
foreground
dump out/ui_after_tap.xml

touch out/recording
(
  i=0
  while [ -f out/recording ]; do
    adb exec-out screencap -p > out/frames/f$(printf %04d $i).png
    i=$((i + 1))
  done
) &
rec=$!
sleep 2

adb shell input text "I%sam%sso%ssorry"
sleep 1
foreground
adb shell input keyevent 62
sleep 2
foreground
dump out/ui_after_first_word.xml
adb shell input text "Sorry"
sleep 1
adb shell input keyevent 55
sleep 2
adb shell input keyevent 62
adb shell input text "SORRY"
sleep 1
adb shell input keyevent 56
sleep 3

rm out/recording
wait $rec
dump out/ui_final.xml
foreground
adb logcat -d > out/logcat.txt

ffmpeg -y -framerate 3 -i out/frames/f%04d.png -vf "scale=trunc(iw/4)*2:trunc(ih/4)*2" -c:v libx264 -pix_fmt yuv420p out/demo.mp4

python3 - <<'PY'
import re
xml = open("out/ui_final.xml").read()
texts = re.findall(r'class="android.widget.EditText"[^>]*text="([^"]*)"', xml) + re.findall(r'text="([^"]*)"[^>]*class="android.widget.EditText"', xml)
print(texts)
assert any("so " in t and "sorry" not in t.lower() for t in texts), texts
PY
