package com.smartcloudbrain.medicalrecord;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication(scanBasePackages = "com.smartcloudbrain")
public class MedicalRecordServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(MedicalRecordServiceApplication.class, args);
  }
}

