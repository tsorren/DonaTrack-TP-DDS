# Contrato GraphQL de `donaciones-service` (solo lectura)

> **Estado:** `[PROPOSED]`, a la espera del primer control del dueño del frente (SPEC-11, D03).
> Después de ese control el esquema no cambia (D16).
> **Esquema:** [`donaciones.graphqls`](donaciones.graphqls). La copia que lee el servicio va en
> `donaciones-service/src/main/resources/graphql/` y un test de contrato exige que sea idéntica
> (D14).
> **Origen:** [SPEC-11](../../specs/active/SPEC-11-graphql-de-solo-lectura-en-donaciones.md),
> tarea G1. Implementa el ADR "GraphQL de solo lectura en Donaciones" (`proposed`, PR 987).

## 1. Qué es

`[DOCUMENTED]` Una sola ruta, `POST /api/donaciones/graphql`, que responde consultas de lectura
sobre tres raíces: necesidades, entidades y donaciones. No hay mutaciones ni suscripciones: las
escrituras siguen por REST (Q19). No hay tipo Donante ni ningún dato de una persona (E7-S04c).

| Raíz | Qué devuelve | Acceso | Lectura que usa |
|---|---|---|---|
| `necesidades` | Necesidades vigentes, con su entidad | Pública, sin token | Método nuevo paginado de `INecesidadesService` (lote `g2-necesidades`) |
| `entidades` | Entidades activas, vista pública | Pública, sin token | `IEntidadBeneficiariaService.buscarPublicas` |
| `donaciones` | Donaciones independientes, con su necesidad y su entidad | Con rol, la regla de `GET /donaciones-independientes` | `IDonacionesIndependientesService.obtenerConFiltros` |

## 2. Cómo se llama

- `POST /api/donaciones/graphql`, cuerpo JSON `{"query": "...", "variables": {...}}`.
- Sin token se pueden pedir `necesidades` y `entidades`. Para `donaciones` va
  `Authorization: Bearer <JWT>`, el mismo de REST.
- El gateway propaga `X-Trace-Id`; los errores lo devuelven en `extensions.traceId`.
- Las fechas viajan como texto ISO-8601: `AAAA-MM-DD`, y con hora `AAAA-MM-DDThh:mm:ss`, sin
  zona. Los identificadores son UUID en texto (tipo `ID`).

### 2.1 Errores

`[PROPOSED]` Un error de una raíz deja ese campo en `null` (las tres raíces son anulables) y suma
una entrada en `errors` con el código del catálogo (`docs/arquitectura/catalogo-errores.md`):

| Caso | `extensions.codigo` |
|---|---|
| `pagina` negativa, `tamanio` fuera de 1 a 50, texto de más de 100 caracteres, fecha mal escrita | `ERR-CSR-003` |
| `donaciones` sin token, con la seguridad encendida | `ERR-CSR-401` |
| `donaciones` con un rol o un filtro que la regla no admite | `ERR-CSR-403` |
| Cualquier otro error del servicio | El código que traiga la excepción, o `ERR-CSR-500` |

La traducción la deja el lote `g2-armazon`. Los rechazos por profundidad, complejidad o *timeout*
los define `g3-limites`.

## 3. Consultas de ejemplo

Los ids son sintéticos.

### 3.1 `necesidades`: la lista pública, con la entidad que pide

```graphql
query NecesidadesDeUnaCategoria($categoria: ID) {
  necesidades(filtro: { categoriaId: $categoria, tipo: EXTRAORDINARIA }, pagina: 0, tamanio: 10) {
    total
    elementos {
      id
      descripcion
      cantidadNecesitada
      fechaFin
      entidad { razonSocial rubro }
    }
  }
}
```

Variables: `{"categoria": "00000000-0000-4000-8000-000000000001"}`.

```json
{
  "data": {
    "necesidades": {
      "total": 1,
      "elementos": [
        {
          "id": "00000000-0000-4000-8000-0000000000a1",
          "descripcion": "Frazadas para el invierno",
          "cantidadNecesitada": 40,
          "fechaFin": null,
          "entidad": { "razonSocial": "Comedor de ejemplo", "rubro": "Comedor" }
        }
      ]
    }
  }
}
```

### 3.2 `entidades`: búsqueda por nombre

```graphql
query EntidadesPorNombre {
  entidades(filtro: { nombre: "comedor" }, tamanio: 5) {
    total
    pagina
    elementos { id razonSocial }
  }
}
```

### 3.3 `donaciones`: las de una entidad, con token de esa entidad

