package br.com.aurora.validacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ValidadorProcedimentoTest {

    @Test
    @DisplayName("T15b Todos os 5.024 procedimentos da Tabela Unificada do SIGTAP (09/2026) têm DV válido")
    void todosOsCodigosOficiais() throws IOException {
        List<String> codigos;
        try (var in = getClass().getResourceAsStream("/sigtap_202609_codigos_procedimento.txt")) {
            codigos = new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().filter(l -> !l.isBlank()).toList();
        }
        assertEquals(5024, codigos.size());
        List<String> invalidos = codigos.stream().filter(c -> !ValidadorProcedimento.valido(c)).toList();
        assertTrue(invalidos.isEmpty(), "Códigos oficiais recusados: " + invalidos);
    }

    @Test
    @DisplayName("Resto 10 vira DV 0: 0101010010 (soma 21, resto 10)")
    void restoDez() {
        assertEquals(0, ValidadorProcedimento.digitoVerificador("010101001"));
        assertTrue(ValidadorProcedimento.valido("0101010010"));
    }

    @Test
    @DisplayName("Consulta médica em atenção especializada: 0301010072")
    void exemploConhecido() {
        assertTrue(ValidadorProcedimento.valido("0301010072"));
    }

    @ParameterizedTest(name = "inválido: \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"0301010071", "0301010073", "030101007", "03010100722", "03.01.01.007-2", "030101007X"})
    void invalidos(String codigo) {
        assertFalse(ValidadorProcedimento.valido(codigo));
    }

    @Test
    @DisplayName("Trocar qualquer dígito de um código válido invalida o código (exceto quando o DV coincide)")
    void alteracaoDeUmDigito() {
        String valido = "0301010072";
        int detectados = 0;
        int total = 0;
        for (int pos = 0; pos < 9; pos++) {
            for (char d = '0'; d <= '9'; d++) {
                if (valido.charAt(pos) == d) continue;
                String alterado = valido.substring(0, pos) + d + valido.substring(pos + 1);
                total++;
                if (!ValidadorProcedimento.valido(alterado)) detectados++;
            }
        }
        // Com pesos 1..9 e módulo 11, toda troca de um único dígito é detectada.
        assertEquals(total, detectados);
    }
}
