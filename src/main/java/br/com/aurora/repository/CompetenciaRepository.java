package br.com.aurora.repository;

import java.time.YearMonth;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.aurora.domain.Competencia;

public interface CompetenciaRepository extends JpaRepository<Competencia, Long> {

    Optional<Competencia> findByMesAno(YearMonth mesAno);

    /**
     * Cria a competência ABERTA se ainda não existir. Seguro com lançamentos simultâneos:
     * a restrição UNIQUE de mes_ano decide, sem erro.
     */
    @Modifying
    @Query(value = "INSERT INTO competencia (mes_ano, situacao) VALUES (:mesAno, 'ABERTA') "
            + "ON CONFLICT (mes_ano) DO NOTHING", nativeQuery = true)
    void criarSeNaoExistir(@Param("mesAno") String mesAno);
}
