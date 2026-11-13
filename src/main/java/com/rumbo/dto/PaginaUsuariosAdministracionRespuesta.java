package com.rumbo.dto;
import java.util.List;
public record PaginaUsuariosAdministracionRespuesta(List<UsuarioAdministracionResumen> usuarios,
        int pagina, int tamano, long totalUsuarios, int totalPaginas) {}
