#!/usr/bin/env bash
set -euo pipefail
project_dir="$(cd "$(dirname "$0")/.." && pwd)"
apk="$project_dir/app/build/outputs/apk/debug/app-debug.apk"
if [[ -n "${ADB:-}" ]]; then
    adb_command="$ADB"
elif command -v adb >/dev/null 2>&1; then
    adb_command="$(command -v adb)"
else
    sdk_dir="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
    if [[ -z "$sdk_dir" && -f "$project_dir/local.properties" ]]; then
        sdk_dir="$(sed -n 's/^sdk.dir=//p' "$project_dir/local.properties")"
    fi
    adb_command="$sdk_dir/platform-tools/adb"
fi
if [[ ! -x "$adb_command" ]]; then
    echo 'Nie znaleziono adb. Ustaw ANDROID_HOME na katalog Android SDK.' >&2
    exit 1
fi
if [[ ! -f "$apk" ]]; then
    echo 'Najpierw zbuduj aplikację: ./gradlew assembleDebug' >&2
    exit 1
fi
devices="$("$adb_command" devices)"
count="$(printf '%s\n' "$devices" | awk '$2 == "device" { count++ } END { print count+0 }')"
if [[ "$count" != 1 ]]; then
    printf '%s\n' "$devices"
    echo 'Podłącz jeden telefon i zaakceptuj na nim zgodę na debugowanie USB.' >&2
    exit 1
fi
"$adb_command" install -r "$apk"
"$adb_command" reverse tcp:8000 tcp:8000
"$adb_command" shell am start -n pl.unorank.mobile.debug/pl.unorank.mobile.MainActivity
echo 'UnoRank uruchomiony. Zaloguj się kontem administratora backendu.'
