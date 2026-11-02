package com.rumbo.persistencia;
import com.rumbo.modelo.RegistroSupervisionUsuarios;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface ConsultaHistorialSupervisorRepository extends Repository<RegistroSupervisionUsuarios,String> {
    @Query("select r from RegistroSupervisionUsuarios r where r.idSupervisor = :supervisor "
            + "and (:tipo = 'TODAS' or r.tipo = :tipo) and (:decision = 'TODAS' or r.decision = :decision)")
    Page<RegistroSupervisionUsuarios> consultarPropias(@Param("supervisor") String supervisor,
            @Param("tipo") String tipo,@Param("decision") String decision,Pageable pagina);
}
