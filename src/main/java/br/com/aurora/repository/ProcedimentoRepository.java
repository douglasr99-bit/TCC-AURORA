package br.com.aurora.repository;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.aurora.domain.Procedimento;

public interface ProcedimentoRepository extends JpaRepository<Procedimento, Long> {

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Long id);

    @Query("""
            select p from Procedimento p
            where lower(p.descricao) like lower(concat('%', :q, '%')) or p.codigo like concat(:q, '%')
            order by p.codigo""")
    List<Procedimento> buscar(@Param("q") String q, Limit limite);
}
