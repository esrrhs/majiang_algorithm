#!/usr/bin/env bash
# 在 gq.esrrhs.xyz (139.186.122.226) 上部署 Go 版麻将服务并下线 Java 服务。
# 用法: ./deploy.sh (通过 ssh root@<host> 执行;依赖服务器上已有 data/ 查表文件或本地上传)
set -euo pipefail

HOST="${1:-root@139.186.122.226}"
DIR="$(cd "$(dirname "$0")" && pwd)"

echo "=== 0. 构建 linux/amd64 二进制 ==="
if [ ! -f "$DIR/majiangserver-linux-amd64" ]; then
  (cd "$DIR/../../go" && GOOS=linux GOARCH=amd64 CGO_ENABLED=0 go build -trimpath \
    -ldflags="-s -w" -o "$DIR/majiangserver-linux-amd64" ./cmd/majiangserver)
fi

echo "=== 1. 上传二进制与 systemd 单元 ==="
scp "$DIR/majiangserver-linux-amd64" "$HOST:/opt/majiang/majiangserver.new"
scp "$DIR/majiangserver.service" "$HOST:/etc/systemd/system/majiangserver.service"

echo "=== 2. 服务器侧:数据文件、启动服务 ==="
ssh "$HOST" bash -s <<'EOF'
set -euo pipefail
cd /opt/majiang
# 查表文件:若本机没有则从 Java 服务目录复用
if [ ! -f data/majiang_clien_normal.txt ] && [ ! -f majiang_clien_normal.txt ]; then
  for cand in /opt/majiang_algorithm /root/majiang_algorithm /srv/majiang_algorithm; do
    if [ -f "$cand/data/majiang_clien_normal.txt" ]; then mkdir -p data && cp -n "$cand"/data/majiang_*_*.txt data/; break; fi
    if [ -f "$cand/majiang_clien_normal.txt" ]; then mkdir -p data && cp -n "$cand"/majiang_*_*.txt data/; break; fi
  done
fi
mv -f majiangserver.new majiangserver && chmod +x majiangserver
systemctl daemon-reload
systemctl enable majiangserver
systemctl restart majiangserver
sleep 2
systemctl --no-pager status majiangserver | head -8
curl -s -m 5 http://127.0.0.1:18090/api/status && echo
EOF

echo "=== 3. 完成:请确认 /api/status 返回 ok 后,手动切换 nginx vhost 并下线 Java 服务 ==="
