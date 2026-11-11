package br.com.aurora.dto;

import java.time.LocalDateTime;

public record ReaberturaDto(LocalDateTime data, String motivo, String usuario) {
}
