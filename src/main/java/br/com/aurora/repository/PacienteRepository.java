package br.com.aurora.repository;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.aurora.domain.Paciente;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    boolean existsByCns(String cns);

    boolean existsByCnsAndIdNot(String cns, Long id);

    @Query("""
            select p from Paciente p
            where lower(p.nome) like lower(concat('%', :q, '%')) or p.cns like concat(:q, '%')
            order by p.nome""")
    List<Paciente> buscar(@Param("q") String q, Limit limite);
}
