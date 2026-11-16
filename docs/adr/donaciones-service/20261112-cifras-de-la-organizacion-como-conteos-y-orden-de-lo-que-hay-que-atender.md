# Cifras de la organización como conteos en Donaciones, orden "lo que hay que atender primero" y filtros de Organizaciones en el servidor

- Status: proposed
- Date: 2026-11-12
- Deciders: el usuario (aprobación automática de la spec `R11-p2-listas-restantes` con las opciones recomendadas, 2026-11-12); el detalle de la construcción y de los arreglos de la revisión, el constructor del lote `p2-listas-restantes` (agente, sin consulta); este registro, el cierre documental de la oleada P2 (agente). Pendiente de aprobación del equipo en el PR
- Tags: donaciones, contrato, rest, rendimiento, paginado, cliente-liviano, r11, entrega-6

> **Escrito después del código**, en el cierre documental de la oleada P2 (2026-11-12), como desvío
> acordado del ciclo ARCHITECTURAL. `[OBSERVED]` El código actual ya implementa esta decisión
> (`DonacionesIndependientesController#conteos`, `ConteosDeEntidadDTO`,
> `OrdenDonacionesIndependientes`, `FiltroOrganizaciones`; OpenAPI de Donaciones desde 1.11.0).
> Código existente no es aprobación.

## Contexto y Problema

`[OBSERVED]` Las pantallas de la organización leían una página de 100 asignadas y contaban,
agrupaban y ordenaban en memoria: la cantidad de cada filtro de Recibidas, las cifras por estado
de Seguir entregas, el resumen de cada necesidad y "recibidas este mes". Con páginas de 20, la
página 1 podía no traer las que esperan en la puerta, y una cifra sacada del largo de una página
miente. En Organizaciones del admin, que ya iba de a una página, se habían quitado los filtros
Estado, Necesidades y Tipo porque solo miraban la página.

La spec del lote no pedía ADR (aplica el [ADR de cifras como conteos](./20261111-cifras-del-admin-como-conteos-en-un-resumen.md)
a un caso nuevo). El cierre lo registra porque suma un recurso nuevo (`/conteos`), un orden con
semántica de negocio que antes no existía en el servicio y un filtro `id` que se eligió para no
tocar la seguridad.

**Gate A:** recurso y orden nuevos en el contrato. **Gate B:** contrato público y un trade-off de
rendimiento (orden por fecha de registro y no por último cambio) y de seguridad (cómo lee la
organización una sola donación).

## Alternativas Consideradas

1. **Un endpoint de conteos por organización** (`GET /donaciones-independientes/conteos?entidadId=`
   con `necesidadId` opcional) más `orden=ATENCION`, un filtro opcional `id` en el listado y los
   filtros `estado`, `conNecesidades` y `rubro` en `GET /api/entidades` (elegida).
2. **Conteos dentro del listado** (un campo de totales por estado en cada respuesta paginada).
3. **Contar y ordenar en el cliente** sobre una página grande (status quo).
4. **Para el acta de Confirmar recepción, abrirle a la organización `GET /donaciones-independientes/{clave}`.**
5. **Ordenar "lo que hay que atender" por la fecha del último cambio de estado.**

## Decisión

Elegida la alternativa 1.

- `GET /donaciones-independientes/conteos` devuelve `porEstado` (siempre los 8 estados, en el orden
  del ciclo de vida, con 0 si no hay), `entregadasEnElMes` (con el reloj del servicio inyectado,
  `Clock`, zona de Buenos Aires, como el resumen del admin) y `porNecesidad` (solo las necesidades
  con alguna asignada), con un solo `GROUP BY` por estado y necesidad. Mismos permisos que el
  listado: ADMIN, o la organización sobre sí misma.
- `orden=ATENCION` en `GET /donaciones-independientes` (enum `OrdenDonacionesIndependientes`,
  `RECIENTES` por defecto): en la puerta; con problema (`ENTREGA_FALLIDA`, `VENCIDA`); en camino;
  por salir; recibidas. Dentro de cada grupo, por fecha de registro, la más reciente primero, con
  desempate por id.
- Filtro opcional `id` en el listado (arreglos de la ronda 1): el acta de Confirmar recepción lee
  solo su donación con la regla de permisos del listado (`entidadId` propio). Una donación de otra
  organización da una página vacía. Se agregó al final de `FiltroDonacionesIndependientes` y
  `CriterioDonacionesIndependientes`, con un constructor de la forma anterior para no tocar los
  lugares que los construyen.
- `GET /api/entidades` filtra por `estado` (`ACTIVA` o `BAJA`), `conNecesidades` (alguna necesidad
  vigente y no vencida hoy) y `rubro` (recortado, sin distinguir mayúsculas ni tildes; vacío no
  filtra). `FiltroOrganizaciones` pasó a `dto/entidadBeneficiaria` porque la regla ArchUnit prohíbe
  que un `@RestController` dependa de `repositories`.
- Sin migraciones: los índices que hacían falta ya existían.

## Consecuencias

- Bueno: cada cifra de pantalla sale de un conteo del servicio; la página 1 de Recibidas trae
  primero lo que espera en la puerta.
- Bueno: no cambió ningún permiso; el acta sigue sin poder leer donaciones de otra organización.
- Malo: dentro de cada grupo el orden es por fecha de registro, no por el último cambio (aceptado en
  la pregunta 4 de la aprobación: evita una subconsulta al historial por página).
- Malo: Necesidades hace hasta 2 llamadas por necesidad vigente (3 en curso y 3 recibidas), y
  ninguna si el conteo da 0; con muchas vigentes por organización convendría un listado agrupado.
- Malo: la cifra y la página pueden diferir un instante si un estado cambia entre las dos llamadas.
- Neutro: el inicio de la organización sigue leyendo una página de 100 con `orden=ATENCION`, fuera
  de alcance ([DTI-39](../DEUDA_TECNICA.md#dti-39), ítem 1; [DTI-43](../DEUDA_TECNICA.md#dti-43)).

## Análisis de las alternativas descartadas

- **2. Conteos en el listado:** cada página pagaría el `GROUP BY` aunque la pantalla no lo use, y
  cambiaría el cuerpo de un listado que ya tiene clientes (no sería aditivo).
- **3. Contar en el cliente:** es lo que la regla 1 prohíbe; con páginas, además, da cifras falsas.
- **4. Abrir la lectura por clave a la organización:** `[OBSERVED]` esa ruta tiene `@PreAuthorize`
  solo para el donante dueño o el admin; abrirla habría cambiado la seguridad. El filtro `id` usa la
  regla que la organización ya tiene en el listado.
- **5. Último cambio de estado:** más fiel a "lo último que pasó", pero pide una subconsulta al
  historial en cada página o un índice nuevo; se deja para cuando la demo muestre que hace falta.

## Enlaces

- Spec: [R11-p2-listas-restantes](../../specs/completed/R11-p2-listas-restantes-spec.md) (§3, §6.3 y §6.4)
- ADR hermano: [seguimientos e historial paginados por defecto](../20261112-seguimientos-de-logistica-e-historial-del-ranking-paginados-por-defecto.md)
- Contrato: [openapi-donaciones.yaml](../../arquitectura/contratos/openapi-donaciones.yaml)
- Deuda: [DTI-39](../DEUDA_TECNICA.md#dti-39), [DTI-43](../DEUDA_TECNICA.md#dti-43)
