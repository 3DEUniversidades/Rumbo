package com.rumbo.dto;

public record NotificacionResponse(
        String idNotificacion,
        String titulo,
        String mensaje,
        boolean leida,
        String fecha) {
}
