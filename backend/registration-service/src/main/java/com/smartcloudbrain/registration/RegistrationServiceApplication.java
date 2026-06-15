package com.smartcloudbrain.registration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.smartcloudbrain")
public class RegistrationServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(RegistrationServiceApplication.class, args);
  }
}

