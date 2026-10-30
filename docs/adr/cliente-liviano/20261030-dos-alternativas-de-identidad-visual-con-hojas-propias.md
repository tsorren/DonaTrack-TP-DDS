# Dos alternativas de identidad visual, cada una con sus propias hojas de estilo

- Status: proposed
- Date: 2026-10-30
- Deciders: tsorren, al elegir entre tres opciones de convivencia en la sesión de la Alternativa Cozy / Medieval
- Tags: cliente-liviano, interfaz, identidad, tokens, entrega-6

> **Origen:** pedido del usuario: diseñar una segunda identidad visual, la **Alternativa Cozy /
> Medieval**, sin romper la actual, que pasa a llamarse **Alternativa Glassmorphism**
> ([bitácora](../../interfaz/bitacora.md), 2026-10-30). `[OBSERVED]` Implementada en
> `VariantesInterceptor`, `templates/layout.html` y `static/css/alternativas/cozy-medieval/` (rama
> `E6_alternativa_cozy_medieval`).

## Contexto y Problema

D20 ([ADR-E6-06](./20261030-stack-del-cliente-liviano-thymeleaf-htmx-pico-tokens-y-mapas.md)) y
RNF-UI-11 piden **un solo `tokens.css`** como fuente de la identidad, con valores base y tokens de
uso, y un `app.css` que solo use tokens de uso.

El equipo quiere comparar dos identidades sobre las mismas pantallas para entender qué mensaje
transmitir, cuál es su identidad y a qué usuarios les habla:

- **Glassmorphism**, profesional y seria: el prototipo v2 con la paleta Grafito cálido y menta;
- **Cozy / Medieval**, cálida y de aventura: pergamino, tinta, madera y latón
  ([brief](../../interfaz/identidad/brief-cozy-medieval.md)).

Glassmorphism tiene que quedar intacta y utilizable.

Problema: ¿cómo conviven dos identidades sobre las mismas plantillas?

## Atributos de Calidad y Drivers de Decisión

Pesos que reflejan las prioridades del usuario: Glassmorphism intacta 30 · libertad de diseño de
la nueva alternativa 20 · comparar las dos sobre las mismas pantallas 20 · costo de mantener cada
pantalla 20 · reversibilidad 10.

## Relaciones Arquitectónicas

1. **Refina:** [ADR-E6-06](./20261030-stack-del-cliente-liviano-thymeleaf-htmx-pico-tokens-y-mapas.md)
   (`proposed`), decisión D20: el `tokens.css` sigue siendo la única fuente de la identidad, pero
   **de cada alternativa**. No cambia el stack: sigue Pico.css tematizado por sus variables.
2. **Complementa:** [verificación visual con Playwright y axe](./20261030-verificacion-visual-con-playwright-y-axe.md)
   (`proposed`): la comprobación recorre todas las alternativas.

## Alternativas Consideradas

| Alternativa | Glassmorphism intacta | Libertad | Comparar | Costo por pantalla | Reversible | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|
| A. Un `tokens.css` con una sección por alternativa y `app.css` compartido; la nueva suma sus adornos aparte | 4 | 3 | 5 | 4 | 5 | 4,1 |
| **B. Una hoja completa por alternativa (`tokens.css` y `app.css` propios), elegida con `?alternativa=`** | 5 | 5 | 5 | 2 | 5 | **4,4** |
| C. Una rama propia para la nueva alternativa | 5 | 5 | 1 | 1 | 3 | 3,2 |

El agente recomendó A, por el costo de mantenimiento. El usuario eligió B.

## Resultado de la Decisión

Se elige **B**:

- **Glassmorphism** sigue en `static/css/tokens.css` y `static/css/app.css`, sin cambios, y es la
  que se ve por defecto.
- Cada alternativa nueva vive en `static/css/alternativas/<nombre>/`, con su `tokens.css` y su
  `app.css` completos. Hoy hay una: `cozy-medieval`. Su `app.css` no hereda nada del de
  Glassmorphism.
- **Plantillas, fragmentos y textos se comparten.** Lo propio de cada alternativa va solo en sus
  hojas: colores, letras, formas y adornos (como máscaras SVG en `static/img/<nombre>/`).
- En el perfil `maqueta`, `?alternativa=<nombre>` elige la alternativa. `VariantesInterceptor` la
  valida contra las carpetas que existen y `layout.html` carga sus dos hojas. Se combina con
  `?variante=` de las comparaciones A/B. Sin el parámetro, y fuera del perfil `maqueta`, rige
  Glassmorphism.
- D20 rige dentro de cada alternativa: dos niveles de tokens, `--pico-*` solo en su
  `tokens.css`, pantallas y componentes solo con tokens de uso. Las dos alternativas declaran los
  mismos puntos de quiebre, porque comparten las plantillas.
- `InterfazFitnessTest` aplica sus reglas a cada `tokens.css`: contraste AA de los mismos pares de
  tokens de uso y quiebres declarados.
- `VerificacionVisualTest`, sin propiedades, recorre Glassmorphism y cada alternativa, y las
  compara en el visor. Con `-Dverificacion.alternativa=<nombre>`, las variantes A/B se comparan
  sobre esa alternativa.

## Consecuencias

### Positivas

- Glassmorphism no cambia: sus hojas no se tocan.
- La nueva alternativa se diseña desde cero, sin pelear con las reglas de la otra.
- Las dos se comparan lado a lado sobre las mismas pantallas y los mismos datos.
- Quitar una alternativa es borrar su carpeta.

### Negativas

- **Cada componente o pantalla nueva se estiliza dos veces**, una en cada `app.css`, y la revisión
  visual se duplica. Si se olvida una, la pantalla queda sin estilo en esa alternativa: la
  verificación visual recorre las dos y suele detectarlo (contraste, toque o desborde), pero no
  siempre.
- RNF-UI-11 dice "un solo `tokens.css`". Con este ADR se lee "uno por alternativa". Si se acepta,
  hay que ajustar ese texto de los requerimientos.
- Copiar bloques entre los dos `app.css` puede contar como código duplicado en Sonar: cada hoja se
  escribe para su alternativa.
- Fuera del perfil `maqueta` no hay forma de elegir la alternativa. Si el equipo quiere publicar
  las dos, hace falta decidir cómo se elige (por ejemplo, una propiedad de configuración).
- Dentro de la maqueta, `?alternativa=` no pasa de una página a otra: los enlaces internos lo
  pierden y hay que agregarlo a cada dirección. Alcanza para comparar y para la verificación
  visual, que lo agrega en cada página.

## Análisis por alternativa

- **A. Una hoja de tokens con secciones:** menos código que mantener, pero el `tokens.css` de
  Glassmorphism cambia (suma una sección) y la nueva alternativa queda atada a los componentes de
  la otra; los adornos propios pelean con reglas que no son suyas.
- **C. Rama propia:** cero código de convivencia, pero las dos no se pueden ver juntas, cada
  pantalla nueva hay que llevarla a las dos ramas y unir la rama pisaría a Glassmorphism.

## Validación

- `[VERIFIED]` 2026-10-30: `InterfazFitnessTest` (6 tests) y `VariantesInterceptorTest` (8 tests,
  3 nuevos para `?alternativa=`) en verde.
- `[VERIFIED]` 2026-10-30: `mvn test -pl cliente-liviano -Pvisual` recorre Glassmorphism y
  `cozy-medieval` en `/estilos` y `/estilos/armazon`, sin hallazgos (`reporte.json` vacío).
