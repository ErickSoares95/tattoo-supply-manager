package com.ericksoares.tattoo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class TattooSupplyManagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(TattooSupplyManagerApplication.class, args);
	}

}
