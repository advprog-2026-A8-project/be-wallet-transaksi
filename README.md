# BE Wallet Transaksi
PIC : Jaysen Lestari - 2406395335  
Backend service untuk manajemen wallet, top up, payment callback, dan contract API untuk integrasi antar-service.

## API Documentation

Saat aplikasi running:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

`Authorize` di Swagger memakai format:

```text
Bearer <JWT_TOKEN>
```

## Endpoint Summary

### Wallet API (`/wallet`)

- `GET /wallet/{userId}`
- `POST /wallet`
- `POST /wallet/topup`
- `POST /wallet/topup/initiate`
- `POST /wallet/pay`
- `POST /wallet/refund`
- `POST /wallet/payments/callback`
- `POST /wallet/withdraw`
- `GET /wallet/{userId}/transactions`

### Contract API (`/api/contracts/wallet`)

- `POST /api/contracts/wallet/check-balance`
- `POST /api/contracts/wallet/deduct`
- `POST /api/contracts/wallet/refund`

## Configuration

Copy `.env.example` ke `.env`, lalu isi value yang dibutuhkan.

Variabel utama:

- `POSTGRES_HOST`
- `POSTGRES_PORT`
- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `JWT_SECRET`
- `JWT_EXPIRATION`
- `MIDTRANS_SERVER_KEY`
- `MIDTRANS_CLIENT_KEY`
- `MIDTRANS_MERCHANT_ID`

## Run with Docker Compose

Menjalankan app + PostgreSQL untuk local testing yang disesuaikan dengan behavior production dengan memakai profile:

- `main` profile:
  - App: `6000`
  - Postgres: `6001`
- `dev` profile:
  - App: `6002`
  - Postgres: `6003`

Jalankan `dev`:

```powershell
docker compose --profile dev up --build
```

Jalankan `main`:

```powershell
docker compose --profile main up --build
```
