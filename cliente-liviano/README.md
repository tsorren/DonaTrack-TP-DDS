# cliente-liviano — Interfaz web de DonaTrack

> **Estado:** en construcción (Entrega 6). La maqueta navegable tiene el recorrido público, el del
> donante y el de la entidad, más el inicio del admin, con datos de ejemplo y una sola identidad
> visual. Desde la F0 del plan con datos reales (el plan está en el
> [PR 938](https://github.com/tsorren/DonaTrack-TP-DDS/pull/938)), el Ranking del admin consume Incentivos y Mis donaciones del donante, Donaciones; las demás pantallas siguen con datos de ejemplo.
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
mvn -pl cliente-liviano spring-boot:run                                       # con las APIs (INCENTIVOS_URL y DONACIONES_URL)
mvn clean test -pl cliente-liviano -am                                        # reglas de tokens, contraste y vistas
mvn test -pl cliente-liviano -Pvisual                                         # capturas, accesibilidad, toque y desbordes
```

- Puerto 8090. El perfil `maqueta` usa datos de ejemplo y admite `?escenario=<nombre>` (vacío,
  lento, error, sin permiso) y `?variante=<tema>-a|b` para las comparaciones A/B. `/` es la
  portada; cada pantalla se ve en `/<pantalla>` (por ejemplo, `/donante-inicio`).
- Sin el perfil `maqueta`, cada puerto usa su adaptador REST (`cliente.datos.<puerto>=rest`, en
  `application.properties`) con la URL de su API (`cliente.api.<api>.url`); las pantallas que
  todavía no pasaron a datos reales se siguen sirviendo con los datos de las plantillas. Un puerto
  solo se elige con `--cliente.datos.ranking=maqueta|rest`. Sin login todavía, el donante entra con
  `?donante=<id>` (la sesión lo recuerda; `cliente.datos.cuenta=parametro`); sin id, Mis
  donaciones manda a Ingresar. Cada pedido genera un `traceId` que
  viaja a las APIs en `X-Trace-Id` y es el código de soporte de las páginas de error.
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
