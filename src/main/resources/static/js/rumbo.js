/**
 * Personaje visual compartido. Observa mensajes de la interfaz; no consulta
 * servicios, modifica formularios ni sustituye la lógica de las pantallas.
 */
(() => {
    "use strict";

    const SELECTOR_MENSAJES = [
        '[role="status"]', '[role="alert"]', '.estado-vista',
        '.mensaje-error', '.mensaje-exito', '.mensaje-formulario',
        '.mensaje-gestion', '.mensaje-guardado', '.campo-error',
        'p[id^="estado-"]', 'p[id^="mensaje-"]', 'p[id^="error-"]',
        '#historial-resultados', '#lista-entradas > .texto-secundario',
        'button:disabled', '[aria-busy="true"]'
    ].join(",");

    const TEXTO_ERROR = /\b(no se (?:pudo|pudieron|ha podido)|error|incorrect[oa]s?|invalid[oa]s?|no coincide|no tienes permiso|no puedes)\b/;
    const TEXTO_VALIDACION = /^(?:escribe|selecciona|recarga|intenta nuevamente)\b|\b(?:obligatori[oa]|debe(?:s|n)?)\b/;
    const TEXTO_CARGA = /\b(cargando|guardando|consultando|comprobando|subiendo|generando|preparando|descargando|ingresando|registrando|enviando|actualizando|eliminando|agregando)\b/;
    const TEXTO_VACIO = /\b((?:todavia|aun) no (?:tienes|has|hay)|no (?:hay|se encontraron) (?:entradas|actividades|resultados|notificaciones|publicaciones|registros|solicitudes|decisiones|pruebas|cambios|carreras|mensajes)|sin (?:entradas|resultados|notificaciones|registros))\b/;
    // Se reconocen confirmaciones de la pantalla, nunca el mero clic en Guardar.
    const TEXTO_EXITO = /^(?:(?:perfil|entrada|resultado|cambio|cuenta|foto(?: de perfil)?|solicitud|actividad|carrera|publicacion|respuesta|copia|adjunto|archivo)\b[^.!?]{0,70}\b(?:guardad[oa]|actualizad[oa]|cread[oa]|registrad[oa]|enviad[oa]|agregad[oa]|eliminad[oa]|list[oa])\b|se (?:guardo|actualizo|registro|envio|agrego|elimino)\b|cambios guardados\b|estado registrado\b|decision registrada\b|comparacion generada\b)/;

    function normalizar(texto) {
        return texto.normalize("NFD").replace(/[\u0300-\u036f]/g, "")
            .replace(/\s+/g, " ").trim().toLowerCase();
    }

    function clasificar(elemento, texto) {
        if (!texto) return null;
        if (elemento.matches("button")) {
            return TEXTO_CARGA.test(texto) ? "cargando" : null;
        }
        if (TEXTO_ERROR.test(texto)) return "atencion";
        if (TEXTO_CARGA.test(texto)) return "cargando";
        if (TEXTO_VACIO.test(texto)) return "vacio";
        if (TEXTO_VALIDACION.test(texto)
            || elemento.matches(".mensaje-error, .alerta-error, .campo-error")) return "atencion";
        if (elemento.matches(".mensaje-exito, .alerta-exito") || TEXTO_EXITO.test(texto)) return "confirmado";
        return null;
    }

    function crearPersonaje(tamano) {
        const personaje = document.createElement("span");
        personaje.className = "rumbo-personaje rumbo-personaje--" + tamano;
        personaje.dataset.rumboEstado = "reposo";
        personaje.setAttribute("aria-hidden", "true");
        personaje.innerHTML = '<span class="rumbo-cuerpo"><span class="rumbo-ojo"></span><span class="rumbo-ojo"></span></span><span class="rumbo-sombra"></span>';
        return personaje;
    }

    function iniciar() {
        if (document.documentElement.dataset.rumboInicializado) return;
        document.documentElement.dataset.rumboInicializado = "true";

        const movimientoReducido = window.matchMedia("(prefers-reduced-motion: reduce)");
        const registros = new WeakMap();
        const textosModificados = new WeakSet();
        let principal;
        let revisionPendiente = false;
        let orden = 0;
        let reaccionHasta = 0;
        let saludoHasta = 0;
        let temporizador;

        function crearSaludo(tamano) {
            const boton = document.createElement("button");
            boton.type = "button";
            boton.className = "rumbo-saludo";
            boton.setAttribute("aria-label", "Saludar a Rumbo");
            boton.appendChild(crearPersonaje(tamano));
            boton.addEventListener("click", saludar);
            return boton;
        }

        document.querySelectorAll(".barra-lateral-marca a, [data-rumbo-marca]").forEach(marca => {
            const figura = crearPersonaje("marca");
            const anterior = marca.querySelector("svg");
            if (anterior) anterior.replaceWith(figura);
            else marca.prepend(figura);
        });
        document.querySelectorAll(".barra-superior-movil .marca-nombre").forEach(marca => {
            marca.prepend(crearPersonaje("mini"));
        });

        const acceso = document.querySelector(".pagina-acceso > header");
        const bienvenida = document.getElementById("bienvenida");
        if (acceso) {
            const titulo = acceso.querySelector("h1");
            const marca = document.createElement("div");
            marca.className = "rumbo-marca-acceso";
            const boton = crearSaludo("acceso");
            titulo.before(marca);
            marca.append(boton, titulo);
            principal = boton.firstElementChild;
        } else if (bienvenida) {
            const boton = crearSaludo("inicio");
            bienvenida.classList.add("rumbo-bienvenida");
            bienvenida.appendChild(boton);
            principal = boton.firstElementChild;
        } else {
            const encabezado = document.querySelector(".contenido-principal-app > header");
            if (encabezado) {
                principal = crearPersonaje("encabezado");
                encabezado.classList.add("rumbo-encabezado");
                encabezado.appendChild(principal);
            }
        }
        if (!principal) return;
        principal.dataset.rumboPrincipal = "true";

        function mostrar(estado, animar = false) {
            if (principal.dataset.rumboEstado === estado && !animar) return;
            principal.dataset.rumboEstado = estado;
            document.documentElement.dataset.rumboOcupado = String(
                ["cargando", "saludo", "atencion", "confirmado"].includes(estado)
            );
            delete principal.dataset.rumboMovimiento;
            if (animar && !movimientoReducido.matches && !document.hidden) {
                // Reinicia únicamente el gesto del personaje, sin mover el contenido.
                void principal.offsetWidth;
                principal.dataset.rumboMovimiento = estado;
            }
        }

        function programarRevision() {
            if (revisionPendiente || document.hidden) return;
            revisionPendiente = true;
            window.requestAnimationFrame(() => {
                revisionPendiente = false;
                revisar();
            });
        }

        function visible(elemento) {
            return !elemento.closest('[hidden], [aria-hidden="true"], dialog:not([open])')
                && elemento.getClientRects().length > 0
                && getComputedStyle(elemento).visibility !== "hidden";
        }

        function revisar(inicial = false) {
            const estados = [];
            document.querySelectorAll(SELECTOR_MENSAJES).forEach(elemento => {
                // El historial con artículos contiene texto del usuario, no un aviso.
                const esHistorial = elemento.id === "historial-resultados";
                const ocupado = elemento.getAttribute("aria-busy") === "true";
                const texto = ocupado ? "ocupado" : normalizar(elemento.textContent || "");
                const estado = esHistorial && elemento.childElementCount > 0 ? null
                    : ocupado ? "cargando" : clasificar(elemento, texto);
                const anterior = registros.get(elemento);
                const cambio = !anterior || anterior.texto !== texto || anterior.estado !== estado
                    || textosModificados.has(elemento);
                textosModificados.delete(elemento);
                const registro = cambio ? {
                    texto, estado, orden: ++orden, instante: Date.now(), reaccionado: inicial
                } : anterior;
                registros.set(elemento, registro);

                const seVe = estado && visible(elemento);
                const decorar = seVe && !ocupado && !elemento.matches("button, .campo-error");
                if (decorar) {
                    if (elemento.dataset.rumboIndicador !== estado) elemento.dataset.rumboIndicador = estado;
                } else {
                    delete elemento.dataset.rumboIndicador;
                }
                if (seVe) estados.push(registro);
            });

            if (estados.some(estado => estado.estado === "cargando")) {
                reaccionHasta = 0;
                saludoHasta = 0;
                mostrar("cargando");
                return;
            }
            const ahora = Date.now();
            const reacciones = estados.filter(estado => !estado.reaccionado
                && ["atencion", "confirmado"].includes(estado.estado)
                && ahora - estado.instante < 5000);
            if (reacciones.length) {
                const reciente = reacciones.reduce((a, b) => a.orden > b.orden ? a : b);
                reacciones.forEach(estado => { estado.reaccionado = true; });
                reaccionHasta = ahora + 2400;
                saludoHasta = 0;
                mostrar(reciente.estado, true);
                window.clearTimeout(temporizador);
                temporizador = window.setTimeout(programarRevision, 2450);
            } else if (ahora >= reaccionHasta && ahora >= saludoHasta) {
                mostrar(estados.some(estado => estado.estado === "vacio") ? "vacio" : "reposo");
            }
        }

        function saludar() {
            if (principal.dataset.rumboEstado === "cargando") return;
            reaccionHasta = 0;
            saludoHasta = Date.now() + 950;
            mostrar("saludo", true);
            window.clearTimeout(temporizador);
            temporizador = window.setTimeout(programarRevision, 1000);
        }

        revisar(true);
        if (acceso || bienvenida) {
            const clave = "rumbo.saludo." + (acceso ? "acceso" : "inicio") + ".v1";
            let mostrarSaludo = true;
            try {
                mostrarSaludo = sessionStorage.getItem(clave) !== "visto";
                sessionStorage.setItem(clave, "visto");
            } catch {
                // La preferencia es opcional cuando el navegador restringe el almacenamiento.
            }
            if (mostrarSaludo) saludar();
        }

        const observador = new MutationObserver(cambios => {
            if (cambios.some(cambio => {
                const elemento = cambio.target.nodeType === Node.ELEMENT_NODE
                    ? cambio.target : cambio.target.parentElement;
                return elemento && !elemento.closest(".rumbo-personaje, .rumbo-saludo");
            })) programarRevision();
            cambios.forEach(cambio => {
                if (!["childList", "characterData"].includes(cambio.type)) return;
                const elemento = cambio.target.nodeType === Node.ELEMENT_NODE
                    ? cambio.target : cambio.target.parentElement;
                const aviso = elemento?.closest(SELECTOR_MENSAJES);
                if (aviso) textosModificados.add(aviso);
            });
        });
        observador.observe(document.body, {
            childList: true, characterData: true, subtree: true, attributes: true,
            attributeFilter: ["hidden", "class", "style", "aria-busy", "disabled", "open"]
        });

        document.addEventListener("visibilitychange", () => {
            document.documentElement.dataset.rumboPausado = String(document.hidden);
            if (!document.hidden) programarRevision();
        });
        movimientoReducido.addEventListener("change", () => {
            delete principal.dataset.rumboMovimiento;
            programarRevision();
        });
        window.addEventListener("pagehide", () => {
            observador.disconnect();
            window.clearTimeout(temporizador);
        });
        window.addEventListener("pageshow", evento => {
            if (evento.persisted) {
                observador.observe(document.body, {
                    childList: true, characterData: true, subtree: true, attributes: true,
                    attributeFilter: ["hidden", "class", "style", "aria-busy", "disabled", "open"]
                });
                document.documentElement.dataset.rumboPausado = String(document.hidden);
                programarRevision();
            }
        });
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", iniciar, { once: true });
    } else {
        iniciar();
    }
})();
