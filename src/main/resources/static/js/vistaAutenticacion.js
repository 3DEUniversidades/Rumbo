const seccionLogin = document.getElementById('login');
const seccionRegistro = document.getElementById('registro');

function mostrarSeccion(seccion) {
    seccionLogin.hidden = seccion !== 'login';
    seccionRegistro.hidden = seccion !== 'registro';
}

document.getElementById('ir-a-registro').addEventListener('click', function (event) {
    event.preventDefault();
    mostrarSeccion('registro');
});

document.getElementById('ir-a-login').addEventListener('click', function (event) {
    event.preventDefault();
    mostrarSeccion('login');
});

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
            window.location.href = '/inicio.html';
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

const registroForm = document.getElementById('registro-form');

function limpiarErroresRegistro() {
    document.getElementById('registro-error').style.display = 'none';
    document.getElementById('registro-exito').style.display = 'none';
    registroForm.querySelectorAll('.campo-error').forEach(function (span) {
        span.textContent = '';
        span.style.display = 'none';
    });
}

function marcarErrorCampo(campo, mensaje) {
    const span = registroForm.querySelector('.campo-error[data-campo="' + campo + '"]');
    if (span) {
        span.textContent = mensaje;
        span.style.display = 'block';
    } else {
        const general = document.getElementById('registro-error');
        general.textContent = mensaje;
        general.style.display = 'block';
    }
}

function validarRegistroCliente(datos) {
    let valido = true;

    if (datos.nombreUsuario === '') {
        marcarErrorCampo('nombreUsuario', 'El nombre de usuario es obligatorio');
        valido = false;
    }
    if (datos.nombreCompleto === '') {
        marcarErrorCampo('nombreCompleto', 'El nombre completo es obligatorio');
        valido = false;
    }
    if (datos.correo === '') {
        marcarErrorCampo('correo', 'El correo electronico es obligatorio');
        valido = false;
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(datos.correo)) {
        marcarErrorCampo('correo', 'El correo electronico no tiene un formato valido');
        valido = false;
    }
    if (datos.contrasenia.length < 8) {
        marcarErrorCampo('contrasenia', 'La contrasenia debe tener al menos 8 caracteres');
        valido = false;
    }
    if (datos.rol === '') {
        marcarErrorCampo('rol', 'El rol es obligatorio');
        valido = false;
    }

    return valido;
}

registroForm.addEventListener('submit', async function (event) {
    event.preventDefault();

    const datos = {
        nombreUsuario: document.getElementById('registro-nombreUsuario').value.trim(),
        nombreCompleto: document.getElementById('registro-nombreCompleto').value.trim(),
        correo: document.getElementById('registro-correo').value.trim(),
        contrasenia: document.getElementById('registro-contrasenia').value,
        rol: document.getElementById('registro-rol').value
    };

    const btn = this.querySelector('button[type="submit"]');
    const mensajeError = document.getElementById('registro-error');
    const mensajeExito = document.getElementById('registro-exito');

    limpiarErroresRegistro();

    if (!validarRegistroCliente(datos)) {
        return;
    }

    btn.disabled = true;
    btn.textContent = 'Registrando...';

    try {
        const respuesta = await fetch('/api/auth/registro', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(datos)
        });

        if (respuesta.ok) {
            registroForm.reset();
            mensajeExito.textContent = 'Cuenta creada correctamente. Ya puedes iniciar sesion.';
            mensajeExito.style.display = 'block';
            document.getElementById('correo').value = datos.correo;
            setTimeout(function () {
                mostrarSeccion('login');
                mensajeExito.style.display = 'none';
            }, 1800);
        } else {
            const cuerpo = await respuesta.json().catch(function () {
                return {};
            });

            if (cuerpo.campos) {
                Object.keys(cuerpo.campos).forEach(function (campo) {
                    marcarErrorCampo(campo, cuerpo.campos[campo]);
                });
            } else {
                mensajeError.textContent = cuerpo.error || 'No se pudo completar el registro';
                mensajeError.style.display = 'block';
            }
        }
    } catch (error) {
        mensajeError.textContent = 'No se pudo conectar con el servidor';
        mensajeError.style.display = 'block';
    } finally {
        btn.disabled = false;
        btn.textContent = 'Registrarme';
    }
});
