package br.com.aurora.repository;

import java.time.YearMonth;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.aurora.domain.Competencia;

public interface CompetenciaRepository extends JpaRepository<Competencia, Long> {

    Optional<Competencia> findByMesAno(YearMonth mesAno);
}
