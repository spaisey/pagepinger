package uk.co.itello.glastonpinger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@SpringBootApplication
public class GlastonPingerApplication {

	public static void main(String[] args) {
		SpringApplication.run(GlastonPingerApplication.class, args);
	}

	@Bean
	public Executor executor() {
		return Executors.newSingleThreadExecutor();
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
}
