# ADR-E6-07 — Posición del camión: fuente, privacidad y actualización

- Status: proposed
- Date: 2026-11-06
- Deciders: Grupo 5 (D23, marcada ✅ en [`requerimientos-e6.md`](../requerimientos/requerimientos-e6.md)); D22 y la revisión de la D24, tsorren (2026-11-06), con el plan de trabajo sin autenticación; frecuencia de consulta y motivo del cambio de la D24, propuestos por el agente y aprobados por tsorren el mismo día
- Tags: logistica, cliente-liviano, seguimiento, privacidad, htmx, polling, puertos-y-adaptadores, entrega-6

> **Origen:** decisiones D22, D23 y D24 de `requerimientos-e6.md` (§9 y §10) y tarea 20 del
> [plan E6 sin autenticación](../requerimientos/plan-e6-trabajo-sin-auth.md). La implementación es la
> tarea 6 del plan (HB-05 y HB-06) y la deuda queda en [DTI-35](./DEUDA_TECNICA.md#dti-35). No se toca
> código en esta tarea.

## Contexto y Problema

El enunciado de E6 pide que el donante vea en un mapa sus entregas activas con el camión, su
ubicación y la última actualización (RF-09), y que la entidad vea las entregas que va a recibir
(RF-14). La landing y el mapa público muestran además las donaciones entregadas (RF-02).

Qué hay hoy:

- `[OBSERVED]` `SeguimientoEntregaResponseDTO` informa la ruta, el orden de visita y el avance por
  paradas, pero no la posición del camión: su Javadoc dice "sin GPS en tiempo real".
  `ParadaSeguimientoDTO` no expone direcciones de otras entregas.
- `[OBSERVED]` `Camion` (`logistica-service/.../models/entities/camiones/Camion.java`) guarda
  patente, capacidades, estado y `rutaId`; no tiene posición. Las direcciones sí tienen coordenadas
  (`CoordenadasGeograficas`, `rutas/direccion/`).
- `[OBSERVED]` `GET /api/entregas/mapa` devuelve `EntregaMapaPublicoResponseDTO` con `latitud` y
  `longitud` exactas, aunque su Javadoc dice que no publica la dirección exacta.
- `[OBSERVED]` Logística ya usa un adaptador simulado detrás de un puerto para un sistema externo:
  `ProveedorExternoPlanificacionSimulado implements IServicioExternoPlanificacion`.
- `[DOCUMENTED]` La versión anterior de la D24 y RNF-PRIV-01 pedían coordenadas aproximadas (~1 km)
  en el mapa público y en la posición del camión, salvo para el admin.

Problema: ¿de dónde sale la posición del camión, quién la ve con qué precisión y cómo llega
actualizada al navegador?

## Atributos de Calidad y Drivers de Decisión

Pesos `[INFERRED]`: utilidad del seguimiento para donantes y entidades (RF-09, RF-14) 25 · costo y
riesgo antes del 13/11 20 · privacidad real de las entidades 20 · consistencia con la arquitectura
vigente (REST, gateway, puertos y adaptadores) 15 · testabilidad 10 · camino a una fuente real 10.

## Relaciones Arquitectónicas

1. **Respeta:** [estilo de microservicios con capas y puertos y adaptadores](./20261009-estilo-de-microservicios-con-capas-y-puertos-y-adaptadores.md)
   (`proposed`): la fuente de posición es un puerto de salida con un adaptador simulado.
2. **Respeta:** [estados operativos y administrativos del camión](./logistica-service/20260630-estrategia-de-modelado-para-los-estados-operativos-y-administrativos-del-camion.md)
   (`accepted`): la posición es un dato del camión, no un estado nuevo.
3. **Se apoya en:** [ADR-E6-06, stack del cliente liviano](./cliente-liviano/20261030-stack-del-cliente-liviano-thymeleaf-htmx-pico-tokens-y-mapas.md)
   (`proposed`): HTMX para la actualización y MapLibre para el mapa.
4. **Revisa:** la D24 de `requerimientos-e6.md` y RNF-PRIV-01/02, que no son ADRs. Ningún ADR
   aprobado fija coordenadas aproximadas, así que no se edita ninguno.
5. **Respeta:** [política de evolución de contratos](./20261028-politica-de-evolucion-de-contratos-entre-servicios.md)
   (`accepted`): el campo nuevo del seguimiento es aditivo.

## Alternativas Consideradas

Puntajes de 1 (peor) a 5 (mejor), estimados por el agente (`[INFERRED]`).

### D23 — Fuente de la posición

| Alternativa | Utilidad | Costo | Privacidad | Consistencia | Testab. | Camino real | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| **F1. Simulada detrás del puerto `FuentePosicionCamion`, avanza cada 30 s** | 5 | 4 | 4 | 5 | 5 | 5 | **4,6** |
| F2. Derivada de la última parada confirmada | 2 | 5 | 4 | 5 | 5 | 1 | 3,65 |
| F3. Callback del proveedor externo de logística | 5 | 2 | 4 | 3 | 3 | 5 | 3,7 |
| F4. GPS del chofer desde el navegador | 5 | 1 | 3 | 2 | 2 | 5 | 3,05 |

### D24 — Precisión de la posición del camión y del mapa público

| Alternativa | Utilidad | Costo | Privacidad | Consistencia | Testab. | Camino real | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| **P1. Exacta para todos** | 5 | 5 | 2 | 5 | 5 | 5 | **4,4** |
| P2. Aproximada (~1 km) para todos salvo el admin (versión anterior) | 3 | 3 | 3 | 4 | 3 | 4 | 3,25 |
| P3. Camión exacto solo para quien tiene una parada en esa ruta; aproximado para el resto | 4 | 2 | 3 | 3 | 3 | 4 | 3,15 |
| P4. Sin camión en el mapa salvo para el admin | 1 | 5 | 5 | 4 | 5 | 3 | 3,65 |

### D22 — Actualización en el navegador

| Criterio (peso) | **A. Polling HTMX** | B. Server-Sent Events | C. WebSocket |
|---|:-:|:-:|:-:|
| Simplicidad y costo (20%) | 5 | 2 | 1 |
| Escalabilidad (15%) | 5 | 3 | 3 |
| Frescura (15%) | 3 | 5 | 5 |
| Seguridad (15%) | 4 | 3 | 3 |
| Testabilidad (10%) | 5 | 2 | 2 |
| Resiliencia (10%) | 4 | 3 | 3 |
| Consistencia arquitectónica (10%) | 5 | 3 | 2 |
| Observabilidad (5%) | 3 | 3 | 3 |
| **Total** | **4,35** | **3,00** | **2,70** |

La matriz de A y B es la de la D22 en `requerimientos-e6.md` (versión del 2026-11-06). WebSocket se
suma para dejar escrito por qué se descarta.

## Resultado de la Decisión

Se eligen **F1**, **P1** y **A**.

1. **Fuente simulada detrás de un puerto (D23).**
   - Puerto de salida `FuentePosicionCamion` en Logística; adaptador `FuentePosicionCamionSimulada`.
   - Cada 30 s (`@Scheduled`, con `Clock` inyectado para los tests) avanza cada camión con ruta
     `EN_CURSO` un tramo entre las coordenadas de sus paradas, en el orden de visita.
   - La posición es un value object `PosicionCamion(latitud, longitud, registradaEn)` dentro del
     documento del camión. `registradaEn` es la hora de origen del dato.
   - Idempotencia: una posición con `registradaEn` anterior o igual a la guardada se descarta. Así,
     un adaptador real que reintente o desordene mensajes no hace retroceder al camión.
   - Un callback del proveedor externo o un GPS real se suman como otro adaptador del mismo puerto,
     sin rediseño.
2. **Precisión exacta para todos (D24 revisada).**
   - La posición del camión en el seguimiento y las coordenadas de `GET /api/entregas/mapa` son las
     exactas, para cualquier rol y también sin login.
   - Lo que no cambia: el texto de la dirección (calle, altura, piso, departamento, código postal) no
     sale en el mapa público ni en las paradas ajenas. Solo lo ve quien tiene la entrega (RNF-PRIV-02).
   - Motivo: las direcciones de destino son de entidades beneficiarias, que son organizaciones, no
     personas. Desplazar el punto ~1 km da una privacidad engañosa: el camión igual se detiene en el
     punto exacto, y la lista de entidades es conocida. Además, simplifica el código y hace más fiel la
     demo.
   - HB-07 ya no cambia coordenadas: queda solo la corrección del Javadoc de
     `EntregaMapaPublicoResponseDTO` (tarea 22 del plan).
3. **Polling con HTMX (D22).**
   - El seguimiento suma un campo aditivo `posicionCamion` (`latitud`, `longitud`, `registradaEn`) en
     `SeguimientoEntregaResponseDTO`, `null` cuando el camión no está en ruta (HB-06).
   - El cliente liviano expone un endpoint propio que devuelve **solo el fragmento HTML** del marcador
     del camión y su "última actualización". La página lo pide con
     `hx-trigger="every 30s [document.visibilityState=='visible']"`.
   - 30 s es la frecuencia de la fuente: consultar más seguido no trae datos más frescos.
   - El fragmento es una plantilla Thymeleaf aislada, para que un SSE futuro (extensión `sse` de
     HTMX, `sse-swap`) empuje el mismo HTML sin rehacer la vista.
   - SSE pasa a convenir si el negocio pide frescura de pocos segundos, si el rate limiting de E7
     vuelve caro el polling o si E7 adopta gRPC en streaming entre Logística y el cliente.

Justificación: F1 sigue el patrón de puertos y adaptadores que Logística ya usa (`IServicioExternoPlanificacion`)
y deja la fuente real como un adaptador más (OCP, inversión de dependencias). `PosicionCamion` es un
value object del agregado `Camion` (DDD), su Experto (GRASP): ningún otro agregado la escribe. El polling
(A) no abre un canal nuevo entre servicios ni hace que el cliente consuma RabbitMQ: reusa REST y el gateway.

## Consecuencias

### Positivas

- RF-09 y RF-14 muestran un camión que se mueve y su "última actualización", sin infraestructura
  nueva.
- Cada consulta se autoriza de nuevo con el token vigente cuando llegue SPEC-06.
- Cambiar la simulación por un GPS real o por el proveedor externo no toca dominio, DTO ni cliente.
- El mapa público y el seguimiento usan una sola regla de precisión: no hay redondeo que mantener ni
  testear.

### Negativas

- **Privacidad:** el recorrido exacto del camión, visto a lo largo del tiempo, deja ver dónde reciben
  **otras** entidades de la misma ruta. Se acepta porque son organizaciones con dirección
  institucional; si se suman destinatarios que sean personas, esta decisión se revisa.
- El mapa público muestra el punto exacto de cada entrega completada, aunque no el texto de la
  dirección.
- Con 200 mapas abiertos a 30 s son ~7 consultas por segundo hacia Logística. El rate limiting de E7
  tiene que contarlas o excluirlas.
- El marcador salta cada 30 s; no hay movimiento continuo.
- El simulador escribe en Mongo cada 30 s por cada camión en ruta.
- `[OBSERVED]` Textos que hoy prometen lo contrario: las plantillas `donante-seguimiento`,
  `entidad-seguimiento-confirmar`, `publico-mapa`, `publico-legales` y `viaje-datos` del cliente
  liviano, y las fichas de flujo `raices-seguir-entregas`, `raices-confirmar-recepcion`,
  `raices-publico-navegable`, `raices-legales` y `raices-ab-huecos-backend`, dicen que la ubicación es
  aproximada. Hay que corregirlos antes de publicar el mapa con datos reales, en especial los
  legales (DTI-35, ítem 4).

## Análisis por alternativa

- **F1.** Pro: realista en la demo y con la fuente real como un adaptador más. Contra: un
  `@Scheduled` y un campo nuevo en el documento del camión.
- **F2.** Pro: casi sin código. Contra: el camión salta de parada en parada y no hay "en camino"
  entre dos paradas; ningún camino a un GPS real.
- **F3.** Pro: es la fuente real. Contra: contrato de entrada, seguridad e idempotencia nuevos, con
  un proveedor que hoy es la misma Logística en otra instancia.
- **F4.** Pro: datos reales. Contra: un rol nuevo `CHOFER`, login del chofer y permisos de ubicación
  del navegador; fuera de alcance de E6.
- **P1.** Pro: una sola regla, sin costo, útil para todos. Contra: el recorrido expone las paradas
  de otras entidades.
- **P2.** Pro: oculta el punto exacto a donantes y entidades. Contra: el desplazamiento se anula
  cruzando el punto con la lista conocida de entidades o mirando dónde se detiene el camión; además,
  la entidad ve su propio camión a ~1 km de su puerta.
- **P3.** Pro: exacto para quien lo espera. Contra: necesita pertenencia (HB-13, SPEC-06) y dos
  versiones del mismo DTO; sin auth no se puede implementar en E6.
- **P4.** Pro: máxima privacidad. Contra: incumple RF-09 ("camión, ubicación y última
  actualización").
- **A.** Pro: stateless, trivial de testear, sin canal nuevo. Contra: hasta 30 s de retraso más la
  frecuencia de la fuente, y tráfico de lectura constante.
- **B.** Pro: frescura inmediata y movimiento continuo. Contra: un evento nuevo, el cliente
  consumiendo RabbitMQ (un canal nuevo hacia la UI), heartbeats, conexiones que sobreviven al
  vencimiento del token y filtro por pertenencia en cada evento. Con la fuente a 30 s, ahorra como
  mucho 30 s.
- **C.** Pro: canal bidireccional. Contra: el navegador no le manda nada al servidor; suma la
  complejidad de B sin beneficio `[REJECTED]` en la D22.

## Validación

- Test del dominio: `PosicionCamion` rechaza coordenadas fuera de rango y la posición más vieja que
  la guardada no reemplaza a la nueva.
- Test del adaptador simulado con `Clock` fijo: dos ticks avanzan el camión dos tramos en el orden
  de visita; un camión sin ruta `EN_CURSO` no se mueve.
- Test de contrato: `SeguimientoEntregaResponseDTO` con `posicionCamion` opcional, en
  `openapi-logistica.yaml`.
- Test del cliente: el endpoint de fragmento devuelve solo el marcador, sin `<html>`.

## Trabajo futuro

- [DTI-35](./DEUDA_TECNICA.md#dti-35): implementación (tarea 6 del plan, HB-05 y HB-06).
- Tarea 22 del plan: Javadoc de `EntregaMapaPublicoResponseDTO` alineado con P1.
- E7: dimensionar el rate limiting para las consultas del seguimiento o pasar a SSE.
