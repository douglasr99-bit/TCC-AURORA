package br.com.aurora.bpa;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import br.com.aurora.domain.Atendimento;
import br.com.aurora.domain.Estabelecimento;
import br.com.aurora.domain.Modalidade;
import br.com.aurora.domain.Paciente;
import br.com.aurora.domain.Profissional;

/**
 * Monta o arquivo de exportação do BPA a partir dos atendimentos de uma competência,
 * seguindo docs/Layout_Exportacao_BPA.pdf (DATASUS). Classe sem acesso a banco: recebe tudo
 * pronto e devolve o texto, para poder ser testada linha a linha.
 *
 * <ul>
 * <li>BPA-C: atendimentos consolidados por CBO, procedimento e idade (quantidades somadas),
 * 20 linhas por folha.</li>
 * <li>BPA-I: um registro por atendimento; folhas por profissional (CNS + CBO), 99 linhas por
 * folha (BPA Magnético, versão 02.68).</li>
 * <li>Folhas numeradas em sequência no arquivo inteiro (BPA-C primeiro), de 001 a 999.</li>
 * <li>Campo de controle: (soma dos códigos de procedimento + quantidades) mod 1111 + 1111.</li>
 * </ul>
 */
public final class GeradorArquivoBpa {

    public static final String VERSAO = "AURORA 1.0";
    static final String FIM_DE_LINHA = "\r\n";
    static final int MAX_FOLHAS = 999;
    static final int MAX_QUANTIDADE = 999_999;

    private static final DateTimeFormatter AAAAMM = DateTimeFormatter.ofPattern("yyyyMM");

    private GeradorArquivoBpa() {
    }

    public static ArquivoBpa gerar(Estabelecimento estabelecimento, YearMonth competencia, List<Atendimento> atendimentos) {
        String cmp = competencia.format(AAAAMM);
        String cnes = estabelecimento.getCnes();

        List<RegistroBpaC> bpaC = montarBpaC(cnes, cmp, atendimentos);
        int proximaFolha = bpaC.isEmpty() ? 1 : bpaC.get(bpaC.size() - 1).folha() + 1;
        List<RegistroBpaI> bpaI = montarBpaI(cnes, cmp, atendimentos, proximaFolha);

        int folhas = bpaI.isEmpty()
                ? (bpaC.isEmpty() ? 0 : bpaC.get(bpaC.size() - 1).folha())
                : bpaI.get(bpaI.size() - 1).folha();
        if (folhas > MAX_FOLHAS) {
            throw new ArquivoBpaException("A produção desta competência passa de " + MAX_FOLHAS
                    + " folhas, o limite do arquivo BPA.");
        }

        long soma = 0;
        for (RegistroBpaC r : bpaC) {
            soma += Long.parseLong(r.procedimento()) + r.quantidade();
        }
        for (RegistroBpaI r : bpaI) {
            soma += Long.parseLong(r.procedimento()) + r.quantidade();
        }
        int controle = campoDeControle(soma);

        Cabecalho cabecalho = new Cabecalho(cmp, bpaC.size() + bpaI.size(), folhas, controle,
                estabelecimento.getNome(), estabelecimento.getSigla(), estabelecimento.getCnpj(),
                estabelecimento.getOrgaoDestino(), estabelecimento.getIndicadorDestino(), VERSAO);

        StringBuilder sb = new StringBuilder((Cabecalho.TAMANHO + 2)
                + bpaC.size() * (RegistroBpaC.TAMANHO + 2) + bpaI.size() * (RegistroBpaI.TAMANHO + 2));
        sb.append(cabecalho.linha()).append(FIM_DE_LINHA);
        bpaC.forEach(r -> sb.append(r.linha()).append(FIM_DE_LINHA));
        bpaI.forEach(r -> sb.append(r.linha()).append(FIM_DE_LINHA));

        return new ArquivoBpa(sb.toString(), bpaC.size() + bpaI.size(), bpaC.size(), bpaI.size(), folhas, controle);
    }

