document.addEventListener('DOMContentLoaded', async function () {
    const lista = document.getElementById('lista-actividades');
    const hitos = document.getElementById('hitos-actividades');
    const recorrido = document.getElementById('recorrido-desplazable');
    const panel = document.getElementById('panel-actividad');
    const mensaje = document.getElementById('mensaje-linea-tiempo');
    const resumen = document.getElementById('resumen-progreso');
    const reintentar = document.getElementById('reintentar-carga');
    const vistas = new Map();
    const nodos = new Map();
    let actividades = [];
    let actividadesConPrueba = new Set();
    let seleccionada = null;
    let entradasBitacora = null;
    let errorBitacora = '';
    const nombresEstado = {
        PENDIENTE: 'Pendiente', COMPLETADA: 'Completada',
        RECHAZADA: 'Decidiste no realizarla', NO_REALIZADA: 'No realizada'
    };

    reintentar.addEventListener('click', cargarDatos);
    window.addEventListener('resize', actualizarAyudaRecorrido);
    document.getElementById('volver-al-recorrido').addEventListener('click', () => {
        nodos.get(seleccionada)?.boton.focus({ preventScroll: true });
        document.querySelector('.recorrido').scrollIntoView({ block: 'start', behavior: movimientoPermitido() });
    });
    try {
        const sesion = await obtenerSesion();
        if (!sesion) return;
        if (sesion.rol !== 'ESTUDIANTE') {
            mensaje.textContent = 'Esta sección está disponible para estudiantes.';
            resumen.textContent = 'Progreso no disponible.';
            return;
        }
        await cargarDatos();
    } catch (error) {
        mensaje.textContent = 'No se pudo comprobar la sesión.';
        resumen.textContent = 'Recarga la página para intentarlo nuevamente.';
    }

    function crear(etiqueta, clase, texto) {
        const elemento = document.createElement(etiqueta);
        if (clase) elemento.className = clase;
        if (texto != null) elemento.textContent = texto;
        return elemento;
    }
    function movimientoPermitido() {
        return window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth';
    }
    function actualizarAyudaRecorrido() {
        document.getElementById('ayuda-recorrido-largo').hidden = recorrido.hidden
            || recorrido.scrollWidth <= recorrido.clientWidth + 1
            || window.matchMedia('(max-width: 640px)').matches;
    }
    function nombreEstado(actividad) {
        return nombresEstado[actividad.estado] ?? actividad.estado;
    }
    // Las fechas sin zona conservan el día y la hora enviados por el servidor.
    function formatearFecha(valor, incluirHora = false, corta = false) {
        if (!valor) return 'Sin registro';
        const partes = String(valor).match(/^(\d{4})-(\d{2})-(\d{2})(?:[T ](\d{2}):(\d{2}))?/);
        if (!partes) return String(valor);
        const fecha = new Date(Number(partes[1]), Number(partes[2]) - 1,
            Number(partes[3]), Number(partes[4] ?? 12), Number(partes[5] ?? 0));
        if (Number.isNaN(fecha.getTime())) return String(valor);
        return new Intl.DateTimeFormat('es-GT', {
            day: 'numeric', month: 'short', ...(corta ? {} : { year: 'numeric' }),
            ...(incluirHora ? { hour: '2-digit', minute: '2-digit' } : {})
        }).format(fecha);
    }
    async function comprobarRespuesta(respuesta, alternativa) {
        if (respuesta.status === 401) {
            window.location.replace('/login.html');
            throw new Error('La sesión terminó. Inicia sesión nuevamente.');
        }
        if (respuesta.status === 403) throw new Error('No tienes permiso para realizar esta operación.');
        if (!respuesta.ok) {
            const error = await respuesta.json().catch(() => ({}));
            const detalle = error.campos ? Object.values(error.campos).join(' ') : error.error;
            throw new Error(detalle || alternativa);
        }
        return respuesta;
    }
    async function consultarJson(ruta) {
        const respuesta = await fetch(ruta, { credentials: 'same-origin', cache: 'no-store' });
        await comprobarRespuesta(respuesta, 'No se pudieron consultar los datos.');
        return respuesta.json();
    }
    async function enviar(ruta, metodo, contenido) {
        const respuesta = await fetch(ruta, {
            method: metodo, credentials: 'same-origin',
            headers: contenido === undefined ? {} : { 'Content-Type': 'application/json' },
            body: contenido === undefined ? undefined : JSON.stringify(contenido)
        });
        return comprobarRespuesta(respuesta, 'No se pudo guardar el cambio.');
    }

    async function cargarDatos() {
        reintentar.hidden = true;
        reintentar.disabled = true;
        mensaje.textContent = 'Cargando actividades...';
        resumen.textContent = 'Cargando progreso...';
        entradasBitacora = null;
        errorBitacora = '';
        // La bitácora puede fallar sin impedir consultar o guardar las actividades.
        const consultaBitacora = consultarJson('/api/bitacora')
            .then(entradas => ({ entradas })).catch(error => ({ error }));
        try {
            const [disponibles, progreso, pruebas] = await Promise.all([
                consultarJson('/api/mi-progreso/actividades'),
                consultarJson('/api/mi-progreso/resumen'),
                consultarJson('/api/pruebas-vocacionales').catch(() => [])
            ]);
            actividades = disponibles;
            actividadesConPrueba = new Set(pruebas.map(prueba => prueba.idActividad));
            vistas.clear(); nodos.clear(); lista.replaceChildren(); hitos.replaceChildren();
            recorrido.style.setProperty('--cantidad-hitos', actividades.length || 1);
            actividades.forEach((actividad, indice) => {
                crearHito(actividad, indice);
                const vista = crearTarjeta(actividad, indice);
                vistas.set(actividad.idActividad, vista);
                lista.append(vista.tarjeta);
            });
            actualizarProgreso(progreso);
            const hayActividades = actividades.length > 0;
            recorrido.hidden = !hayActividades;
            panel.hidden = !hayActividades;
            document.getElementById('leyenda-recorrido').hidden = !hayActividades;
            actualizarAyudaRecorrido();
            mensaje.textContent = hayActividades ? '' : 'Todavía no hay actividades disponibles.';
            if (hayActividades) {
                const solicitada = new URLSearchParams(window.location.search).get('actividad');
                const inicial = actividades.find(a => a.idActividad === (solicitada || seleccionada))
                    ?? actividades.find(a => a.estado === 'PENDIENTE')
                    ?? actividades.find(a => a.estado !== 'COMPLETADA')
                    ?? actividades[actividades.length - 1];
                seleccionarActividad(inicial.idActividad, false);
            }
            const bitacora = await consultaBitacora;
            if (entradasBitacora === null) {
                if (bitacora.error) errorBitacora = 'No se pudieron consultar las entradas de tu bitácora. Puedes volver a intentarlo al guardar una constancia.';
                else entradasBitacora = bitacora.entradas;
            }
            pintarEntradas();
        } catch (error) {
            lista.replaceChildren(); hitos.replaceChildren();
            recorrido.hidden = true; panel.hidden = true;
            document.getElementById('leyenda-recorrido').hidden = true;
            document.getElementById('ayuda-recorrido-largo').hidden = true;
            resumen.textContent = 'Progreso no disponible.';
            mensaje.textContent = error.message;
            reintentar.hidden = false;
        } finally { reintentar.disabled = false; }
    }
    function actualizarProgreso(progreso) {
        resumen.replaceChildren(crear('strong', '', progreso.actividadesCompletadas), document.createTextNode(' '),
            crear('span', '', `de ${progreso.totalActividades} actividades completadas`));
        document.querySelector('.recorrido-fin').dataset.completo = String(
            progreso.totalActividades > 0 && progreso.actividadesCompletadas === progreso.totalActividades);
    }
    function crearHito(actividad, indice) {
        const elemento = crear('li', 'recorrido-hito');
        const fecha = crear('time', 'recorrido-hito-fecha', formatearFecha(actividad.fecha, false, true));
        fecha.dateTime = actividad.fecha;
        const boton = crear('button', 'recorrido-hito-boton', indice + 1);
        boton.type = 'button';
        boton.setAttribute('aria-controls', `actividad-detalle-${indice + 1}`);
        const etiqueta = crear('div', 'recorrido-hito-etiqueta');
        const estado = crear('span', 'recorrido-hito-estado');
        etiqueta.append(crear('span', 'recorrido-hito-nombre', actividad.descripcion), estado);
        elemento.append(fecha, boton, etiqueta);
        nodos.set(actividad.idActividad, { elemento, boton, estado, numero: indice + 1 });
        actualizarHito(actividad);
        boton.addEventListener('click', () => seleccionarActividad(actividad.idActividad, true));
        hitos.append(elemento);
    }
    function actualizarHito(actividad) {
        const nodo = nodos.get(actividad.idActividad);
        if (!nodo) return;
        nodo.elemento.dataset.estado = actividad.estado;
        nodo.estado.textContent = (actividad.estado === 'COMPLETADA' ? '✓ ' : '') + nombreEstado(actividad);
        nodo.boton.setAttribute('aria-label', `Actividad ${nodo.numero}: ${actividad.descripcion}. ${nombreEstado(actividad)}`);
    }
    function seleccionarActividad(id, enfocar) {
        if (!vistas.has(id)) return;
        seleccionada = id;
        // Ocultar conserva los formularios y sus borradores al recorrer otros hitos.
        for (const [idActividad, vista] of vistas) {
            vista.tarjeta.hidden = idActividad !== id;
            nodos.get(idActividad).boton.setAttribute('aria-pressed', String(idActividad === id));
        }
        if (enfocar) {
            vistas.get(id).titulo.focus({ preventScroll: true });
            panel.scrollIntoView({ block: 'start', behavior: movimientoPermitido() });
        }
    }
    function aplicarActividad(actualizada, opciones) {
        const indice = actividades.findIndex(a => a.idActividad === actualizada.idActividad);
        if (indice >= 0) actividades[indice] = actualizada;
        vistas.get(actualizada.idActividad)?.actualizar(actualizada, opciones);
        actualizarHito(actualizada);
    }
    function pintarEntradas() {
        for (const vista of vistas.values()) vista.pintarEntradas();
    }
    function textoDecision(actividad) {
        return actividad.decidioRealizar == null ? 'Todavía no has respondido'
            : actividad.decidioRealizar ? 'Sí realizaré la actividad' : 'No realizaré la actividad';
    }
    function contenidoConstancia(actividad) {
        return ['Seguimiento de mi proceso vocacional', `Actividad: ${actividad.descripcion}`,
            `Fecha programada: ${formatearFecha(actividad.fecha)}`, `Estado al guardar: ${nombreEstado(actividad)}`,
            `Última actualización de la actividad: ${formatearFecha(actividad.fechaActualizacion, true)}`,
            `Mi decisión: ${textoDecision(actividad)}`, '', 'Razón de mi decisión:',
            actividad.razonDecision?.trim() || 'Sin razón registrada.', '', 'Resultados y anotaciones:',
            actividad.resultadosRegistrados?.trim() || 'Sin resultados registrados.'].join('\n');
    }
    function agregarDato(contenedor, etiqueta, valor, convertirEnlaces = false) {
        const fila = crear('div');
        const contenido = crear('dd', '', valor == null || String(valor).trim() === '' ? 'Sin información registrada.' : String(valor));
        if (convertirEnlaces) agregarEnlaces(contenido);
        fila.append(crear('dt', '', etiqueta), contenido);
        contenedor.append(fila);
        return contenido;
    }

    function agregarEnlaces(contenedor) {
        const texto = contenedor.textContent;
        const direcciones = /https?:\/\/[^\s<>"']+/gi;
        let posicion = 0;
        contenedor.replaceChildren();

        for (const coincidencia of texto.matchAll(direcciones)) {
            const direccion = coincidencia[0].replace(/[.,;:!?)\]}]+$/, '');

            try {
                new URL(direccion);
            } catch (error) {
                continue;
            }

            contenedor.append(document.createTextNode(texto.slice(posicion, coincidencia.index)));

            const enlace = document.createElement('a');
            enlace.href = direccion;
            enlace.textContent = direccion;
            enlace.target = '_blank';
            enlace.rel = 'noopener noreferrer';
            contenedor.appendChild(enlace);
            posicion = coincidencia.index + direccion.length;
        }

        contenedor.append(document.createTextNode(texto.slice(posicion)));
    }

    function crearTarjeta(inicial, indice) {
        let actividad = inicial;
        const tarjeta = crear('article', 'actividad-card actividad-detalle');
        tarjeta.id = `actividad-detalle-${indice + 1}`;
        tarjeta.hidden = true;
        const cabecera = crear('div', 'actividad-detalle-cabecera');
        const nombre = crear('div');
        nombre.append(crear('p', 'actividad-detalle-numero', `Actividad ${indice + 1} de ${actividades.length}`));
        const titulo = crear('h3', '', actividad.descripcion);
        titulo.id = `titulo-actividad-${indice + 1}`;
        titulo.tabIndex = -1;
        tarjeta.setAttribute('aria-labelledby', titulo.id);
        nombre.append(titulo);
        const estado = crear('p', 'actividad-detalle-estado');
        cabecera.append(nombre, estado);
        const fechas = crear('dl', 'actividad-fechas');
        agregarDato(fechas, 'Fecha programada', formatearFecha(actividad.fecha));
        const actualizacion = agregarDato(fechas, 'Última actualización', 'Sin registro');
        const cuerpo = crear('div', 'actividad-cuerpo');
        const informacion = crear('div', 'actividad-informacion');
        const contexto = crear('p', 'actividad-contexto-detalle', actividad.contexto);
        contexto.hidden = !actividad.contexto;
        const instrucciones = crear('details', 'actividad-instrucciones');
        const datos = crear('dl');
        [['Tipo', actividad.tipoActividad], ['Instrucciones', actividad.instrucciones],
            ['Acciones a realizar', actividad.accionesARealizar], ['Método recomendado', actividad.metodoRecomendado],
            ['Herramientas', actividad.herramientasDisponibles], ['Información que debes guardar', actividad.informacionAGuardar]
        ].forEach(([etiqueta, valor]) => agregarDato(datos, etiqueta, valor,
            etiqueta === 'Herramientas' || etiqueta === 'Método recomendado'));
        instrucciones.append(crear('summary', '', 'Instrucciones y recursos de la actividad'), datos);
        informacion.append(contexto, instrucciones);
        if (actividadesConPrueba.has(actividad.idActividad)) {
            const enlace = crear('a', 'btn btn-secundario btn-pequeno', 'Ver prueba externa y registrar resultado');
            enlace.href = '/test-vocacional.html?actividad=' + encodeURIComponent(actividad.idActividad);
            const acciones = crear('div', 'actividad-acciones'); acciones.append(enlace); informacion.append(acciones);
        }
        const formulario = crear('form', 'formulario-avance');
        // Solo la estructura fija usa HTML. Los datos recibidos usan textContent/value.
        formulario.innerHTML = `
            <fieldset><legend>Mi decisión</legend>
                <label>¿Realizarás esta actividad?
                    <select name="decision" required><option value="">Selecciona una respuesta</option>
                        <option value="true">Sí la realizaré</option><option value="false">No la realizaré</option></select>
                </label>
                <label>Razón de tu decisión<textarea name="razon" rows="3" required></textarea></label>
                <div class="actividad-acciones">
                    <button type="submit" class="btn btn-secundario btn-pequeno" data-accion="decision">Guardar decisión</button>
                    <button type="submit" class="btn btn-pequeno" data-accion="completada" formnovalidate>Marcar completada</button>
                    <button type="submit" class="btn btn-secundario btn-pequeno" data-accion="no-realizada" formnovalidate>No realizada</button>
                </div>
            </fieldset><p role="status" class="mensaje-guardado"></p>`;
        const campos = formulario.querySelector('fieldset');
        const seleccion = formulario.elements.namedItem('decision');
        const razon = formulario.elements.namedItem('razon');
        const aviso = formulario.querySelector('.mensaje-guardado');
        const completada = formulario.querySelector('[data-accion="completada"]');
        informacion.append(formulario);

        const reflexion = crear('div', 'actividad-reflexion');
        const formularioResultados = crear('form', 'formulario-avance');
        formularioResultados.innerHTML = `
            <fieldset><legend>Mis resultados y anotaciones</legend>
                <label>Resultados o conclusiones de esta actividad<textarea name="resultados" rows="6"></textarea></label>
                <p class="actividad-ayuda">Guardar resultados no marca la actividad como completada. Puedes vaciar el campo para borrar el contenido anterior.</p>
                <button type="submit" class="btn btn-pequeno">Guardar resultados</button>
            </fieldset><p role="status" class="mensaje-guardado"></p>`;
        const camposResultados = formularioResultados.querySelector('fieldset');
        const entradaResultados = formularioResultados.elements.namedItem('resultados');
        const avisoResultados = formularioResultados.querySelector('.mensaje-guardado');
        const bitacora = crear('section', 'actividad-bitacora');
        bitacora.setAttribute('aria-label', 'Seguimiento en mi bitácora');
        bitacora.append(crear('p', 'actividad-ayuda', 'Conserva una copia del estado, la fecha disponible y tus anotaciones guardadas. Se incluirá al descargar tu bitácora; los cambios posteriores no modifican esa copia.'));
        const botonBitacora = crear('button', 'btn btn-secundario btn-pequeno', 'Guardar constancia en mi bitácora');
        botonBitacora.type = 'button';
        const enlaceBitacora = crear('a', 'btn btn-secundario btn-pequeno', 'Ver mi bitácora');
        enlaceBitacora.href = '/bitacora.html';
        const accionesBitacora = crear('div', 'actividad-acciones'); accionesBitacora.append(botonBitacora, enlaceBitacora);
        const avisoBitacora = crear('p', 'mensaje-bitacora'); avisoBitacora.setAttribute('role', 'status');
        const estadoEntradas = crear('p', 'actividad-ayuda');
        const entradas = crear('details', 'actividad-bitacora-entradas');
        const tituloEntradas = crear('summary');
        const listaEntradas = crear('ul', 'actividad-bitacora-lista');
        entradas.append(tituloEntradas, listaEntradas);
        bitacora.append(accionesBitacora, avisoBitacora, estadoEntradas, entradas);
        reflexion.append(formularioResultados, bitacora); cuerpo.append(informacion, reflexion);
        tarjeta.append(cabecera, fechas, cuerpo);

        function valorDecision(item) {
            return item.decidioRealizar == null ? '' : String(item.decidioRealizar);
        }
        function actualizar(actualizada, opciones = {}) {
            const decisionSinCambios = seleccion.value === valorDecision(actividad)
                && razon.value.trim() === (actividad.razonDecision ?? '').trim();
            const resultadosSinCambios = entradaResultados.value.trim() === (actividad.resultadosRegistrados ?? '').trim();
            actividad = actualizada;
            estado.textContent = (actividad.estado === 'COMPLETADA' ? '✓ ' : '') + nombreEstado(actividad);
            tarjeta.classList.toggle('actividad-completada', actividad.estado === 'COMPLETADA');
            actualizacion.textContent = formatearFecha(actividad.fechaActualizacion, true);
            if (opciones.decision || decisionSinCambios) { seleccion.value = valorDecision(actividad); razon.value = actividad.razonDecision ?? ''; }
            if (opciones.resultados || resultadosSinCambios) entradaResultados.value = actividad.resultadosRegistrados ?? '';
            completada.disabled = actividad.estado === 'COMPLETADA';
        }
        function ocupar(ocupada) {
            campos.disabled = ocupada; camposResultados.disabled = ocupada; botonBitacora.disabled = ocupada;
        }
        function pintarEntradasActividad() {
            listaEntradas.replaceChildren();
            const relacionadas = (entradasBitacora ?? []).filter(entrada => entrada.idActividad === actividad.idActividad);
            estadoEntradas.textContent = entradasBitacora === null ? errorBitacora || 'Consultando tu bitácora...'
                : relacionadas.length ? '' : 'Todavía no hay entradas vinculadas a esta actividad.';
            entradas.hidden = relacionadas.length === 0;
            tituloEntradas.textContent = `Entradas relacionadas (${relacionadas.length})`;
            relacionadas.forEach(entrada => {
                const item = crear('li');
                const fecha = crear('time', '', 'Guardada el ' + formatearFecha(entrada.fechaCreacion, true));
                fecha.dateTime = entrada.fechaCreacion.replace(' ', 'T');
                item.append(fecha, crear('p', '', entrada.contenido)); listaEntradas.append(item);
            });
        }

        formulario.addEventListener('submit', async evento => {
            evento.preventDefault();
            const accion = evento.submitter?.dataset.accion ?? 'decision';
            if (accion !== 'completada' && razon.value.trim() === '') { aviso.textContent = 'Escribe una razón antes de guardar.'; razon.focus(); return; }
            let contenido;
            if (accion === 'decision') {
                if (seleccion.value === '') { aviso.textContent = 'Selecciona si realizarás la actividad.'; seleccion.focus(); return; }
                contenido = { decidioRealizar: seleccion.value === 'true', razon: razon.value };
            } else if (accion === 'no-realizada') contenido = { razon: razon.value };
            ocupar(true); aviso.textContent = 'Guardando...';
            let guardado = false;
            try {
                const ruta = '/api/mi-progreso/actividades/' + encodeURIComponent(actividad.idActividad);
                await enviar(ruta + '/' + accion, 'PUT', contenido); guardado = true;
                const [actualizada, progreso] = await Promise.all([consultarJson(ruta), consultarJson('/api/mi-progreso/resumen')]);
                aplicarActividad(actualizada, { decision: true }); actualizarProgreso(progreso);
                aviso.textContent = 'Cambio guardado correctamente.';
            } catch (error) {
                aviso.textContent = guardado ? 'El cambio se guardó, pero no se pudo actualizar la vista. Recarga la página para consultar el estado.' : error.message;
            } finally { ocupar(false); }
        });
        formularioResultados.addEventListener('submit', async evento => {
            evento.preventDefault(); ocupar(true); avisoResultados.textContent = 'Guardando resultados...';
            let guardado = false;
            try {
                const ruta = '/api/mi-progreso/actividades/' + encodeURIComponent(actividad.idActividad);
                await enviar(ruta + '/resultados', 'PUT', { resultados: entradaResultados.value }); guardado = true;
                aplicarActividad(await consultarJson(ruta), { resultados: true });
                avisoResultados.textContent = 'Resultados guardados correctamente.'; avisoBitacora.textContent = '';
            } catch (error) {
                avisoResultados.textContent = guardado ? 'Los resultados se guardaron, pero no se pudo actualizar la vista. Recarga la página para consultarlos.' : error.message;
            } finally { ocupar(false); }
        });
        botonBitacora.addEventListener('click', async () => {
            if (entradaResultados.value.trim() !== (actividad.resultadosRegistrados ?? '').trim()) {
                avisoBitacora.textContent = 'Guarda tus resultados antes de crear la constancia.'; entradaResultados.focus(); return;
            }
            if (seleccion.value !== valorDecision(actividad) || razon.value.trim() !== (actividad.razonDecision ?? '').trim()) {
                avisoBitacora.textContent = 'Guarda tu decisión antes de crear la constancia.'; razon.focus(); return;
            }
            ocupar(true); avisoBitacora.textContent = 'Guardando constancia en tu bitácora...';
            let guardado = false;
            try {
                const ruta = '/api/mi-progreso/actividades/' + encodeURIComponent(actividad.idActividad);
                const [actualizada, existentes] = await Promise.all([consultarJson(ruta), consultarJson('/api/bitacora')]);
                aplicarActividad(actualizada); entradasBitacora = existentes;
                const contenido = contenidoConstancia(actualizada);
                // Comprobar al reabrir la página o reintentar tras una respuesta perdida.
                if (existentes.some(entrada => entrada.idActividad === actividad.idActividad && entrada.contenido === contenido)) {
                    pintarEntradas(); avisoBitacora.textContent = 'Esta constancia ya está guardada en tu bitácora.'; return;
                }
                const respuesta = await enviar('/api/bitacora', 'POST', { contenido, idActividad: actividad.idActividad });
                guardado = true;
                entradasBitacora = [await respuesta.json(), ...existentes]; errorBitacora = ''; pintarEntradas();
                avisoBitacora.textContent = 'Constancia guardada en tu bitácora. Se incluirá cuando la descargues.';
            } catch (error) {
                avisoBitacora.textContent = guardado ? 'La constancia se guardó, pero no se pudo actualizar la lista. Abre tu bitácora para consultarla.' : error.message;
            } finally { ocupar(false); }
        });
        actualizar(inicial, { decision: true, resultados: true }); pintarEntradasActividad();
        return { tarjeta, titulo, actualizar, pintarEntradas: pintarEntradasActividad };
    }
});
