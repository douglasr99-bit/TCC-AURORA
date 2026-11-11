package br.com.aurora.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.aurora.domain.Atendimento;
import br.com.aurora.domain.Competencia;
import br.com.aurora.domain.Modalidade;
import br.com.aurora.domain.Paciente;
import br.com.aurora.domain.Procedimento;
import br.com.aurora.domain.Profissional;
import br.com.aurora.dto.AtendimentoEntrada;
import br.com.aurora.dto.AtendimentoSaida;
import br.com.aurora.exception.Erros;
import br.com.aurora.exception.NaoEncontradoException;
import br.com.aurora.exception.NegocioException;
import br.com.aurora.repository.AtendimentoRepository;
import br.com.aurora.repository.CompetenciaRepository;
import br.com.aurora.repository.PacienteRepository;
import br.com.aurora.repository.ProcedimentoRepository;
import br.com.aurora.repository.ProfissionalRepository;
import br.com.aurora.repository.UsuarioRepository;
import br.com.aurora.validacao.Texto;

/**
 * Lançamento, consulta, edição e exclusão de atendimentos (RF07, RF08, RF10).
 * A competência é a do mês da data do atendimento (não a da digitação).
 */
@Service
public class AtendimentoService {

    static final int IDADE_MAXIMA = 130;

    private final AtendimentoRepository atendimentos;
    private final CompetenciaRepository competencias;
    private final CompetenciaService competenciaService;
    private final ProfissionalRepository profissionais;
    private final ProcedimentoRepository procedimentos;
    private final PacienteRepository pacientes;
    private final UsuarioRepository usuarios;
    private final TabelaService tabelas;
    private final Clock relogio;

    public AtendimentoService(AtendimentoRepository atendimentos, CompetenciaRepository competencias,
            CompetenciaService competenciaService, ProfissionalRepository profissionais,
            ProcedimentoRepository procedimentos, PacienteRepository pacientes, UsuarioRepository usuarios,
            TabelaService tabelas, Clock relogio) {
        this.atendimentos = atendimentos;
        this.competencias = competencias;
        this.competenciaService = competenciaService;
        this.profissionais = profissionais;
        this.procedimentos = procedimentos;
        this.pacientes = pacientes;
        this.usuarios = usuarios;
        this.tabelas = tabelas;
        this.relogio = relogio;
    }

    // ---------- consulta ----------

