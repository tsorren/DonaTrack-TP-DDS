# Una sola identidad visual en el cliente liviano: Raíces es el sitio y se retiran las alternativas

- Status: accepted
- Date: 2026-11-03
- Deciders: tsorren, al pedir que el cliente liviano quede solo con Raíces
- Tags: cliente-liviano, interfaz, identidad, tokens, entrega-6

> **Origen:** pedido del usuario del 2026-11-03: "en cliente liviano eliminar las alternativas
> glassmorphism y cozy medieval, dejar solo Raices", con dos condiciones dichas en el relevamiento:
> Raíces se sirve en la raíz del sitio y la documentación no tiene que condicionar la creatividad de
> los agentes que diseñen después ([bitácora](../../interfaz/bitacora.md), 2026-11-03).

## Contexto y Problema

- `[DOCUMENTED]` El [ADR de dos alternativas](./20261030-dos-alternativas-de-identidad-visual-con-hojas-propias.md)
  (`proposed`) fijó dos identidades con su juego completo de plantillas y sus hojas: Glassmorphism
  (por defecto, catálogo `/estilos`) y Cozy / Medieval (`?alternativa=cozy-medieval`), con los mismos
  datos y acciones vigilados por test y una tabla de voz por alternativa.
- `[DOCUMENTED]` El [ADR de la tercera alternativa](./20261101-tercera-alternativa-raices-como-prototipo-propio.md)
  (`proposed`) puso a Raíces en un prototipo propio (`/prototipo/raices/`), sin juego de plantillas
  "hasta que se elija".
- `[OBSERVED]` Raíces es la única con el recorrido completo de los tres roles (146 rutas en la
  verificación visual); Glassmorphism tenía solo el catálogo y Cozy un prototipo de 22 pantallas.
  El grupo eligió Raíces (sitio A, 2026-11-03, `handoff-e6-raices.md`).
- `[OBSERVED]` Con tres identidades, las skills de interfaz, el workflow `corrida-creativa` y las
  plantillas de encargo usan a Glassmorphism y a Cozy como ejemplos y como rutas fijas.

Problema: ¿cómo queda el cliente liviano con una sola identidad, sin la maquinaria de convivencia y
sin que los ejemplos de las identidades viejas guíen los diseños que vengan?

## Atributos de Calidad y Drivers de Decisión

Simplicidad del código y de las reglas 30 · direcciones estables para el grupo y la cátedra 20 ·
neutralidad de la guía para diseñar 25 · reversibilidad 10 · costo del cambio 15.

## Relaciones Arquitectónicas

1. **Reemplaza (`superseded`, aprobado por tsorren el 2026-11-04, al aceptar este ADR):** el
   [ADR de dos alternativas](./20261030-dos-alternativas-de-identidad-visual-con-hojas-propias.md) y el
   [ADR de la tercera alternativa](./20261101-tercera-alternativa-raices-como-prototipo-propio.md).
   Ninguno se edita; este ADR dice dónde quedó lo que enlazan
   ([archivo histórico](../../interfaz/historico/README.md)).
2. **Respeta:** [ADR-E6-06](./20261030-stack-del-cliente-liviano-thymeleaf-htmx-pico-tokens-y-mapas.md)
   (`proposed`): el mismo stack y los dos niveles de tokens (D20).
3. **Ajusta:** [verificación visual con Playwright y axe](./20261030-verificacion-visual-con-playwright-y-axe.md)
   (`proposed`): una sola pasada por el sitio, más las variantes A/B; sin pasada por alternativa.

## Alternativas Consideradas

Puntajes de 1 a 5; estimaciones del agente (`[INFERRED]`).

### Dónde se sirve Raíces

| Alternativa | Simplicidad | Direcciones estables | Neutralidad | Reversible | Costo | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|
| **S1. En la raíz (`/`, `/<pantalla>`), con plantillas y hojas en `templates/` y `static/css/`** | 5 | 5 | 5 | 4 | 3 | **4,6** |
| S2. Donde está (`/prototipo/raices/`), con `/` redirigiendo ahí | 4 | 3 | 2 | 5 | 5 | 3,6 |

S2 deja el nombre de la identidad en cada dirección y en cada ruta de archivo: las skills tendrían
que nombrarla, y "prototipo" dejaría de ser cierto. El usuario eligió S1.

### Qué pasa con la maquinaria de alternativas

| Alternativa | Simplicidad | Neutralidad | Reversible | Costo | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|
| **M1. Se borra `?alternativa=` y sus reglas (gemelas, voz, catálogo, juego completo); queda `?variante=`** | 5 | 5 | 3 | 4 | **4,5** |
| M2. Se deja el mecanismo vacío, para una identidad futura | 2 | 3 | 5 | 5 | 3,4 |

M2 es código y reglas sin uso (YAGNI); el historial de git conserva M1 si hiciera falta volver.

