package com.nnp.kubernetes_integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class KubernetesIntegrationApplication {

	public static void main(String[] args) {
		SpringApplication.run(KubernetesIntegrationApplication.class, args);
	}

	// Build Trigger

}
