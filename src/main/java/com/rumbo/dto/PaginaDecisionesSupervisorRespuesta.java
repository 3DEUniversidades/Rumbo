package com.rumbo.dto;
import java.util.List;
public record PaginaDecisionesSupervisorRespuesta(List<DecisionHistorialSupervisorRespuesta> decisiones,
        int pagina,int totalPaginas,long totalDecisiones) {}
