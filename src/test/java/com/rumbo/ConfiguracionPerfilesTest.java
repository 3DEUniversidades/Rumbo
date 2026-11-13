package com.rumbo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfiguracionPerfilesTest {

    // Simula un .env de PostgreSQL incluso cuando se activa H2 o test.
    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withPropertyValues(
                    "DB_URL=jdbc:postgresql://localhost:5432/base_prueba_config",
                    "DB_USERNAME=usuario_config",
                    "DB_PASSWORD=clave_config_ficticia",
                    "DB_DDL_AUTO=validate",
                    "DB_SQL_INIT_MODE=always");

    @Test
    void sinPerfilUsaPostgresqlYLasVariablesDeConexion() {
        contexto.withPropertyValues("spring.profiles.active=").run(contexto -> {
            var entorno = contexto.getEnvironment();
            assertEquals("org.postgresql.Driver", entorno.getProperty("spring.datasource.driver-class-name"));
            assertEquals("jdbc:postgresql://localhost:5432/base_prueba_config",
                    entorno.getProperty("spring.datasource.url"));
            assertEquals("usuario_config", entorno.getProperty("spring.datasource.username"));
            assertEquals("clave_config_ficticia", entorno.getProperty("spring.datasource.password"));
            assertEquals("validate", entorno.getProperty("spring.jpa.hibernate.ddl-auto"));
            assertEquals("always", entorno.getProperty("spring.sql.init.mode"));
            assertEquals("classpath:data.sql,classpath:data-desarrollo.sql",
                    entorno.getProperty("spring.sql.init.data-locations"));
            assertEquals("false", entorno.getProperty("spring.h2.console.enabled"));
        });
    }

    @Test
    void perfilH2ReemplazaLaConexionAunqueExistanVariablesPostgresql() {
        contexto.withPropertyValues("spring.profiles.active=h2").run(contexto -> {
            var entorno = contexto.getEnvironment();
            assertEquals("org.h2.Driver", entorno.getProperty("spring.datasource.driver-class-name"));
            assertEquals("jdbc:h2:mem:rumbo;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                    entorno.getProperty("spring.datasource.url"));
            assertEquals("sa", entorno.getProperty("spring.datasource.username"));
            assertEquals("", entorno.getProperty("spring.datasource.password"));
            assertEquals("create-drop", entorno.getProperty("spring.jpa.hibernate.ddl-auto"));
            assertEquals("never", entorno.getProperty("spring.sql.init.mode"));
            assertEquals("true", entorno.getProperty("spring.h2.console.enabled"));
        });
    }

    @Test
    void perfilTestUsaH2SinDatosInicialesNiConsola() {
        contexto.withPropertyValues("spring.profiles.active=test").run(contexto -> {
            var entorno = contexto.getEnvironment();
            assertEquals("org.h2.Driver", entorno.getProperty("spring.datasource.driver-class-name"));
            assertEquals("jdbc:h2:mem:testdb", entorno.getProperty("spring.datasource.url"));
            assertEquals("sa", entorno.getProperty("spring.datasource.username"));
            assertEquals("", entorno.getProperty("spring.datasource.password"));
            assertEquals("create-drop", entorno.getProperty("spring.jpa.hibernate.ddl-auto"));
            assertEquals("never", entorno.getProperty("spring.sql.init.mode"));
            assertEquals("false", entorno.getProperty("spring.h2.console.enabled"));
        });
    }

    @Test
    void railwayUsaSoloElCatalogoPublicoYConfiguraLaSesionParaHttps() {
        contexto.withPropertyValues("spring.profiles.active=railway").run(contexto -> {
            var entorno = contexto.getEnvironment();
            assertEquals("org.postgresql.Driver", entorno.getProperty("spring.datasource.driver-class-name"));
            assertEquals("jdbc:postgresql://localhost:5432/base_prueba_config",
                    entorno.getProperty("spring.datasource.url"));
            assertEquals("classpath:data.sql", entorno.getProperty("spring.sql.init.data-locations"));
            assertEquals("always", entorno.getProperty("spring.sql.init.mode"));
            assertEquals("false", entorno.getProperty("spring.jpa.show-sql"));
            assertEquals("false", entorno.getProperty("spring.h2.console.enabled"));
            assertEquals("framework", entorno.getProperty("server.forward-headers-strategy"));
            assertEquals("true", entorno.getProperty("server.servlet.session.cookie.secure"));
            assertEquals("true", entorno.getProperty("server.servlet.session.cookie.http-only"));
            assertEquals("lax", entorno.getProperty("server.servlet.session.cookie.same-site"));
        });
    }

    @Test
    void railwayPermiteDesactivarLaCargaTrasElPrimerDespliegue() {
        contexto.withPropertyValues("spring.profiles.active=railway", "DB_SQL_INIT_MODE=never").run(contexto -> {
            assertEquals("never", contexto.getEnvironment().getProperty("spring.sql.init.mode"));
        });
    }
}
