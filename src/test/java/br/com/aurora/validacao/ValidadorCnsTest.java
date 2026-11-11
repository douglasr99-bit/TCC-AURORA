package br.com.aurora.validacao;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Validação do CNS (algoritmo da ANS). Números fictícios, montados à mão:
 * a soma usa os pesos 15, 14, ... sobre os dígitos.
 */
class ValidadorCnsTest {

    @Test
    @DisplayName("CNS definitivo: base 10000000000 → soma 15, resto 4, DV 7 → 100000000000007")
    void definitivoSimples() {
        assertTrue(ValidadorCns.valido("100000000000007"));
    }

    @Test
    @DisplayName("CNS definitivo com DV 11 → 0: base 10000000008 → 100000000080000")
    void definitivoDvOnze() {
        assertTrue(ValidadorCns.valido("100000000080000"));
    }

    @Test
    @DisplayName("CNS definitivo com DV 10: base 10000000006, soma 45+2=47, resto 3, DV 8 → ...001 8")
    void definitivoDvDez() {
        assertTrue(ValidadorCns.valido("100000000060018"));
        assertFalse(ValidadorCns.valido("100000000060000"), "com DV 10 o trecho deve ser 001");
    }

    @Test
    @DisplayName("CNS provisório: soma ponderada dos 15 dígitos múltipla de 11 (8×15 + 1×1 = 121)")
    void provisorio() {
        assertTrue(ValidadorCns.valido("800000000000001"));
    }

    @ParameterizedTest(name = "inválido: \"{0}\"")
    @DisplayName("T14 CNS com dígito verificador errado, tamanho errado ou letras é recusado")
    @NullAndEmptySource
    @ValueSource(strings = {
            "100000000000008",   // dígito final trocado
            "800000000000002",   // provisório com soma não múltipla de 11
            "000000000000000",   // todos zeros
            "10000000000000",    // 14 dígitos
            "1000000000000070",  // 16 dígitos
            "10000000000000A",   // letra
            "100 000000000007"   // espaço
    })
    void invalidos(String cns) {
        assertFalse(ValidadorCns.valido(cns));
    }
}
