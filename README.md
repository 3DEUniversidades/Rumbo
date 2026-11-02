# Rumbo — Orientacion Vocacional

Plataforma web (PWA) que ayuda a estudiantes de bachillerato a explorar carreras universarias, hacer pruebas vocacionales, llevar una bitacora personal y comparar carreras.

## Requisitos

- **Java 21** instalado ([descargar](https://adoptium.net/))
- Variable de entorno `JAVA_HOME` apuntando a la carpeta del JDK
- No se requiere instalar Maven (se usa Maven Wrapper)
- PostgreSQL 18+ instalado localmente o Docker Desktop para levantar PostgreSQL con `docker compose`. Para trabajar sin PostgreSQL, usar el perfil local `h2`.

### Configurar JAVA_HOME

**Windows (PowerShell como administrador):**
```powershell
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Java\jdk-21", "User")
```

**Linux / macOS:** agregar a `~/.bashrc` o `~/.zshrc`:
```bash
export JAVA_HOME="/ruta/al/jdk-21"
```

## Configuracion local

PostgreSQL es la base predeterminada del equipo. La configuracion compartida esta
en `application.properties`; cada integrante coloca sus credenciales en `.env`,
en la raiz del proyecto. Spring Boot carga ese archivo automaticamente y Git lo
ignora.

Si aun no tienes `.env`, copia la plantilla:

```bash
cp .env.example .env
```

En Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

Si ya existe, conserva sus valores y agrega las variables que falten. Completa
`DB_PASSWORD` con tu contrasena local. `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`
configuran Spring Boot; Docker utiliza el mismo usuario y contrasena. Su variable
`POSTGRES_DB` debe coincidir con el nombre de la base incluido en `DB_URL`.

| Archivo | Funcion |
|---|---|
| `src/main/resources/application.properties` | Configuracion comun y PostgreSQL predeterminado |
| `src/main/resources/application-h2.properties` | Alternativa local con H2 en memoria |
| `src/test/resources/application-test.properties` | H2 aislado para pruebas automatizadas |
| `.env.example` | Plantilla de variables locales sin credenciales reales |
| `.env` | Valores personales, ignorados por Git |

Los antiguos perfiles `postgres` y `local` ya no son necesarios. Traslada a
`.env` los valores personales que tuvieras en `application-local.properties` y
deja de activar esos perfiles. Las propiedades de un perfil activo reemplazan
las equivalentes de `application.properties`.

## Ejecutar con PostgreSQL (predeterminado)

### Opcion A: PostgreSQL con Docker

1. Completar `.env` como se indica arriba.
2. Levantar PostgreSQL:

```bash
docker compose up -d postgres
```

Si ya existe un volumen de PostgreSQL, usa las credenciales con las que se creo:
cambiar `.env` no modifica los usuarios ni contrasenas de una base existente.

3. Ejecutar Spring Boot, sin seleccionar perfiles:

```bash
./mvnw spring-boot:run
```

En Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Abrir http://localhost:8081. Puedes cambiar el puerto con `PORT` en `.env`.

Con la plantilla, la conexion queda asi:

| Dato | Valor |
|---|---|
| Base de datos | `vocacional_db` |
| Usuario | `rumbo_app` |
| Password | El valor que completes en `DB_PASSWORD` |
| Host | `localhost` |
| Puerto | `5432` |

### Opcion B: PostgreSQL instalado localmente

1. Crear el usuario y la base desde una sesion administradora de PostgreSQL,
   eligiendo una contrasena propia:

```sql
CREATE USER rumbo_app WITH PASSWORD 'elige_tu_contrasena';
CREATE DATABASE vocacional_db OWNER rumbo_app;
```

2. Configurar `.env` con la URL, el usuario y la contrasena correspondientes.
3. Arrancar con `./mvnw spring-boot:run` o `.\mvnw.cmd spring-boot:run`.

### Como revisar datos cargados

Puedes revisar los datos de tres formas:

1. Desde la aplicacion web, usando las vistas y endpoints REST.
2. Desde endpoints JSON, por ejemplo `GET http://localhost:8081/api/carreras`.
3. Desde PostgreSQL, conectandose a `vocacional_db` con un cliente como pgAdmin, DBeaver o `psql`.

En PostgreSQL, `data.sql` carga los datos iniciales despues de que JPA prepare el
esquema. Para desactivar esa carga, configura `DB_SQL_INIT_MODE=never` en `.env`.
`DB_DDL_AUTO=update` permite ajustar el esquema durante el desarrollo.

## Ejecutar en local con H2 (alternativa)

Si no puedes instalar o arrancar PostgreSQL, activa `h2`:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
```

En Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=h2"
```

Abrir http://localhost:8081, o el puerto configurado en `PORT`. Este perfil
reemplaza la conexion aunque `.env` contenga variables de PostgreSQL. No requiere
una base externa, empieza sin datos iniciales y pierde sus datos al detenerse.
Puedes registrar cuentas y probar los flujos desde la pagina. `data.sql` se
desactiva porque contiene instrucciones especificas de PostgreSQL.

La consola esta en `/h2-console`, con URL JDBC
`jdbc:h2:mem:rumbo;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`, usuario `sa` y
contrasena vacia. Cloudinary se configura igual que con PostgreSQL.

## Pruebas automatizadas

```powershell
.\mvnw.cmd test
```

En Linux/macOS: `./mvnw test`. Las pruebas que cargan Spring activan el perfil
`test`: usan H2, sin consola ni `data.sql`, y no requieren PostgreSQL o Docker.
Las pruebas de archivos usan almacenamiento de prueba, sin llamar a Cloudinary.

El JDK recomendado es 21. Si ejecutas con JDK 25, la version actual de Mockito
requiere `-Dnet.bytebuddy.experimental=true`, por ejemplo:

```powershell
.\mvnw.cmd test "-Dnet.bytebuddy.experimental=true"
```

Los cambios que dependan de SQL o del comportamiento propio de PostgreSQL tambien
deben verificarse en esa base.

## Usuario por defecto

Al iniciar con PostgreSQL y la carga de datos activa, `data.sql` siembra un
usuario administrador para probar el login. Este usuario no se crea en H2:

| Dato | Valor |
|---|---|
| Correo | `admin@rumbo.com` |
| Password | `clave-de-prueba` |
| Nombre | `Administrador General` |
| Rol | `ADMIN` |

La contrasena se guarda siempre como hash BCrypt, nunca en texto plano.

## Registro de cuentas

Endpoint publico para crear una cuenta de estudiante o colaborador.

`POST /api/auth/registro`

Solicitud:

```json
{
  "nombreUsuario": "juanperez",
  "nombreCompleto": "Juan Perez",
  "correo": "juan@uvg.edu.gt",
  "contrasenia": "contrasenia123",
  "rol": "ESTUDIANTE"
}
```

`rol` acepta unicamente `ESTUDIANTE` o `COLABORADOR`. Un colaborador debe usar un correo
del dominio institucional configurado en `rumbo.correo.dominio-institucional`
(por defecto `uvg.edu.gt`). El estudiante queda con estado `NO_VERIFICADO` y el
colaborador con `VERIFICADO`.

Respuesta `201 Created`:

```json
{
  "idUsuario": "3f1c...",
  "nombreUsuario": "juanperez",
  "nombreCompleto": "Juan Perez",
  "correo": "juan@uvg.edu.gt",
  "rol": "ESTUDIANTE",
  "estadoVerificacion": "NO_VERIFICADO"
}
```

Errores:

| Codigo | Caso | Cuerpo |
|---|---|---|
| `400` | Campos vacios, correo mal formado, contrasenia de menos de 8 caracteres, correo no institucional o rol no permitido | `{"error":"Datos invalidos","campos":{"correo":"..."}}` |
| `409` | Nombre de usuario o correo ya en uso (sin distinguir mayusculas) | `{"error":"El correo electronico ya esta en uso"}` |

La respuesta nunca incluye la contrasena ni su hash.

Ejemplo con curl:

```bash
curl -X POST http://localhost:8081/api/auth/registro \
  -H "Content-Type: application/json" \
  -d "{\"nombreUsuario\":\"juanperez\",\"nombreCompleto\":\"Juan Perez\",\"correo\":\"juan@uvg.edu.gt\",\"contrasenia\":\"contrasenia123\",\"rol\":\"ESTUDIANTE\"}"
```

## Sesion y permisos

El login crea una sesion HTTP en el servidor mediante la cookie `JSESSIONID`. La
sesion guarda el id del usuario, su nombre, su rol y la fecha de inicio. El
frontend no usa `localStorage` como fuente de autenticacion y no se debe enviar
el rol desde el cliente para autorizar una operacion.

Consultar la sesion actual:

`GET /api/auth/sesion`

Cierra la sesion e invalida la cookie:

`POST /api/auth/logout`

Las rutas protegidas responden `401 Unauthorized` cuando no existe una sesion y
`403 Forbidden` cuando la sesion existe, pero el rol no tiene permiso. El acceso
a carreras requiere cualquier sesion autenticada. La linea de tiempo de
actividades requiere el rol `ESTUDIANTE`.

Después de iniciar sesión, todos los usuarios son dirigidos a `/inicio.html`,
que concentra la navegación común. El menú muestra el foro solo a usuarios
verificados y conserva las opciones permitidas por la sesión. La gestión del
catálogo continúa restringida al rol encargado de carreras.

## Solicitud de verificacion

Un estudiante no verificado puede consultar su estado con
`GET /api/verificacion/estado` y crear una solicitud con
`POST /api/verificacion/solicitudes`. El id del usuario se obtiene de la
sesion del servidor. Una solicitud pendiente bloquea solicitudes duplicadas;
una solicitud rechazada permite volver a solicitar. Los colaboradores no
pueden crear solicitudes.

## Perfiles y cambios de cuenta

`GET /api/perfil` permite a cualquier usuario autenticado consultar su propio
perfil. Estudiantes y colaboradores pueden editar directamente sus datos
personales, pero los cambios de nombre de usuario y nombre completo se envían
como solicitud pendiente mediante `POST /api/perfil/solicitudes`. Mientras no
exista un flujo de revisión implementado, la solicitud no modifica la cuenta.

Los usuarios autenticados pueden consultar el perfil público de estudiantes y
colaboradores con `GET /api/perfil/publico/{idUsuario}`. La respuesta pública
solo contiene nombre de usuario, nombre completo, descripción personal y foto
de perfil. Desde el foro, el autor de una publicación o respuesta abre esta
información y puede navegar a sus publicaciones filtradas.

Estudiantes y colaboradores pueden subir una foto con
`POST /api/perfil/foto` usando el campo multipart `archivo`, y quitarla con
`DELETE /api/perfil/foto`. Se aceptan JPG, PNG y WebP de hasta 5 MB; la imagen
se guarda en Cloudinary y se devuelve una URL pública recortada a 320x320.

## Bitácora y adjuntos

Los estudiantes pueden adjuntar imágenes JPG/PNG/WebP o PDF a sus entradas de
bitácora. Cada entrada admite hasta 5 archivos de 10 MB como máximo.

- `POST /api/bitacora/{idEntrada}/adjuntos` con el campo multipart `archivo`.
- `GET /api/bitacora/{idEntrada}/adjuntos` lista los adjuntos.
- `GET /api/bitacora/{idEntrada}/adjuntos/{idAdjunto}/contenido` descarga el archivo.
- `DELETE /api/bitacora/{idEntrada}/adjuntos/{idAdjunto}` elimina el adjunto.

Los adjuntos son privados: solo el dueño de la entrada puede listarlos,
descargarlos o eliminarlos. El contenido no se sirve con una URL pública de
Cloudinary; el backend valida la sesión y hace de proxy con una URL firmada.
Para los PDF (`raw`) utiliza la API de descarga privada de Cloudinary con una
autorización que caduca en 5 minutos, usada únicamente por el servidor. Así se
evita depender de la entrega de PDF por CDN, que puede estar bloqueada aunque
la subida esté permitida. Los PDF ya adjuntados usan la misma ruta de descarga.
`GET /api/bitacora` incluye los adjuntos de cada entrada.

## Archivos y Cloudinary

Las fotos y los adjuntos se guardan en Cloudinary. Completa las variables
`CLOUDINARY_*` en tu `.env` o exportalas antes de arrancar la aplicación:

```powershell
$env:CLOUDINARY_CLOUD_NAME="tu-cloud"
$env:CLOUDINARY_API_KEY="tu-api-key"
$env:CLOUDINARY_API_SECRET="tu-api-secret"
```

En Linux/macOS:

```bash
export CLOUDINARY_CLOUD_NAME="tu-cloud"
export CLOUDINARY_API_KEY="tu-api-key"
export CLOUDINARY_API_SECRET="tu-api-secret"
```

No guardes estas credenciales en el repositorio. Si faltan, la aplicación
arranca, pero las operaciones de subida, descarga y eliminación de archivos
responden `503 Service Unavailable`.

## Supervisión de usuarios

El menú de inicio muestra la opción **Supervisión de usuarios** únicamente a
los roles `SUPERVISOR_DE_USUARIOS` y `ADMIN`. La pantalla
`/supervision-usuarios.html` también está protegida por rol en el backend.

El tablero consulta solicitudes pendientes mediante:

- `GET /api/supervision/usuarios/verificaciones`
- `GET /api/supervision/usuarios/perfiles`

Las decisiones se envían con `PUT` a la solicitud correspondiente usando
`{"decision":"APROBAR"}` o `{"decision":"RECHAZAR","motivo":"..."}`.
Rechazar exige un motivo. Aprobar una verificación cambia al estudiante a
`VERIFICADO`; aprobar un cambio de perfil aplica el nombre de usuario y nombre
completo propuestos. Cada decisión crea una notificación y un registro de
supervisión con supervisor, solicitud, tipo, decisión y fecha.

Las notificaciones del usuario se consultan con
`GET /api/supervision/usuarios/notificaciones` y se marcan como leídas con
`PUT /api/supervision/usuarios/notificaciones/{idNotificacion}/leida`.

## Comandos

| Comando | Descripcion |
|---|---|
| `./mvnw spring-boot:run` | Arranca con PostgreSQL (predeterminado) |
| `./mvnw spring-boot:run -Dspring-boot.run.profiles=h2` | Arranca sin PostgreSQL, con H2 en memoria |
| `docker compose up -d postgres` | Levanta PostgreSQL local con Docker |
| `./mvnw test` | Ejecuta los tests con H2 |
| `./mvnw clean package` | Compila el `.jar` para produccion |

> **Windows:** reemplaza `./mvnw` por `mvnw.cmd` en los comandos anteriores.

## Estructura

```
src/main/java/com/rumbo/
  RumboApplication.java      # Punto de entrada
  modelo/                    # Entidades JPA
  persistencia/              # Repositorios JPA
  servicio/                  # Logica de negocio
  controlador/               # Controllers REST

src/main/resources/
  static/                    # Frontend PWA (HTML, CSS, JS)
  application.properties     # Configuracion comun y PostgreSQL predeterminado
  application-h2.properties  # Alternativa local con H2 en memoria

src/test/resources/
  application-test.properties # H2 aislado para pruebas

.env.example                 # Plantilla de variables locales
.env                         # Credenciales locales (ignorado por Git)
```

## Stack

- Java + Spring Boot 3.4
- Spring Data JPA + PostgreSQL (predeterminado) / H2 (alternativa local y tests)
- spring-security-crypto (hash BCrypt)
- Cloudinary (fotos de perfil y adjuntos privados)
- Frontend vanilla (HTML/CSS/JS) como PWA
