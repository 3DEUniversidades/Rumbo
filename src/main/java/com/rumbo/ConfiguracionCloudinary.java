package com.rumbo;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PropiedadesCloudinary.class)
public class ConfiguracionCloudinary {

    @Bean
    public Cloudinary cloudinary(PropiedadesCloudinary propiedades) {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", propiedades.getCloudName(),
                "api_key", propiedades.getApiKey(),
                "api_secret", propiedades.getApiSecret(),
                "secure", true
        ));
    }
}
