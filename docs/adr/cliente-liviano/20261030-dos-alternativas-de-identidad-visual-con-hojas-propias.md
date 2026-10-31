# Dos alternativas de identidad visual, cada una con sus propias hojas de estilo y plantillas

- Status: proposed
- Date: 2026-10-30
- Deciders: tsorren, al elegir entre tres opciones de hojas de estilo y cuatro de plantillas en la sesión de la Alternativa Cozy / Medieval
- Tags: cliente-liviano, interfaz, identidad, tokens, entrega-6

> **Origen:** pedido del usuario: diseñar una segunda identidad visual, la **Alternativa Cozy /
> Medieval**, sin romper la actual, que pasa a llamarse **Alternativa Glassmorphism**
> ([bitácora](../../interfaz/bitacora.md), 2026-10-30). `[OBSERVED]` Implementada en
> `VariantesInterceptor`, `templates/layout.html`, `static/css/alternativas/cozy-medieval/` y
> `templates/alternativas/cozy-medieval/`.
>
> **Actualización (2026-10-30):** la [directiva de estructura de Cozy](../../interfaz/estructura-cozy-medieval.md#cómo-se-separa-la-estructura)
> cambió la parte de plantillas. Antes se compartían con Glassmorphism; ahora cada alternativa tiene
> las suyas. El ADR sigue `proposed` y se editó en lugar de crear un sucesor, porque nunca se publicó.
>
> **Segunda actualización (2026-10-30):** el usuario pidió que Cozy hable con voz propia. La regla
> "textos iguales" pasa a "**los mismos datos y acciones; voz propia en títulos y rótulos**" (tercera
> pregunta, Textos). Sigue `proposed`, por el mismo motivo.

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

Problema: ¿cómo conviven dos identidades sobre las mismas pantallas? Son dos preguntas:

1. **Estilos:** ¿comparten las hojas de estilo o cada una tiene las suyas?
2. **Plantillas:** ¿comparten el HTML (layouts, fragmentos y páginas) o cada una tiene el suyo?
   Surgió cuando la directiva de estructura de Cozy pidió cambiar cómo se arma cada pantalla
   (hoja con regiones, barra lateral con pestaña, portada, mapa), algo que las hojas de estilo solas
   no alcanzan a hacer.
3. **Textos:** ¿cada alternativa dice lo mismo con las mismas palabras, o puede tener su voz?
   Surgió cuando el usuario pidió para Cozy títulos y rótulos propios ("Tu viaje", "Estás acá").

## Atributos de Calidad y Drivers de Decisión

Pesos que reflejan las prioridades del usuario: Glassmorphism intacta 30 · libertad de diseño de
la nueva alternativa 20 · comparar las dos sobre las mismas pantallas 20 · costo de mantener cada
pantalla 20 · reversibilidad 10. Valen para las dos preguntas.

## Relaciones Arquitectónicas

1. **Refina:** [ADR-E6-06](./20261030-stack-del-cliente-liviano-thymeleaf-htmx-pico-tokens-y-mapas.md)
   (`proposed`), decisión D20: el `tokens.css` sigue siendo la única fuente de la identidad, pero
   **de cada alternativa**. No cambia el stack: sigue Pico.css tematizado por sus variables.
2. **Complementa:** [verificación visual con Playwright y axe](./20261030-verificacion-visual-con-playwright-y-axe.md)
   (`proposed`): la comprobación recorre todas las alternativas.
3. **Se apoya en:** la [directiva de estructura y navegación de Cozy](../../interfaz/estructura-cozy-medieval.md)
   (aprobada por el usuario), que pide el juego propio de plantillas.

## Alternativas Consideradas

### Hojas de estilo

| Alternativa | Glassmorphism intacta | Libertad | Comparar | Costo por pantalla | Reversible | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|
| A. Un `tokens.css` con una sección por alternativa y `app.css` compartido; la nueva suma sus adornos aparte | 4 | 3 | 5 | 4 | 5 | 4,1 |
| **B. Una hoja completa por alternativa (`tokens.css` y `app.css` propios), elegida con `?alternativa=`** | 5 | 5 | 5 | 2 | 5 | **4,4** |
| C. Una rama propia para la nueva alternativa | 5 | 5 | 1 | 1 | 3 | 3,2 |

El agente recomendó A, por el costo de mantenimiento. El usuario eligió B.

### Plantillas

Puntajes de 1 a 5; son estimaciones del agente (`[INFERRED]`). Además de los pesos, hay un requisito
que decide si la opción sirve: ¿permite las decisiones de estructura de la
[directiva](../../interfaz/estructura-cozy-medieval.md)?

| Alternativa | Permite la estructura de Cozy | Glassmorphism intacta | Libertad | Comparar | Costo por pantalla | Reversible | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| P1. Plantillas compartidas; todo lo propio en las hojas de estilo (lo que había) | No | 5 | 1 | 5 | 5 | 5 | 4,2, descartada por el requisito |
| P2. Plantillas compartidas con desvíos dentro de cada una (`th:if` por alternativa) | Sí, con lío | 2 | 3 | 4 | 3 | 3 | 2,9 |
| P3. Piezas propias de Cozy con respaldo en las compartidas: si falta una, se usa la de Glassmorphism | Sí | 5 | 4 | 5 | 3 | 4 | 4,3 |
| **P4. Un juego completo de plantillas por alternativa (`templates/alternativas/<nombre>/`)** | Sí | 5 | 5 | 5 | 2 | 4 | **4,3** |

Con estos pesos, P3 y P4 empatan. El agente recomendó P3, porque cada pieza nueva solo se duplica si
Cozy la necesita. El usuario eligió **P4**: prefirió libertad total, sin depender de las piezas
compartidas. Es decir, la libertad pesó más que el costo, algo que la tabla no refleja del todo.

### Textos

El usuario ya eligió la voz propia; la pregunta es cómo vigilarla. Requisito que decide si la opción
sirve: ¿garantiza que las dos alternativas muestran los mismos datos y las mismas acciones?
Puntajes de 1 a 5, estimaciones del agente (`[INFERRED]`).

| Alternativa | Garantiza datos y acciones | Glassmorphism intacta | Libertad | Comparar | Costo por pantalla | Reversible | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| T1. Textos iguales, con test (lo que había) | Sí | 5 | 1 | 5 | 4 | 5 | 3,9, descartada: no deja la voz propia |
| T2. Voz libre, sin test de textos | No | 5 | 5 | 3 | 5 | 5 | 4,6, descartada por el requisito |
| T3. Voz marcada en cada plantilla (un atributo con el texto de Glassmorphism) | Sí | 5 | 3 | 5 | 3 | 4 | 4,1 |
| **T4. Mismos datos, acciones y nombres de controles, con test; voz en una tabla por alternativa** | Sí | 5 | 4 | 5 | 3 | 5 | **4,4** |

Se elige **T4**: el test compara datos, acciones y nombres de controles, y deja pasar solo los
títulos y rótulos de la tabla. T3 mezcla la regla con el marcado, ensucia el HTML publicado y no
puede expresar un rótulo que Cozy no usa.

## Resultado de la Decisión

Se elige **B**:

- **Glassmorphism** sigue en `static/css/tokens.css` y `static/css/app.css`, sin cambios, y es la
  que se ve por defecto.
- Cada alternativa nueva vive en `static/css/alternativas/<nombre>/`, con su `tokens.css` y su
  `app.css` completos. Hoy hay una: `cozy-medieval`. Su `app.css` no hereda nada del de
  Glassmorphism.
- **Plantillas por alternativa.** Glassmorphism sigue en `templates/` (`layout.html`,
  `layout-privado.html`, `fragmentos/` y `paginas/`), sin cambios. Cada alternativa nueva tiene su
  copia completa en `templates/alternativas/<nombre>/`, con sus referencias internas (`~{...}`)
  apuntando a las suyas. El juego no se apoya en las piezas de Glassmorphism: si a una alternativa
  le falta una plantilla, un test lo detecta (el interceptor, para no romper la maqueta, usa la de
  Glassmorphism si falta una página, pero eso no es parte del diseño). Los adornos (como máscaras SVG en `static/img/<nombre>/`) siguen en sus hojas.
- **Mismos datos y acciones, con test; voz propia en títulos y rótulos (T4).** Los textos se
  escriben en cada juego de plantillas. `InterfazFitnessTest` exige que cada par de gemelas tenga:
  - los mismos textos (nodos de texto, `aria-label`, `alt`, `title`, `placeholder`, `data-etiqueta`
    y los textos entre comillas de los atributos `th:*`), salvo los títulos y rótulos de la tabla de
    voz de la alternativa (`cliente-liviano/src/test/resources/voz/<nombre>.txt`); en el catálogo
    `/estilos`, la tabla también admite la descripción de un componente que la alternativa dibuja
    de otro modo (en Cozy, la navegación son cajones y no una barra);
  - los mismos datos del modelo: las expresiones `${…}` que escriben texto o que reciben los
    fragmentos (los íconos, siempre decorativos, no cuentan);
  - los mismos controles (enlaces, botones, campos, formularios y `summary`), cada uno con su
    destino y las piezas de su nombre accesible, en cualquier orden.

  La tabla de voz no admite números (son datos) y cada línea tiene que estar en uso. Un rótulo de
  la tabla puesto en un control rompe la comparación de controles. El test también exige que cada
  plantilla de `templates/` tenga su gemela en cada alternativa (y al revés), y que el catálogo
  `/estilos` de cada alternativa (`templates/alternativas/<nombre>/paginas/estilos.html`) muestre
  todos los fragmentos de esa alternativa.
- En el perfil `maqueta`, `?alternativa=<nombre>` elige la alternativa. `VariantesInterceptor` la
  valida contra las carpetas que existen, `layout.html` carga sus dos hojas y la vista
  `paginas/x` pasa a `alternativas/<nombre>/paginas/x` si esa plantilla existe. Se combina con
  `?variante=` de las comparaciones A/B; una variante de estructura (`templates/variantes/<x>.html`)
  reemplaza la página entera y gana sobre la de la alternativa. Sin el parámetro, y fuera del perfil
  `maqueta`, rige Glassmorphism.
- D20 rige dentro de cada alternativa: dos niveles de tokens, `--pico-*` solo en su
  `tokens.css`, pantallas y componentes solo con tokens de uso. Las dos alternativas declaran los
  mismos puntos de quiebre, para comparar sobre los mismos anchos.
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
- Cozy arma cada pantalla a su modo (regiones, barra con pestaña, portada, mapa) sin tocar nada de
  Glassmorphism ni depender de sus piezas.
- Quitar una alternativa es borrar sus dos carpetas (`static/css/alternativas/<nombre>/` y
  `templates/alternativas/<nombre>/`).

### Negativas

- **Cada pantalla y cada componente nuevo se escribe dos veces, en HTML y en estilos**, una por
  alternativa: el doble de trabajo y de revisión visual. Si se olvida una, la pantalla queda sin estilo en esa alternativa: la
  verificación visual recorre las dos y suele detectarlo (contraste, toque o desborde), pero no
  siempre.
- **Los textos se duplican** en los dos juegos de plantillas. Un test vigila que los datos y las
  acciones sean los mismos, pero un cambio de texto hay que hacerlo en dos lugares, y un título
  propio, además, en la tabla de voz.
- **El test no sabe qué es un título.** La tabla de voz podría declarar como rótulo un dato sin
  números (un nombre, por ejemplo). Lo frenan la revisión de la tabla y la regla de los números, no
  el test.
- **Un arreglo de estructura hay que llevarlo a las dos.** Corregir un error de HTML en una
  pantalla de Glassmorphism no corrige su gemela de Cozy. Los tests de gemelas atrapan que falte un
  archivo o un texto, no que las dos estructuras se hayan desviado por error.
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

Hojas de estilo:

- **A. Una hoja de tokens con secciones:** menos código que mantener, pero el `tokens.css` de
  Glassmorphism cambia (suma una sección) y la nueva alternativa queda atada a los componentes de
  la otra; los adornos propios pelean con reglas que no son suyas.
- **C. Rama propia:** cero código de convivencia, pero las dos no se pueden ver juntas, cada
  pantalla nueva hay que llevarla a las dos ramas y unir la rama pisaría a Glassmorphism.

Plantillas:

- **P1. Solo estilos:** es lo más barato, pero no deja hacer la mayoría de las decisiones de
  estructura de Cozy (reparto de la hoja, barra con pestaña, portada, listas por etapa).
- **P2. Desvíos dentro de las compartidas:** cada cambio de Cozy tocaría archivos de Glassmorphism,
  que tiene que quedar intacta, y cada plantilla se llena de condiciones por alternativa.
- **P3. Piezas propias con respaldo (la recomendada):** menos duplicación, pero Cozy depende de las
  piezas compartidas, y un cambio en una de ellas puede romper la otra alternativa sin avisar.
- **P4. Juego completo (elegida):** libertad total y aislamiento entre alternativas, a cambio del
  doble de HTML y de estilos por pantalla y por componente, y de textos duplicados que un test
  vigila.

Textos:

- **T1. Textos iguales:** lo más simple de vigilar, pero no deja la voz que pidió el usuario.
- **T2. Voz libre:** cero costo, pero nada impide que una alternativa muestre otro dato, pierda una
  acción o cambie el nombre de un botón sin que nadie lo note.
- **T3. Voz marcada en la plantilla:** la excepción queda junto al texto, pero el atributo viaja al
  HTML publicado, no puede decir "este rótulo no va" y mezcla la regla con el marcado.
- **T4. Datos, acciones y controles con test; voz en tabla (elegida):** la libertad que pidió el
  usuario y la garantía de comparar lo mismo, a cambio de una tabla más que mantener.

## Validación

- `[VERIFIED]` 2026-10-30, con las plantillas todavía compartidas: `InterfazFitnessTest` (6 tests) y
  `VariantesInterceptorTest` (8 tests, 3 nuevos para `?alternativa=`) en verde.
- `[VERIFIED]` 2026-10-30, con el juego de plantillas de Cozy: `InterfazFitnessTest` (9 tests: 4
  nuevos de gemelas, textos iguales, referencias propias y catálogo por alternativa) y
  `VariantesInterceptorTest` (11 tests: 3 nuevos para el juego propio y para la variante de
  estructura que gana) en verde. Prueba de mutación: borrar una gemela, cambiar un texto, apuntar
  a un fragmento de Glassmorphism y sacar un fragmento del catálogo de Cozy hacen fallar 4 tests.
- `[VERIFIED]` 2026-10-30: `mvn test -pl cliente-liviano -Pvisual` recorre Glassmorphism y
  `cozy-medieval` en `/estilos` y `/estilos/armazon`, sin hallazgos (`reporte.json` vacío).
- `[VERIFIED]` 2026-10-31, con la voz propia (T4): `InterfazFitnessTest` (10 tests, con
  `lasGemelasTienenLosMismosDatosYAcciones` y `laVozPropiaSoloCambiaTitulosYRotulos`) en verde sobre
  el juego de Cozy rediseñado. Prueba de mutación sobre las plantillas finales: cambiar un título
  declarado en la tabla pasa; fallan, cada una en su guardia, cambiar un dato ("15 kg" por "16 kg"),
  mostrar otro dato del modelo en la cifra, cambiar el destino de "Ver seguimiento", renombrar ese
  botón con una línea de voz, poner números en la voz, dejar una línea de voz sin uso y quitar
  "Ver el detalle".
