package br.com.aurora.exception;

import java.util.List;

/** Corpo das respostas de erro: {@code {"erros": [{"campo": ..., "mensagem": ...}]}}. */
public record RespostaErro(List<ErroCampo> erros) {

    public static RespostaErro de(String campo, String mensagem) {
        return new RespostaErro(List.of(new ErroCampo(campo, mensagem)));
    }
}
