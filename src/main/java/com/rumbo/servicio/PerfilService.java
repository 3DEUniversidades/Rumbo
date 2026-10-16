package com.rumbo.servicio;

import com.rumbo.dto.PerfilRequestDTO;
import com.rumbo.dto.PerfilResponseDTO;
import com.rumbo.modelo.Carrera;
import com.rumbo.modelo.Perfil;
import com.rumbo.modelo.Rol;
import com.rumbo.modelo.Usuario;
import com.rumbo.persistencia.CarreraRepository;
import com.rumbo.persistencia.PerfilRepository;
import com.rumbo.persistencia.UsuarioRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PerfilService {

    private final PerfilRepository perfilRepository;
    private final UsuarioRepository usuarioRepository;
    private final CarreraRepository carreraRepository;
    private final ValidadorDatos validadorDatos;

    public PerfilService(PerfilRepository perfilRepository, UsuarioRepository usuarioRepository,
                         CarreraRepository carreraRepository, ValidadorDatos validadorDatos) {
        this.perfilRepository = perfilRepository;
        this.usuarioRepository = usuarioRepository;
        this.carreraRepository = carreraRepository;
        this.validadorDatos = validadorDatos;
    }

    @Transactional
    public Perfil crearPerfil(Usuario usuario) {
        return perfilRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                .orElseGet(() -> perfilRepository.save(new Perfil(usuario)));
    }

    @Transactional
    public PerfilResponseDTO consultar(String idUsuario) {
        Usuario usuario = buscarUsuario(idUsuario);
        return mapear(crearPerfil(usuario));
    }

    @Transactional
    public PerfilResponseDTO actualizar(String idUsuario, PerfilRequestDTO solicitud) {
        Usuario usuario = buscarUsuario(idUsuario);
        Perfil perfil = crearPerfil(usuario);
        Map<String, String> errores = validar(solicitud, usuario.getRol());
        if (!errores.isEmpty()) {
            throw new ExcepcionValidacion(errores);
        }

        perfil.setNivelEstudios(validadorDatos.normalizar(solicitud.getNivelEstudios()));
        perfil.setDescripcionPersonal(validadorDatos.normalizar(solicitud.getDescripcionPersonal()));
        perfil.setAreaLaboral(validadorDatos.normalizar(solicitud.getAreaLaboral()));
        perfil.setCarrera(carreraRepository.findById(solicitud.getCarreraId()).orElseThrow(
                () -> new ExcepcionValidacion(Map.of("carreraId", "La carrera no existe"))));

        return mapear(perfilRepository.save(perfil));
    }

    public boolean estaCompleto(String idUsuario) {
        Usuario usuario = buscarUsuario(idUsuario);
        Perfil perfil = perfilRepository.findByUsuarioIdUsuario(idUsuario).orElse(null);
        if (perfil == null) {
            return false;
        }
        boolean datosComunes = campoCompleto(perfil.getNivelEstudios())
                && campoCompleto(perfil.getDescripcionPersonal())
                && perfil.getCarrera() != null;
        if (usuario.getRol() == Rol.ESTUDIANTE) {
            return datosComunes;
        }
        return usuario.getRol() == Rol.COLABORADOR
                && datosComunes && campoCompleto(perfil.getAreaLaboral());
    }

    private Map<String, String> validar(PerfilRequestDTO solicitud, Rol rol) {
        Map<String, String> errores = new LinkedHashMap<>();
        if (solicitud == null) {
            errores.put("general", "No se recibieron datos del perfil");
            return errores;
        }
        if (!campoCompleto(solicitud.getNivelEstudios())) {
            errores.put("nivelEstudios", "El nivel de estudios es obligatorio");
        }
        if (solicitud.getCarreraId() == null) {
            errores.put("carreraId", "La carrera es obligatoria");
        }
        if (!campoCompleto(solicitud.getDescripcionPersonal())) {
            errores.put("descripcionPersonal", "La descripcion personal es obligatoria");
        }
        if (rol == Rol.COLABORADOR && !campoCompleto(solicitud.getAreaLaboral())) {
            errores.put("areaLaboral", "El area laboral es obligatoria");
        }
        if (rol == Rol.ESTUDIANTE && campoCompleto(solicitud.getAreaLaboral())) {
            errores.put("areaLaboral", "El area laboral no aplica a estudiantes");
        }
        return errores;
    }

    private Usuario buscarUsuario(String idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ExcepcionAutenticacion("Usuario no encontrado"));
    }

    private boolean campoCompleto(String valor) {
        return validadorDatos.validarCampoObligatorio(valor);
    }

    private PerfilResponseDTO mapear(Perfil perfil) {
        Carrera carrera = perfil.getCarrera();
        Usuario usuario = perfil.getUsuario();
        return new PerfilResponseDTO(
                usuario.getIdUsuario(),
                usuario.getNombreUsuario(),
                usuario.getNombreCompleto(),
                usuario.getCorreo(),
                usuario.getRol().name(),
                perfil.getNivelEstudios(),
                carrera == null ? null : carrera.getId(),
                carrera == null ? null : carrera.getNombre(),
                perfil.getAreaLaboral(),
                perfil.getDescripcionPersonal()
        );
    }
}
