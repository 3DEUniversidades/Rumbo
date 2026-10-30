document.addEventListener('DOMContentLoaded', async () => {
    const enlace = document.getElementById('enlace-gestion-actividades');
    if (!enlace) return;
    try {
        const sesion = await obtenerSesion();
        enlace.hidden = !sesion || !['ENCARGADO_DE_CARRERAS', 'ADMIN'].includes(sesion.rol);
    } catch {
        enlace.hidden = true;
    }
});
