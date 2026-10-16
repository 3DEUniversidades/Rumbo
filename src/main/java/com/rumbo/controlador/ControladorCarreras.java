package com.rumbo.controlador;

import com.rumbo.dto.CarreraRespuesta;
import com.rumbo.modelo.Carrera;
import com.rumbo.servicio.CarreraService;
import com.rumbo.servicio.GestorPermisos;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/carreras")
public class ControladorCarreras {

    private final CarreraService carreraService;
    private final GestorPermisos gestorPermisos;

    public ControladorCarreras(CarreraService carreraService, GestorPermisos gestorPermisos) {
        this.carreraService = carreraService;
        this.gestorPermisos = gestorPermisos;
    }

    @GetMapping
    public List<CarreraRespuesta> consultarCarreras(HttpSession sesion) {
        gestorPermisos.exigirSesion(sesion);
        return carreraService.consultarCarreras()
                .stream()
                .map(this::mapearRespuesta)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CarreraRespuesta> buscarPorId(@PathVariable Long id, HttpSession sesion) {
        gestorPermisos.exigirSesion(sesion);
        return carreraService.buscarPorId(id)
                .map(this::mapearRespuesta)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private CarreraRespuesta mapearRespuesta(Carrera carrera) {
        return new CarreraRespuesta(
                carrera.getId(),
                carrera.getNombre(),
                carrera.getDuracion(),
                carrera.getCosto(),
                carrera.getModalidad(),
                carrera.getCargaCreditos(),
                carrera.getHabilidadesRequeridas(),
                carrera.getOportunidadesLaborales(),
                carrera.getSalarioEstimado(),
                carrera.getEmpleabilidad(),
                carrera.getPdfOficialUrl(),
                carrera.getUrlCarreraOficial()
        );
    }
}
