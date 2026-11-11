package br.com.aurora.validacao;

public final class Texto {

    private Texto() {
    }

    /** Remove espaços das pontas; texto vazio vira nulo. */
    public static String limpo(String valor) {
        if (valor == null) {
            return null;
        }
        String t = valor.strip();
        return t.isEmpty() ? null : t;
    }
}
