const accesosRegistro = [...document.querySelectorAll('[data-crear-cuenta]')];
const opcionesSinSesion = [...document.querySelectorAll('[data-sin-sesion]')];
const textosRegistro = accesosRegistro.map(enlace => enlace.textContent);

// La portada es pública. Un 401 mantiene las opciones de acceso, sin redirigir.
async function actualizarAccesosPortada() {
    accesosRegistro.forEach((enlace, indice) => {
        enlace.href = '/login.html#crear-cuenta';
        enlace.textContent = textosRegistro[indice];
    });
    opcionesSinSesion.forEach(elemento => { elemento.hidden = false; });

    const controlador = new AbortController();
    const limite = window.setTimeout(() => controlador.abort(), 4000);
    try {
        const respuesta = await fetch('/api/auth/sesion', {
            credentials: 'same-origin', cache: 'no-store', signal: controlador.signal
        });
        if (!respuesta.ok) return;
        const sesion = await respuesta.json();
        if (!sesion.idUsuario) return;

        accesosRegistro.forEach(enlace => {
            enlace.href = '/inicio.html';
            enlace.textContent = enlace.dataset.textoSesion || 'Continuar mi proceso';
        });
        opcionesSinSesion.forEach(elemento => { elemento.hidden = true; });
    } catch {
        // Los enlaces de registro/login siguen funcionando si falla la consulta.
    } finally {
        window.clearTimeout(limite);
    }
}

window.addEventListener('pageshow', actualizarAccesosPortada);
