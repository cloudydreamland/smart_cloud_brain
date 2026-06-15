package com.smartcloudbrain.doctor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.smartcloudbrain")
public class DoctorServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(DoctorServiceApplication.class, args);
  }
}