    @Transactional(readOnly = true)
    public List<AtendimentoSaida> listarDaCompetencia(String mesAno) {
        YearMonth ym = CompetenciaService.interpretar(mesAno, "competencia");
        return competencias.findByMesAno(ym)
                .map(c -> atendimentos.listarDaCompetencia(c).stream().map(AtendimentoSaida::de).toList())
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public List<AtendimentoSaida> listarDaData(LocalDate data) {
        return atendimentos.listarDaData(data).stream().map(AtendimentoSaida::de).toList();
    }

    @Transactional(readOnly = true)
    public AtendimentoSaida obter(Long id) {
        return AtendimentoSaida.de(carregar(id));
    }

    // ---------- lançamento ----------

    @Transactional
    public AtendimentoSaida lancar(AtendimentoEntrada dados, Long idUsuario) {
        Validado v = validar(dados);
        Atendimento a = new Atendimento();
        aplicar(a, v, dados);
        a.setUsuario(usuarios.getReferenceById(idUsuario));
        return AtendimentoSaida.de(atendimentos.save(a));
    }

    @Transactional
    public AtendimentoSaida atualizar(Long id, AtendimentoEntrada dados) {
        Atendimento a = carregar(id);
        exigirAberta(a.getCompetencia(), "corrigir");
        Validado v = validar(dados);
        aplicar(a, v, dados);
        return AtendimentoSaida.de(a);
    }

    @Transactional
    public void excluir(Long id) {
        Atendimento a = carregar(id);
        exigirAberta(a.getCompetencia(), "excluir");
        atendimentos.delete(a);
    }

    // ---------- regras (RF08) ----------

    /** Resultado da validação: entidades carregadas e dados calculados. */
    record Validado(Competencia competencia, Profissional profissional, Procedimento procedimento,
            Paciente paciente, Integer idade) {
    }

    Validado validar(AtendimentoEntrada d) {
        Erros erros = new Erros();
        LocalDate hoje = LocalDate.now(relogio);

        if (d.data().isAfter(hoje)) {
            erros.add("data", "A data do atendimento não pode ser no futuro. Confira o dia, o mês e o ano.");
        }

        Profissional profissional = profissionais.findById(d.profissionalId()).orElse(null);
        if (profissional == null) {
            erros.add("profissionalId", "Profissional não encontrado. Busque e escolha de novo.");
        } else if (!profissional.isAtivo()) {
            erros.add("profissionalId", "Este profissional está inativo. Escolha outro ou peça ao administrador "
                    + "para reativá-lo.");
        }

        Procedimento procedimento = procedimentos.findById(d.procedimentoId()).orElse(null);
        if (procedimento == null) {
            erros.add("procedimentoId", "Procedimento não encontrado. Busque e escolha de novo.");
        }

        Paciente paciente = null;
        Integer idade = null;
        String cid = Texto.limpo(d.cid());
        String carater = Texto.limpo(d.caraterAtendimento());

        if (procedimento != null && procedimento.getModalidade() == Modalidade.BPA_I) {
            if (d.pacienteId() == null) {
                erros.add("pacienteId", "Este procedimento é BPA-I (individualizado): informe o CNS do paciente "
                        + "e busque o cadastro.");
            } else {
                paciente = pacientes.findById(d.pacienteId()).orElse(null);
                if (paciente == null) {
                    erros.add("pacienteId", "Paciente não encontrado. Busque pelo CNS de novo.");
                } else if (paciente.getDataNascimento().isAfter(d.data())) {
                    erros.add("data", "A data do atendimento é anterior ao nascimento do paciente. Confira a data.");
                } else {
                    idade = Period.between(paciente.getDataNascimento(), d.data()).getYears();
                    if (idade > IDADE_MAXIMA) {
                        erros.add("pacienteId", "Pela data de nascimento do cadastro, o paciente teria mais de 130 "
                                + "anos. Corrija o cadastro do paciente.");
                    }
                }
            }
            if (carater == null) {
                erros.add("caraterAtendimento", "Escolha o caráter do atendimento (ex.: 01 – Eletivo).");
            } else if (!tabelas.contem(TabelaService.CARATER_ATENDIMENTO, carater)) {
                erros.add("caraterAtendimento", "Escolha um caráter de atendimento da lista.");
            }
        } else if (procedimento != null) {
            // BPA-C: consolidado, sem paciente; idade obrigatória.
            if (d.pacienteId() != null) {
                erros.add("pacienteId", "Procedimento BPA-C (consolidado) não identifica o paciente. "
                        + "Remova o paciente deste lançamento.");
            }
            if (d.idade() == null) {
                erros.add("idade", "Informe a idade do paciente atendido (exigida no BPA-C).");
            } else if (d.idade() < 0 || d.idade() > IDADE_MAXIMA) {
                erros.add("idade", "A idade deve estar entre 0 e 130 anos.");
            } else {
                idade = d.idade();
            }
            if (carater != null) {
                erros.add("caraterAtendimento", "O caráter de atendimento só é informado em procedimentos BPA-I. "
                        + "Deixe este campo em branco.");
            }
        }

        if (procedimento != null && procedimento.isExigeCid() && cid == null) {
            erros.add("cid", "Este procedimento exige o CID-10. Informe o código da doença (ex.: J069).");
        } else if (cid != null && !erros.temErroEm("cid") && !tabelas.contem(TabelaService.CID10, cid)) {
            erros.add("cid", "CID-10 não encontrado na tabela do SIA. Confira o código (sem ponto).");
        }

        erros.lancarSeHouver();

        // Só depois de tudo certo: competência do mês da data, criada ABERTA se for a primeira.
        Competencia competencia = competenciaService.obterOuCriar(YearMonth.from(d.data()));
        if (CompetenciaService.fechada(competencia)) {
            throw new NegocioException("data", "A competência " + CompetenciaService.formatar(competencia.getMesAno())
                    + " está fechada e não aceita lançamentos. Para lançar nela, peça ao responsável pela "
                    + "produção que reabra a competência.");
        }
        return new Validado(competencia, profissional, procedimento, paciente, idade);
    }

    private static void aplicar(Atendimento a, Validado v, AtendimentoEntrada d) {
        a.setCompetencia(v.competencia());
        a.setData(d.data());
        a.setProfissional(v.profissional());
        a.setProcedimento(v.procedimento());
        a.setPaciente(v.paciente());
        a.setQuantidade(d.quantidade());
        a.setIdade(v.idade());
        a.setCid(Texto.limpo(d.cid()));
        a.setCaraterAtendimento(v.procedimento().getModalidade() == Modalidade.BPA_I
                ? Texto.limpo(d.caraterAtendimento()) : null);
    }

    private void exigirAberta(Competencia c, String acao) {
        if (CompetenciaService.fechada(c)) {
            throw new NegocioException(null, "Este lançamento é da competência "
                    + CompetenciaService.formatar(c.getMesAno()) + ", que está fechada. Para " + acao
                    + ", peça ao responsável pela produção que reabra a competência.");
        }
    }

    private Atendimento carregar(Long id) {
        return atendimentos.buscarCompleto(id)
                .orElseThrow(() -> new NaoEncontradoException("Lançamento não encontrado."));
    }
}
