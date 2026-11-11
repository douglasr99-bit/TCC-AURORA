package br.com.aurora.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.aurora.domain.Estabelecimento;

public interface EstabelecimentoRepository extends JpaRepository<Estabelecimento, Long> {

    /** Uma instalação = um estabelecimento. */
    Optional<Estabelecimento> findFirstByOrderByIdAsc();
}
