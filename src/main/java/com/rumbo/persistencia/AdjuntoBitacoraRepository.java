package com.rumbo.persistencia;

import com.rumbo.modelo.AdjuntoBitacora;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdjuntoBitacoraRepository
        extends JpaRepository<AdjuntoBitacora, String> {

    long countByEntrada_IdEntrada(String idEntrada);
}