```graphql
query DonacionesDeMiEntidad($entidad: ID!) {
  donaciones(
    filtro: { entidadId: $entidad, estado: [EN_TRASLADO, EN_PUERTA] }
    orden: ATENCION
    tamanio: 20
  ) {
    total
    elementos {
      id
      estado
      fechaRegistro
      cantidad
      items { cantidad bien { descripcion fechaVencimiento } categoria { nombre unidad } }
      historial { estadoNuevo fecha }
      necesidad { descripcion cantidadNecesitada }
    }
  }
}
```

Sin token, o con el token de otra entidad, la respuesta es:

```json
{
  "data": { "donaciones": null },
  "errors": [
    {
      "message": "Acceso denegado",
      "path": ["donaciones"],
      "extensions": { "codigo": "ERR-CSR-403", "traceId": "<el X-Trace-Id>" }
    }
  ]
}
```

(Sin token, el código es `ERR-CSR-401`.)

## 4. Las siete preguntas de SPEC-11, §7.5

Cada respuesta es `[PROPOSED]` hasta que el dueño la apruebe en el primer control.

### 4.1 Los campos de cada tipo

**Respuesta.** Salen de los DTO públicos de SPEC-11, §7.1, renombrados a la convención del esquema
y sin nada de una persona:

| Tipo | DTO de origen | Campos | Qué queda afuera |
|---|---|---|---|
| `Necesidad` | `NecesidadPublicaDTO` | `id`, `entidadId` (de `idEntidad`), `subcategoriaId` (de `idSubcategoria`), `descripcion`, `cantidadNecesitada`, `tipo`, `fechaFin`, y la relación `entidad` | Nada: el DTO ya es público |
| `Entidad` | `EntidadPublicaDTO` | `id`, `razonSocial`, `rubro` | Documento, dirección, contactos y representantes: no están en el DTO (D26) |
| `Donacion` | `DonacionIndependienteResponseDTO` | `id`, `descripcion`, `estado` (de `estadoActual`), `fechaRegistro`, `cantidad`, `cancelacionPedida`, `entidadId` (de `entidadBeneficiariaId`), `necesidadId`, `items`, `historial`, y las relaciones `necesidad` y `entidad` | `quienDono` (D09) y `donacionOriginalId` |
| `ItemDonacion`, `Bien`, `Subcategoria`, `Categoria` | `ItemDonacionIndependienteResponseDTO` y `BienNormalizadoDTO` | Todos | Nada |
| `CambioEstado` | `CambioEstadoDIResponseDTO` | `estadoAnterior`, `estadoNuevo`, `fecha` (de `timestamp`) | `actor` y `justificacion` |

**Alternativa descartada:** copiar los DTO tal cual, con sus nombres (`idEntidad`,
`entidadBeneficiariaId`, `timestamp`). **Por qué:** el esquema es un contrato nuevo y conviene un
solo estilo de nombres; el mapeo ya es obligatorio para descartar `quienDono`, `actor` y
`justificacion`.

**Sobre `donacionOriginalId`.** No es un dato de una persona, pero es el id de la donación que el
donante registró, la que sí tiene al donante. La pantalla no lo necesita y dejarlo afuera evita un
puente hacia el donante por REST. Alternativa descartada: exponerlo, porque está en el DTO.

**Sobre los estados.** `[OBSERVED]` El DTO trae `estadoActual`, `estadoAnterior` y `estadoNuevo`
como el nombre simple de la clase del estado (`EnDeposito`, `EnTraslado`…;
`DonacionIndependienteMapper`). El esquema usa el enum `EstadoDonacion`, con los nombres de
`TipoEstadoDonacion` (`EN_DEPOSITO`, `EN_TRASLADO`…), los mismos que recibe el filtro `estado`. El
resolver traduce de uno a otro al mapear: las ocho clases corresponden una a una con los ocho
valores. Alternativa descartada: `String` con el nombre de la clase, como REST, porque el filtro
y la salida usarían dos vocabularios distintos.

**Sobre los escalares.** No hay escalares propios (`Date`, `UUID`): los ids son `ID` y las fechas,
`String` ISO-8601. Alternativa descartada: escalares tipados, porque piden
`graphql-java-extended-scalars` (una dependencia más, fuera del `que` de G2) o un `Coercing`
propio por cada uno.

### 4.2 Los filtros de cada consulta

**Respuesta.**

