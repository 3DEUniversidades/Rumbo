document.getElementById('login-form').addEventListener('submit', async function (event) {
    event.preventDefault();

    const correo = document.getElementById('correo').value.trim();
    const contrasenia = document.getElementById('contrasenia').value;
    const btn = this.querySelector('button[type="submit"]');
    const mensajeError = document.getElementById('mensaje-error');
    const mensajeExito = document.getElementById('mensaje-exito');

    mensajeError.style.display = 'none';
    mensajeExito.style.display = 'none';

    btn.disabled = true;
    btn.textContent = 'Ingresando...';

    try {
        const respuesta = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ correo, contrasenia })
        });

        if (respuesta.ok) {
            const datos = await respuesta.json();
            localStorage.setItem('sesionRumbo', JSON.stringify({
                idUsuario: datos.idUsuario,
                rol: datos.rol,
                nombreCompleto: datos.nombreCompleto
            }));
            mensajeExito.textContent = 'Bienvenido/a, ' + datos.nombreCompleto;
            mensajeExito.style.display = 'block';
            // TODO: redirigir segun rol cuando existan esas vistas
        } else {
            mensajeError.textContent = 'Correo o contrasena incorrectos';
            mensajeError.style.display = 'block';
        }
    } catch (error) {
        mensajeError.textContent = 'No se pudo conectar con el servidor';
        mensajeError.style.display = 'block';
    } finally {
        btn.disabled = false;
        btn.textContent = 'Ingresar';
    }
});
