# Rumbo — Orientacion Vocacional

Plataforma web (PWA) que ayuda a estudiantes de bachillerato a explorar carreras universarias, hacer pruebas vocacionales, llevar una bitacora personal y comparar carreras.

## Requisitos

- **Java 21** instalado ([descargar](https://adoptium.net/))
- Variable de entorno `JAVA_HOME` apuntando a la carpeta del JDK
- No se requiere instalar Maven (se usa Maven Wrapper)
- PostgreSQL 18+ instalado localmente o Docker Desktop para levantar PostgreSQL con `docker compose`

### Configurar JAVA_HOME

**Windows (PowerShell como administrador):**
```powershell
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Java\jdk-21", "User")
```

**Linux / macOS:** agregar a `~/.bashrc` o `~/.zshrc`:
```bash
export JAVA_HOME="/ruta/al/jdk-21"
```

## Ejecutar en local con H2

Por defecto el proyecto usa H2 en memoria. Esto permite abrir la aplicacion sin configurar PostgreSQL, util para revisar la PWA o correr pruebas rapidas.

```bash
./mvnw spring-boot:run
```

Abrir http://localhost:8080

> **Windows sin Git Bash:** usa `mvnw.cmd spring-boot:run` en su lugar.

## Base de datos local con PostgreSQL

La base objetivo del proyecto es PostgreSQL. El repositorio no guarda credenciales reales ni archivos de datos de PostgreSQL. Solo guarda la configuracion reproducible para que cada integrante o el auxiliar pueda levantar la base.

### Opcion A: PostgreSQL con Docker

1. Copiar el archivo de ejemplo:

```bash
cp .env.example .env
```

En Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

2. Levantar PostgreSQL:

```bash
docker compose up -d postgres
```

3. Ejecutar Spring Boot usando el perfil PostgreSQL:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

En Windows:

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=postgres
```

Si PowerShell interpreta mal el argumento del perfil, usa comillas:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=postgres"
```

Con la configuracion de ejemplo, la conexion queda asi:

| Dato | Valor |
|---|---|
| Base de datos | `vocacional_db` |
| Usuario | `rumbo_app` |
| Password | `rumbo_app_dev` |
| Host | `localhost` |
| Puerto | `5432` |

### Opcion B: PostgreSQL instalado localmente

1. Crear la base y usuario en PostgreSQL:

```sql
CREATE DATABASE vocacional_db;
CREATE USER rumbo_app WITH PASSWORD 'rumbo_app_dev';
GRANT ALL PRIVILEGES ON DATABASE vocacional_db TO rumbo_app;
```

2. Copiar el ejemplo local:

```bash
cp src/main/resources/application-local.example.properties src/main/resources/application-local.properties
```

En Windows PowerShell:

```powershell
Copy-Item src/main/resources/application-local.example.properties src/main/resources/application-local.properties
```

3. Ajustar usuario/password si son diferentes.

4. Ejecutar con perfiles `postgres,local`:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres,local
```

En Windows:

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=postgres,local
```

Si PowerShell interpreta mal el argumento del perfil, usa comillas:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=postgres,local"
```

`application-local.properties` esta ignorado por Git y no debe subirse al repositorio.

### Como revisar datos cargados

Cuando existan entidades JPA y datos iniciales, el auxiliar podra revisar los datos de tres formas:

1. Desde la aplicacion web, usando las vistas y endpoints REST.
2. Desde endpoints JSON, por ejemplo `GET http://localhost:8080/api/carreras` cuando exista el controlador.
3. Desde PostgreSQL, conectandose a `vocacional_db` con un cliente como pgAdmin, DBeaver o `psql`.

Si agregamos carreras iniciales con `data.sql`, tambien se documentara el modo de activarlas. Por ahora `spring.sql.init.mode` queda en `never` para evitar insertar datos antes de que existan entidades y tablas revisadas.

## Usuario por defecto

Al iniciar, `data.sql` siembra un usuario administrador para probar el login:

| Dato | Valor |
|---|---|
| Correo | `admin@rumbo.com` |
| Password | `clave-de-prueba` |
| Nombre | `Administrador General` |
| Rol | `ADMIN` |

La contrasena se guarda siempre como hash BCrypt, nunca en texto plano.

## Comandos

| Comando | Descripcion |
|---|---|
| `./mvnw spring-boot:run` | Arranca el servidor en local |
| `./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres` | Arranca usando PostgreSQL |
| `docker compose up -d postgres` | Levanta PostgreSQL local con Docker |
| `./mvnw test` | Ejecuta los tests |
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
  application.properties     # Configuracion
  application-postgres.properties
                              # Perfil PostgreSQL reproducible
  application-local.example.properties
                              # Plantilla local privada
```

## Stack

- Java + Spring Boot 3.4
- Spring Data JPA + H2 (dev rapido/tests) / PostgreSQL (desarrollo compartido y produccion futura)
- spring-security-crypto (hash BCrypt)
- Frontend vanilla (HTML/CSS/JS) como PWA
