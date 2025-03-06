package br.com.naheroback;

import org.springframework.boot.SpringApplication;

public class TestNaheroBackApplication {

	public static void main(String[] args) {
		SpringApplication.from(NaheroBackApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
