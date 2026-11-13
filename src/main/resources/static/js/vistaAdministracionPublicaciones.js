document.addEventListener('DOMContentLoaded', async () => {
    const contenido = document.getElementById('contenido-publicaciones-admin');
    const estado = document.getElementById('estado-publicaciones-admin');
    const lista = document.getElementById('lista-publicaciones-admin');
    const anterior = document.getElementById('anterior-publicaciones-admin');
    const siguiente = document.getElementById('siguiente-publicaciones-admin');
    const actualizar = document.getElementById('actualizar-publicaciones-admin');
    const paginaTexto = document.getElementById('pagina-publicaciones-admin');
    let pagina = 0, totalPaginas = 0, ocupado = false;
    function botones() {
        anterior.disabled = ocupado || pagina === 0; siguiente.disabled = ocupado || pagina + 1 >= totalPaginas;
        actualizar.disabled = ocupado; lista.querySelectorAll('button').forEach(b => b.disabled = ocupado);
    }
    async function api(ruta, opciones = {}) {
        const r = await fetch(ruta, {credentials: 'same-origin', cache: 'no-store', ...opciones});
        if (r.status === 401) { window.location.replace('/login.html'); throw new Error('Inicia sesión nuevamente.'); }
        if (!r.ok) {
            const error = await r.json().catch(() => ({}));
            throw new Error(error.campos ? Object.values(error.campos).join(' ') : error.error || (r.status === 404 ? 'La publicación ya no está disponible.' : 'No se pudo realizar la operación.'));
        }
        return r.json();
    }
    async function eliminar(publicacion) {
        if (ocupado) return;
        const confirmar = window.prompt('Eliminar la publicación «' + publicacion.titulo + '». Escribe su identificador para confirmar: ' + publicacion.id);
        if (confirmar === null) return;
        if (confirmar.trim() !== String(publicacion.id)) { estado.textContent = 'El identificador no coincide. La publicación se conserva.'; return; }
        const motivo = window.prompt('Motivo de eliminación (obligatorio, máximo 1000 caracteres):');
        if (motivo === null) return;
        if (!motivo.trim() || motivo.trim().length > 1000) { estado.textContent = 'Escribe un motivo de 1 a 1000 caracteres.'; return; }
        ocupado = true; botones(); estado.textContent = 'Eliminando publicación...';
        let eliminada = false;
        try {
            await api('/api/admin/publicaciones/' + encodeURIComponent(publicacion.id), {
                method: 'DELETE', headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({idConfirmacion: publicacion.id, motivo: motivo.trim()})
            });
            eliminada = true;
        } catch (error) { estado.textContent = error.message; }
        finally { ocupado = false; botones(); }
        if (eliminada) {
            const recargada = await cargar(0);
            estado.textContent = recargada ? 'Publicación eliminada y acción registrada.' : 'Publicación eliminada y acción registrada. Pulsa Actualizar para recargar la lista.';
        }
    }
    async function cargar(destino = pagina) {
        if (ocupado) return false;
        ocupado = true; botones(); estado.textContent = 'Consultando publicaciones...'; lista.replaceChildren(); paginaTexto.textContent = '';
        try {
            const r = await api('/api/admin/publicaciones?pagina=' + destino + '&tamano=20');
            pagina = r.pagina; totalPaginas = r.totalPaginas;
            if (pagina > 0 && !r.publicaciones.length) { pagina = Math.max(0, totalPaginas - 1); estado.textContent = 'La lista cambió. Pulsa Actualizar.'; return false; }
            for (const publicacion of r.publicaciones) {
                const tarjeta = document.createElement('article'); tarjeta.className = 'card publicacion-admin';
                const titulo = document.createElement('h3'); titulo.textContent = '#' + publicacion.id + ' — ' + publicacion.titulo;
                const info = document.createElement('p'); info.textContent = 'Autor: ' + publicacion.nombreUsuarioAutor + ' · Estado: ' + publicacion.estado + ' · ' + new Date(publicacion.fecha).toLocaleString('es-GT');
                const detalles = document.createElement('details'), abrir = document.createElement('summary'), texto = document.createElement('p');
                abrir.textContent = 'Ver contenido'; texto.textContent = publicacion.contenido; texto.style.whiteSpace = 'pre-wrap'; texto.style.overflowWrap = 'anywhere'; detalles.append(abrir, texto);
                const borrar = document.createElement('button'); borrar.type = 'button'; borrar.className = 'btn btn-secundario'; borrar.textContent = 'Eliminar publicación';
                borrar.setAttribute('aria-label', 'Eliminar publicación ' + publicacion.titulo); borrar.addEventListener('click', () => eliminar(publicacion));
                tarjeta.append(titulo, info, detalles, borrar); lista.append(tarjeta);
            }
            estado.textContent = r.totalPublicaciones ? r.totalPublicaciones + ' publicaciones disponibles.' : 'No hay publicaciones disponibles para administrar.';
            paginaTexto.textContent = totalPaginas ? 'Página ' + (pagina + 1) + ' de ' + totalPaginas : '';
            return true;
        } catch (error) { estado.textContent = error.message; return false; }
        finally { ocupado = false; botones(); }
    }
    anterior.addEventListener('click', () => cargar(pagina - 1)); siguiente.addEventListener('click', () => cargar(pagina + 1)); actualizar.addEventListener('click', () => cargar());
    try {
        const sesion = await obtenerSesion(); if (!sesion) return;
        if (sesion.rol !== 'ADMIN') { estado.textContent = 'Esta sección requiere una cuenta administradora.'; return; }
        contenido.hidden = false; await cargar();
    } catch (error) { estado.textContent = error.message; }
});
