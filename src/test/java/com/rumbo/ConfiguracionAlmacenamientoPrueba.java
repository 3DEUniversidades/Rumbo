package com.rumbo;

import com.rumbo.servicio.AlmacenamientoArchivos;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
public class ConfiguracionAlmacenamientoPrueba {

    @Bean
    @Primary
    public AlmacenamientoArchivos almacenamientoArchivos() {
        return new AlmacenamientoEnMemoria();
    }
}
