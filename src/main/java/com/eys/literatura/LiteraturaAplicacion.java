package com.eys.literatura;

import com.eys.literatura.principal.Principal;
import com.eys.literatura.service.ConsumoAPI;
import com.eys.literatura.service.ConvierteDatos;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;

@SpringBootApplication
public class LiteraturaAplicacion implements CommandLineRunner {

	private final Principal principal;

	// Constructor que inyecta `Principal` con `@Lazy`
	public LiteraturaAplicacion(@Lazy Principal principal) {
		this.principal = principal;
	}

	public static void main(String[] args) {
		SpringApplication.run(LiteraturaAplicacion.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		principal.muestraElMenu();
	}

	@Bean
	public ConsumoAPI consumoAPI() {
		return new ConsumoAPI();
	}

	@Bean
	public ConvierteDatos conversor() {
		return new ConvierteDatos();
	}

	public Principal getPrincipal() {
		return principal;
	}
}
