document.addEventListener('DOMContentLoaded', async () => {
    const enlace = document.getElementById('enlace-gestion-actividades');
    if (!enlace) return;
    try {
        const sesion = await obtenerSesion();
        enlace.hidden = !sesion || sesion.rol !== 'ENCARGADO_DE_CARRERAS';
    } catch {
        enlace.hidden = true;
    }
});
