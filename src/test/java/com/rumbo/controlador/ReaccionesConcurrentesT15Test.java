package com.rumbo.controlador;

import com.rumbo.modelo.*;
import com.rumbo.persistencia.*;
import com.rumbo.servicio.GestorPermisos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="spring.sql.init.mode=never")
@AutoConfigureMockMvc @ActiveProfiles("test")
class ReaccionesConcurrentesT15Test {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired PublicacionRepository publicaciones;
    @Autowired PlatformTransactionManager transacciones;
    private String a,b;private Long post;
    private MockHttpSession sesion(String id){MockHttpSession s=new MockHttpSession();s.setAttribute(GestorPermisos.ATRIBUTO_SESION,
            new SesionUsuario(id,"Prueba concurrente",Rol.COLABORADOR,Instant.now().toString()));return s;}
    @BeforeEach void preparar(){
        String prefijo="t15-conc-"+UUID.randomUUID();a=prefijo+"-a";b=prefijo+"-b";
        new TransactionTemplate(transacciones).executeWithoutResult(tx->{
            String hash="$2a$10$TpLF6dwLPJqJiTr25OrXj.GblWwrnBOb1QyFtXs0vr0hjwvld9bji";
            Usuario autor=usuarios.save(new Usuario(a,a,"Autor T15",a+"@example.com",hash,Rol.COLABORADOR,EstadoVerificacion.VERIFICADO));
            usuarios.save(new Usuario(b,b,"Lector T15",b+"@example.com",hash,Rol.COLABORADOR,EstadoVerificacion.VERIFICADO));
            post=publicaciones.saveAndFlush(new Publicacion(autor,"Publicacion concurrente","Contenido",Publicacion.Tipo.EXPERIENCIA,List.of())).getId();
        });
    }
    @AfterEach void limpiar(){new TransactionTemplate(transacciones).executeWithoutResult(tx->{
        if(post!=null){publicaciones.findById(post).ifPresent(publicaciones::delete);publicaciones.flush();}
        if(a!=null)usuarios.findById(a).ifPresent(usuarios::delete);if(b!=null)usuarios.findById(b).ifPresent(usuarios::delete);usuarios.flush();
    });}
    private String ruta(){return "/api/foro/publicaciones/"+post+"/me-gusta";}
    private int peticion(String id,boolean marcar)throws Exception{
        MockHttpServletRequestBuilder peticion=marcar?put(ruta()):delete(ruta());
        return mvc.perform(peticion.session(sesion(id))).andReturn().getResponse().getStatus();
    }
    private void simultaneas(String primero,String segundo,boolean marcar)throws Exception{
        ExecutorService ejecutor=Executors.newFixedThreadPool(2);CountDownLatch listos=new CountDownLatch(2),inicio=new CountDownLatch(1);
        try{
            Callable<Integer> uno=()->{listos.countDown();if(!inicio.await(10,TimeUnit.SECONDS))throw new TimeoutException("Inicio de prueba");return peticion(primero,marcar);};
            Callable<Integer> dos=()->{listos.countDown();if(!inicio.await(10,TimeUnit.SECONDS))throw new TimeoutException("Inicio de prueba");return peticion(segundo,marcar);};
            Future<Integer> f1=ejecutor.submit(uno),f2=ejecutor.submit(dos);assertTrue(listos.await(10,TimeUnit.SECONDS));inicio.countDown();
            assertEquals(200,f1.get(20,TimeUnit.SECONDS).intValue());assertEquals(200,f2.get(20,TimeUnit.SECONDS).intValue());
        }finally{inicio.countDown();ejecutor.shutdownNow();assertTrue(ejecutor.awaitTermination(10,TimeUnit.SECONDS));}
    }
    private void cantidad(int esperada)throws Exception{mvc.perform(get("/api/foro/publicaciones/"+post).session(sesion(a)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.cantidadReacciones").value(esperada));}
    @Test void dobleClickSimultaneoDeUnaCuentaNoDuplicaReaccion()throws Exception{simultaneas(a,a,true);cantidad(1);}
    @Test void dosCuentasSimultaneasNoPierdenReaccionesYAmbasPuedenRetirarlas()throws Exception{simultaneas(a,b,true);cantidad(2);simultaneas(a,b,false);cantidad(0);}
    @Test void retiradasSimultaneasDeLaMismaCuentaSonIdempotentes()throws Exception{assertEquals(200,peticion(a,true));simultaneas(a,a,false);cantidad(0);}
}
