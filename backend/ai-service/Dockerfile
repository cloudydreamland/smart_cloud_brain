FROM maven:3.9.10-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY backend/pom.xml .
COPY backend/common-lib/pom.xml common-lib/pom.xml
COPY backend/ai-api/pom.xml ai-api/pom.xml
COPY backend/gateway-service/pom.xml gateway-service/pom.xml
COPY backend/auth-service/pom.xml auth-service/pom.xml
COPY backend/patient-service/pom.xml patient-service/pom.xml
COPY backend/doctor-service/pom.xml doctor-service/pom.xml
COPY backend/registration-service/pom.xml registration-service/pom.xml
COPY backend/triage-service/pom.xml triage-service/pom.xml
COPY backend/medical-record-service/pom.xml medical-record-service/pom.xml
COPY backend/prescription-service/pom.xml prescription-service/pom.xml
COPY backend/ai-service/pom.xml ai-service/pom.xml
COPY backend/notification-service/pom.xml notification-service/pom.xml
COPY backend/admin-service/pom.xml admin-service/pom.xml
COPY backend/common-lib/src common-lib/src
COPY backend/ai-api/src ai-api/src
COPY backend/ai-service/src ai-service/src
RUN mvn -pl ai-service -am -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /workspace/ai-service/target/ai-service.jar /app/ai-service.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/ai-service.jar"]
