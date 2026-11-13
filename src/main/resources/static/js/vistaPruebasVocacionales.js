document.addEventListener('DOMContentLoaded', async () => {
    const contenido = document.getElementById('contenido-pruebas');
    const estado = document.getElementById('estado-pruebas');
    const lista = document.getElementById('lista-pruebas');
    const historial = document.getElementById('historial-resultados');
    const filtro = document.getElementById('filtro-tipo-prueba');
    const recargar = document.getElementById('recargar-pruebas');
    const todas = document.getElementById('todas-las-pruebas');
    const idActividad = new URLSearchParams(window.location.search).get('actividad');
    const pendientes = new Set();
    let enProceso = false;
    const etiquetas = { fechaRealizacion: 'Fecha de realización', resultado: 'Resultado externo', interpretacionPersonal: 'Reflexión personal' };

    function bloquear(valor) {
        enProceso = valor;
        recargar.disabled = valor;
        filtro.disabled = valor;
        lista.querySelectorAll('fieldset').forEach(campo => { campo.disabled = valor; });
    }

    async function api(ruta, opciones = {}) {
        const respuesta = await fetch(ruta, { credentials: 'same-origin', cache: 'no-store', ...opciones });
        if (respuesta.status === 401) {
            window.location.replace('/login.html');
            throw new Error('Tu sesión terminó. Inicia sesión nuevamente.');
        }
        if (!respuesta.ok) {
            const cuerpo = await respuesta.json().catch(() => ({}));
            const campos = cuerpo.campos ? Object.entries(cuerpo.campos)
                .map(([campo, mensaje]) => `${etiquetas[campo] ?? campo}: ${mensaje}`).join('\n') : '';
            throw new Error(campos || cuerpo.error || 'No se pudo realizar la operación.');
        }
        return respuesta.json();
    }

    function par(dl, etiqueta, valor) {
        const dt = document.createElement('dt'); dt.textContent = etiqueta;
        const dd = document.createElement('dd'); dd.textContent = valor || 'Sin información registrada.';
        dl.append(dt, dd);
        return dd;
    }

    function enlaceSeguro(texto, direccion) {
        const enlace = document.createElement('a'); enlace.textContent = texto;
        enlace.className = 'btn btn-secundario';
        try {
            const url = new URL(direccion);
            if (url.protocol !== 'https:') throw new Error('Protocolo no permitido');
            enlace.href = url.href;
            enlace.target = '_blank'; enlace.rel = 'noopener noreferrer';
        } catch { enlace.hidden = true; }
        return enlace;
    }

    function aplicarFiltro() {
        let visibles = 0;
        lista.querySelectorAll('.prueba-tarjeta').forEach(tarjeta => {
            tarjeta.hidden = filtro.value !== '' && tarjeta.dataset.tipo !== filtro.value;
            if (!tarjeta.hidden) visibles++;
        });
        estado.textContent = visibles
            ? (idActividad ? 'Pruebas asociadas a la actividad seleccionada.' : 'Consulta las instrucciones antes de abrir una prueba externa.')
            : 'No hay pruebas disponibles para esta selección.';
    }

    function mostrarHistorial(resultados) {
        historial.replaceChildren();
        if (!resultados.length) { historial.textContent = 'Todavía no has registrado resultados de pruebas externas.'; return; }
        resultados.forEach(resultado => {
            const articulo = document.createElement('article'); articulo.className = 'resultado-historial';
            const titulo = document.createElement('h3'); titulo.textContent = resultado.nombrePrueba;
            const fecha = document.createElement('p'); fecha.textContent = 'Realizada: ' + resultado.fechaRealizacion;
            const texto = document.createElement('p'); texto.textContent = resultado.resultado;
            const reflexion = document.createElement('p'); reflexion.textContent = 'Reflexión personal: ' + (resultado.interpretacionPersonal || 'Sin reflexión registrada.');
            articulo.append(titulo, fecha, texto, reflexion);
            if (!resultado.pruebaDisponible) {
                const aviso = document.createElement('p'); aviso.textContent = 'Historial conservado: la actividad fue retirada de la línea de tiempo.'; articulo.appendChild(aviso);
            }
            historial.appendChild(articulo);
        });
    }

    function crearTarjeta(prueba) {
        const tarjeta = document.createElement('article'); tarjeta.className = 'prueba-tarjeta'; tarjeta.dataset.tipo = prueba.tipo;
        const titulo = document.createElement('h2'); titulo.textContent = prueba.nombre;
        const proveedor = document.createElement('p'); proveedor.textContent = 'Fuente: ' + prueba.proveedor;
        const proposito = document.createElement('p'); proposito.textContent = prueba.proposito;
        const detalles = document.createElement('details');
        const abrir = document.createElement('summary'); abrir.textContent = 'Instrucciones, condiciones e interpretación';
        const datos = document.createElement('dl');
        par(datos, 'Contexto recomendado', prueba.contexto);
        par(datos, 'Instrucciones', prueba.instrucciones);
        par(datos, 'Método', prueba.metodo);
        par(datos, 'Condiciones de acceso', prueba.condicionesAcceso);
        par(datos, 'Cómo interpretar y explorar carreras', prueba.orientacionInterpretacion);
        detalles.append(abrir, datos);
        const enlaces = document.createElement('div'); enlaces.className = 'enlaces-prueba';
        enlaces.append(enlaceSeguro('Abrir recurso externo', prueba.enlaceExterno), enlaceSeguro('Interpretación y recursos oficiales', prueba.recursoInterpretacionUrl));
        const linea = document.createElement('a'); linea.href = '/linea-tiempo.html'; linea.className = 'btn btn-secundario'; linea.textContent = 'Volver a mis actividades'; enlaces.append(linea);
        const form = document.createElement('form'); form.className = 'formulario-resultado-prueba';
        form.innerHTML = `
            <fieldset>
                <legend>Mi resultado privado</legend>
                <label>Fecha en que realizaste la prueba externa<input type="date" name="fechaRealizacion" required></label>
                <label>Resultado obtenido fuera de Rumbo<textarea name="resultado" rows="4" maxlength="6000" required placeholder="Registra las áreas o puntuaciones tal como aparecen en tu reporte externo."></textarea></label>
                <label>Mi reflexión y opciones que quiero explorar (opcional)<textarea name="interpretacionPersonal" rows="3" maxlength="4000"></textarea></label>
                <p>Registra información real. Tu reflexión personal no es una interpretación profesional. Rumbo conserva un resultado por prueba; volver a guardar actualiza ese registro.</p>
                <button type="submit" class="btn">Guardar mi resultado</button>
                <button type="button" class="btn btn-secundario" data-accion="copiar-bitacora" hidden>Agregar resultado guardado a mi bitácora</button>
                <p>La opción de bitácora crea una copia del resultado guardado y la relaciona con esta actividad. Si después corriges el resultado, la copia anterior conserva su contenido.</p>
            </fieldset>
            <p class="mensaje-resultado-prueba" role="status" aria-live="polite"></p>`;
        const fecha = form.elements.namedItem('fechaRealizacion');
        const resultado = form.elements.namedItem('resultado');
        const interpretacion = form.elements.namedItem('interpretacionPersonal');
        const aviso = form.querySelector('.mensaje-resultado-prueba');
        const botonBitacora = form.querySelector('[data-accion="copiar-bitacora"]');
        let ultimaCopia = null;
        function firmaGuardada() { return JSON.stringify(prueba.miResultado); }
        function actualizarBotonBitacora() {
            botonBitacora.hidden = !prueba.miResultado;
            botonBitacora.disabled = pendientes.has(prueba.idPrueba) || ultimaCopia === firmaGuardada();
        }
        actualizarBotonBitacora();
        botonBitacora.addEventListener('click', async () => {
            if (enProceso || !prueba.miResultado || pendientes.has(prueba.idPrueba) || ultimaCopia === firmaGuardada()) return;
            const guardado = prueba.miResultado;
            const texto = [prueba.nombre, 'Fecha de realización: ' + guardado.fechaRealizacion,
                'Resultado externo:', guardado.resultado,
                'Reflexión personal:', guardado.interpretacionPersonal || 'Sin reflexión registrada.'].join('\n\n');
            bloquear(true); aviso.textContent = 'Agregando el resultado guardado a tu bitácora...';
            try {
                await api('/api/bitacora', {
                    method: 'POST', headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ contenido: texto, idActividad: prueba.idActividad })
                });
                ultimaCopia = firmaGuardada();
                aviso.textContent = 'Copia agregada. Puedes consultarla desde Mi bitácora.';
            } catch (error) { aviso.textContent = error.message; }
            finally { actualizarBotonBitacora(); bloquear(false); }
        });
        const partesFecha = new Intl.DateTimeFormat('en', { timeZone: 'America/Guatemala', year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(new Date());
        const parteFecha = tipo => partesFecha.find(parte => parte.type === tipo).value;
        fecha.max = `${parteFecha('year')}-${parteFecha('month')}-${parteFecha('day')}`;
        fecha.value = prueba.miResultado?.fechaRealizacion ?? '';
        resultado.value = prueba.miResultado?.resultado ?? '';
        interpretacion.value = prueba.miResultado?.interpretacionPersonal ?? '';
        if (prueba.miResultado) aviso.textContent = 'Ya tienes un resultado guardado. Puedes corregirlo.';
        form.addEventListener('input', evento => {
            pendientes.add(prueba.idPrueba);
            actualizarBotonBitacora();
            if (typeof evento.target.setCustomValidity === 'function') evento.target.setCustomValidity('');
        });
        form.addEventListener('submit', async evento => {
            evento.preventDefault();
            if (enProceso) return;
            if (!resultado.value.trim()) {
                resultado.setCustomValidity('Escribe el resultado de la prueba externa.'); resultado.reportValidity(); return;
            }
            const solicitud = { fechaRealizacion: fecha.value, resultado: resultado.value, interpretacionPersonal: interpretacion.value };
            bloquear(true); aviso.textContent = 'Guardando...';
            let guardado = false;
            try {
                const datos = await api('/api/pruebas-vocacionales/' + encodeURIComponent(prueba.idPrueba) + '/mi-resultado', {
                    method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(solicitud)
                });
                guardado = true; prueba.miResultado = datos; pendientes.delete(prueba.idPrueba);
                actualizarBotonBitacora();
                fecha.value = datos.fechaRealizacion; resultado.value = datos.resultado; interpretacion.value = datos.interpretacionPersonal ?? '';
                aviso.textContent = 'Resultado guardado. La actividad conserva su estado; márcala como completada desde la línea de tiempo cuando corresponda.';
                mostrarHistorial(await api('/api/pruebas-vocacionales/mis-resultados'));
            } catch (error) {
                aviso.textContent = guardado
                    ? 'El resultado se guardó, pero no se pudo actualizar el historial. Puedes recargar para consultarlo.'
                    : error.message;
            } finally { bloquear(false); }
        });
        tarjeta.append(titulo, proveedor, proposito, detalles, enlaces, form);
        return tarjeta;
    }

    async function cargar() {
        if (enProceso) return;
        if (pendientes.size && !window.confirm('¿Descartar los cambios sin guardar y actualizar la página?')) return;
        bloquear(true); estado.textContent = 'Cargando pruebas y resultados...';
        try {
            const ruta = idActividad ? '/api/pruebas-vocacionales/por-actividad/' + encodeURIComponent(idActividad) : '/api/pruebas-vocacionales';
            const [pruebas, resultados] = await Promise.all([api(ruta), api('/api/pruebas-vocacionales/mis-resultados')]);
            pendientes.clear();
            lista.replaceChildren(...pruebas.map(crearTarjeta));
            mostrarHistorial(resultados); aplicarFiltro();
        } catch (error) { estado.textContent = error.message + ' Puedes usar «Actualizar» para reintentar.'; }
        finally { bloquear(false); }
    }

    filtro.addEventListener('change', aplicarFiltro);
    recargar.addEventListener('click', cargar);
    window.addEventListener('beforeunload', evento => {
        if (pendientes.size) { evento.preventDefault(); evento.returnValue = ''; }
    });
    try {
        const sesion = await obtenerSesion();
        if (!sesion) return;
        if (sesion.rol !== 'ESTUDIANTE') { estado.textContent = 'Esta sección está disponible para estudiantes.'; return; }
        contenido.hidden = false; todas.hidden = !idActividad; await cargar();
    } catch (error) { estado.textContent = error.message; }
});
