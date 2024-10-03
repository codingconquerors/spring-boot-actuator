package com.springboot.actuator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ActuatorApplication {

	public static void main(String[] args) throws InterruptedException {
		Thread.sleep(20000);
		SpringApplication.run(ActuatorApplication.class, args);
	}

}
