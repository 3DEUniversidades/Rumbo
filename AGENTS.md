# Rumbo — Sistema de Orientación Vocacional
Plataforma web (PWA) que ayuda a estudiantes de bachillerato a explorar carreras universitarias, hacer pruebas vocacionales, llevar una bitácora personal de su proceso y comparar carreras entre sí.

## Stack
- Lenguaje: Java (POO)
- Framework / runtime: Spring Boot (Spring Web + Spring Data JPA), con Maven como gestor de dependencias y build
- Base de datos: PostgreSQL vía Spring Data JPA
- Frontend: HTML/CSS/JavaScript vanilla, empaquetado como PWA (`manifest.json` + Service Worker), servido como recursos estáticos desde `src/main/resources/static` dentro del mismo proyecto Spring Boot — un solo artefacto desplegable, sin CORS
- Tests: JUnit 5 (incluido por defecto en `spring-boot-starter-test`) — aún sin cobertura definida, pendiente de que el equipo la establezca

## Comandos
- `mvn spring-boot:run` — arranca el servidor en local (sirve backend y frontend en el mismo puerto)
- `mvn test` — ejecuta los tests (deben pasar antes de cada commit)
- *(pendiente)* — no hay linter configurado todavía; se recomienda Checkstyle o Spotless antes de la primera PR
- `mvn clean package` — compila el `.jar` para producción

## Estructura del proyecto
- `src/main/java/com/rumbo/modelo/` — clases de modelo (entidades JPA: Usuario, Carrera, Actividad, etc.), sin lógica de acceso a datos ni de permisos
- `src/main/java/com/rumbo/persistencia/` — interfaces `JpaRepository`, una por entidad
- `src/main/java/com/rumbo/servicio/` — lógica de negocio y validaciones, apoyada en los repositorios
- `src/main/java/com/rumbo/controlador/` — controladores REST (`@RestController`); reciben y devuelven JSON, validan permisos vía `GestorPermisos`
- `src/main/resources/static/` — frontend de la PWA (HTML, CSS, JS, `manifest.json`, `service-worker.js`)
- `src/test/java/...` — tests, con la misma estructura de paquetes que `src/main/java`

## Convenciones
- Nombres de clases en español, siguiendo el patrón ya definido en el diseño (`Controlador` + nombre, `Persistencia` + nombre, `Servicio` + nombre); PascalCase para clases, camelCase para métodos y variables
- Los tests van en `src/test/java`, replicando la ruta de paquete de la clase que prueban (`FooService.java` → `FooServiceTest.java`)
- Los controladores son siempre `@RestController` puros — nunca `@Controller` con Thymeleaf ni renderizado del lado del servidor
- Toda entrada del usuario se valida en el Controlador (apoyándose en `ValidadorDatos` y `GestorPermisos`) antes de tocar cualquier Modelo o clase de Persistencia

## No hagas
- No instales dependencias nuevas en `pom.xml` sin avisar al equipo — es un repo compartido entre los tres, y una dependencia mal elegida puede afectar a los demás sin que lo noten de inmediato
- No uses `spring-boot-starter-security` completo — solo `spring-security-crypto`, para el hash de contraseñas (BCrypt) sin activar autenticación automática en todas las rutas
- No subas `application-local.properties`, credenciales de PostgreSQL, ni ningún archivo con secretos al repositorio
- No guardes contraseñas en texto plano bajo ninguna circunstancia — siempre pasan por hash antes de tocar la base de datos

## Flujo de trabajo
- Antes de una tarea no trivial, propón un plan y espera mi OK.
- Una tarea a la vez; al terminar, dime qué cambiaste para que lo revise.
- Si no estás seguro al 80%, pregunta. No inventes.

## Documentación

