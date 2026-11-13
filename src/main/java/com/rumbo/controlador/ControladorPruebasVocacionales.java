package com.rumbo.controlador;

import com.rumbo.dto.*;
import com.rumbo.modelo.Rol;
import com.rumbo.servicio.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pruebas-vocacionales")
public class ControladorPruebasVocacionales {
    private final PruebasVocacionalesService servicio;
    private final GestorPermisos permisos;
    private final ValidadorResultadoPrueba validador;

    public ControladorPruebasVocacionales(PruebasVocacionalesService servicio,
            GestorPermisos permisos, ValidadorResultadoPrueba validador) {
        this.servicio = servicio; this.permisos = permisos; this.validador = validador;
    }

    private String estudiante(HttpSession sesion) {
        return permisos.exigirRol(sesion, Rol.ESTUDIANTE).getIdUsuario();
    }
    @GetMapping
    public List<PruebaVocacionalRespuesta> consultar(HttpSession sesion) {
        return servicio.consultarPruebas(estudiante(sesion));
    }
    @GetMapping("/por-actividad/{idActividad}")
    public List<PruebaVocacionalRespuesta> porActividad(@PathVariable String idActividad, HttpSession sesion) {
        return servicio.consultarPorActividad(estudiante(sesion), idActividad);
    }
    @GetMapping("/mis-resultados")
    public List<ResultadoPruebaRespuesta> resultados(HttpSession sesion) {
        return servicio.consultarMisResultados(estudiante(sesion));
    }
    @GetMapping("/{idPrueba}")
    public PruebaVocacionalRespuesta detalle(@PathVariable String idPrueba, HttpSession sesion) {
        return servicio.consultarPrueba(estudiante(sesion), idPrueba);
    }
    @PutMapping("/{idPrueba}/mi-resultado")
    public ResultadoPruebaRespuesta guardar(@PathVariable String idPrueba,
            @RequestBody ResultadoPruebaSolicitud solicitud, HttpSession sesion) {
        String idUsuario = estudiante(sesion);
        validador.exigirValido(solicitud);
        return servicio.guardarResultado(idUsuario, idPrueba, solicitud);
    }
    @ExceptionHandler(ExcepcionPruebaNoEncontrada.class)
    public ResponseEntity<Map<String, String>> noEncontrada(ExcepcionPruebaNoEncontrada e) {
        return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> conflicto(DataIntegrityViolationException e) {
        return ResponseEntity.status(409).body(Map.of("error", "El resultado cambio durante la operacion. Actualiza la pagina y vuelve a intentarlo."));
    }
}
