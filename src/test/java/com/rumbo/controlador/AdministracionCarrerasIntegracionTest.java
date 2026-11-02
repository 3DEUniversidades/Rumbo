package com.rumbo.controlador;

import com.rumbo.modelo.*;
import com.rumbo.persistencia.*;
import com.rumbo.servicio.GestorPermisos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="spring.sql.init.mode=never")
@AutoConfigureMockMvc @ActiveProfiles("test")
class AdministracionCarrerasIntegracionTest {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired CarreraRepository carreras;
    @Autowired RegistroActualizacionCarreraRepository registros;
    @Autowired PlatformTransactionManager transacciones;
    private Long carreraId;
    private String id(Rol rol){return "validacarrera-"+rol.name();}
    private MockHttpSession sesion(Rol rol){MockHttpSession s=new MockHttpSession();s.setAttribute(GestorPermisos.ATRIBUTO_SESION,
            new SesionUsuario(id(rol),"Prueba",rol,"2026-11-02T00:00:00Z"));return s;}
    @BeforeEach void preparar(){
        new TransactionTemplate(transacciones).executeWithoutResult(tx->{
            for(Rol rol:Rol.values())usuarios.save(new Usuario(id(rol),"validacarrera_"+rol.name().toLowerCase(java.util.Locale.ROOT),
                    "Prueba "+rol,rol.name().toLowerCase(java.util.Locale.ROOT)+"-carrera@example.com",
                    "$2a$10$TpLF6dwLPJqJiTr25OrXj.GblWwrnBOb1QyFtXs0vr0hjwvld9bji",rol,EstadoVerificacion.VERIFICADO));
            Carrera c=new Carrera("Carrera de demostracion",5,new BigDecimal("1000.00"));c.setDescripcion("Descripcion original");carreraId=carreras.saveAndFlush(c).getId();
        });
    }
    @AfterEach void limpiar(){
        new TransactionTemplate(transacciones).executeWithoutResult(tx->{
            if(carreraId!=null){registros.deleteAll(registros.findByCarreraIdOrderByFechaDesc(carreraId));registros.flush();carreras.deleteById(carreraId);carreras.flush();}
            for(Rol rol:Rol.values())usuarios.findById(id(rol)).ifPresent(usuarios::delete);
            usuarios.flush();
        });
    }
    @ParameterizedTest @EnumSource(value=Rol.class,names={"ADMIN","ENCARGADO_DE_CARRERAS"})
    void ambosRolesActualizanConHistorialYResponsableReal(Rol rol)throws Exception{
        mvc.perform(put("/api/carreras/"+carreraId).session(sesion(rol)).header("X-Usuario-Id","otro-usuario")
                .contentType(APPLICATION_JSON).content("{\"descripcion\":\"Descripcion actualizada\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.descripcion").value("Descripcion actualizada"));
        var cambios=registros.findByCarreraIdOrderByFechaDesc(carreraId);assertEquals(1,cambios.size());
        assertEquals(id(rol),cambios.get(0).getResponsable());assertEquals("Descripcion original",cambios.get(0).getValorAnterior());
        if(rol==Rol.ADMIN){mvc.perform(get("/api/admin/historial?origen=CARRERAS").session(sesion(rol)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.registros[0].valorNuevo").value("Descripcion actualizada"));}
    }
    @ParameterizedTest @EnumSource(value=Rol.class,names={"ADMIN","ENCARGADO_DE_CARRERAS"})
    void compartenValidacionesYErroresNoGuardanCambiosParciales(Rol rol)throws Exception{
        for(String dato:new String[]{"{\"nombre\":\"\"}","{\"duracion\":-1}","{\"costo\":-1}",
                "{\"pdfOficialUrl\":\"javascript:alert(1)\"}","{\"campoDesconocido\":\"valor\"}",
                "{\"descripcion\":\"Cambio que debe revertirse\",\"duracion\":-1}"}){
            mvc.perform(put("/api/carreras/"+carreraId).session(sesion(rol)).contentType(APPLICATION_JSON).content(dato))
                    .andExpect(status().isBadRequest());
            assertEquals("Descripcion original",carreras.findById(carreraId).orElseThrow().getDescripcion());
            assertEquals(0,registros.findByCarreraIdOrderByFechaDesc(carreraId).size());
        }
    }
    @Test void noGeneraAuditoriaCuandoNoHayCambiosYNingunoEditaCarreraInexistente()throws Exception{
        mvc.perform(put("/api/carreras/"+carreraId).session(sesion(Rol.ADMIN)).contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());assertEquals(0,registros.findByCarreraIdOrderByFechaDesc(carreraId).size());
        mvc.perform(put("/api/carreras/999999999").session(sesion(Rol.ADMIN)).contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
    }
    @ParameterizedTest @EnumSource(value=Rol.class,names={"ADMIN","ENCARGADO_DE_CARRERAS"},mode=EnumSource.Mode.EXCLUDE)
    void otrosRolesNoActualizan(Rol rol)throws Exception{
        mvc.perform(put("/api/carreras/"+carreraId).session(sesion(rol)).contentType(APPLICATION_JSON).content("{\"descripcion\":\"No permitido\"}"))
                .andExpect(status().isForbidden());assertEquals("Descripcion original",carreras.findById(carreraId).orElseThrow().getDescripcion());
    }
    @Test void administradorEliminadoNoActualizaYNadieEditaSinSesion()throws Exception{
        mvc.perform(put("/api/carreras/"+carreraId).contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        new TransactionTemplate(transacciones).executeWithoutResult(tx->{Usuario u=usuarios.findById(id(Rol.ADMIN)).orElseThrow();u.eliminarCuenta();usuarios.save(u);});
        mvc.perform(put("/api/carreras/"+carreraId).session(sesion(Rol.ADMIN)).contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
