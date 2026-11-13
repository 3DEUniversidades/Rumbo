package com.rumbo.dto;
import java.util.List;
public record PaginaHistorialAdministrativoRespuesta(List<RegistroHistorialAdministrativoRespuesta> registros,
        OrigenHistorialAdministrativo origen, int pagina, int totalPaginas, long totalRegistros) {}
