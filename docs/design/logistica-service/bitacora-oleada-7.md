# Bitácora — Oleada 7: limpieza legacy

**Branch:** `E4_refactor_logistica`
**Referencia del plan:** `docs/design/logistica-service/plan-refactor-logistica-service.md` → Oleada 7 (RF-07/RF-08)

---

## Limpiezas realizadas

### Imports

Se reemplazaron los wildcard imports por imports explícitos en:

- `IEntregasController`;
- `IRutasController`;
- `CamionesController`;
- `ChoferesController`.

El barrido sobre esos cuatro archivos devuelve cero `import ...*`.

### API de entregas de `Ruta`

Se eligió `Ruta.getEntregaIds()` como nombre único porque la lista contiene UUID y no entidades
`Entrega`. Se eliminaron `getEntregas()` y `obtenerEntregas()`, y se actualizaron todos los call
sites de producción y tests.

El getter conserva `List.copyOf`, por lo que mantiene el snapshot defensivo e inmutable.

### Disponibilidad de seguimiento

La regla `estado != PENDIENTE` ahora se expresa como
`Ruta.tieneSeguimientoDisponible()`. `RutaMapper` pregunta al agregado y sólo conserva la tarea de
generar/mapear la URL.

### Queries de repositorio

`ValidadorPatentes` recibe `ICamionRepository` y consulta
`findByPatente(ValidadorPatentes.normalizar(patente))`; `CamionesService.crear` dejó de cargar y
recorrer `findAll()`.

El Application Service de planificación ya usaba `findSinRuta()` y `findDisponibles()` después de
Oleada 5. `GeneradorDeRutas` tampoco realizaba refiltrados. Se verificó que ninguno de los tres
puntos objetivo conserve llamadas a `findAll()`.

---

## DTOs sin uso: decisión conservadora

`EntregaPlanificadaDTO` y `SolicitudPlanificacionRequestDTO` siguen sin call sites reales: el
barrido sólo encuentra sus declaraciones.

El circuito vigente utiliza:

- `PlanificacionSolicitada` al invocar `IServicioExternoPlanificacion`;
- `CallbackPlanificacionRequestDTO` y `RutaPlanificadaDTO` para devolver el resultado.

**Recomendación:** eliminar ambos DTOs en un RF específico, porque duplican formas que el contrato
actual ya resolvió y mantenerlos sugiere falsamente un endpoint operativo. En esta oleada se
conservaron intactos, dado que podrían representar un contrato externo todavía no conectado y se
pidió no borrarlos sin aprobación previa.

---

## Anonimización de direcciones

Se confirmó en las dos fuentes textuales del DC de Logística
(`lucid/logistica-clases.puml` y `diagrama-de-clases-logistica.puml`) que `Direccion`, `Localidad`,
`Provincia` y `Pais` deben exponer `anonimizar(): void`.

Como los records eran inmutables e incompatibles con ese contrato mutante, se convirtieron en
clases finales preservando:

- los mismos constructores;
- los mismos accesores estilo record (`calle()`, `localidad()`, `nombre()`, etc.);
- igualdad y representación por valor mediante Lombok;
- las validaciones existentes de `Direccion`.

`Direccion.anonimizar()` sustituye los datos por valores no identificables y propaga la operación a
Localidad → Provincia → País. El patrón coincide con el valor `ANONIMIZADO` y los valores técnicos
de dirección usados por el mecanismo de privacidad del monorepo.

---

## Tests y barridos

- Se agregó una prueba canónica de anonimización en cascada.
- `RutaTest` cubre disponibilidad de seguimiento en PENDIENTE, EN_TRASLADO y COMPLETADA.
- `CamionesServiceTest` usa el validador respaldado por `findByPatente`.
- Se corrigió el nombre de clase `AlgoritmoOrdenadorDeEntregaTest` para que sea singular y coincida
  con el archivo/concepto probado.

```text
wildcard imports en los cuatro controllers = 0
getEntregas()/obtenerEntregas() = 0
findAll() en los puntos objetivo = 0
clases de test terminadas en Tests = 0
```

---

## Verificaciones

- [x] Suite del módulo en verde: 205 tests, 0 fallas, 0 errores.
- [x] Reactor completo en verde.

```text
common-lib                 SUCCESS — 32 tests
donaciones-service         SUCCESS — 394 tests
notificaciones-service     SUCCESS — 64 tests
incentivos-service         SUCCESS — 189 tests
logistica-service          SUCCESS — 205 tests
integration-tests          SUCCESS — tests omitidos por su configuración Maven
BUILD SUCCESS — 01:16 min
```
