package com.rumbo.dto;
import java.util.List;
public record PaginaRegistrosAdministracionRespuesta(List<RegistroAdministracionRespuesta> registros,
        int pagina, int totalPaginas, long totalRegistros) {}
