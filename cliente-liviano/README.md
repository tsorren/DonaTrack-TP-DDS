# cliente-liviano — Interfaz web de DonaTrack

> **Estado:** en construcción (Entrega 6). La maqueta navegable tiene el recorrido público, el del
> donante, el de la entidad y el del admin, con datos de ejemplo y una sola identidad visual. Fuera
> del perfil `maqueta`, las pantallas leen las APIs de Donaciones, Logística, Incentivos y
> Notificaciones, y sus formularios guardan de verdad (corrida `conexion-pantallas`, 2026-11-10,
> [SPEC-08](../docs/specs/active/SPEC-08-conexion-de-pantallas.md)). Quedan con datos de ejemplo
> Entidades y necesidades y el Mapa de entregadas del público, y Misiones del donante; lo que cada
> pantalla todavía no puede mostrar está en las
> [brechas de datos por pantalla](../docs/requerimientos/brechas-de-datos-por-pantalla-e6.md).
> UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5

---

## Qué es

Es la interfaz web del sistema: un servidor Spring MVC que renderiza vistas Thymeleaf, mejoradas
con HTMX y estilizadas con Pico.css y `tokens.css` (decisiones D17–D21 de
[`requerimientos-e6.md`](../docs/requerimientos/requerimientos-e6.md)). No tiene lógica de negocio:
consume las APIs REST de los servicios a través del gateway.

## Antes de tocar este módulo

- **Reglas del módulo:** [`AGENTS.md`](AGENTS.md). Las respetan las personas y los agentes.
- **Proceso de diseño** (identidad visual, flujos, pantallas, A/B, revisión):
  [`docs/interfaz/README.md`](../docs/interfaz/README.md).
- **Decisiones de diseño tomadas:** [`docs/interfaz/bitacora.md`](../docs/interfaz/bitacora.md).
- **Decisiones de arquitectura:** [`docs/adr/cliente-liviano/`](../docs/adr/cliente-liviano/).

## Cómo levantarlo

```bash
mvn -pl cliente-liviano spring-boot:run -Dspring-boot.run.profiles=maqueta   # http://localhost:8090/
mvn -pl cliente-liviano spring-boot:run                                       # con las APIs (INCENTIVOS_URL, DONACIONES_URL y LOGISTICA_URL)
mvn clean test -pl cliente-liviano -am                                        # reglas de tokens, contraste y vistas
mvn test -pl cliente-liviano -Pvisual                                         # capturas, accesibilidad, toque y desbordes
```

- Puerto 8090. El perfil `maqueta` usa datos de ejemplo y admite `?escenario=<nombre>` (vacío,
  lento, error, sin permiso) y `?variante=<tema>-a|b` para las comparaciones A/B. `/` es la
  portada; cada pantalla se ve en `/<pantalla>` (por ejemplo, `/donante-inicio`).
- Sin el perfil `maqueta`, cada puerto usa su adaptador REST (`cliente.datos.<puerto>=rest`, en
  `application.properties`) con la URL de su API (`cliente.api.<api>.url`: `DONACIONES_URL`,
  `LOGISTICA_URL`, `INCENTIVOS_URL` y `NOTIFICACIONES_URL`); las pantallas que todavía no pasaron a
  datos reales se siguen sirviendo con los datos de las plantillas. Un puerto solo se elige con
  `--cliente.datos.ranking=maqueta|rest`. Quién es la cuenta lo dice el puerto `CuentaActual`: con
  ingreso por Keycloak sale del token; el `?donante=<id>` y el `?entidad=<id>` (la sesión los
  recuerda) valen solo en el perfil `maqueta` con `cliente.datos.cuenta=parametro`. Cada envío real
  (POST, redirección y aviso en `#avisos`, con indicador de carga) termina en `web/Envios`. Cada
  pedido genera un `traceId` que viaja a las APIs en `X-Trace-Id` y es el código de soporte de las
  páginas de error.
- En Claude Code, `.claude/launch.json` lo levanta en el panel del navegador: `cliente-liviano`
  (maqueta), `cliente-liviano-rest`, `incentivos` y `donaciones` (los dos en memoria, sin RabbitMQ).
- La verificación visual baja Playwright y Chromium la primera vez (unos 200 MB) y deja el
  resultado en `target/capturas/`. No corre en el CI
  ([DTI-28](../docs/adr/DEUDA_TECNICA.md#dti-28)).

## Responsabilidades

- **Interfaz de usuario:** las pantallas del [mapa del sitio](../docs/interfaz/mapa-del-sitio.md)
  para el público, los donantes, las entidades y el admin.
- **Consumo de APIs REST:** los contratos de `donaciones-service`, `incentivos-service`,
  `logistica-service` y `notificaciones-service`, a través del gateway.
- **Sesión del usuario:** integración con `auth-service`; los tokens viven del lado del servidor
  (RF-CL-01).

## Contratos REST disponibles para consumo

- [`docs/arquitectura/contratos-rest.md`](../docs/arquitectura/contratos-rest.md): catálogo
  consolidado de endpoints.
- [`docs/arquitectura/contratos/`](../docs/arquitectura/contratos/): especificaciones OpenAPI 3.0.
