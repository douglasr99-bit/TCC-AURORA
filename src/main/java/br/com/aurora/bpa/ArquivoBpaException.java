package br.com.aurora.bpa;

/** A produção não cabe nas regras do layout (ex.: mais de 999 folhas). */
public class ArquivoBpaException extends RuntimeException {

    public ArquivoBpaException(String mensagem) {
        super(mensagem);
    }
}
