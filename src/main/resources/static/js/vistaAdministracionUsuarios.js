document.addEventListener('DOMContentLoaded', async () => {
    const contenido = document.getElementById('contenido-admin-usuarios');
    const estado = document.getElementById('estado-admin-usuarios');
    const lista = document.getElementById('lista-admin-usuarios');
    const paginaTexto = document.getElementById('pagina-admin-usuarios');
    const anterior = document.getElementById('anterior-admin-usuarios');
    const siguiente = document.getElementById('siguiente-admin-usuarios');
    const actualizar = document.getElementById('actualizar-admin-usuarios');
    const detalle = document.getElementById('detalle-admin-usuario');
    const estadoDetalle = document.getElementById('estado-detalle-admin');
    const datos = document.getElementById('datos-admin-usuario');
    let pagina = 0, totalPaginas = 0, cargando = false, solicitudDetalle = 0, idActual = null;
    const roles = {ESTUDIANTE: 'Estudiante', COLABORADOR: 'Colaborador', SUPERVISOR_DE_USUARIOS: 'Supervisor de usuarios', SUPERVISOR_DE_MENSAJES: 'Supervisor de mensajes', ENCARGADO_DE_CARRERAS: 'Encargado de carreras', ADMIN: 'Administrador'};
    async function api(ruta, opciones = {}) {
        const r = await fetch(ruta, {credentials: 'same-origin', cache: 'no-store', ...opciones});
        if (r.status === 401) { window.location.replace('/login.html'); throw new Error('Inicia sesión nuevamente.'); }
        if (r.status === 403) throw new Error('Esta consulta requiere una cuenta de administrador vigente.');
        if (r.status === 404) throw new Error('La cuenta ya no está disponible.');
        if (!r.ok) {
            const error = await r.json().catch(() => ({}));
            throw new Error(error.campos ? Object.values(error.campos).join(' ') : error.error || 'No se pudo realizar la operación. Inténtalo nuevamente.');
        }
        return r.json();
    }
    function botones() { anterior.disabled = cargando || pagina === 0; siguiente.disabled = cargando || pagina + 1 >= totalPaginas; actualizar.disabled = cargando; }
    function par(nombre, valor) {
        const dt = document.createElement('dt'), dd = document.createElement('dd');
        dt.textContent = nombre; dd.textContent = valor || 'Sin datos registrados'; datos.append(dt, dd);
    }
    async function consultar(id) {
        const numero = ++solicitudDetalle;
        detalle.hidden = false; datos.replaceChildren(); estadoDetalle.textContent = 'Consultando perfil...';
        try {
            const u = await api('/api/admin/usuarios/' + encodeURIComponent(id));
            if (numero !== solicitudDetalle) return;
            estadoDetalle.textContent = u.perfilDisponible ? 'Datos actuales del perfil.' : 'Esta cuenta no tiene un perfil adicional registrado.';
            par('Nombre de usuario', u.nombreUsuario); par('Nombre completo', u.nombreCompleto);
            par('Correo', u.correo); par('Rol', roles[u.rol] || u.rol); par('Verificación', u.estadoVerificacion);
            if (u.perfilDisponible) {
                par('Nivel de estudios', u.nivelEstudios); par('Descripción personal', u.descripcionPersonal);
                par('Área laboral', u.areaLaboral); par('Carrera', u.nombreCarrera);
                if (u.fotoUrl) {
                    try {
                        const url = new URL(u.fotoUrl);
                        if (url.protocol === 'https:') {
                            const foto = document.createElement('img'); foto.src = url.href; foto.alt = 'Foto de perfil de ' + u.nombreUsuario;
                            foto.width = 120; foto.height = 120; foto.loading = 'lazy'; foto.referrerPolicy = 'no-referrer'; datos.append(foto);
                        }
                    } catch { /* Una foto inválida no impide consultar el perfil. */ }
                }
            }
            document.getElementById('titulo-detalle-admin').focus();
        } catch (error) { if (numero === solicitudDetalle) estadoDetalle.textContent = error.message; }
    }
    async function eliminar(u) {
        if (cargando || u.idUsuario === idActual) return;
        const confirmacion = window.prompt('Eliminar cuenta de ' + u.nombreCompleto + '. La cuenta perderá el acceso y sus datos vinculados se conservarán en la base. Para confirmar, escribe exactamente: ' + u.nombreUsuario);
        if (confirmacion === null) return;
        if (confirmacion.trim() !== u.nombreUsuario) { estado.textContent = 'El nombre de usuario no coincide. No se eliminó la cuenta.'; return; }
        const motivo = window.prompt('Motivo de la eliminación (obligatorio, máximo 1000 caracteres):');
        if (motivo === null) return;
        if (!motivo.trim() || motivo.trim().length > 1000) { estado.textContent = 'Escribe un motivo de 1 a 1000 caracteres. No se eliminó la cuenta.'; return; }
        cargando = true; botones(); lista.querySelectorAll('button').forEach(b => b.disabled = true);
        ++solicitudDetalle; detalle.hidden = true; datos.replaceChildren();
        estado.textContent = 'Eliminando cuenta...';
        let completada = false;
        try {
            await api('/api/admin/usuarios/' + encodeURIComponent(u.idUsuario), {
                method: 'DELETE', headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({nombreUsuarioConfirmacion: confirmacion.trim(), motivo: motivo.trim()})
            });
            completada = true;
        } catch (error) { estado.textContent = error.message; }
        finally { cargando = false; botones(); lista.querySelectorAll('button').forEach(b => b.disabled = b.dataset.propia === 'si'); }
        if (completada) {
            const listaActualizada = await cargar(0); await cargarRegistros(0);
            estado.textContent = listaActualizada
                ? 'Cuenta eliminada. La acción quedó registrada.'
                : 'Cuenta eliminada y acción registrada. No se pudo recargar la lista; pulsa Actualizar.';
        }
    }

    async function cargar(destino = pagina) {
        if (cargando) return;
        cargando = true; botones(); estado.textContent = 'Cargando cuentas...';
        ++solicitudDetalle; detalle.hidden = true; datos.replaceChildren(); lista.replaceChildren(); paginaTexto.textContent = '';
        try {
            const r = await api('/api/admin/usuarios?pagina=' + destino + '&tamano=20');
            pagina = r.pagina; totalPaginas = r.totalPaginas;
            if (pagina > 0 && !r.usuarios.length) { pagina = Math.max(0, totalPaginas - 1); estado.textContent = 'La lista cambió. Pulsa Actualizar para consultar la última página disponible.'; return; }
            estado.textContent = r.totalUsuarios + ' cuentas registradas.';
            paginaTexto.textContent = totalPaginas ? 'Página ' + (pagina + 1) + ' de ' + totalPaginas : 'No hay cuentas para mostrar.';
            for (const u of r.usuarios) {
                const li = document.createElement('li'), info = document.createElement('div');
                const nombre = document.createElement('p'), rol = document.createElement('p'), boton = document.createElement('button');
                nombre.textContent = u.nombreCompleto + ' (@' + u.nombreUsuario + ')'; rol.textContent = roles[u.rol] || u.rol;
                boton.type = 'button'; boton.className = 'btn btn-secundario'; boton.textContent = 'Ver perfil';
                boton.setAttribute('aria-label', 'Ver perfil de ' + u.nombreUsuario); boton.addEventListener('click', () => consultar(u.idUsuario));
                const borrar = document.createElement('button'); borrar.type = 'button'; borrar.className = 'btn btn-secundario';
                borrar.textContent = 'Eliminar cuenta'; borrar.setAttribute('aria-label', 'Eliminar cuenta de ' + u.nombreUsuario);
                borrar.disabled = u.idUsuario === idActual; borrar.dataset.propia = borrar.disabled ? 'si' : 'no';
                if (borrar.disabled) borrar.title = 'No puedes eliminar tu propia cuenta administradora';
                borrar.addEventListener('click', () => eliminar(u));
                const acciones = document.createElement('div'); acciones.className = 'acciones-cuenta-admin'; acciones.append(boton, borrar);
                info.append(nombre, rol); li.append(info, acciones); lista.append(li);
            }
            return true;
        } catch (error) { estado.textContent = error.message; return false; }
        finally { cargando = false; botones(); }
    }
    let paginaRegistros = 0, paginasRegistros = 0, cargandoRegistros = false;
    const listaRegistros = document.getElementById('lista-registros-admin');
    const avisoRegistros = document.getElementById('estado-registros-admin');
    const anteriorRegistros = document.getElementById('anterior-registros-admin');
    const siguienteRegistros = document.getElementById('siguiente-registros-admin');
    const actualizarRegistros = document.getElementById('actualizar-registros-admin');
    async function cargarRegistros(destino = paginaRegistros) {
        if (cargandoRegistros) return;
        cargandoRegistros = true;
        anteriorRegistros.disabled = siguienteRegistros.disabled = actualizarRegistros.disabled = true;
        avisoRegistros.textContent = 'Consultando registros...'; listaRegistros.replaceChildren();
        try {
            const r = await api('/api/admin/registros?pagina=' + destino + '&tamano=20');
            paginaRegistros = r.pagina; paginasRegistros = r.totalPaginas;
            avisoRegistros.textContent = r.totalRegistros ? r.totalRegistros + ' acciones registradas.' : 'Todavía no hay acciones administrativas registradas.';
            document.getElementById('pagina-registros-admin').textContent = paginasRegistros ? 'Página ' + (paginaRegistros + 1) + ' de ' + paginasRegistros : '';
            for (const registro of r.registros) {
                const li = document.createElement('li');
                const objetivo = registro.accion === 'ELIMINAR_PUBLICACION'
                    ? 'la publicación #' + registro.idPublicacion + ' «' + registro.tituloPublicacion + '» de ' + registro.usuarioObjetivo
                    : 'la cuenta ' + registro.usuarioObjetivo + ' (' + (roles[registro.rolObjetivo] || registro.rolObjetivo) + ')';
                li.textContent = new Date(registro.fecha).toLocaleString('es-GT') + ' — ' + registro.nombreAdministrador + ' eliminó ' + objetivo + '. Motivo: ' + registro.motivo;
                listaRegistros.append(li);
            }
        } catch (error) { avisoRegistros.textContent = error.message; }
        finally {
            cargandoRegistros = false; actualizarRegistros.disabled = false;
            anteriorRegistros.disabled = paginaRegistros === 0; siguienteRegistros.disabled = paginaRegistros + 1 >= paginasRegistros;
        }
    }
    anteriorRegistros.addEventListener('click', () => cargarRegistros(paginaRegistros - 1));
    siguienteRegistros.addEventListener('click', () => cargarRegistros(paginaRegistros + 1));
    actualizarRegistros.addEventListener('click', () => cargarRegistros());

    anterior.addEventListener('click', () => cargar(pagina - 1)); siguiente.addEventListener('click', () => cargar(pagina + 1)); actualizar.addEventListener('click', () => cargar());
    try {
        const sesion = await obtenerSesion(); if (!sesion) return;
        if (sesion.rol !== 'ADMIN') { estado.textContent = 'Esta sección está disponible para administradores.'; return; }
        idActual = sesion.idUsuario; contenido.hidden = false; await Promise.all([cargar(), cargarRegistros()]);
    } catch (error) { estado.textContent = error.message; }
});
