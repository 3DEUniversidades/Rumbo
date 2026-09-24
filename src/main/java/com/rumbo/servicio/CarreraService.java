package com.rumbo.servicio;

import com.rumbo.modelo.Carrera;
import com.rumbo.persistencia.CarreraRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class CarreraService {

    private final CarreraRepository carreraRepository;

    public CarreraService(CarreraRepository carreraRepository) {
        this.carreraRepository = carreraRepository;
    }

    public List<Carrera> consultarCarreras() {
        return carreraRepository.findAll();
    }

    public Optional<Carrera> buscarPorId(Long id) {
        return carreraRepository.findById(id);
    }
}
