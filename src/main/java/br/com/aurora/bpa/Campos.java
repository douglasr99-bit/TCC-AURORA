package br.com.aurora.bpa;

import java.text.Normalizer;

/**
 * Preenchimento dos campos de largura fixa do layout de exportação do BPA (DATASUS):
 * NUM com zeros à esquerda, ALFA com espaços à direita, campos vazios com brancos.
 * Nunca trunca: valor maior que o campo é erro de programação (os tamanhos são
 * garantidos na validação do cadastro e do lançamento).
 */
final class Campos {

    private Campos() {
    }

    /** NUM obrigatório: zeros à esquerda até completar o tamanho. */
    static String num(long valor, int tamanho, String campo) {
        return num(Long.toString(valor), tamanho, campo);
    }

    static String num(String digitos, int tamanho, String campo) {
        if (digitos == null || !digitos.matches("\\d+")) {
            throw new IllegalArgumentException("Campo " + campo + " deve ser numérico: " + digitos);
        }
        exigirTamanho(digitos, tamanho, campo);
        return "0".repeat(tamanho - digitos.length()) + digitos;
    }

    /** NUM opcional: se vazio, brancos; se preenchido, zeros à esquerda. */
    static String numOuBrancos(String digitos, int tamanho, String campo) {
        return digitos == null || digitos.isBlank() ? brancos(tamanho) : num(digitos.strip(), tamanho, campo);
    }

    /** ALFA: texto em ASCII, espaços à direita até completar o tamanho. */
    static String alfa(String texto, int tamanho, String campo) {
        String t = texto == null ? "" : ascii(texto);
        exigirTamanho(t, tamanho, campo);
        return t + " ".repeat(tamanho - t.length());
    }

    static String brancos(int tamanho) {
        return " ".repeat(tamanho);
    }

    /**
     * Converte para o padrão ASCII do arquivo: maiúsculas, sem acentos (Ç → C) e sem
     * caracteres fora da faixa imprimível, que viram espaço.
     */
    static String ascii(String texto) {
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        StringBuilder sb = new StringBuilder(semAcento.length());
        for (char c : semAcento.toUpperCase().toCharArray()) {
            sb.append(c >= 32 && c <= 126 ? c : ' ');
        }
        return sb.toString().strip();
    }

    private static void exigirTamanho(String valor, int tamanho, String campo) {
        if (valor.length() > tamanho) {
            throw new IllegalArgumentException("Campo " + campo + " passa de " + tamanho + " posições: " + valor.length());
        }
    }
}
