package com.blockverse.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class BlockVerseApplication {

	public static void main(String[] args) {
		SpringApplication.run(BlockVerseApplication.class, args);
	}

}
