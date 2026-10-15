#!/usr/bin/env bash
# 在 gq 主机上部署 FakeLua 版麻将服务。
# 用法: ./deploy.sh [user@host]
# 依赖: 本机已编好 fakelua 与 majiangserver_fakelua;远端 /opt/majiang/data 有查表文件。
set -euo pipefail

HOST="${1:-root@139.186.122.226}"
DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$DIR/../.." && pwd)"
FAKELUA_ROOT="${FAKELUA_ROOT:-$ROOT/../fakelua}"
SERVER_BUILD="${SERVER_BUILD:-$ROOT/server/build}"

echo "=== 0. 确认本地二进制 ==="
if [ ! -x "$SERVER_BUILD/majiangserver_fakelua" ]; then
  echo "missing $SERVER_BUILD/majiangserver_fakelua — build server/ first" >&2
  exit 1
fi
FAKELUA_SO="$(ls "$FAKELUA_ROOT"/build/lib64/libfakelua.so* 2>/dev/null | head -1 || true)"
if [ -z "$FAKELUA_SO" ]; then
  echo "missing libfakelua.so under $FAKELUA_ROOT/build/lib64" >&2
  exit 1
fi

echo "=== 1. 上传二进制、脚本、静态资源与 systemd 单元 ==="
ssh "$HOST" "mkdir -p /opt/majiang/lib /opt/majiang/lua /opt/majiang/static /opt/majiang/data"
scp "$SERVER_BUILD/majiangserver_fakelua" "$HOST:/opt/majiang/majiangserver_fakelua.new"
scp -d "$FAKELUA_ROOT"/build/lib64/libfakelua.so* "$HOST:/opt/majiang/lib/" || \
  scp "$FAKELUA_ROOT"/build/lib64/libfakelua.so* "$HOST:/opt/majiang/lib/"
scp -r "$ROOT/server/lua/"* "$HOST:/opt/majiang/lua/"
scp -r "$ROOT/server/static/"* "$HOST:/opt/majiang/static/"
scp "$DIR/majiangserver.service" "$HOST:/etc/systemd/system/majiangserver.service"

echo "=== 2. 服务器侧:数据文件、启动服务 ==="
ssh "$HOST" bash -s <<'EOF'
set -euo pipefail
cd /opt/majiang
if [ ! -f data/majiang_clien_normal.txt ]; then
  for cand in /opt/majiang_algorithm /root/majiang_algorithm /srv/majiang_algorithm; do
    if [ -f "$cand/data/majiang_clien_normal.txt" ]; then
      cp -n "$cand"/data/majiang_*_*.txt data/ || true
      break
    fi
  done
fi
mv -f majiangserver_fakelua.new majiangserver_fakelua && chmod +x majiangserver_fakelua
# 兼容旧单元名
systemctl stop majiangserver 2>/dev/null || true
systemctl daemon-reload
systemctl enable majiangserver
systemctl restart majiangserver
sleep 2
systemctl --no-pager status majiangserver | head -12
curl -s -m 5 http://127.0.0.1:18090/api/status && echo
EOF

echo "=== 3. 完成:确认 /api/status 后按需切换 nginx ==="
