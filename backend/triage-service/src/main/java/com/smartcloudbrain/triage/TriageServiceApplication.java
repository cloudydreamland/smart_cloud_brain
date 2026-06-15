package com.smartcloudbrain.triage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication(scanBasePackages = "com.smartcloudbrain")
public class TriageServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(TriageServiceApplication.class, args);
  }
}

