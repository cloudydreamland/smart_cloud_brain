package com.smartcloudbrain.diagnosis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class DiagnosisServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(DiagnosisServiceApplication.class, args);
  }
}
