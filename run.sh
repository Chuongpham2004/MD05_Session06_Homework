#!/usr/bin/env bash
# =====================================================================
#  run.sh - Build và khởi chạy toàn bộ hệ thống Ngân hàng bằng 1 lệnh
#  Cách dùng:  chmod +x run.sh && ./run.sh
# =====================================================================
set -euo pipefail

# Luôn chạy tại thư mục chứa script (thư mục gốc monorepo)
cd "$(dirname "$0")"

SERVICES=(core-db account-service transaction-service api-gateway)
TIMEOUT=300   # số giây tối đa chờ toàn hệ thống healthy

step() { printf '\n\033[1;36m==> %s\033[0m\n' "$*"; }
ok()   { printf '\033[1;32m[OK]\033[0m %s\n' "$*"; }
fail() { printf '\033[1;31m[LỖI]\033[0m %s\n' "$*" >&2; exit 1; }

# ---------- 0. Kiểm tra môi trường ----------
step "Kiểm tra môi trường"
command -v docker >/dev/null 2>&1 || fail "Chưa cài Docker"
docker info >/dev/null 2>&1      || fail "Docker chưa chạy (hãy bật Docker Desktop)"
if [ ! -f .env ]; then
  fail "Chưa có file .env. Hãy tạo từ mẫu:  cp .env.example .env  rồi điền giá trị thật"
fi
ok "Docker sẵn sàng, đã có file .env"

# ---------- 1. Dọn dẹp container cũ ----------
step "Dọn dẹp container cũ (docker compose down)"
# Không dùng -v để GIỮ dữ liệu trong volume pg-data
docker compose down --remove-orphans

# ---------- 2. Build image mới nhất và chạy ngầm ----------
step "Build image mới nhất và khởi chạy ngầm (docker compose up -d --build)"
docker compose up -d --build

# ---------- 3. Theo dõi thứ tự các container lên trạng thái healthy ----------
step "Chờ các service lên trạng thái healthy (tối đa ${TIMEOUT}s)"
declare -A reported=()
start=$(date +%s)
while :; do
  all_healthy=true
  for svc in "${SERVICES[@]}"; do
    status=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$svc" 2>/dev/null || echo "missing")
    restarts=$(docker inspect -f '{{.RestartCount}}' "$svc" 2>/dev/null || echo 0)
    [ "$restarts" -gt 0 ] && fail "$svc đã bị restart $restarts lần. Xem log: docker logs $svc"
    [ "$status" = "unhealthy" ] && fail "$svc ở trạng thái unhealthy. Xem log: docker logs $svc"
    if [ "$status" = "healthy" ]; then
      if [ -z "${reported[$svc]:-}" ]; then
        reported[$svc]=1
        printf '  %-22s \033[1;32mhealthy\033[0m   (sau %ss)\n' "$svc" "$(( $(date +%s) - start ))"
      fi
    else
      all_healthy=false
    fi
  done
  $all_healthy && break
  [ $(( $(date +%s) - start )) -ge $TIMEOUT ] && fail "Quá ${TIMEOUT}s mà hệ thống chưa healthy. Xem: docker compose ps"
  sleep 2
done

# ---------- 4. Tổng kết ----------
step "Trạng thái hệ thống"
docker compose ps --format 'table {{.Name}}\t{{.Status}}\t{{.Ports}}'

step "Thứ tự khởi động thực tế (theo thời điểm container start)"
for svc in "${SERVICES[@]}"; do
  printf '%s  %s\n' "$(docker inspect -f '{{.State.StartedAt}}' "$svc" | cut -c12-23)" "$svc"
done | sort

echo
ok "Hệ thống đã sẵn sàng. Điểm truy cập DUY NHẤT: http://localhost/api/v1/..."
echo "     Ví dụ:  curl http://localhost/api/v1/accounts"
