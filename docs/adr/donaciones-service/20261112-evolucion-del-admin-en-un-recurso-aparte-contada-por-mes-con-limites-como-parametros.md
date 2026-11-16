# Evolución del inicio del admin en un recurso aparte, contada por mes con los límites de cada mes como parámetros

- Status: proposed
- Date: 2026-11-12
- Deciders: el usuario (aprobación automática de la ficha R11-42 con las opciones recomendadas, 2026-11-12); los detalles de la construcción, el constructor del lote `p2-admin-graficos` (agente, sin consulta); este registro, el cierre documental de la oleada P2 (agente). Pendiente de aprobación del equipo en el PR
- Tags: donaciones, contrato, rest, persistencia, zona-horaria, cliente-liviano, r11, entrega-6

> **Escrito después del código**, en el cierre documental de la oleada P2 (2026-11-12), como desvío
> acordado del ciclo ARCHITECTURAL. `[OBSERVED]` El código actual ya implementa esta decisión
> (`EvolucionAdminService`, `EvolucionAdminDTO`,
> `DonacionIndependienteRepositoryJpaAdapter#contarRegistradasPorMes` y
> `#contarQuePasaronAPorMes`; OpenAPI de Donaciones 1.12.0). Código existente no es aprobación.
>
> La ficha (§9, primera decisión) decía que no hacía falta ADR, porque el endpoint es aditivo y
> aplica las cifras contadas en la base de R11-01. El cierre lo registra igual porque la
> construcción tomó una decisión que la ficha no tenía: cómo se cuenta un mes cuando el servicio
> guarda en UTC y la pantalla habla en hora de Buenos Aires.

## Contexto y Problema

R11-42 pide dos gráficos en el inicio del admin: registradas y entregas confirmadas de los últimos
12 meses, por mes y por categoría. `[OBSERVED]` Donaciones guarda las fechas con
`hibernate.jdbc.time_zone=UTC`. Agrupar en la base por `date_trunc('month', …)` o `to_char` agrupa
por mes de UTC: una donación del 31 a las 22 h de Buenos Aires caería en el mes siguiente. Además,
el resumen del admin (`GET /api/donaciones/resumen`) ya está en la pantalla y una falla de las
series no debería taparlo.

Problema: ¿dónde viven las series y cómo se cuenta un mes sin traer las donaciones?

**Gate A:** recurso nuevo y una regla de agrupación temporal que el servicio no tenía. **Gate B:**
contrato público, consistencia temporal (la misma regla que el
[ADR de consistencia temporal](../20260901-consistencia-temporal-y-normalizacion-semantica-de-eventos.md)
cuida para los eventos) y rendimiento.

## Alternativas Consideradas

1. **Recurso aparte `GET /api/donaciones/resumen/evolucion`** (solo ADMIN, sin parámetros), con una
   sola consulta por serie que tiene una columna por mes (`count(*) FILTER (WHERE … >= :desdeN AND
   … < :hastaN)`) y los límites de cada mes calculados en Java con el reloj del servicio (elegida).
2. **Agrupar en la base** con `date_trunc`/`to_char` y `AT TIME ZONE`.
3. **Campos nuevos en `/resumen`.**
4. **Traer las donaciones del año y contar en el cliente.**
5. **Una tabla de agregados por mes** mantenida al registrar y al entregar.

## Decisión

Elegida la alternativa 1.

- `EvolucionAdminService` (servicio aparte de `ResumenAdminService`, para que el resumen no sume la
  dependencia del catálogo ni cambien sus tests) arma 12 meses contando el actual
  (`America/Argentina/Buenos_Aires`), o desde el mes de la primera donación si es posterior.
- Los métodos por mes del puerto reciben `YearMonth desde` y `hasta` (los dos incluidos); el
  adaptador pasa a la base los límites de cada mes como parámetros, convertidos con la misma zona
  con que se guardaron las fechas. Los métodos por categoría reciben el tramo `[desde, hasta)` en
  `LocalDateTime`.
- "Entregas confirmadas" cuenta las que pasaron a `ENTREGADA`, la misma cuenta que
  `entregadasEnElMes` del resumen.
- La unidad es la donación independiente. El catálogo de categorías (8 hoy) se lee entero dentro de
  Donaciones para incluir las que dieron 0, con un tope de 8 más "Otras"; las donaciones nunca se
  traen, se cuentan.
- El cliente dibuja SVG en el servidor, sin librería; una falla del recurso muestra el aviso dentro
  de la tarjeta de los gráficos y deja el resto del inicio.

## Consecuencias

- Bueno: el mes de cada donación es el de Buenos Aires, igual que el resto de las cifras de la
  pantalla; dos consultas por serie, sin traer filas.
- Bueno: una falla de las series no tapa las cifras de arriba (el mismo criterio que la tarjeta de
  Camiones, R11-21).
- Malo: la consulta se arma con 12 columnas y 24 parámetros; cambiar la ventana a otra cantidad de
  meses cambia la consulta.
- Malo: el total por categoría lo suma el cliente sobre las filas; coincide con el total por mes
  solo mientras cada independiente tenga una sola subcategoría (supuesto de la ficha, sin un test
  que lo fije) ([DTI-43](../DEUDA_TECNICA.md#dti-43)).
- Neutro: si el catálogo de categorías crece, convendrá un `LEFT JOIN` desde la categoría en lugar
  de `findAll`.

## Análisis de las alternativas descartadas

- **2. Agrupar en la base con la zona:** posible con `AT TIME ZONE`, pero dependería de que la base
  y el servicio interpreten igual el `timestamp` sin zona que guarda Hibernate; con los límites
  como parámetros la conversión es la de Java, la misma del guardado.
- **3. Campos en `/resumen`:** una falla o una consulta lenta de las series taparía las cifras, y el
  resumen dejaría de ser barato.
- **4. Contar en el cliente:** rompe la regla 1 del backlog (miles de filas por pedido).
- **5. Tabla de agregados:** más rápida al leer, pero suma escritura en dos flujos y una migración
  para algo que se ve una vez por visita al inicio; no se justifica con los volúmenes de la demo.

## Enlaces

- Ficha: [R11-42 · Los últimos 12 meses, en gráficos](../../interfaz/flujos/raices-entidad-navegable.md#r11-42--los-últimos-12-meses-en-gráficos)
- Patrón de origen: [cifras del admin como conteos](./20261111-cifras-del-admin-como-conteos-en-un-resumen.md)
- Contrato: [openapi-donaciones.yaml](../../arquitectura/contratos/openapi-donaciones.yaml) (1.12.0)
- Deuda: [DTI-43](../DEUDA_TECNICA.md#dti-43)