    /** Observação do layout: soma dos códigos e quantidades, resto por 1111, mais 1111. */
    static int campoDeControle(long soma) {
        return (int) (soma % 1111) + 1111;
    }

    // ---------- BPA-C ----------

    private record ChaveC(String cbo, String procedimento, int idade) {
    }

    private static List<RegistroBpaC> montarBpaC(String cnes, String cmp, List<Atendimento> atendimentos) {
        Map<ChaveC, Long> consolidado = new TreeMap<>(Comparator.comparing(ChaveC::procedimento)
                .thenComparing(ChaveC::cbo).thenComparingInt(ChaveC::idade));
        for (Atendimento a : atendimentos) {
            if (a.getProcedimento().getModalidade() != Modalidade.BPA_C) continue;
            ChaveC chave = new ChaveC(a.getProfissional().getCbo(), a.getProcedimento().getCodigo(), a.getIdade());
            consolidado.merge(chave, (long) a.getQuantidade(), Long::sum);
        }

        List<RegistroBpaC> registros = new ArrayList<>();
        int indice = 0;
        for (var item : consolidado.entrySet()) {
            long restante = item.getValue();
            // Quantidade consolidada acima de 6 dígitos é dividida em mais de uma linha.
            while (restante > 0) {
                int qt = (int) Math.min(restante, MAX_QUANTIDADE);
                restante -= qt;
                int folha = indice / RegistroBpaC.LINHAS_POR_FOLHA + 1;
                int seq = indice % RegistroBpaC.LINHAS_POR_FOLHA + 1;
                ChaveC c = item.getKey();
                registros.add(new RegistroBpaC(cnes, cmp, c.cbo(), folha, seq, c.procedimento(), c.idade(), qt));
                indice++;
            }
        }
        return registros;
    }

    // ---------- BPA-I ----------

    private static List<RegistroBpaI> montarBpaI(String cnes, String cmp, List<Atendimento> atendimentos, int primeiraFolha) {
        // Folhas por profissional (CNS + CBO), em ordem de nome; atendimentos por data.
        Map<String, List<Atendimento>> porProfissional = new LinkedHashMap<>();
        atendimentos.stream()
                .filter(a -> a.getProcedimento().getModalidade() == Modalidade.BPA_I)
                .sorted(Comparator.comparing((Atendimento a) -> a.getProfissional().getNome())
                        .thenComparing(a -> a.getProfissional().getCns())
                        .thenComparing(a -> a.getProfissional().getCbo())
                        .thenComparing(Atendimento::getData)
                        .thenComparing(a -> a.getId() == null ? 0L : a.getId()))
                .forEach(a -> porProfissional
                        .computeIfAbsent(a.getProfissional().getCns() + a.getProfissional().getCbo(), k -> new ArrayList<>())
                        .add(a));

        List<RegistroBpaI> registros = new ArrayList<>();
        int folha = primeiraFolha;
        for (List<Atendimento> lista : porProfissional.values()) {
            for (int i = 0; i < lista.size(); i++) {
                if (i > 0 && i % RegistroBpaI.LINHAS_POR_FOLHA == 0) folha++;
                registros.add(registroI(cnes, cmp, lista.get(i), folha, i % RegistroBpaI.LINHAS_POR_FOLHA + 1));
            }
            folha++;
        }
        return registros;
    }

    private static RegistroBpaI registroI(String cnes, String cmp, Atendimento a, int folha, int seq) {
        Profissional pr = a.getProfissional();
        Paciente p = a.getPaciente();
        return new RegistroBpaI(cnes, cmp, pr.getCns(), pr.getCbo(), a.getData(), folha, seq,
                a.getProcedimento().getCodigo(), p.getCns(), p.getSexo().name().charAt(0), p.getCodigoMunicipio(),
                a.getCid(), a.getIdade(), a.getQuantidade(), a.getCaraterAtendimento(), p.getNome(),
                p.getDataNascimento(), p.getRacaCor(), p.getEtnia(), p.getNacionalidade(), p.getCep(),
                p.getLogradouro(), p.getNumero(), p.getBairro(), p.getTelefone());
    }
}
