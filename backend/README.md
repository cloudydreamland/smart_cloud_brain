# Backend Microservice Layout

The backend is a gateway-first Spring Cloud microservice system. Frontend traffic enters through `gateway-service` only; domain services receive trusted user context from the gateway through `X-User-Id`, `X-User-Role` and `X-User-Name`.

```text
backend/
  common-lib/             shared result, error code, JWT, user context, events and exceptions
  ai-api/                 service-to-service AI contract DTOs
  gateway-service/        frontend entry, JWT validation, CORS and route forwarding
  auth-service/           patient, doctor and admin login
  patient-service/        patient profile and patient center
  doctor-service/         departments and doctors
  registration-service/   registration creation, cancellation and status flow
  triage-service/         triage orchestration and records
  medical-record-service/ medical record generation and records
  prescription-service/   prescription, prescription items, AI check records and outbox events
  ai-service/             independent AI capability service, mock-provider ready
  notification-service/   RabbitMQ notification consumer, WebSocket push and notification history
  admin-service/          catalog maintenance and KingbaseES-backed search APIs
```

KingbaseES is the acceptance database and the only source of truth. RabbitMQ carries asynchronous prescription and notification events. Knowledge, drug and Prompt template search is served by `admin-service` directly from KingbaseES tables.
