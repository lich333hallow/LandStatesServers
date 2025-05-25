package ru.lich333hallow.LandStatesServer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
public class LandStatesServerApplication {
	public static void main(String[] args) {
		SpringApplication.run(LandStatesServerApplication.class, args);
		System.out.println("Server started on 8080");
	}
}
