package com.rumbo;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rumbo.cloudinary")
public class PropiedadesCloudinary {

    private String cloudName;
    private String apiKey;
    private String apiSecret;
    private String carpetaBase = "rumbo";

    public boolean estaConfigurado() {
        return tieneTexto(cloudName) && tieneTexto(apiKey) && tieneTexto(apiSecret);
    }

    private boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    public String getCloudName() {
        return cloudName;
    }

    public void setCloudName(String cloudName) {
        this.cloudName = cloudName;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }

    public String getCarpetaBase() {
        return carpetaBase;
    }

    public void setCarpetaBase(String carpetaBase) {
        this.carpetaBase = carpetaBase;
    }
}
