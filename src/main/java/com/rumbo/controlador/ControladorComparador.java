package com.rumbo.controlador;

import com.rumbo.modelo.ComparacionCarreras;
import com.rumbo.servicio.ComparadorService;
import com.rumbo.servicio.ExcepcionCarreraNoEncontrada;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.rumbo.servicio.GestorPermisos;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/comparador")
public class ControladorComparador {

    private final ComparadorService comparadorService;
    private final GestorPermisos gestorPermisos;

    public ControladorComparador(ComparadorService comparadorService, GestorPermisos gestorPermisos) {
        this.comparadorService = comparadorService;
        this.gestorPermisos = gestorPermisos;
    }

    @GetMapping
    public ResponseEntity<?> comparar(
            @RequestParam(required = false) List<Long> ids, HttpSession sesion) {

        gestorPermisos.exigirSesion(sesion);

        if (ids == null || ids.size() < 2) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Selecciona al menos dos carreras."));
        }

        if (ids.stream().anyMatch(id -> id == null || id < 1)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Los identificadores no son válidos."));
        }

        if (new HashSet<>(ids).size() != ids.size()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No puedes repetir una carrera."));
        }

        try {
            ComparacionCarreras resultado = comparadorService.comparar(ids);

            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .body(resultado);

        } catch (ExcepcionCarreraNoEncontrada e) {
            return ResponseEntity.status(404)
                    .body(Map.of("error",
                            "Una de las carreras seleccionadas ya no existe."));
        }
    }
}