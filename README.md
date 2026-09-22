# Rumbo — Orientacion Vocacional

Plataforma web (PWA) que ayuda a estudiantes de bachillerato a explorar carreras universarias, hacer pruebas vocacionales, llevar una bitacora personal y comparar carreras.

## Requisitos

- **Java 21** instalado ([descargar](https://adoptium.net/))
- Variable de entorno `JAVA_HOME` apuntando a la carpeta del JDK
- No se requiere instalar Maven (se usa Maven Wrapper)

### Configurar JAVA_HOME

**Windows (PowerShell como administrador):**
```powershell
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Java\jdk-21", "User")
```

**Linux / macOS:** agregar a `~/.bashrc` o `~/.zshrc`:
```bash
export JAVA_HOME="/ruta/al/jdk-21"
```

## Ejecutar en local

```bash
./mvnw spring-boot:run
```

Abrir http://localhost:8080

> **Windows sin Git Bash:** usa `mvnw.cmd spring-boot:run` en su lugar.

## Comandos

| Comando | Descripcion |
|---|---|
| `./mvnw spring-boot:run` | Arranca el servidor en local |
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
```

## Stack

- Java + Spring Boot 3.4
- Spring Data JPA + H2 (dev) / PostgreSQL (produccion)
- spring-security-crypto (hash BCrypt)
- Frontend vanilla (HTML/CSS/JS) como PWA
