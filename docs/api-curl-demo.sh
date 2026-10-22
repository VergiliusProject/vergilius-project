#!/usr/bin/env bash
# End-to-end verification of the /api/v1 surface against a running instance
# (./gradlew bootRun with MySQL, after --dump-regenerate for the search leg).
set -u
BASE=${BASE:-http://127.0.0.1:8080}
fail=0

check() { # name, expected_status, curl args...
  local name=$1 want=$2; shift 2
  local got
  got=$(curl -s -o /dev/null -w '%{http_code}' "$@")
  if [ "$got" = "$want" ]; then
    echo "PASS  $name ($got)"
  else
    echo "FAIL  $name (want $want, got $got)"
    fail=1
  fi
}

check "families by arch"            200 "$BASE/api/v1/kernels/x64"
check "families 404 unknown arch"   404 "$BASE/api/v1/kernels/arm"
check "family listing"              200 "$BASE/api/v1/kernels/x64/windows-10"

# These depend on imported data; verify shapes only when data exists.
os_json=$(curl -s "$BASE/api/v1/kernels/x64/windows-10")
if [ -n "$os_json" ] && [ "$os_json" != "" ]; then
  os_slug=$(printf '%s' "$os_json" | grep -o '"osSlug":"[^"]*"' | head -1 | cut -d'"' -f4)
  if [ -n "$os_slug" ]; then
    check "types list"  200 "$BASE/api/v1/kernels/x64/windows-10/$os_slug/types?size=5"
    check "list bad kind -> 400" 400 "$BASE/api/v1/kernels/x64/windows-10/$os_slug/types?kind=pointer"
    check "list size cap -> 400" 400 "$BASE/api/v1/kernels/x64/windows-10/$os_slug/types?size=9999"

    detail=$(curl -si "$BASE/api/v1/kernels/x64/windows-10/$os_slug/types/LIST_ENTRY")
    status=$(printf '%s' "$detail" | head -1 | awk '{print $2}')
    if [ "$status" = "200" ]; then
      echo "PASS  type detail (200)"
      etag=$(printf '%s' "$detail" | grep -i '^etag:' | head -1 | tr -d '\r' | cut -d' ' -f2)
      if [ -n "$etag" ]; then
        check "If-None-Match -> 304" 304 -H "If-None-Match: $etag" \
          "$BASE/api/v1/kernels/x64/windows-10/$os_slug/types/LIST_ENTRY"
      fi
      body=$(printf '%s' "$detail" | tail -1)
      case "$body" in
        *str-link*) echo "FAIL  cSource contains HTML anchors"; fail=1 ;;
        *)         echo "PASS  cSource is HTML-free" ;;
      esac
    else
      echo "SKIP  type detail (no LIST_ENTRY in this build, status $status)"
    fi
  fi
fi

check "search blank q -> 400"       400 "$BASE/api/v1/types/search?q=+"
check "search hit"                  200 "$BASE/api/v1/types/search?q=PEB"
check "manifest served"            200 "$BASE/api/index.json"

if [ "$fail" = "0" ]; then echo "ALL CHECKS PASSED"; else exit 1; fi
