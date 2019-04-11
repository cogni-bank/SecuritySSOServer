package com.cognibank.securityMicroservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;

@SpringBootApplication()
@EnableJdbcHttpSession
public class SecurityMicroserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SecurityMicroserviceApplication.class, args);
	}

}
