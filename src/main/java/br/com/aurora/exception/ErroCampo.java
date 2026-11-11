package br.com.aurora.exception;

/**
 * Um problema encontrado na requisição. {@code campo} é o nome do campo do formulário
 * (ou nulo quando o erro não é de um campo específico).
 */
public record ErroCampo(String campo, String mensagem) {
}
