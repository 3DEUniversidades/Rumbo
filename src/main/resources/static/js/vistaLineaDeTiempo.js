document.addEventListener('DOMContentLoaded', async function () {
    const lista = document.getElementById('lista-actividades');
    const mensaje = document.getElementById('mensaje-linea-tiempo');
    const resumen = document.getElementById('resumen-progreso');
    const barra = document.getElementById('barra-progreso');
    const reintentar = document.getElementById('reintentar-carga');

    let actividadesConPrueba = new Set();

    const nombresEstado = {
        PENDIENTE: 'Pendiente',
        COMPLETADA: 'Completada',
        RECHAZADA: 'Decidiste no realizarla',
        NO_REALIZADA: 'No realizada'
    };

    reintentar.addEventListener('click', cargarDatos);

    try {
        const sesion = await obtenerSesion();

        if (!sesion) {
            return;
        }

        if (sesion.rol !== 'ESTUDIANTE') {
            mensaje.textContent =
                'Esta sección está disponible para estudiantes.';
            resumen.textContent = 'Progreso no disponible.';
            return;
        }

        await cargarDatos();
    } catch (error) {
        mensaje.textContent = 'No se pudo comprobar la sesión.';
        resumen.textContent = 'Recarga la página para intentarlo nuevamente.';
    }

    async function consultarJson(ruta) {
        const respuesta = await fetch(ruta, {
            credentials: 'same-origin',
            cache: 'no-store'
        });

        if (respuesta.status === 401) {
            window.location.replace('/login.html');
            throw new Error('La sesión terminó. Inicia sesión nuevamente.');
        }

        if (respuesta.status === 403) {
            throw new Error('No tienes permiso para consultar estos datos.');
        }

        if (!respuesta.ok) {
            throw new Error('No se pudieron consultar los datos.');
        }

        return respuesta.json();
    }

    async function cargarDatos() {
        reintentar.hidden = true;
        reintentar.disabled = true;
        mensaje.textContent = 'Cargando actividades...';
        resumen.textContent = 'Cargando progreso...';

        try {
            const [actividades, progreso, pruebas] = await Promise.all([
                consultarJson('/api/mi-progreso/actividades'),
                consultarJson('/api/mi-progreso/resumen'),
                consultarJson('/api/pruebas-vocacionales').catch(() => [])
            ]);

            actividadesConPrueba = new Set(pruebas.map(prueba => prueba.idActividad));
            lista.replaceChildren();

            actividades.forEach(function (actividad) {
                lista.appendChild(crearTarjeta(actividad));
            });

            barra.value = progreso.porcentaje;

            const porcentaje = new Intl.NumberFormat('es-GT', {
                maximumFractionDigits: 1
            }).format(progreso.porcentaje);

            resumen.textContent =
                `${progreso.actividadesCompletadas} de ` +
                `${progreso.totalActividades} actividades completadas ` +
                `(${porcentaje} %).`;

            mensaje.textContent = actividades.length === 0
                ? 'Todavía no hay actividades disponibles.'
                : '';
        } catch (error) {
            lista.replaceChildren();
            barra.value = 0;
            resumen.textContent = 'Progreso no disponible.';
            mensaje.textContent = error.message;
            reintentar.hidden = false;
        } finally {
            reintentar.disabled = false;
        }
    }

    function crearTarjeta(actividad) {
        const tarjeta = document.createElement('article');
        tarjeta.className = 'actividad-card';

        tarjeta.classList.toggle(
            'actividad-completada',
            actividad.estado === 'COMPLETADA'
        );

        const titulo = document.createElement('h3');
        titulo.textContent = actividad.descripcion;

        const fecha = document.createElement('p');
        fecha.textContent = `Fecha: ${actividad.fecha}`;

        const estado = document.createElement('p');
        estado.textContent =
            'Estado: ' + (nombresEstado[actividad.estado] ?? actividad.estado);

        const detalles = document.createElement('details');

        const abrir = document.createElement('summary');
        abrir.textContent = 'Ver detalles de la actividad';

        const datos = document.createElement('dl');

        agregarDato(datos, 'Tipo', actividad.tipoActividad);
        agregarDato(datos, 'Contexto', actividad.contexto);
        agregarDato(datos, 'Instrucciones', actividad.instrucciones);
        agregarDato(datos, 'Acciones a realizar', actividad.accionesARealizar);
        agregarDato(datos, 'Método recomendado', actividad.metodoRecomendado);
        agregarDato(datos, 'Herramientas', actividad.herramientasDisponibles);
        agregarDato(
            datos,
            'Información que debes guardar',
            actividad.informacionAGuardar
        );

        const decision = actividad.decidioRealizar == null
            ? 'Todavía no has respondido'
            : actividad.decidioRealizar
                ? 'Sí realizaré la actividad'
                : 'No realizaré la actividad';

        const campoDecision = agregarDato(datos, 'Tu decisión', decision);
        const campoRazon = agregarDato(datos, 'Tu razón', actividad.razonDecision);
        const campoResultados = agregarDato(
    datos,
    'Tus resultados',
    actividad.resultadosRegistrados
);

        const formulario = document.createElement('form');
formulario.className = 'formulario-avance';

// Este HTML es fijo. Los datos del estudiante se asignan con value.
formulario.innerHTML = `
    <fieldset>
        <legend>Mi decisión</legend>

        <label>
            ¿Realizarás esta actividad?
            <select name="decision" required>
                <option value="">Selecciona una respuesta</option>
                <option value="true">Sí la realizaré</option>
                <option value="false">No la realizaré</option>
            </select>
        </label>

        <label>
            Razón de tu decisión
            <textarea name="razon" rows="3" required></textarea>
        </label>

        <button type="submit" class="btn" data-accion="decision">
    Guardar decisión
</button>

<button type="submit" class="btn" data-accion="completada"
        formnovalidate>
    Marcar completada
</button>

<button type="submit" class="btn" data-accion="no-realizada"
        formnovalidate>
    No realizada
</button>
    </fieldset>

    <p role="status" class="mensaje-guardado"></p>
`;

const campos = formulario.querySelector('fieldset');
const seleccion = formulario.elements.namedItem('decision');
const razon = formulario.elements.namedItem('razon');
const aviso = formulario.querySelector('.mensaje-guardado');

seleccion.value = actividad.decidioRealizar == null
    ? ''
    : String(actividad.decidioRealizar);

razon.value = actividad.razonDecision ?? '';

formulario.addEventListener('submit', async function (evento) {
    evento.preventDefault();

const accion = evento.submitter?.dataset.accion ?? 'decision';

if (accion !== 'completada' && razon.value.trim() === '') {
    aviso.textContent = 'Escribe una razón antes de guardar.';
    razon.focus();
    return;
}

let contenido;

if (accion === 'decision') {
    if (seleccion.value === '') {
        aviso.textContent = 'Selecciona si realizarás la actividad.';
        seleccion.focus();
        return;
    }

    contenido = {
        decidioRealizar: seleccion.value === 'true',
        razon: razon.value
    };
} else if (accion === 'no-realizada') {
    contenido = {
        razon: razon.value
    };
}

    campos.disabled = true;
    camposResultados.disabled = true;
    aviso.textContent = 'Guardando...';

    let guardado = false;

    try {
        const ruta = '/api/mi-progreso/actividades/' +
            encodeURIComponent(actividad.idActividad);

        const respuesta = await fetch(ruta + '/' + accion, {
    method: 'PUT',
    credentials: 'same-origin',
    headers: contenido === undefined
        ? {}
        : { 'Content-Type': 'application/json' },
    body: contenido === undefined
        ? undefined
        : JSON.stringify(contenido)
});

        if (respuesta.status === 401) {
            window.location.replace('/login.html');
            return;
        }

        if (!respuesta.ok) {
            const error = await respuesta.json().catch(() => ({}));

            throw new Error(
                error.error || 'No se pudo guardar el cambio.'
            );
        }

        guardado = true;

        const [actualizada, progreso] = await Promise.all([
            consultarJson(ruta),
            consultarJson('/api/mi-progreso/resumen')
        ]);

        actividad = actualizada;

        estado.textContent =
            'Estado: ' + nombresEstado[actualizada.estado];

        tarjeta.classList.toggle(
            'actividad-completada',
            actualizada.estado === 'COMPLETADA'
        );

        campoDecision.textContent = actualizada.decidioRealizar
            ? 'Sí realizaré la actividad'
            : 'No realizaré la actividad';

        campoRazon.textContent =
    actualizada.razonDecision ?? 'Sin información registrada.';

razon.value = actualizada.razonDecision ?? '';

seleccion.value = actualizada.decidioRealizar == null
    ? ''
    : String(actualizada.decidioRealizar);

        barra.value = progreso.porcentaje;

        const porcentaje = new Intl.NumberFormat('es-GT', {
            maximumFractionDigits: 1
        }).format(progreso.porcentaje);

        resumen.textContent =
            `${progreso.actividadesCompletadas} de ` +
            `${progreso.totalActividades} actividades completadas ` +
            `(${porcentaje} %).`;

        aviso.textContent = 'Cambio guardado correctamente.';
    } catch (error) {
        aviso.textContent = guardado
    ? 'El cambio se guardó, pero no se pudo actualizar la vista. ' +
      'Recarga la página para consultar el estado.'
    : error.message;
    } finally {
        campos.disabled = false;
camposResultados.disabled = false;
    }
});

const formularioResultados = document.createElement('form');
formularioResultados.className = 'formulario-avance';

formularioResultados.innerHTML = `
    <fieldset>
        <legend>Mis resultados</legend>

        <label>
            Resultados o conclusiones de esta actividad
            <textarea name="resultados" rows="4"></textarea>
        </label>

        <p>
            Guardar resultados no marca la actividad como completada.
            Puedes vaciar el campo para borrar el contenido anterior.
        </p>

        <button type="submit" class="btn">
            Guardar resultados
        </button>

        <button type="button" class="btn btn-secundario"
                data-accion="bitacora" hidden>
            Agregar resultados a mi bitácora
        </button>
    </fieldset>

    <p class="mensaje-bitacora" role="status"></p>

    <p role="status" class="mensaje-guardado"></p>
`;

const camposResultados = formularioResultados.querySelector('fieldset');
const entradaResultados =
    formularioResultados.elements.namedItem('resultados');
const avisoResultados =
    formularioResultados.querySelector('.mensaje-guardado');
const botonBitacora =
    formularioResultados.querySelector('[data-accion="bitacora"]');
const avisoBitacora =
    formularioResultados.querySelector('.mensaje-bitacora');

entradaResultados.value = actividad.resultadosRegistrados ?? '';

function actualizarBotonBitacora(resultados) {
    botonBitacora.hidden =
        resultados == null || resultados.trim() === '';
}

actualizarBotonBitacora(actividad.resultadosRegistrados);

botonBitacora.addEventListener('click', async function () {
    botonBitacora.disabled = true;
    avisoBitacora.textContent = 'Agregando a tu bitácora...';
    avisoBitacora.className = 'mensaje-bitacora';

    try {
        const respuesta = await fetch(
            '/api/bitacora/actividades/' +
            encodeURIComponent(actividad.idActividad) +
            '/resultados',
            {
                method: 'POST',
                credentials: 'same-origin'
            }
        );

        if (respuesta.status === 401) {
            window.location.replace('/login.html');
            return;
        }

        if (!respuesta.ok) {
            const error = await respuesta.json().catch(() => ({}));
            const detalle = error.campos
                ? Object.values(error.campos).join(' ')
                : error.error;

            throw new Error(
                detalle || 'No se pudieron agregar los resultados.'
            );
        }

        avisoBitacora.textContent =
            'Resultados agregados a tu bitácora.';
        avisoBitacora.className =
            'mensaje-bitacora mensaje-formulario mensaje-exito';
    } catch (error) {
        avisoBitacora.textContent = error.message;
        avisoBitacora.className =
            'mensaje-bitacora mensaje-formulario mensaje-error';
    } finally {
        botonBitacora.disabled = false;
    }
});

formularioResultados.addEventListener('submit', async function (evento) {
    evento.preventDefault();

    // Evitar operaciones simultáneas dentro de esta tarjeta.
    campos.disabled = true;
    camposResultados.disabled = true;
    avisoResultados.textContent = 'Guardando resultados...';

    let guardado = false;

    try {
        const ruta = '/api/mi-progreso/actividades/' +
            encodeURIComponent(actividad.idActividad);

        const respuesta = await fetch(ruta + '/resultados', {
            method: 'PUT',
            credentials: 'same-origin',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                resultados: entradaResultados.value
            })
        });

        if (respuesta.status === 401) {
            window.location.replace('/login.html');
            return;
        }

        if (!respuesta.ok) {
            const error = await respuesta.json().catch(() => ({}));

            throw new Error(
                error.error || 'No se pudieron guardar los resultados.'
            );
        }

        guardado = true;

        const actualizada = await consultarJson(ruta);
        actividad = actualizada;

        entradaResultados.value =
            actualizada.resultadosRegistrados ?? '';

        campoResultados.textContent =
            actualizada.resultadosRegistrados?.trim()
                ? actualizada.resultadosRegistrados
                : 'Sin información registrada.';

        actualizarBotonBitacora(actualizada.resultadosRegistrados);
        avisoBitacora.textContent = '';
        avisoBitacora.className = 'mensaje-bitacora';

        avisoResultados.textContent =
            'Resultados guardados correctamente.';
    } catch (error) {
        avisoResultados.textContent = guardado
            ? 'Los resultados se guardaron, pero no se pudo actualizar ' +
              'la vista. Recarga la página para consultarlos.'
            : error.message;
    } finally {
        campos.disabled = false;
        camposResultados.disabled = false;
    }
});

detalles.append(abrir, datos, formulario, formularioResultados);
tarjeta.append(titulo, fecha, estado, detalles);
if (actividadesConPrueba.has(actividad.idActividad)) {
    const enlacePrueba = document.createElement('a');
    enlacePrueba.className = 'btn btn-secundario';
    enlacePrueba.href = '/test-vocacional.html?actividad=' + encodeURIComponent(actividad.idActividad);
    enlacePrueba.textContent = 'Ver prueba externa y registrar resultado';
    enlacePrueba.style.marginTop = '1rem';
    tarjeta.appendChild(enlacePrueba);
}


return tarjeta;
    }

    function agregarDato(contenedor, etiqueta, valor) {
        const termino = document.createElement('dt');
        termino.textContent = etiqueta;

        const descripcion = document.createElement('dd');
        descripcion.textContent =
            valor == null || String(valor).trim() === ''
                ? 'Sin información registrada.'
                : String(valor);

        contenedor.append(termino, descripcion);
        return descripcion;
    }
});
