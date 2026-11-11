package br.com.aurora.bpa;

/**
 * Arquivo BPA montado.
 *
 * @param conteudo texto completo (ASCII, linhas terminadas em CR LF)
 * @param registros quantidade de linhas de produção (BPA-C + BPA-I), sem o cabeçalho
 * @param folhas quantidade de folhas
 * @param controle campo de controle do cabeçalho
 */
public record ArquivoBpa(String conteudo, int registros, int registrosBpaC, int registrosBpaI, int folhas, int controle) {
}
