package br.com.aurora.bpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.aurora.domain.Atendimento;
import br.com.aurora.domain.Estabelecimento;
import br.com.aurora.domain.Modalidade;
import br.com.aurora.domain.Paciente;
import br.com.aurora.domain.Procedimento;
import br.com.aurora.domain.Profissional;
import br.com.aurora.domain.Sexo;

/**
 * Arquivo BPA conferido campo a campo contra docs/Layout_Exportacao_BPA.pdf.
 * As posições dos testes seguem a numeração do layout (início 1); só dados fictícios.
 */
class GeradorArquivoBpaTest {

    private static final YearMonth NOV = YearMonth.of(2026, 11);

    /** Trecho [ini..fim] na numeração do layout (1 = primeira posição). */
    private static String pos(String linha, int ini, int fim) {
        return linha.substring(ini - 1, fim);
    }

    private static Estabelecimento estabelecimento() {
        Estabelecimento e = new Estabelecimento();
        e.setCnes("1234567");
        e.setNome("UBS São João");
        e.setSigla("UBSSJ");
        e.setCnpj("12345678000199");
        e.setOrgaoDestino("Secretaria Municipal de Saúde");
        e.setIndicadorDestino('M');
        return e;
    }

    private static Profissional profissional(String nome, String cns, String cbo) {
        Profissional p = new Profissional();
        p.setNome(nome); p.setCns(cns); p.setCbo(cbo);
        return p;
    }

    private static Procedimento procedimento(String codigo, Modalidade m) {
        Procedimento p = new Procedimento();
        p.setCodigo(codigo); p.setDescricao("PROC " + codigo); p.setModalidade(m);
        return p;
    }

    private static Atendimento bpaC(Profissional pr, Procedimento proc, int idade, int qt) {
        Atendimento a = new Atendimento();
        a.setProfissional(pr); a.setProcedimento(proc); a.setData(LocalDate.of(2026, 11, 5));
        a.setIdade(idade); a.setQuantidade(qt);
        return a;
    }

    private static Paciente paciente() {
        Paciente p = new Paciente();
        p.setCns("800000000000001");
        p.setNome("José da Conceição");
        p.setDataNascimento(LocalDate.of(1980, 6, 15));
        p.setSexo(Sexo.M);
        p.setRacaCor("03");
        p.setNacionalidade("010");
        p.setCodigoMunicipio("355030");
        p.setCep("01001000");
        p.setLogradouro("Rua das Flores");
        p.setBairro("Centro");
        p.setTelefone("1133334444");
        return p;
    }

    private static Atendimento bpaI(Profissional pr, Procedimento proc, Paciente pac, LocalDate data) {
        Atendimento a = new Atendimento();
        a.setProfissional(pr); a.setProcedimento(proc); a.setPaciente(pac); a.setData(data);
        a.setIdade(46); a.setQuantidade(1); a.setCid("J069"); a.setCaraterAtendimento("01");
        return a;
    }

    private static List<String> linhas(ArquivoBpa arquivo) {
        assertThat(arquivo.conteudo()).endsWith("\r\n");
        return List.of(arquivo.conteudo().split("\r\n"));
    }

    @Test
    @DisplayName("T13a Cada linha tem o tamanho exato do layout e termina em CR LF; arquivo só com ASCII")
    void tamanhosEAscii() {
        var ana = profissional("Ana", "100000000000007", "225142");
        var arquivo = GeradorArquivoBpa.gerar(estabelecimento(), NOV, List.of(
                bpaC(ana, procedimento("0101010010", Modalidade.BPA_C), 30, 2),
                bpaI(ana, procedimento("0301010072", Modalidade.BPA_I), paciente(), LocalDate.of(2026, 11, 10))));

        List<String> l = linhas(arquivo);
        assertThat(l).hasSize(3);
        assertThat(l.get(0)).hasSize(Cabecalho.TAMANHO);
        assertThat(l.get(1)).hasSize(RegistroBpaC.TAMANHO);
        assertThat(l.get(2)).hasSize(RegistroBpaI.TAMANHO);
        assertThat(arquivo.conteudo().split("\r\n", -1)).hasSize(4);           // nenhum LF solto
        byte[] bytes = arquivo.conteudo().getBytes(StandardCharsets.UTF_8);
        for (byte b : bytes) {
            assertThat(b & 0xFF).as("caractere fora do ASCII").isLessThan(128);
        }
    }

