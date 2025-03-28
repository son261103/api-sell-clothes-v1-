package com.example.api_sell_clothes_v1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ApiSellClothesV1Application {

	public static void main(String[] args) {
		SpringApplication.run(ApiSellClothesV1Application.class, args);
	}

}
