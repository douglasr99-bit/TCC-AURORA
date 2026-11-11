package br.com.aurora.exception;

import java.util.List;

/** Erro de validação ou de regra de negócio. Vira HTTP 400. */
public class NegocioException extends RuntimeException {

    private final List<ErroCampo> erros;

    public NegocioException(List<ErroCampo> erros) {
        super("Erro de validação");
        this.erros = List.copyOf(erros);
    }

    public NegocioException(String campo, String mensagem) {
        this(List.of(new ErroCampo(campo, mensagem)));
    }

    public List<ErroCampo> getErros() {
        return erros;
    }
}