### Cómo se diseña una propuesta nueva de identidad o un rediseño

| Alternativa | Simplicidad | Neutralidad | Reversible | Costo | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|
| **P1. Sobre el sitio, en su propia rama: conceptos como variantes; el ganador, aplicado al sitio; antes y después por capturas** | 5 | 4 | 5 | 4 | **4,5** |
| P2. Volver a una carpeta de prototipo por propuesta, como Raíces | 3 | 4 | 4 | 2 | 3,4 |

La rama propia fue descartada en el ADR de dos alternativas (opción C) porque las dos identidades
tenían que convivir y verse juntas. Con una sola identidad ese motivo ya no rige: la rama es la
propuesta y el merge es la decisión.

### Qué pasa con los documentos de las identidades retiradas

| Alternativa | Neutralidad | Trazabilidad | **Total** |
|---|:-:|:-:|:-:|
| **D1. A `docs/interfaz/historico/`, marcado como "no leer para diseñar"; fuera del índice de contexto** | 4 | 5 | **4,5** |
| D2. Borrarlos | 5 | 2 | 3,5 |
| D3. Dejarlos donde están | 1 | 5 | 3 |

El usuario eligió D1.

## Resultado de la Decisión

S1 + M1 + P1 + D1:

- `[OBSERVED]` `SitioController` (perfil `maqueta`) sirve `/` (la portada) y `/<pantalla>`
  (`templates/<pantalla>.html`); una sección del menú sin plantilla muestra `sin-dibujar`. Se
  borraron `EstilosController`, `/estilos`, `templates/{paginas,fragmentos,alternativas,prototipo}/`,
  `static/css/alternativas/`, las hojas, imágenes, scripts y fuentes de Glassmorphism y de Cozy.
- `[OBSERVED]` `VariantesInterceptor` solo atiende `?variante=`. `InterfazFitnessTest` mantiene las
  reglas de tokens, quiebres, estilos en línea y contraste AA; se quitaron las de gemelas, voz,
  catálogo y juego completo. `VerificacionVisualTest` hace una pasada por el sitio.
- `[OBSERVED]` El brief de Raíces es `docs/interfaz/identidad/brief.md` y sus referencias,
  `identidad/referencias/`. Los briefs, referencias, estructura, flujo y evidencia de Glassmorphism
  y Cozy están en [`docs/interfaz/historico/`](../../interfaz/historico/README.md).
- `[OBSERVED]` Las skills de interfaz, el workflow `corrida-creativa` y las plantillas de encargo
  no nombran ninguna identidad concreta como ejemplo, tampoco a Raíces.

## Consecuencias

- **Positivas:** una pantalla se escribe una vez; desaparecen las reglas de gemelas y voz; las
  direcciones son las del sitio; la guía para diseñar no arrastra estilos anteriores.
- **Negativas:** comparar dos identidades lado a lado ya no se puede sin dos ramas; las capturas de
  antes y después reemplazan esa comparación. Los enlaces de los dos ADR anteriores a los documentos
  movidos quedan rotos, porque los ADR no se editan; el [archivo histórico](../../interfaz/historico/README.md)
  dice dónde está cada uno.
- **Riesgo:** si el grupo quisiera otra identidad conviviendo, habría que volver a M2 o P2 con un ADR
  nuevo.

## Análisis por alternativa

- **S1. Raíces en la raíz:** a favor, direcciones y rutas de archivo sin nombre de identidad, y
  "prototipo" deja de mentir; en contra, cambia todas las direcciones y los enlaces escritos a mano
  (una sola vez, verificado por el test que recorre los enlaces).
- **S2. Raíces donde está:** a favor, cero cambios de rutas; en contra, el nombre de la identidad
  queda en cada dirección y las skills tendrían que nombrarla.
- **M1. Borrar `?alternativa=`:** a favor, menos código y reglas; en contra, volver a convivir pide
  rehacerlo (está en el historial de git).
- **M2. Dejarlo vacío:** a favor, listo para una identidad futura; en contra, reglas y tests sin
  nada que vigilar (YAGNI).
- **P1. Propuesta en su rama:** a favor, una pantalla se escribe una vez y el merge decide; en
  contra, dos identidades no se ven juntas en la misma maqueta (lo reemplaza el antes y después).
- **P2. Carpeta de prototipo por propuesta:** a favor, convivencia en la maqueta; en contra, vuelve
  la duplicación de pantallas y de rutas.
- **D1. Archivo histórico:** a favor, conserva la trazabilidad de la bitácora y los ADR; en contra,
  un agente podría leerlo (lo mitiga el aviso y su ausencia del índice de contexto).
- **D2. Borrar:** a favor, neutralidad total; en contra, la bitácora y los ADR quedan sin respaldo
  legible fuera de git.
- **D3. Dejarlos:** a favor, sin costo; en contra, los agentes los leen como guía vigente.
