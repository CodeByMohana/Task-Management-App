package com.app.taskmanagement.label;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class LabelServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(LabelServiceApplication.class, args);
	}

}
