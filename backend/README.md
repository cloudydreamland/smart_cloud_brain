# Backend Pure Microservice Layout

```text
backend/
  common-lib/             shared result, error code, exception, role model
  ai-api/                 service-to-service AI contract DTOs and internal paths
  gateway-service/        Spring Cloud Gateway, routing, CORS, rate limiting
  auth-service/           login, JWT, role, token verification
  patient-service/        patient profile and patient center
  doctor-service/         doctors, departments, schedules and slots
  registration-service/   registration creation, cancellation and status flow
  triage-service/         triage orchestration, recommendation and records
  medical-record-service/ medical record generation orchestration and records
  prescription-service/   prescription, prescription items and check records
  ai-service/             independent AI capability service
    controller/           internal AI endpoints
    application/          AI orchestration and task logging
    provider/             model provider SPI
    provider/mock/        mock provider for demo and fallback
    service/              prompt template service
    config/               AI provider configuration
    exception/            AI service exception handling
  notification-service/   WebSocket notification and notification history
  admin-service/          admin console aggregation and operation orchestration
  diagnosis-service/      compatibility service for the current runnable demo
```

The backend documentation uses a pure microservice architecture as the single target topology. Frontend traffic enters only through `gateway-service`; domain services communicate through service discovery and OpenFeign; each service owns its own schema or database boundary.

`diagnosis-service` is retained as a compatibility module while the existing runnable code is split into the target domain services. New code should land in the domain service that owns the behavior.
