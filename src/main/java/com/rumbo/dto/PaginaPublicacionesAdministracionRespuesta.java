package com.rumbo.dto;
import java.util.List;
public record PaginaPublicacionesAdministracionRespuesta(List<PublicacionAdministracionRespuesta> publicaciones,
        int pagina, int totalPaginas, long totalPublicaciones) {}
