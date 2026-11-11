package br.com.aurora.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.aurora.domain.Atendimento;
import br.com.aurora.domain.Competencia;

public interface AtendimentoRepository extends JpaRepository<Atendimento, Long> {

    /** Atendimentos da competência em uma consulta só (sem N+1). */
    @Query("""
            select a from Atendimento a
            join fetch a.profissional join fetch a.procedimento join fetch a.usuario
            left join fetch a.paciente
            where a.competencia = :competencia
            order by a.data desc, a.id desc""")
    List<Atendimento> listarDaCompetencia(@Param("competencia") Competencia competencia);

    @Query("""
            select a from Atendimento a
            join fetch a.profissional join fetch a.procedimento join fetch a.usuario
            left join fetch a.paciente
            where a.data = :data
            order by a.id desc""")
    List<Atendimento> listarDaData(@Param("data") LocalDate data);

    @Query("""
            select a from Atendimento a
            join fetch a.competencia join fetch a.profissional join fetch a.procedimento join fetch a.usuario
            left join fetch a.paciente
            where a.id = :id""")
    Optional<Atendimento> buscarCompleto(@Param("id") Long id);
}
