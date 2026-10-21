# Direcciones normalizadas e inmutables en Logística

- Status: proposed
- Date: 2026-10-07
- Deciders: Equipo de Logística; revisado en la reunión grupal del 06/10/2026
- Tags: logística, persistencia, normalización, direcciones, inmutabilidad

## Contexto y Problema

Cada entrega tiene un destino: la dirección de la entidad beneficiaria. En el dominio, `Direccion` contiene una `Localidad`, que pertenece a una `Provincia`, que pertenece a un `Pais`. Al pasar a PostgreSQL hubo que decidir cómo guardar esa jerarquía.

Donaciones resolvió el mismo problema desnormalizando: pasó las cuatro tablas a columnas de una sola, para evitar cuatro joins en cada lectura. La bitácora de la Oleada 10 de Logística proponía lo mismo (dirección embebida en `entrega`). En la reunión del 06/10 se planteó si Logística debía seguir ese criterio.

Aparece un segundo problema, independiente de la forma de guardar: si una localidad cambia de nombre, las entregas viejas no pueden pasar a mostrar el nombre nuevo, porque el historial dejaría de coincidir con lo que efectivamente ocurrió (por ejemplo, con un comprobante ya emitido).

## Atributos de Calidad y Drivers de Decisión

* **Integridad y trazabilidad:** el historial de entregas debe reflejar el destino tal como era al momento de la entrega.
* **Consultas por zona:** Logística es el servicio que arma rutas; agrupar o filtrar entregas por localidad o provincia es una consulta propia de su dominio.
* **Consistencia de los datos de referencia:** evitar que el mismo lugar aparezca escrito de varias formas ("CABA", "Capital Federal", "caba").

## Alternativas Consideradas

* **Dirección embebida en `entrega` (desnormalizada):** lectura sin joins y el historial queda fijo, porque cada fila guarda su propio texto. A cambio, el mismo lugar se repite con distintas grafías y no hay un catálogo para agrupar por zona.
* **Tablas normalizadas (`pais`, `provincia`, `localidad`, `direccion`) con datos modificables:** catálogo único, pero un `UPDATE` sobre una localidad cambia el pasado de todas las entregas que la usan.
* **Tablas normalizadas e inmutables:** catálogo único y el historial queda fijo, porque nada se modifica después de insertarse.

## Resultado de la Decisión

Alternativa elegida: **tablas normalizadas e inmutables**.

`[OBSERVED]` El código actual ya implementa esta decisión, salvo la excepción de anonimización descripta abajo.

* Cada entrega crea su propia fila en `direccion`. País, provincia y localidad se buscan por nombre y solo se insertan si no existen (`EntregasRepositoryJpaAdapter`), con `UNIQUE (nombre)`, `UNIQUE (id_pais, nombre)` y `UNIQUE (id_provincia, nombre)`.
* Ninguna de estas tablas tiene camino de actualización: no hay endpoint ni caso de uso que modifique una dirección o un catálogo. Si un lugar cambia de nombre, se crea un registro nuevo. La regla queda anotada en el DER y en el documento auxiliar de restricciones.
* Las dos formas de proteger el historial son válidas. Donaciones copia el texto en cada fila; Logística mantiene un catálogo que no se modifica. Lo que inclina a Logística hacia el catálogo es que agrupar entregas por zona es parte de su trabajo. Hoy `AlgoritmoOrdenadorSimple` no usa la localidad (está anotado como deuda en su Javadoc), pero el modelo ya deja disponible ese dato para un ordenamiento por cercanía.

**Excepción: supresión de datos personales.** El ADR grupal de privacidad (`20260519-privacidad-de-usuarios`) exige poder anonimizar a pedido. En Logística, la anonimización solo alcanza a los datos propios de la dirección (calle, altura, piso, departamento y código postal), que son los que identifican a alguien. Los catálogos no se anonimizan: un nombre de localidad no es un dato personal y lo comparten todas las entregas de esa zona.

## Consecuencias

### Positivas

* El historial de entregas no cambia aunque cambie el nombre de un lugar.
* Un mismo lugar existe una sola vez, lo que permite agrupar y filtrar por zona sin depender de cómo se escribió.

### Negativas

* Leer una dirección completa requiere tres joins (`direccion` → `localidad` → `provincia` → `pais`), el costo que Donaciones quiso evitar.
* La inmutabilidad la garantiza la aplicación, no la base: no hay trigger que impida un `UPDATE` manual.
* Pendiente de implementación: hoy `Direccion.anonimizar()` también renombra la localidad, la provincia y el país. Hay que limitarlo a los campos de la dirección y ajustar `DireccionTest`.