    @Test
    @DisplayName("T13b Cabeçalho com os dados do estabelecimento, totais e campo de controle")
    void cabecalho() {
        var ana = profissional("Ana", "100000000000007", "225142");
        var proc = procedimento("0101010010", Modalidade.BPA_C);
        var arquivo = GeradorArquivoBpa.gerar(estabelecimento(), NOV, List.of(bpaC(ana, proc, 30, 2), bpaC(ana, proc, 40, 3)));
        String h = linhas(arquivo).get(0);

        assertThat(pos(h, 1, 2)).isEqualTo("01");
        assertThat(pos(h, 3, 7)).isEqualTo("#BPA#");
        assertThat(pos(h, 8, 13)).isEqualTo("202611");
        assertThat(pos(h, 14, 19)).isEqualTo("000002");                         // 2 linhas
        assertThat(pos(h, 20, 25)).isEqualTo("000001");                         // 1 folha
        // controle = (101010010 + 2 + 101010010 + 3) mod 1111 + 1111
        long soma = 101010010L + 2 + 101010010L + 3;
        assertThat(pos(h, 26, 29)).isEqualTo(String.valueOf(soma % 1111 + 1111));
        assertThat(pos(h, 30, 59)).isEqualTo(String.format("%-30s", "UBS SAO JOAO"));
        assertThat(pos(h, 60, 65)).isEqualTo("UBSSJ ");
        assertThat(pos(h, 66, 79)).isEqualTo("12345678000199");
        assertThat(pos(h, 80, 119)).isEqualTo(String.format("%-40s", "SECRETARIA MUNICIPAL DE SAUDE"));
        assertThat(pos(h, 120, 120)).isEqualTo("M");
        assertThat(pos(h, 121, 130)).isEqualTo("AURORA 1.0");
    }

    @Test
    @DisplayName("Campo de controle fica no domínio [1111..2221]")
    void dominioDoControle() {
        assertThat(GeradorArquivoBpa.campoDeControle(0)).isEqualTo(1111);
        assertThat(GeradorArquivoBpa.campoDeControle(1110)).isEqualTo(2221);
        assertThat(GeradorArquivoBpa.campoDeControle(1111)).isEqualTo(1111);
    }

    @Test
    @DisplayName("T13c BPA-C consolida por CBO, procedimento e idade somando as quantidades")
    void consolidacaoBpaC() {
        var ana = profissional("Ana", "100000000000007", "225142");
        var bia = profissional("Bia", "100000000080000", "225142");     // mesmo CBO, outra pessoa
        var proc = procedimento("0101010010", Modalidade.BPA_C);
        var arquivo = GeradorArquivoBpa.gerar(estabelecimento(), NOV, List.of(
                bpaC(ana, proc, 30, 2), bpaC(bia, proc, 30, 5),            // mesma chave → 7
                bpaC(ana, proc, 31, 1)));                                   // idade diferente → outra linha
        List<String> l = linhas(arquivo);
        assertThat(arquivo.registrosBpaC()).isEqualTo(2);

        String r = l.get(1);
        assertThat(pos(r, 1, 2)).isEqualTo("02");
        assertThat(pos(r, 3, 9)).isEqualTo("1234567");
        assertThat(pos(r, 10, 15)).isEqualTo("202611");
        assertThat(pos(r, 16, 21)).isEqualTo("225142");
        assertThat(pos(r, 22, 24)).isEqualTo("001");
        assertThat(pos(r, 25, 26)).isEqualTo("01");
        assertThat(pos(r, 27, 36)).isEqualTo("0101010010");
        assertThat(pos(r, 37, 39)).isEqualTo("030");
        assertThat(pos(r, 40, 45)).isEqualTo("000007");
        assertThat(pos(r, 46, 48)).isEqualTo("BPA");
        assertThat(pos(l.get(2), 25, 26)).isEqualTo("02");
        assertThat(pos(l.get(2), 37, 39)).isEqualTo("031");
    }

