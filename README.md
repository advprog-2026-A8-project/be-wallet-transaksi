# C4 Model of the Current Architecture

## Context Diagram

![alt text](/assets/SystemContext.png)

## Container Diagram

![alt text](/assets/ContainerDiagram.png)

## Deployment Diagram

![alt text](/assets/DeploymentDiagram.png)

## Risk Analysis & Architecture Modification

![alt text](/assets/RiskStormingMatrix.png)
![alt text](/assets/ModifiedArchitectureDiagram.png)

### Refleksi Risk Storming

Risk Storming diterapkan karena arsitektur saat ini tidak lagi cukup dinilai hanya dari sisi kelengkapan fitur, tetapi juga harus dilihat dari sisi risiko operasional jangka panjang. Ketika JSON berkembang dan digunakan dalam skala yang lebih besar, perhatian utama arsitektur bergeser ke aspek skalabilitas, konsistensi data, ketahanan sistem, dan observability pada banyak service yang saling terhubung.

Teknik ini membantu tim mengidentifikasi risiko secara sistematis berdasarkan kemungkinan terjadinya dan besar dampaknya, bukan hanya berdasarkan intuisi. Dengan memetakan risiko seperti overselling saat war, inkonsistensi antarservice, bottleneck pada Order Service, dan keterbatasan observability ke dalam matriks likelihood-impact, tim dapat memprioritaskan masalah yang paling penting untuk keberlanjutan sistem.

Risk Storming juga bermanfaat karena menghubungkan diskusi arsitektur dengan keputusan desain yang konkret. Hasil akhirnya bukan hanya daftar risiko, tetapi juga usulan future architecture yang lebih jelas, termasuk penambahan API Gateway, reservation cache untuk kontrol flash sale, event bus untuk menjaga konsistensi berbasis saga, serta dukungan observability dan audit yang lebih kuat.

# Individu (Jaysen Lestari)

## Component Diagram

![alt text](/assets/ComponentDiagram.png)

## Code Diagram

![alt text](/assets/CodeDiagram.png)

## Profiling

### Before Profiling
![alt text](/assets/Jmeter.png)

![alt text](/assets/Jprofiler.png)

![alt text](/assets/Jprofiler1.png)

![alt text](/assets/Profiling.png)

![alt text](/assets/Profiling1.png)

![alt text](/assets/Profiling2.png)

![alt text](/assets/Profiling3.png)

Kita dapat mengidentifikasi adanya performa yang kurang optimal pada path deductBalanceForOrder dan refundBalanceForOrder. 

### After Profiling
![alt text](/assets/AfterProfiling.png)

![alt text](/assets/AfterProfiling2.png)

Profiling pada proyek ini menggunakan pendekatan **workload-driven profiling** dengan **JMeter sebagai sumber utama pengukuran**.  
Artinya, keputusan optimasi dilakukan berdasarkan metrik performa saat skenario beban dijalankan, bukan hanya inspeksi kode statis.

Justifikasi metode profiling:
1. JMeter merepresentasikan trafik nyata (read + mutation + contract flow) sehingga bottleneck terlihat pada kondisi concurrent request.
2. Hasil numerik JMeter (latency, throughput, error rate, percentile) dipakai sebagai baseline sebelum optimasi dan pembanding setelah optimasi.
3. Monitoring (Prometheus/Grafana/Loki) digunakan sebagai pendukung observasi runtime untuk memvalidasi perilaku sistem saat test berjalan.

Dengan pendekatan ini, perubahan performa dapat dijustifikasi secara kuantitatif berdasarkan benchmark yang konsisten.

Langkah profiling yang dilakukan:
1. Refactor findTransactionByTypeAndOrderId(...), memakai query terfilter dan exists check
2. Refactor hasSuccessfulPaymentForOrder(), mengubah dari load list + scan ke boolean existence query langsung.
3. Refactor hasPendingPaymentDuplicate()
4. Menambahkan composite index di tabel transactions agar query yang paling sering dipakai saat load test tidak perlu scan/sort besar di database.

## Monitoring

![alt text](/assets/Prometheus.png)

![alt text](/assets/Grafana.png)

![alt text](/assets/Loki.png)

Stack monitoring dirancang agar mampu memantau kondisi sistem dari sisi infrastruktur dan alur bisnis transaksi secara bersamaan. Mekanisme monitoring yang digunakan:
1. **Metrics-based observability** dengan Spring Actuator + Micrometer (`/actuator/prometheus`) untuk mengekspos metrik aplikasi, JVM, HTTP, dan business flow dalam format standar Prometheus.
2. **Dashboard operasional Grafana** untuk memantau service health, request throughput, error rate, serta latency p95/p99 per endpoint secara real-time.
3. **Business metrics khusus domain wallet** (topup/pay/refund/callback/idempotency/grpc/publisher) agar analisis insiden bisa dilakukan sampai level transaksi bisnis.
4. **Log aggregation via Loki + Promtail** untuk mengkorelasikan anomali metrik dengan event log aplikasi secara terpusat.

# BE-Wallet-Transaksi Service

# Deployment

Link Service: `http://ec2-54-243-234-62.compute-1.amazonaws.com:6060`  
Link Grafana: `http://ec2-54-243-234-62.compute-1.amazonaws.com:3000`  
Link Prometheus: `http://ec2-54-243-234-62.compute-1.amazonaws.com:9090`

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
- `docker-compose.yml`, `Dockerfile`: Local runtime.
- `.github/workflows/ci-cd.yml`: Pipeline test, scan, build, deploy.

## API Documentation

Saat aplikasi running:

- Swagger UI: `http://localhost:6060/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:6060/v3/api-docs`

Format `Authorize`:

```text
Bearer <JWT_TOKEN>
```

## Run (Local)

Local run menggunakan profile `main`:

```powershell
docker compose --profile main up --build
```

## Monitoring

Observability stack ditujukan untuk environment `main` (production-like).

1. Jalankan aplikasi utama profile `main`:

```powershell
docker compose --profile main up -d --build
```

2. Jalankan Prometheus + Grafana:

```powershell
docker compose -f docker-compose.monitoring.yml up -d
```

3. Verifikasi endpoint:
- Wallet metrics: `http://localhost:6060/actuator/prometheus`
- Prometheus UI: `http://localhost:9090`
- Grafana UI: `http://localhost:3000`

4. Login Grafana:
- Username: `admin` (default)
- Password: `admin` (default)

5. Tambah data source Prometheus di Grafana:
- URL: `http://prometheus:9090`