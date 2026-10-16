async function obtenerSesion() {
    const respuesta = await fetch('/api/auth/sesion');
    if (respuesta.status === 401) {
        window.location.replace('/login.html');
        return null;
    }
    if (!respuesta.ok) {
        throw new Error('No se pudo consultar la sesion');
    }
    return respuesta.json();
}
