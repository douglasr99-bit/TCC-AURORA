package br.com.aurora.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.aurora.domain.Paciente;
import br.com.aurora.dto.PacienteEntrada;
import br.com.aurora.dto.PacienteSaida;
import br.com.aurora.exception.Erros;
import br.com.aurora.exception.NaoEncontradoException;
import br.com.aurora.repository.PacienteRepository;
import br.com.aurora.validacao.Texto;

/** RF05. Formatos vêm do DTO; aqui ficam as regras que dependem de tabelas ou de outros campos. */
@Service
public class PacienteService {

    static final int LIMITE_BUSCA = 50;
    static final int IDADE_MAXIMA = 130;
    static final String RACA_INDIGENA = "05";
    static final String NACIONALIDADE_BRASILEIRA = "010";

    private final PacienteRepository repositorio;
    private final TabelaService tabelas;
    private final Clock relogio;

    public PacienteService(PacienteRepository repositorio, TabelaService tabelas, Clock relogio) {
        this.repositorio = repositorio;
        this.tabelas = tabelas;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public List<PacienteSaida> buscar(String q) {
        String termo = q == null ? "" : q.strip();
        return repositorio.buscar(termo, Limit.of(LIMITE_BUSCA)).stream().map(PacienteSaida::de).toList();
    }

    @Transactional(readOnly = true)
    public PacienteSaida obter(Long id) {
        return PacienteSaida.de(carregar(id));
    }

    @Transactional
    public PacienteSaida criar(PacienteEntrada dados) {
        Erros erros = validar(dados);
        if (!erros.temErroEm("cns") && repositorio.existsByCns(dados.cns())) {
            erros.add("cns", "Já existe um paciente com este CNS. Use a busca para encontrá-lo.");
        }
        erros.lancarSeHouver();
        Paciente p = new Paciente();
        preencher(p, dados);
        return PacienteSaida.de(repositorio.save(p));
    }

    @Transactional
    public PacienteSaida atualizar(Long id, PacienteEntrada dados) {
        Paciente p = carregar(id);
        Erros erros = validar(dados);
        if (!erros.temErroEm("cns") && repositorio.existsByCnsAndIdNot(dados.cns(), id)) {
            erros.add("cns", "Já existe outro paciente com este CNS.");
        }
        erros.lancarSeHouver();
        preencher(p, dados);
        return PacienteSaida.de(p);
    }

    Erros validar(PacienteEntrada d) {
        Erros erros = new Erros();
        LocalDate hoje = LocalDate.now(relogio);

        if (d.dataNascimento().isAfter(hoje)) {
            erros.add("dataNascimento", "A data de nascimento não pode ser no futuro.");
        } else if (Period.between(d.dataNascimento(), hoje).getYears() > IDADE_MAXIMA) {
            erros.add("dataNascimento", "A idade passa de 130 anos. Confira o ano de nascimento.");
        }

        if (!tabelas.contem(TabelaService.RACA_COR, d.racaCor())) {
            erros.add("racaCor", "Escolha uma raça/cor da lista.");
        }
        if (!tabelas.contem(TabelaService.NACIONALIDADE, d.nacionalidade())) {
            erros.add("nacionalidade", "Escolha uma nacionalidade da lista.");
        }

        // Portaria SAS 508/2010 e Manual do BPA: etnia só para indígena de nacionalidade brasileira.
        String etnia = Texto.limpo(d.etnia());
        boolean exigeEtnia = RACA_INDIGENA.equals(d.racaCor()) && NACIONALIDADE_BRASILEIRA.equals(d.nacionalidade());
        if (exigeEtnia && etnia == null) {
            erros.add("etnia", "Para paciente indígena de nacionalidade brasileira, escolha a etnia.");
        } else if (exigeEtnia && !tabelas.contem(TabelaService.ETNIA, etnia)) {
            erros.add("etnia", "Escolha uma etnia da lista.");
        } else if (!exigeEtnia && etnia != null) {
            erros.add("etnia", "A etnia só é informada para paciente indígena de nacionalidade brasileira. "
                    + "Deixe este campo em branco.");
        }
        return erros;
    }

    private Paciente carregar(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Paciente não encontrado."));
    }

    private static void preencher(Paciente p, PacienteEntrada d) {
        p.setCns(d.cns());
        p.setNome(Texto.limpo(d.nome()));
        p.setDataNascimento(d.dataNascimento());
        p.setSexo(d.sexo());
        p.setRacaCor(d.racaCor());
        p.setEtnia(Texto.limpo(d.etnia()));
        p.setNacionalidade(d.nacionalidade());
        p.setCodigoMunicipio(d.codigoMunicipio());
        p.setCep(Texto.limpo(d.cep()));
        p.setLogradouro(Texto.limpo(d.logradouro()));
        p.setNumero(Texto.limpo(d.numero()));
        p.setBairro(Texto.limpo(d.bairro()));
        p.setTelefone(Texto.limpo(d.telefone()));
    }
}
