# Hệ thống Ngân hàng - Microservices (Monorepo)

Hệ thống gồm 4 container chạy chung mạng nội bộ `banking-network`. Chỉ có **api-gateway** mở cổng ra bên ngoài.

```
                    Client (Postman / Mobile app)
                               │  http://localhost/api/v1/...  (port 80)
                               ▼
                     ┌───────────────────┐
                     │    api-gateway    │  Spring Cloud Gateway
                     └─────────┬─────────┘
          /api/v1/accounts/**  │  /api/v1/transactions/**
              ┌────────────────┴────────────────┐
              ▼                                 ▼
   ┌─────────────────────┐   OpenFeign   ┌─────────────────────┐
   │   account-service   │◄──────────────│ transaction-service │
   │        :8080        │               │        :8080        │
   └──────────┬──────────┘               └──────────┬──────────┘
              └────────────────┬────────────────────┘
                               ▼
                     ┌───────────────────┐
                     │  core-db (PG 15)  │  volume: pg-data
                     └───────────────────┘
                 ── banking-network (không mở port ra host) ──
```

## Cấu trúc thư mục

```
.
├── docker-compose.yml        # File compose tổng: build/run toàn hệ thống
├── run.sh                    # Script khởi chạy 1 lệnh
├── .env.example              # Mẫu biến môi trường (copy thành .env)
├── account-service/          # Quản lý tài khoản         (Spring Boot 4.1)
├── transaction-service/      # Chuyển tiền, gọi account-service qua OpenFeign (Spring Boot 4.0)
├── api-gateway/              # Single Point of Entry     (Spring Cloud Gateway)
├── postman/                  # Postman collections
├── screenshots/              # Ảnh minh chứng từng bài
└── docker-compose-bai1..5.yml  # Các bước trung gian của bài tập
```

Mỗi service là một project Gradle độc lập, có `Dockerfile` multi-stage riêng. Docker tự build file JAR bên trong image, nên máy chạy **không cần cài JDK hay Gradle**.

## Khởi chạy

Yêu cầu: Docker Desktop đang chạy. Trên Windows, chạy lệnh trong **Git Bash** hoặc WSL.

```bash
cp .env.example .env      # lần đầu: tạo file .env rồi điền giá trị thật
chmod +x run.sh
./run.sh
```

`run.sh` sẽ:
1. Dọn dẹp container cũ (`docker compose down`). Dữ liệu trong volume `pg-data` **được giữ lại**.
2. Build image mới nhất và chạy ngầm (`docker compose up -d --build`).
3. Chờ và in ra thứ tự từng service đạt trạng thái `healthy`. Script báo lỗi nếu có service bị restart hoặc unhealthy.

## Thứ tự khởi động

Cấu hình bằng `depends_on` + `condition: service_healthy`:

```
core-db (pg_isready)
   └─► account-service + transaction-service (/actuator/health = UP)
          └─► api-gateway
```

## Bảo mật port

| Service             | Port trong mạng nội bộ | Port mở ra host |
|---------------------|------------------------|-----------------|
| api-gateway         | 8080                   | **80**          |
| account-service     | 8080                   | không có        |
| transaction-service | 8080                   | không có        |
| core-db             | 5432                   | không có        |

## API (gọi qua Gateway)

| Method | URL                                          | Mô tả                         |
|--------|----------------------------------------------|-------------------------------|
| POST   | `/api/v1/accounts`                           | Tạo tài khoản                 |
| GET    | `/api/v1/accounts`                           | Danh sách tài khoản           |
| GET    | `/api/v1/accounts/{id}`                      | Tài khoản theo id             |
| GET    | `/api/v1/accounts/number/{accountNumber}`    | Tài khoản theo số tài khoản   |
| POST   | `/api/v1/transactions/transfer`              | Chuyển tiền                   |
| GET    | `/api/v1/transactions`                       | Lịch sử giao dịch             |
| GET    | `/api/v1/transactions/{id}`                  | Giao dịch theo id             |

Collection Postman: `postman/Banking-API-Gateway.postman_collection.json`.

## Vận hành

```bash
docker compose ps                  # trạng thái các service
docker compose logs -f api-gateway # xem log
docker compose down                # tắt hệ thống (giữ dữ liệu)
docker compose down -v             # ⚠️ tắt VÀ XÓA toàn bộ dữ liệu
```

Mọi response đều có header `X-Request-Id`. Dùng mã này để tra log của một request khi nó đi qua cả 3 service:
`docker logs transaction-service 2>&1 | grep "rid=<mã>"`.
