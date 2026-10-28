package com.rumbo.servicio;

import com.rumbo.modelo.*;
import com.rumbo.persistencia.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!test")
public class InicializadorPruebasVocacionales implements ApplicationRunner {
    private final ActividadRepository actividades;
    private final PruebaVocacionalRepository pruebas;
    public InicializadorPruebasVocacionales(ActividadRepository actividades, PruebaVocacionalRepository pruebas) {
        this.actividades = actividades; this.pruebas = pruebas;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments argumentos) {
        Actividad intereses = actividades.findById("act-0001").orElseGet(() -> actividades.save(new Actividad(
                "act-0001", "2026-10-26", "Explorar intereses vocacionales", "Reconocer actividades que disfrutas",
                "Consulta el perfil externo y registra tus resultados", "Realizar la prueba fuera de Rumbo",
                "Perfil de intereses", "Sitio oficial O*NET", "Areas de interes y reflexiones", "Prueba de intereses")));
        Actividad habilidades = actividades.findById("act-0003").orElseGet(() -> actividades.save(new Actividad(
                "act-0003", "2026-10-26", "Explorar habilidades academicas", "Conocer fortalezas y areas de mejora",
                "Consulta las condiciones institucionales y registra resultados oficiales si ya los tienes",
                "Consultar recursos de preparacion y el proceso externo", "Prueba estandarizada externa",
                "Portal oficial de admisiones", "Resultados y reflexiones personales", "Prueba de habilidades")));

        if (!pruebas.existsById("prueba-onet-intereses")) {
            pruebas.save(new PruebaVocacional("prueba-onet-intereses", intereses,
                    "Perfil de intereses O*NET", TipoPruebaVocacional.INTERESES, "Centro O*NET / Mi Proximo Paso",
                    "Explorar intereses laborales y opciones profesionales a partir de las preferencias personales.",
                    "Realizala con tiempo para reflexionar, sin buscar respuestas correctas. Los intereses no son lo mismo que las habilidades.",
                    "Abre el sitio oficial en espanol. Sigue sus instrucciones y registra aqui el perfil o las areas que te muestre al finalizar.",
                    "Cuestionario externo de intereses; Rumbo no copia preguntas ni calcula puntuaciones.",
                    "https://onetinterestprofiler.org/es/", "https://www.miproximopaso.org/",
                    "Usa la explicacion del proveedor para explorar tus areas de interes. Compara despues esas preferencias con los planes de estudio y requisitos de las carreras de Rumbo. Las ocupaciones externas no equivalen automaticamente a carreras UVG.",
                    "Herramienta externa en espanol. Sigue sus condiciones y politica de privacidad. Rumbo no recibe los resultados automaticamente."));
        }
        if (!pruebas.existsById("prueba-paa-habilidades")) {
            pruebas.save(new PruebaVocacional("prueba-paa-habilidades", habilidades,
                    "PAA: habilidades academicas", TipoPruebaVocacional.HABILIDADES, "College Board / proceso de admision UVG",
                    "Consultar una evaluacion estandarizada externa de capacidades y habilidades academicas para el ingreso universitario.",
                    "Es una prueba de admision, no un test que elige una carrera. Utilizala como una fuente adicional de informacion sobre tu preparacion academica.",
                    "Consulta la informacion oficial de PAA y el portal UVG. Revisa requisitos, fechas y condiciones. Registra resultados reales solo cuando la institucion te los haya entregado; una guia de practica no equivale a un resultado oficial.",
                    "Prueba estandarizada administrada externamente por la institucion; Rumbo no la aplica ni la califica.",
                    "https://admisiones.uvg.edu.gt/", "https://latam.collegeboard.org/paa/",
                    "Consulta tu reporte y las indicaciones de la institucion para reconocer fortalezas y necesidades de preparacion. No conviertas un puntaje en una recomendacion automatica de carrera; considera tambien intereses, plan de estudios y orientacion institucional.",
                    "Requiere el proceso y calendario de la institucion; puede tener requisitos y costos. No es una prueba gratuita inmediata dentro de Rumbo. Informacion UVG: https://www.uvg.edu.gt/admisiones/preguntas-frecuentes/"));
        }
        // No se modifica el contenido existente ni se reactivan actividades eliminadas.
    }
}
