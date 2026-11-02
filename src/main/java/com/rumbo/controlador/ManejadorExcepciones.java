package com.rumbo.controlador;

import com.rumbo.servicio.ExcepcionAlmacenamiento;
import com.rumbo.servicio.ExcepcionBitacora;
import com.rumbo.servicio.ExcepcionConflicto;
import com.rumbo.servicio.ExcepcionAutenticacion;
import com.rumbo.servicio.ExcepcionValidacion;
import com.rumbo.servicio.ExcepcionPermiso;
import com.rumbo.servicio.ExcepcionExportacionBitacora;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

@RestControllerAdvice
public class ManejadorExcepciones {

    @ExceptionHandler(ExcepcionAutenticacion.class)
    public ResponseEntity<?> manejarAutenticacion(ExcepcionAutenticacion e) {
        return ResponseEntity.status(401)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(ExcepcionPermiso.class)
    public ResponseEntity<?> manejarPermiso(ExcepcionPermiso e) {
        return ResponseEntity.status(403)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(ExcepcionValidacion.class)
    public ResponseEntity<?> manejarValidacion(ExcepcionValidacion e) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", "Datos invalidos", "campos", e.getErrores()));
    }

    @ExceptionHandler(ExcepcionConflicto.class)
    public ResponseEntity<?> manejarConflicto(ExcepcionConflicto e) {
        return ResponseEntity.status(409)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(ExcepcionBitacora.class)
    public ResponseEntity<?> manejarBitacora(ExcepcionBitacora e) {
        return ResponseEntity.status(404)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(ExcepcionAlmacenamiento.class)
    public ResponseEntity<?> manejarAlmacenamiento(ExcepcionAlmacenamiento e) {
        return ResponseEntity.status(503)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> manejarArchivoDemasiadoGrande() {
        return ResponseEntity.status(413)
                .body(Map.of("error", "El archivo excede el tamaño máximo permitido"));
    }

    @ExceptionHandler(ExcepcionExportacionBitacora.class)
    public ResponseEntity<?> manejarExportacionBitacora(ExcepcionExportacionBitacora e) {
        return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    }
}
