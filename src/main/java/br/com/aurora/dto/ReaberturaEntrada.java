package br.com.aurora.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReaberturaEntrada(
        @NotBlank(message = "Informe o motivo da reabertura (ex.: correção de lançamentos glosados).")
        @Size(max = 255, message = "O motivo pode ter no máximo 255 caracteres.")
        String motivo) {
}
