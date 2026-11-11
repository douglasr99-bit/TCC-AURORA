package br.com.aurora.validacao;

/**
 * Dígito verificador do código de procedimento da Tabela SUS (SIGTAP), 10 dígitos.
 * DV = (d1×1 + d2×2 + ... + d9×9) mod 11; resto 10 vira 0.
 * Regra verificada contra os 5.024 procedimentos da Tabela Unificada do SIGTAP,
 * competência 09/2026 (ver docs/registro-implementacao.md).
 */
public final class ValidadorProcedimento {

    private ValidadorProcedimento() {
    }

    /** Calcula o dígito verificador a partir dos 9 primeiros dígitos. */
    public static int digitoVerificador(String noveDigitos) {
        if (noveDigitos == null || !noveDigitos.matches("\\d{9}")) {
            throw new IllegalArgumentException("Informe exatamente 9 dígitos");
        }
        int soma = 0;
        for (int i = 0; i < 9; i++) {
            soma += (noveDigitos.charAt(i) - '0') * (i + 1);
        }
        int resto = soma % 11;
        return resto == 10 ? 0 : resto;
    }

    /** Verdadeiro se o código tem 10 dígitos e o último é o dígito verificador correto. */
    public static boolean valido(String codigo) {
        if (codigo == null || !codigo.matches("\\d{10}")) {
            return false;
        }
        return digitoVerificador(codigo.substring(0, 9)) == codigo.charAt(9) - '0';
    }
}
