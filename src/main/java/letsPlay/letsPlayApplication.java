package letsPlay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class letsPlayApplication {
	public static void main(String[] args) {
		SpringApplication.run(letsPlayApplication.class, args);
	}

}