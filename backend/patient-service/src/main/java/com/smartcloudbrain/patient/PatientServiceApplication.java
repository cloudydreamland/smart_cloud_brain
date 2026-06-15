package com.smartcloudbrain.patient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.smartcloudbrain")
public class PatientServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(PatientServiceApplication.class, args);
  }
}

