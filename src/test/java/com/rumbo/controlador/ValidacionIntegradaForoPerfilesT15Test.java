package com.rumbo.controlador;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rumbo.AlmacenamientoEnMemoria;
import com.rumbo.ConfiguracionAlmacenamientoPrueba;
import com.rumbo.dto.CrearPublicacionSolicitud;
import com.rumbo.modelo.*;
import com.rumbo.persistencia.*;
import com.rumbo.servicio.AlmacenamientoArchivos;
import com.rumbo.servicio.GestorPermisos;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="spring.sql.init.mode=never")
@AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
@Import(ConfiguracionAlmacenamientoPrueba.class)
class ValidacionIntegradaForoPerfilesT15Test {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioRepository usuarios;
    @Autowired PerfilRepository perfiles;
    @Autowired PublicacionRepository publicaciones;
    @Autowired SolicitudVerificacionRepository verificaciones;
    @Autowired SolicitudActualizacionPerfilRepository solicitudes;
    @Autowired AlmacenamientoArchivos almacenamiento;
    @Autowired EntityManager em;
    private static final String HASH="$2a$10$TpLF6dwLPJqJiTr25OrXj.GblWwrnBOb1QyFtXs0vr0hjwvld9bji";
    private static final byte[] JPEG={(byte)0xff,(byte)0xd8,(byte)0xff,(byte)0xe0,0,0x10,0x4a,0x46,0x49,0x46,0,1};
    private String id(Rol rol){return "t15-"+rol.name();}
    private MockHttpSession sesion(String id,Rol rol){MockHttpSession s=new MockHttpSession();s.setAttribute(GestorPermisos.ATRIBUTO_SESION,
            new SesionUsuario(id,"Prueba T15",rol,Instant.now().toString()));return s;}
    private MockHttpSession sesion(Rol rol){return sesion(id(rol),rol);}
    @BeforeEach void preparar(){
        for(Rol rol:Rol.values())usuarios.save(new Usuario(id(rol),"t15_"+rol.name().toLowerCase(java.util.Locale.ROOT),
                "Prueba "+rol,id(rol)+"@example.com",HASH,rol,EstadoVerificacion.VERIFICADO));
        usuarios.save(new Usuario("t15-pendiente","t15_pendiente","Estudiante pendiente","t15-pendiente@example.com",HASH,Rol.ESTUDIANTE,EstadoVerificacion.NO_VERIFICADO));
        for(Rol rol:new Rol[]{Rol.ESTUDIANTE,Rol.COLABORADOR}){
            Perfil p=new Perfil(usuarios.findById(id(rol)).orElseThrow());p.setDescripcionPersonal("Descripcion publica");
            p.setNivelEstudios("Dato privado de estudios");p.setAreaLaboral("Dato privado laboral");perfiles.save(p);
        }
        em.flush();em.clear();
    }
    private String foto(String id,Rol rol,String idAjeno)throws Exception{
        var peticion=multipart("/api/perfil/foto").file(new MockMultipartFile("archivo","demo.jpg","image/jpeg",JPEG)).session(sesion(id,rol));
        if(idAjeno!=null)peticion.param("idUsuario",idAjeno);
        String cuerpo=mvc.perform(peticion).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(cuerpo).get("fotoUrl").asText();
    }
    private Long crearPublicacion(Rol rol)throws Exception{
        String cuerpo=mvc.perform(post("/api/foro/publicaciones").session(sesion(rol)).contentType(APPLICATION_JSON)
                .content(json.writeValueAsString(new CrearPublicacionSolicitud("Publicacion T15","Contenido de demostracion",
                        rol==Rol.ESTUDIANTE?"DUDA":"EXPERIENCIA",List.of()))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(cuerpo).get("id").asLong();
    }
    @ParameterizedTest @EnumSource(value=Rol.class,names={"ESTUDIANTE","COLABORADOR"})
    void reemplazaFotoPublicaYRetiraLaAnteriorSinExponerDatosPrivados(Rol propietario)throws Exception{
        String primera=foto(id(propietario),propietario,null);
        String publicIdAnterior=perfiles.findByUsuarioIdUsuario(id(propietario)).orElseThrow().getFotoPublicId();
        String segunda=foto(id(propietario),propietario,null);assertNotEquals(primera,segunda);
        assertFalse(((AlmacenamientoEnMemoria)almacenamiento).contiene(publicIdAnterior));
        String respuesta=mvc.perform(get("/api/perfil/publico/"+id(propietario)).session(sesion(Rol.ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fotoUrl").value(segunda))
                .andReturn().getResponse().getContentAsString();
        Set<String> campos=new HashSet<>();json.readTree(respuesta).fieldNames().forEachRemaining(campos::add);
        assertEquals(Set.of("idUsuario","nombreUsuario","nombreCompleto","descripcionPersonal","fotoUrl"),campos);
        for(int i=0;i<2;i++)mvc.perform(delete("/api/perfil/foto").session(sesion(propietario))).andExpect(status().isOk());
        mvc.perform(get("/api/perfil/publico/"+id(propietario)).session(sesion(Rol.ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fotoUrl").isEmpty());
    }
    @Test void parametrosAjenosNoPermitenCambiarNiEliminarLaFotoDeOtraCuenta()throws Exception{
        String fotoColaborador=foto(id(Rol.COLABORADOR),Rol.COLABORADOR,null);
        String fotoEstudiante=foto(id(Rol.ESTUDIANTE),Rol.ESTUDIANTE,id(Rol.COLABORADOR));
        assertNotEquals(fotoColaborador,fotoEstudiante);
        mvc.perform(delete("/api/perfil/foto").param("idUsuario",id(Rol.COLABORADOR)).session(sesion(Rol.ESTUDIANTE)))
                .andExpect(status().isOk());em.flush();em.clear();
        assertEquals(fotoColaborador,perfiles.findByUsuarioIdUsuario(id(Rol.COLABORADOR)).orElseThrow().getFotoUrl());
        assertNull(perfiles.findByUsuarioIdUsuario(id(Rol.ESTUDIANTE)).orElseThrow().getFotoUrl());
    }
    @Test void perfilPublicoYListadoDelAutorCoincidenConSusAportesVisibles()throws Exception{
        String foto=foto(id(Rol.COLABORADOR),Rol.COLABORADOR,null);Long idPublicacion=crearPublicacion(Rol.COLABORADOR);
        crearPublicacion(Rol.ESTUDIANTE);
        Publicacion retirada=publicaciones.save(new Publicacion(usuarios.findById(id(Rol.COLABORADOR)).orElseThrow(),
                "Retirada","No mostrar",Publicacion.Tipo.EXPERIENCIA,List.of()));retirada.setEstado(Publicacion.Estado.ELIMINADA);em.flush();em.clear();
        mvc.perform(get("/api/perfil/publico/"+id(Rol.COLABORADOR)).session(sesion(Rol.ESTUDIANTE)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fotoUrl").value(foto));
        mvc.perform(get("/api/foro/publicaciones").param("autor",id(Rol.COLABORADOR)).session(sesion(Rol.ESTUDIANTE)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(idPublicacion.intValue()))
                .andExpect(jsonPath("$[0].autor.idUsuario").value(id(Rol.COLABORADOR)));
    }
    @Test void contadoresYReaccionPersonalSonConsistentesEntreDosCuentas()throws Exception{
        Long post=crearPublicacion(Rol.COLABORADOR);String ruta="/api/foro/publicaciones/"+post;
        for(Rol rol:new Rol[]{Rol.ESTUDIANTE,Rol.COLABORADOR}){
            mvc.perform(put(ruta+"/me-gusta").session(sesion(rol))).andExpect(status().isOk());
            mvc.perform(post(ruta+"/respuestas").session(sesion(rol)).contentType(APPLICATION_JSON)
                    .content("{\"contenido\":\"Respuesta de demostracion\"}")).andExpect(status().isCreated());
        }
        mvc.perform(delete(ruta+"/me-gusta").session(sesion(Rol.ESTUDIANTE))).andExpect(status().isOk());
        em.flush();em.clear();
        for(Rol rol:new Rol[]{Rol.ESTUDIANTE,Rol.COLABORADOR}){
            mvc.perform(get(ruta).session(sesion(rol))).andExpect(status().isOk())
                    .andExpect(jsonPath("$.cantidadRespuestas").value(2)).andExpect(jsonPath("$.cantidadReacciones").value(1))
                    .andExpect(jsonPath("$.meGusta").value(rol==Rol.COLABORADOR));
            mvc.perform(get("/api/foro/publicaciones").param("autor",id(Rol.COLABORADOR)).session(sesion(rol)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$[0].cantidadRespuestas").value(2))
                    .andExpect(jsonPath("$[0].cantidadReacciones").value(1));
        }
    }
    @Test void aprobarVerificacionHabilitaForoYQuedaEnHistorialPersonal()throws Exception{
        String solicitud="t15-verificacion";verificaciones.saveAndFlush(new SolicitudVerificacion(solicitud,"t15-pendiente","pendiente",Instant.now().toString()));
        mvc.perform(get("/api/foro/acceso").session(sesion("t15-pendiente",Rol.ESTUDIANTE))).andExpect(status().isForbidden());
        mvc.perform(put("/api/supervision/usuarios/verificaciones/"+solicitud).session(sesion(Rol.SUPERVISOR_DE_USUARIOS))
                .contentType(APPLICATION_JSON).content("{\"decision\":\"APROBAR\",\"motivo\":\"Datos comprobados\"}"))
                .andExpect(status().isNoContent());em.flush();em.clear();
        mvc.perform(get("/api/foro/acceso").session(sesion("t15-pendiente",Rol.ESTUDIANTE))).andExpect(status().isOk());
        mvc.perform(get("/api/perfil/mis-decisiones").session(sesion(Rol.SUPERVISOR_DE_USUARIOS)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalDecisiones").value(1))
                .andExpect(jsonPath("$.decisiones[0].idSolicitud").value(solicitud));
    }
    @ParameterizedTest @ValueSource(strings={"APROBAR","RECHAZAR"})
    void decisionDeCambioDePerfilSeReflejaEnDatosPublicosYEnHistorial(String decision)throws Exception{
        String solicitud="t15-cambio-perfil";Usuario original=usuarios.findById(id(Rol.ESTUDIANTE)).orElseThrow();
        String nombreAnterior=original.getNombreCompleto();
        solicitudes.saveAndFlush(new SolicitudActualizacionPerfil(solicitud,original.getIdUsuario(),"t15_nombre_actualizado",
                "Nombre actualizado T15","pendiente",Instant.now().toString()));
        mvc.perform(put("/api/supervision/usuarios/perfiles/"+solicitud).session(sesion(Rol.SUPERVISOR_DE_USUARIOS))
                .contentType(APPLICATION_JSON).content("{\"decision\":\""+decision+"\",\"motivo\":\"Revision T15\"}"))
                .andExpect(status().isNoContent());em.flush();em.clear();
        mvc.perform(get("/api/perfil/publico/"+id(Rol.ESTUDIANTE)).session(sesion(Rol.COLABORADOR)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombreCompleto").value(decision.equals("APROBAR")?"Nombre actualizado T15":nombreAnterior))
                .andExpect(jsonPath("$.correo").doesNotExist());
        mvc.perform(get("/api/perfil/mis-decisiones?tipo=PERFIL").session(sesion(Rol.SUPERVISOR_DE_USUARIOS)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.decisiones[0].decision").value(decision));
    }
    @ParameterizedTest @EnumSource(value=Rol.class,names={"SUPERVISOR_DE_USUARIOS","SUPERVISOR_DE_MENSAJES","ENCARGADO_DE_CARRERAS"})
    void rolesAdministrativosVerificadosNoAdquierenPermisosDeParticipacionEnForo(Rol rol)throws Exception{
        Long post=crearPublicacion(Rol.COLABORADOR);
        mvc.perform(get("/api/foro/acceso").session(sesion(rol))).andExpect(status().isForbidden());
        mvc.perform(put("/api/foro/publicaciones/"+post+"/me-gusta").session(sesion(rol))).andExpect(status().isForbidden());
        mvc.perform(post("/api/foro/publicaciones/"+post+"/respuestas").session(sesion(rol)).contentType(APPLICATION_JSON)
                .content("{\"contenido\":\"No permitido\"}")).andExpect(status().isForbidden());
    }
    @Test void rechazaFotoGrandeYContenidoFalsoSinSobrescribirLaFotoVigente()throws Exception{
        String previa=foto(id(Rol.ESTUDIANTE),Rol.ESTUDIANTE,null);byte[] grande=new byte[5*1024*1024+1];System.arraycopy(JPEG,0,grande,0,JPEG.length);
        for(MockMultipartFile f:new MockMultipartFile[]{new MockMultipartFile("archivo","grande.jpg","image/jpeg",grande),
                new MockMultipartFile("archivo","falsa.jpg","image/jpeg","texto que no es una imagen".getBytes(java.nio.charset.StandardCharsets.UTF_8))}){
            mvc.perform(multipart("/api/perfil/foto").file(f).session(sesion(Rol.ESTUDIANTE))).andExpect(status().isBadRequest());
            assertEquals(previa,perfiles.findByUsuarioIdUsuario(id(Rol.ESTUDIANTE)).orElseThrow().getFotoUrl());
        }
    }
}