| Consulta | Filtros (`input`) | De dónde salen |
|---|---|---|
| `necesidades` | `entidadId`, `categoriaId`, `subcategoriaId`, `tipo` | `entidadId` es el de `listarPublicas`. Los otros tres los suma el método nuevo del lote `g2-necesidades` |
| `entidades` | `nombre`, `categoriaId` | Exactamente los de `buscarPublicas` |
| `donaciones` | `donanteId`, `entidadId`, `estado` (lista), `necesidadId`, `subcategoriaId`, `categoriaId`, `desde`, `hasta`, `venceHasta`, `id`, `texto`; y el argumento `orden` | Todos los de `FiltroDonacionesIndependientes`, más `q` (como `texto`) y `orden` de `ParametrosListado` |

En las tres, el `filtro` es no nulo con valor por defecto `{}`: se puede omitir, pero no mandar
`null`. Así la regla de acceso de `donaciones` siempre encuentra un objeto en `#filtro`.

**Alternativa descartada en necesidades:** solo `entidadId`, como `listarPublicas`. **Por qué:** la
lista pública se recorre por categoría, igual que las entidades, y sin ese filtro el cliente
tendría que traer todas las páginas. Se descartó también filtrar por texto, para no sumar una
búsqueda sin tildes al repositorio en memoria y al JPA.

**Alternativa descartada en donaciones:** un subconjunto (estado y fechas). **Por qué:** el
servicio ya resuelve todos esos filtros en la base; dejarlos afuera no ahorra trabajo y obliga a
volver a REST.

### 4.3 La forma de la página

**Respuesta.** Un tipo propio por raíz (`PaginaNecesidades`, `PaginaEntidades`,
`PaginaDonaciones`) con `elementos`, `total`, `pagina` y `tamanio`, y argumentos `pagina` (desde
0) y `tamanio`.

**Alternativa descartada:** conexiones con cursor (`edges`, `node`, `pageInfo`, estilo Relay).
**Por qué:** los tres servicios ya paginan por número de página y devuelven `Pagina<T>(elementos,
total)`; un cursor pediría codificar la posición y cambiar la lectura de los repositorios. Se usa
`elementos` y no `items` para que coincida con `Pagina<T>` y no se confunda con `Donacion.items`.

### 4.4 El tope de página

**Respuesta.** `tamanio` por defecto 20 y tope 50. Fuera de 1 a 50, o `pagina` negativa, la raíz
vuelve con `ERR-CSR-003`. El tope se lee de una propiedad (lote `g2-armazon`).

**Alternativa descartada:** 100, el tope de `ParametrosListado`. **Por qué:** el ADR sugiere 50, y
en GraphQL cada elemento puede traer sus relaciones y listas internas, así que una página cuesta
más que en REST. El 50 sigue siendo más que el 20 por defecto de REST.

### 4.5 Qué relaciones anidadas entran

**Respuesta.** Tres, cada una hacia un solo elemento y cargada por lotes (`@BatchMapping`, lote
`g2-relaciones`):

| Relación | Qué devuelve | Lectura por lote |
|---|---|---|
| `Necesidad.entidad` | La vista pública de la entidad que la pide | Entidades por lista de ids |
| `Donacion.necesidad` | La necesidad que cubre, aunque ya no esté vigente; nula si no cubre ninguna | Necesidades por lista de ids, sin filtrar por vigencia |
| `Donacion.entidad` | La vista pública de la entidad asignada, aunque después se haya dado de baja; nula mientras no está asignada | La misma de entidades por lista de ids, sin filtrar por estado |

Con eso el grafo no tiene ciclos: la consulta legítima más profunda llega a cinco niveles
(`donaciones → elementos → necesidad → entidad → razonSocial`, o `… → items → bien → descripcion`).
`[INFERRED]` Es un dato para elegir la profundidad máxima en `g3-limites`, no una decisión de G1.

**Alternativas descartadas:**

- **`Entidad.necesidades`.** Forma un ciclo con `Necesidad.entidad` y pediría una lista paginada
  por padre dentro de un lote. Se cubre con `necesidades(filtro: { entidadId: … })`.
- **`Necesidad.donaciones` o `Entidad.donaciones`.** Prohibido: abriría un segundo camino al tipo
  `Donacion` desde tipos públicos (puerta única, SPEC-11 §4).
- **`Necesidad.subcategoria` con el nombre.** No está en `NecesidadPublicaDTO` y pediría otra
  lectura por lote. El cliente ya tiene el árbol de categorías por REST.

### 4.6 Qué regla de `Acceso` lleva cada consulta y cada relación

**Respuesta.** Cada método con `@QueryMapping`, `@SchemaMapping` o `@BatchMapping` lleva
`@PreAuthorize` con una constante de `Acceso`:

| Método | Regla | Por qué |
|---|---|---|
| `Query.necesidades` | `Acceso.PUBLICO` | Lo mismo que `GET /api/necesidades/publicas` (D08) |
| `Query.entidades` | `Acceso.PUBLICO` | Lo mismo que `GET /api/entidades/publicas` (D08) |
| `Query.donaciones` | Constante nueva con la regla de `DonacionesIndependientesController.obtenerTodas` (D15) | Una sola regla para REST y GraphQL |
| `Necesidad.entidad` | `Acceso.PUBLICO` | Une dos vistas públicas |
| `Donacion.necesidad` | `Acceso.CON_SESION` | Solo se llega desde `donaciones`, que ya pidió rol; la regla repite el piso por si el método se reusa |
| `Donacion.entidad` | `Acceso.CON_SESION` | Ídem |

`[OBSERVED]` La regla de `obtenerTodas` es `ADMIN or hasAuthority('VER_DONACIONES') and
(hasRole('DONANTE') and principal.puedeOperarSobre(#filtro.donanteId()) or hasRole('ENTIDAD') and
principal.puedeOperarSobre(#filtro.entidadId()))`. Para que el resolver use la misma constante, su
parámetro se llama `filtro` y tiene los métodos `donanteId()` y `entidadId()`. El nombre de la
constante lo elige `g2-donaciones`; se propone `Acceso.LISTAR_DONACIONES_INDEPENDIENTES`.

Los campos que son propiedades de un objeto ya mapeado (`items`, `historial`, `bien`, `estado`…)
no llevan resolver: el mapeo al tipo del esquema se hace al construir el objeto, así no aparecen
métodos con `@SchemaMapping` sin regla.

**Alternativa descartada:** `Acceso.PUBLICO` en las dos relaciones de `Donacion`, porque el padre
ya pasó la regla. **Por qué:** con la ruta abierta en el gateway, un método sin piso queda abierto
si mañana se lo reusa desde otro tipo; `CON_SESION` no cuesta nada y la puerta única sigue en la
raíz.

### 4.7 En qué entornos se encienden la introspección y GraphiQL

**Respuesta.** Apagados por defecto en `application.properties`. Se encienden solo en el preprod,
con una variable de entorno en `docker-compose.preprod.yml` (el nombre lo elige `g3-limites`; se
propone `DONACIONES_GRAPHQL_EXPLORAR=true`). Quedan apagados en `docker-compose.yml`,
`docker-compose.demo.yml`, `docker-compose.publica.yml` y el resto.

**Alternativa descartada:** encenderlos "en preprod y demo", como dice el ADR. **Por qué:**
`[OBSERVED]` `docker-compose.demo.yml` es la demo del broker de E4, no una demo de GraphQL, y
`docker-compose.publica.yml` sale por un túnel público: ahí la introspección mostraría el esquema
entero a cualquiera. La divergencia se suma a las de SPEC-11, §6, para el PR 987.

## 5. Comprobación de los invariantes

| Invariante (SPEC-11 §4 y D09) | Dónde se ve en el esquema |
|---|---|
| Sin mutaciones ni suscripciones | Solo existe `type Query` |
| Sin tipo Donante ni datos de una persona | No hay `Donante`, `quienDono`, documento, dirección, contactos ni representantes; `CambioEstado` no tiene `actor` ni `justificacion` |
| Una sola puerta a las donaciones | `Donacion` aparece solo en `PaginaDonaciones`, que solo devuelve `Query.donaciones`. `Necesidad` y `Entidad` no tienen campos de tipo `Donacion` |
| `donanteId` y `entidadId` como filtros | Están en `FiltroDonaciones`, nunca en un tipo de salida |
| D09: la donación es la independiente y enlaza con la necesidad y la entidad | `Donacion` sale de `DonacionIndependienteResponseDTO` y tiene `necesidad` y `entidad` |
| Paginación | Las tres raíces devuelven una página con tope 50; las relaciones devuelven un solo elemento; `items` e `historial` son listas internas |

## 6. Qué toma cada lote de este contrato

| Lote | Qué usa |
|---|---|
| `g2-armazon` | La copia del esquema, el tope 50 por propiedad y la tabla de errores (§2.1) |
| `g2-necesidades` | `Query.necesidades`, `FiltroNecesidades` y el orden fijo por descripción e id |
| `g2-entidades` | `Query.entidades` y `FiltroEntidades` |
| `g2-donaciones` | `Query.donaciones`, `FiltroDonaciones`, `OrdenDonaciones`, el mapeo de §4.1 y la regla de §4.6 |
| `g2-relaciones` | Las tres relaciones de §4.5 y sus reglas de §4.6 |
| `g3-limites` | El dato de profundidad de §4.5 y los entornos de §4.7 |
| `g4-gateway-contrato` | La ruta de §2 y la comparación de las dos copias del esquema |
