package com.rumbo.persistencia;
import com.rumbo.modelo.RegistroAdministracion;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RegistroAdministracionRepository extends JpaRepository<RegistroAdministracion, String> {}
