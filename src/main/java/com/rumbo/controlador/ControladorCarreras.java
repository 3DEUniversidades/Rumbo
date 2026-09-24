package com.rumbo.controlador;

import com.rumbo.dto.CarreraRespuesta;
import com.rumbo.modelo.Carrera;
import com.rumbo.servicio.CarreraService;
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

    public ControladorCarreras(CarreraService carreraService) {
        this.carreraService = carreraService;
    }

    @GetMapping
    public List<CarreraRespuesta> consultarCarreras() {
        return carreraService.consultarCarreras()
                .stream()
                .map(this::mapearRespuesta)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CarreraRespuesta> buscarPorId(@PathVariable Long id) {
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
                carrera.getPensum(),
                carrera.getCosto()
        );
    }
}