    @Test
    @DisplayName("BPA-C: 21 linhas consolidadas ocupam 2 folhas (20 por folha)")
    void folhasBpaC() {
        var ana = profissional("Ana", "100000000000007", "225142");
        var proc = procedimento("0101010010", Modalidade.BPA_C);
        List<Atendimento> lista = new ArrayList<>();
        for (int idade = 0; idade < 21; idade++) lista.add(bpaC(ana, proc, idade, 1));
        var arquivo = GeradorArquivoBpa.gerar(estabelecimento(), NOV, lista);
        List<String> l = linhas(arquivo);
        assertThat(pos(l.get(20), 22, 26)).isEqualTo("00120");    // folha 1, linha 20
        assertThat(pos(l.get(21), 22, 26)).isEqualTo("00201");    // folha 2, linha 1
        assertThat(pos(l.get(0), 20, 25)).isEqualTo("000002");
    }

    @Test
    @DisplayName("T13d BPA-I campo a campo; textos em maiúsculas sem acento; número vazio vira SN")
    void registroBpaI() {
        var ana = profissional("Ana", "100000000000007", "225142");
        var arquivo = GeradorArquivoBpa.gerar(estabelecimento(), NOV, List.of(
                bpaI(ana, procedimento("0301010072", Modalidade.BPA_I), paciente(), LocalDate.of(2026, 11, 10))));
        String r = linhas(arquivo).get(1);

        assertThat(pos(r, 1, 2)).isEqualTo("03");
        assertThat(pos(r, 3, 9)).isEqualTo("1234567");
        assertThat(pos(r, 10, 15)).isEqualTo("202611");
        assertThat(pos(r, 16, 30)).isEqualTo("100000000000007");
        assertThat(pos(r, 31, 36)).isEqualTo("225142");
        assertThat(pos(r, 37, 44)).isEqualTo("20261110");
        assertThat(pos(r, 45, 47)).isEqualTo("001");
        assertThat(pos(r, 48, 49)).isEqualTo("01");
        assertThat(pos(r, 50, 59)).isEqualTo("0301010072");
        assertThat(pos(r, 60, 74)).isEqualTo("800000000000001");
        assertThat(pos(r, 75, 75)).isEqualTo("M");
        assertThat(pos(r, 76, 81)).isEqualTo("355030");
        assertThat(pos(r, 82, 85)).isEqualTo("J069");
        assertThat(pos(r, 86, 88)).isEqualTo("046");
        assertThat(pos(r, 89, 94)).isEqualTo("000001");
        assertThat(pos(r, 95, 96)).isEqualTo("01");
        assertThat(pos(r, 97, 109)).isBlank();
        assertThat(pos(r, 110, 112)).isEqualTo("BPA");
        assertThat(pos(r, 113, 142)).isEqualTo(String.format("%-30s", "JOSE DA CONCEICAO"));
        assertThat(pos(r, 143, 150)).isEqualTo("19800615");
        assertThat(pos(r, 151, 152)).isEqualTo("03");
        assertThat(pos(r, 153, 156)).isBlank();                    // etnia: só para indígena
        assertThat(pos(r, 157, 159)).isEqualTo("010");
        assertThat(pos(r, 160, 191)).isBlank();                    // serviço, classificação, equipe, CNPJ OPM
        assertThat(pos(r, 192, 199)).isEqualTo("01001000");
        assertThat(pos(r, 200, 202)).isBlank();
        assertThat(pos(r, 203, 232)).isEqualTo(String.format("%-30s", "RUA DAS FLORES"));
        assertThat(pos(r, 233, 242)).isBlank();
        assertThat(pos(r, 243, 247)).isEqualTo("SN   ");
        assertThat(pos(r, 248, 277)).isEqualTo(String.format("%-30s", "CENTRO"));
        assertThat(pos(r, 278, 288)).isEqualTo("1133334444 ");
        assertThat(pos(r, 289, 351)).isBlank();                    // e-mail, INE, CPF, situação de rua, sem CPF
    }

