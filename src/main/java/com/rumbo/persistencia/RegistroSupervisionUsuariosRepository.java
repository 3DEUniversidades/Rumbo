package com.rumbo.persistencia;

import com.rumbo.modelo.RegistroSupervisionUsuarios;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistroSupervisionUsuariosRepository
        extends JpaRepository<RegistroSupervisionUsuarios, String> {
}
