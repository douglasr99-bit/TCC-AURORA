package br.com.aurora.service;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.aurora.domain.Profissional;
import br.com.aurora.dto.ProfissionalEntrada;
import br.com.aurora.dto.ProfissionalSaida;
import br.com.aurora.exception.NaoEncontradoException;
import br.com.aurora.exception.NegocioException;
import br.com.aurora.repository.ProfissionalRepository;
import br.com.aurora.validacao.Texto;

/** RF04. */
@Service
public class ProfissionalService {

    static final int LIMITE_BUSCA = 50;

    private final ProfissionalRepository repositorio;

    public ProfissionalService(ProfissionalRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public List<ProfissionalSaida> buscar(String q) {
        String termo = q == null ? "" : q.strip();
        return repositorio.buscar(termo, Limit.of(LIMITE_BUSCA)).stream().map(ProfissionalSaida::de).toList();
    }

    @Transactional(readOnly = true)
    public ProfissionalSaida obter(Long id) {
        return ProfissionalSaida.de(carregar(id));
    }

    @Transactional
    public ProfissionalSaida criar(ProfissionalEntrada dados) {
        if (repositorio.existsByCns(dados.cns())) {
            throw new NegocioException("cns", "Já existe um profissional com este CNS.");
        }
        Profissional p = new Profissional();
        preencher(p, dados);
        return ProfissionalSaida.de(repositorio.save(p));
    }

    @Transactional
    public ProfissionalSaida atualizar(Long id, ProfissionalEntrada dados) {
        Profissional p = carregar(id);
        if (repositorio.existsByCnsAndIdNot(dados.cns(), id)) {
            throw new NegocioException("cns", "Já existe outro profissional com este CNS.");
        }
        preencher(p, dados);
        return ProfissionalSaida.de(p);
    }

    private Profissional carregar(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Profissional não encontrado."));
    }

    private static void preencher(Profissional p, ProfissionalEntrada dados) {
        p.setNome(Texto.limpo(dados.nome()));
        p.setCns(dados.cns());
        p.setCbo(dados.cbo());
        p.setAtivo(dados.ativo() == null || dados.ativo());
    }
}
