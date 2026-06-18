package com.smartcloudbrain.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.smartcloudbrain")
public class AiServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(AiServiceApplication.class, args);
  }
}
