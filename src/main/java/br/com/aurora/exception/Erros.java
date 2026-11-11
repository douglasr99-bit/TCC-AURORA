package br.com.aurora.exception;

import java.util.ArrayList;
import java.util.List;

/** Acumula erros de vários campos para devolvê-los de uma vez. */
public class Erros {

    private final List<ErroCampo> lista = new ArrayList<>();

    public void add(String campo, String mensagem) {
        lista.add(new ErroCampo(campo, mensagem));
    }

    public boolean temErroEm(String campo) {
        return lista.stream().anyMatch(e -> campo.equals(e.campo()));
    }

    public boolean isEmpty() {
        return lista.isEmpty();
    }

    /** Lança {@link NegocioException} se houver algum erro. */
    public void lancarSeHouver() {
        if (!lista.isEmpty()) {
            throw new NegocioException(lista);
        }
    }
}
