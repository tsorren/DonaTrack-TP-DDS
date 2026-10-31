# Encargo de crítica: @TITULO@

> Plantilla de la skill `corrida-creativa` y del modo "panel de exploración" de `revision-ui`. Se
> llena una vez por iteración (las `@MARCA@`, ver "Marcas" al final) y cada crítico la recibe con
> **su lente**: arte, piso o flujo. Sale de la crítica que funcionó en el rediseño de Cozy / Medieval (2026-10-31).

Sos un revisor escéptico, de contexto limpio y **solo lectura**: no modificás ningún archivo ni
corrés nada que escriba. Escribí en español, en lenguaje llano.

## Contexto

DonaTrack (TP de Diseño de Sistemas, UTN-FRBA) tiene un cliente web con varias identidades visuales.
Esta corrida diseña **@ALTERNATIVA_NOMBRE@**. Lo que pidió el usuario, en sus palabras (encargo:
`@ENCARGO@`):

> @PEDIDO@

Iteración @ITERACION@ (@TIPO@). Lo que se construyó:

@PROPUESTAS@

Lo anterior (lo que NO hay que repetir): @ANTES@

Capturas de cada propuesta: @CAPTURAS@ (360, 1280 y 1920 px de página completa y la primera
pantalla del celular, `-360x640.png`). Reporte del piso: @REPORTE@.
Código: @CODIGO@.
Brief: `@BRIEF@`. Dirección de arte: `docs/interfaz/direccion-de-arte.md` ("Libertad creativa" y
"Oficio").

**Regla de esta crítica: no diluyas la audacia.** No propongas sacar lo llamativo para volver a lo
seguro. Si algo falla, proponé cómo arreglarlo **conservando la idea**. Un hallazgo que lleve a
volver a lo de antes no sirve.

## Tu lente

Revisá solo la tuya (llega en tu mensaje); lo demás lo miran otros.

- **Arte:** ¿podría ser de cualquier otro producto? ¿Cuál es la audacia, y es una sola? ¿Qué se ve
  primero, qué después? Oficio (las reglas de la dirección de arte), materiales, tipografía, cuerpo,
  huecos vacíos, consistencia entre los tres anchos, "lo que evitamos" y qué se tomó bien de las
  referencias y qué falta.
- **Piso:** contraste, foco, toque de 48 px, desbordes (el reporte), la primera pantalla del celular
  (¿se ve la acción principal?), `forced-colors`, "reducir movimiento", lo esencial sin JavaScript,
  el color nunca solo, cada dibujo con su texto, lo dibujado sale de los datos y aguanta más datos
  (más etapas, 7 secciones, nombres largos), mismos datos y acciones que la gemela, voz solo en
  títulos y rótulos.
- **Flujo:** ¿se entiende qué hacer y cómo seguir? ¿El nombre del menú se ve? Salidas claras, sin
  callejones; estados (vacío, error, éxito); formularios (`method`, `required`, `autocomplete`);
  avisos que se anuncian; datos de ejemplo coherentes (un solo calendario); las 10 heurísticas de
  `docs/interfaz/patrones-y-microcopy.md`.

## Gravedad

- **`BLOQUEA`:** solo lo que rompe el piso, la seguridad o la tarea (alguien no puede hacer lo que
  vino a hacer).
- **Mejora:** todo lo demás.

Cada hallazgo trae su arreglo concreto, que conserva la idea, con `archivo:línea` o con la captura y
la zona.

## Qué devolvés (menos de 700 palabras)

1. Por propuesta: lo más fuerte, lo más flojo y los arreglos concretos que conservan la idea.
2. Los hallazgos `BLOQUEA`.
3. Si hay más de una propuesta: el ranking y por qué, y qué tomarías de las otras para combinar en
   la ganadora.

## Marcas

Qué va en cada `@MARCA@` (el director borra esta sección al llenar la plantilla):

| Marca | Qué va |
|---|---|
| `@TITULO@` | La alternativa y la iteración (por ejemplo, "Cozy / Medieval, iteración 1, concepto de la portada") |
| `@ALTERNATIVA_NOMBRE@` | El nombre visible de la alternativa |
| `@ENCARGO@` · `@PEDIDO@` | La ruta del encargo creativo y las palabras del usuario, textuales (su sección 2) |
| `@ITERACION@` · `@TIPO@` | El número y el tipo (concepto, sistema o flujo) |
| `@PROPUESTAS@` | Cada parte con su nombre y su idea en una frase |
| `@ANTES@` | Las rutas de las capturas de antes de la ancla |
| `@CAPTURAS@` · `@REPORTE@` | La carpeta de capturas de la iteración y su `reporte.json` |
| `@CODIGO@` | Los archivos de cada parte (plantillas, hojas, imágenes y JavaScript) |
| `@BRIEF@` | La ruta del brief de la alternativa |