    @Test
    @DisplayName("BPA-I: folhas por profissional e 99 linhas por folha")
    void folhasBpaI() {
        var ana = profissional("Ana", "100000000000007", "225142");
        var bia = profissional("Bia", "100000000080000", "223505");
        var proc = procedimento("0301010072", Modalidade.BPA_I);
        List<Atendimento> lista = new ArrayList<>();
        for (int i = 0; i < 100; i++) lista.add(bpaI(ana, proc, paciente(), LocalDate.of(2026, 11, 1 + i % 28)));
        lista.add(bpaI(bia, proc, paciente(), LocalDate.of(2026, 11, 3)));
        var arquivo = GeradorArquivoBpa.gerar(estabelecimento(), NOV, lista);
        List<String> l = linhas(arquivo);

        assertThat(pos(l.get(99), 45, 49)).isEqualTo("00199");     // Ana: folha 1, linha 99
        assertThat(pos(l.get(100), 45, 49)).isEqualTo("00201");    // Ana: folha 2, linha 1
        assertThat(pos(l.get(101), 16, 30)).isEqualTo("100000000080000");
        assertThat(pos(l.get(101), 45, 49)).isEqualTo("00301");    // Bia começa folha nova
        assertThat(pos(l.get(0), 14, 19)).isEqualTo("000101");
        assertThat(pos(l.get(0), 20, 25)).isEqualTo("000003");
    }

    @Test
    @DisplayName("Folhas numeradas em sequência no arquivo: BPA-I continua depois do BPA-C")
    void folhasContinuas() {
        var ana = profissional("Ana", "100000000000007", "225142");
        var arquivo = GeradorArquivoBpa.gerar(estabelecimento(), NOV, List.of(
                bpaC(ana, procedimento("0101010010", Modalidade.BPA_C), 30, 2),
                bpaI(ana, procedimento("0301010072", Modalidade.BPA_I), paciente(), LocalDate.of(2026, 11, 10))));
        assertThat(pos(linhas(arquivo).get(2), 45, 47)).isEqualTo("002");
    }

    @Test
    @DisplayName("Paciente indígena: etnia no registro (inclusive código X999 da tabela do SIA)")
    void etnia() {
        Paciente p = paciente();
        p.setRacaCor("05");
        p.setEtnia("X411");
        var arquivo = GeradorArquivoBpa.gerar(estabelecimento(), NOV, List.of(
                bpaI(profissional("Ana", "100000000000007", "225142"),
                        procedimento("0301010072", Modalidade.BPA_I), p, LocalDate.of(2026, 11, 10))));
        String r = linhas(arquivo).get(1);
        assertThat(pos(r, 151, 152)).isEqualTo("05");
        assertThat(pos(r, 153, 156)).isEqualTo("X411");
    }

    @Test
    @DisplayName("Mais de 999 folhas → recusa com mensagem (limite do layout)")
    void limiteDeFolhas() {
        var proc = procedimento("0301010072", Modalidade.BPA_I);
        List<Atendimento> lista = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            String cns = String.format("%015d", i);   // um profissional por folha (CNS não validado aqui)
            lista.add(bpaI(profissional("P" + i, cns, "225142"), proc, paciente(), LocalDate.of(2026, 11, 10)));
        }
        assertThrows(ArquivoBpaException.class, () -> GeradorArquivoBpa.gerar(estabelecimento(), NOV, lista));
    }

    @Test
    @DisplayName("Texto maior que o campo nunca é truncado em silêncio")
    void naoTrunca() {
        Estabelecimento e = estabelecimento();
        e.setNome("NOME COM MAIS DE TRINTA CARACTERES XX");
        var ana = profissional("Ana", "100000000000007", "225142");
        assertThrows(IllegalArgumentException.class, () -> GeradorArquivoBpa.gerar(e, NOV,
                List.of(bpaC(ana, procedimento("0101010010", Modalidade.BPA_C), 30, 1))));
    }
}
