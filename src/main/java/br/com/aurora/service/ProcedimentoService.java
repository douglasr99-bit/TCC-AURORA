package br.com.aurora.service;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.aurora.domain.Procedimento;
import br.com.aurora.dto.ProcedimentoEntrada;
import br.com.aurora.dto.ProcedimentoSaida;
import br.com.aurora.exception.NaoEncontradoException;
import br.com.aurora.exception.NegocioException;
import br.com.aurora.repository.ProcedimentoRepository;
import br.com.aurora.validacao.Texto;

/** RF06: tabela de procedimentos mantida manualmente (sem importação do SIGTAP). */
@Service
public class ProcedimentoService {

    static final int LIMITE_BUSCA = 50;

    private final ProcedimentoRepository repositorio;

    public ProcedimentoService(ProcedimentoRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public List<ProcedimentoSaida> buscar(String q) {
        String termo = q == null ? "" : q.strip();
        return repositorio.buscar(termo, Limit.of(LIMITE_BUSCA)).stream().map(ProcedimentoSaida::de).toList();
    }

    @Transactional(readOnly = true)
    public ProcedimentoSaida obter(Long id) {
        return ProcedimentoSaida.de(carregar(id));
    }

    @Transactional
    public ProcedimentoSaida criar(ProcedimentoEntrada dados) {
        if (repositorio.existsByCodigo(dados.codigo())) {
            throw new NegocioException("codigo", "Este procedimento já está cadastrado.");
        }
        Procedimento p = new Procedimento();
        preencher(p, dados);
        return ProcedimentoSaida.de(repositorio.save(p));
    }

    @Transactional
    public ProcedimentoSaida atualizar(Long id, ProcedimentoEntrada dados) {
        Procedimento p = carregar(id);
        if (repositorio.existsByCodigoAndIdNot(dados.codigo(), id)) {
            throw new NegocioException("codigo", "Já existe outro procedimento com este código.");
        }
        preencher(p, dados);
        return ProcedimentoSaida.de(p);
    }

    private Procedimento carregar(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Procedimento não encontrado."));
    }

    private static void preencher(Procedimento p, ProcedimentoEntrada dados) {
        p.setCodigo(dados.codigo());
        p.setDescricao(Texto.limpo(dados.descricao()));
        p.setModalidade(dados.modalidade());
        p.setExigeCid(Boolean.TRUE.equals(dados.exigeCid()));
    }
}
