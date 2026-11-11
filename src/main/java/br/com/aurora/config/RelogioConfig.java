package br.com.aurora.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Relógio injetável: as regras de data ("não pode ser no futuro") ficam testáveis. */
@Configuration
public class RelogioConfig {

    @Bean
    Clock relogio() {
        return Clock.systemDefaultZone();
    }
}
