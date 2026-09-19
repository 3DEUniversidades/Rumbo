# Rumbo — Orientacion Vocacional

Plataforma web (PWA) que ayuda a estudiantes de bachillerato a explorar carreras universarias, hacer pruebas vocacionales, llevar una bitacora personal y comparar carreras.

## Requisitos

- **Java 17+** instalado ([descargar](https://adoptium.net/))
- No se requiere instalar Maven (se usa Maven Wrapper)

## Ejecutar en local

### Windows
Doble clic en `start.cmd` y abrir http://localhost:8080

### Linux / macOS
```bash
chmod +x mvnw
./mvnw spring-boot:run
```

Abrir http://localhost:8080

## Comandos

| Comando | Descripcion |
|---|---|
| `./mvnw spring-boot:run` | Arranca el servidor en local |
| `./mvnw test` | Ejecuta los tests |
| `./mvnw clean package` | Compila el `.jar` para produccion |

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
