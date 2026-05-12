# C4 Model of the Current Architecture

## Context Diagram

![alt text](/assets/SystemContext.png)

## Container Diagram

![alt text](/assets/ContainerDiagram.png)

## Deployment Diagram

![alt text](/assets/DeploymentDiagram.png)

## Risk Analysis & Architecture Modification

![]()

# Individu (Jaysen Lestari)

## Code Diagram

![]()

## Profiling

![]()

![]()

## Monitoring

![]()

![]()

# BE-Wallet-Transaksi Service

# Deployment

Link: []()

## Overview
- User dapat membuat wallet berdasarkan `userId`.
- User dapat melihat saldo wallet dan riwayat transaksi.
- User dapat melakukan top-up, pay, refund, dan withdraw sesuai policy akses.
- Service menerima callback payment gateway untuk settlement/failed transaction.
- Service menyediakan contract API untuk integrasi modul Order.

Komponen wallet bertanggung jawab untuk menjaga konsistensi saldo, histori transaksi, dan integrasi status pembayaran lintas service.

## Key Features
- Wallet creation dan balance tracking.
- Transaction mutation: `TOPUP`, `PAYMENT`, `REFUND`, `WITHDRAW`.
- Payment callback handling dengan signature verification.
- Contract endpoint untuk `check-balance`, `deduct`, dan `refund`.
- gRPC contract server untuk integrasi internal.

## Implemented Design Patterns

### 1. Strategy Pattern
- `WalletMutationStrategy` sebagai interface mutasi saldo.
- Implementasi strategy per tipe transaksi untuk memastikan logika debit/kredit terpisah dan mudah diuji.
- `WalletMutationStrategyResolver` memilih strategy berdasarkan `TransactionType`.

### 2. State Pattern
- Entitas `Transaction` menerapkan validasi transisi status melalui state abstraction.
- Mencegah transisi status invalid saat callback dan update lifecycle transaksi.

### 3. Factory Pattern
- Factory digunakan pada layer state transaction untuk memetakan enum status ke behavior state yang sesuai.

## Project Structure (for now)

### Controller Layer
- `WalletController.java`: Endpoint wallet utama.
- `WalletContractController.java`: Endpoint contract untuk integrasi order.
- `GlobalExceptionHandler.java`: Standardisasi respons error API.

### Service Layer
- `WalletService.java`: Kontrak business logic wallet.
- `WalletServiceImpl.java`: Implementasi mutasi saldo, callback transition, dan transaction orchestration.
- `PaymentGatewayClient` + publisher interfaces untuk integrasi eksternal.

### Repository Layer
- `WalletRepository.java`: Operasi persistence wallet.
- `TransactionRepository.java`: Operasi persistence transaksi.

### Model Layer
- `Wallet.java`: Entitas wallet.
- `Transaction.java`: Entitas transaksi.
- `TransactionState` + concrete states: Lifecycle status transaksi.

### gRPC Layer
- `WalletContractGrpcService.java`: Implementasi gRPC untuk contract wallet.
- `GrpcInternalAuthInterceptor.java`: Validasi token internal gRPC.

### Config & Infra
- `application.properties`, `application-prod.properties`: Konfigurasi environment.
- `docker-compose.yml`, `Dockerfile`: Local/dev runtime.
- `.github/workflows/ci-cd.yml`: Pipeline test, scan, build, deploy.

## API Documentation

Saat aplikasi running:

- Swagger UI: `http://localhost:6002/swagger-ui/index.html` (dev profile compose)
- OpenAPI JSON: `http://localhost:6002/v3/api-docs`

Format `Authorize`:

```text
Bearer <JWT_TOKEN>
```

## Run (Local)

Jalankan profile `dev`:

```powershell
docker compose --profile dev up --build
```

Jalankan profile `main`:

```powershell
docker compose --profile main up --build
```