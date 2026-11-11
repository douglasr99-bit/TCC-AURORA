package br.com.aurora.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.aurora.domain.Competencia;
import br.com.aurora.domain.ReaberturaCompetencia;

public interface ReaberturaCompetenciaRepository extends JpaRepository<ReaberturaCompetencia, Long> {

    @Query("""
            select r from ReaberturaCompetencia r join fetch r.usuario
            where r.competencia = :competencia order by r.dataReabertura desc""")
    List<ReaberturaCompetencia> daCompetencia(@Param("competencia") Competencia competencia);
}
