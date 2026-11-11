package br.com.aurora.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.aurora.domain.Estabelecimento;
import br.com.aurora.dto.EstabelecimentoDto;
import br.com.aurora.repository.EstabelecimentoRepository;
import br.com.aurora.validacao.Texto;

/** RF03. Uma instalação = um estabelecimento: o PUT cria na primeira vez e depois atualiza. */
@Service
public class EstabelecimentoService {

    private final EstabelecimentoRepository repositorio;

    public EstabelecimentoService(EstabelecimentoRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public Optional<EstabelecimentoDto> obter() {
        return repositorio.findFirstByOrderByIdAsc().map(EstabelecimentoDto::de);
    }

    @Transactional
    public EstabelecimentoDto salvar(EstabelecimentoDto dados) {
        Estabelecimento e = repositorio.findFirstByOrderByIdAsc().orElseGet(Estabelecimento::new);
        e.setCnes(dados.cnes());
        e.setNome(Texto.limpo(dados.nome()));
        e.setSigla(Texto.limpo(dados.sigla()).toUpperCase());
        e.setCnpj(dados.cnpj());
        e.setOrgaoDestino(Texto.limpo(dados.orgaoDestino()));
        e.setIndicadorDestino(dados.indicadorDestino().charAt(0));
        return EstabelecimentoDto.de(repositorio.save(e));
    }
}
