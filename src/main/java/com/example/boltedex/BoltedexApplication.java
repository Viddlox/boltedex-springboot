package com.example.boltedex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
public class BoltedexApplication {

	public static void main(String[] args) {
		SpringApplication.run(BoltedexApplication.class, args);
	}

}
