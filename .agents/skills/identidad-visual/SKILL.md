---
name: identidad-visual
description: >-
  Define o ajusta la identidad visual del cliente liviano de DonaTrack (personalidad, voz, paleta,
  tipografía, forma, densidad e imagen) con rondas cortas de preguntas al usuario, comparaciones A/B
  y tokens.css. Usar al empezar el diseño gráfico, cuando el usuario quiere cambiar el aspecto del
  sitio o cuando falta docs/interfaz/identidad/brief.md.
---

# Skill: identidad-visual — Identidad visual con el usuario

> **Ámbito:** cliente liviano (E6).
> **Alineación:** [`cliente-liviano/AGENTS.md`](../../../cliente-liviano/AGENTS.md),
> [`docs/interfaz/README.md`](../../../docs/interfaz/README.md) y
> [`direccion-de-arte.md`](../../../docs/interfaz/direccion-de-arte.md) (D19, D20).

## 1. Antes de empezar

1. Leer las reglas para preguntar (README, "Cómo se le pregunta al usuario"), la dirección de arte,
   las pistas para la identidad de [`bocetos-e1.md`](../../../docs/interfaz/bocetos-e1.md) y el
   [tono y comunicación](../../../docs/interfaz/tono-y-comunicacion.md). El tono ya fija el trato,
   la personalidad verbal, los estados y las cifras para todas las alternativas: no se preguntan.
   La identidad visual decide la metáfora y la estética, y solo la voz propia de títulos y rótulos.
2. Si ya existe `docs/interfaz/identidad/brief.md` y el pedido es un ajuste puntual ("probemos otro
   verde"), saltar al paso 4 con el tema que corresponda.

## 2. Rondas de preguntas

Con `AskUserQuestion`: hasta 3 rondas de hasta 3 preguntas, una opción recomendada por pregunta y
un ejemplo en `preview` cuando ayuda. Si el usuario corta, lo que falta queda "abierto" y lo
propone el agente. Las preguntas ya están redactadas; adaptar las opciones al caso.

**Ronda 1 — personalidad**

1. "¿Qué tres palabras deberían describir cómo se ve DonaTrack?" → "Cercana, confiable, viva
   (Recomendada)" · "Seria, transparente, eficiente" · "Alegre, comunitaria, cálida".
2. "¿A qué no queremos parecernos?" → "A un banco o una app de trámites" · "A una ONG antigua" ·
   "A una startup más".
3. "¿Qué tiene que lucirse más?" → "Las fotos de las entregas (Recomendada)" · "Los resultados
   registrados (entregas confirmadas)" · "Las personas y las entidades".

**Ronda 2 — voz propia y forma**

1. "¿Cómo dicen los títulos esta alternativa?" → dos o tres tonos con un título de muestra cada uno
   en el `preview`; el trato (vos) y el resto de la voz vienen del tono común.
2. "¿Qué sensación de formas preferís?" → "Suaves y amables" · "Firmes y precisas" · "Mezcla:
   suaves en botones, firmes en la estructura". Mostrar un `preview` con un botón y una tarjeta de
   cada tipo.
3. "¿Hay algo que tengamos que respetar sí o sí?" → "Nada, hay libertad (Recomendada)" · "Un logo
   o un color existente" · "Algo de accesibilidad extra".

**Ronda 3 — solo si la dirección sigue ambigua**

1. "¿Hay algún sitio o marca que te guste cómo se ve?" (respuesta libre).

Con las respuestas, completar
[`plantillas/brief-identidad.md`](../../../docs/interfaz/plantillas/brief-identidad.md) en
`docs/interfaz/identidad/brief.md`. Mostrar un resumen de 5 líneas y pedir la aprobación con una
sola pregunta.

## 3. Dos direcciones

Armar dos direcciones con las 7 piezas de la dirección de arte: concepto, audacia, tipografía,
color, forma, imagen y movimiento. Tienen que ser **dos mundos distintos**, no dos tonos del mismo.
Antes de construirlas, revisar la lista "Lo que evitamos" y reemplazar todo lo genérico.

## 4. Comparar

Correr la skill `iteracion-ab` con el tema `direccion-general`. Después, solo los temas que
sigan abiertos, en el orden del [catálogo](../../../docs/interfaz/temas-ab.md): `paleta`,
`tipografia`, `forma` y `densidad`.

## 5. Llevar a `tokens.css`

1. Valores base con prefijo `--p-*`, tokens de uso (`--color-*`, `--fuente-*`, etc.) y asignación
   de `--pico-*` a partir de los usos, todo en
   `cliente-liviano/src/main/resources/static/css/tokens.css` (Glassmorphism) o en el
   `tokens.css` de la alternativa que se está diseñando, en `static/css/alternativas/<nombre>/`.
   Una alternativa nueva lleva además su brief en `docs/interfaz/identidad/` y su `app.css`.
2. Puntos de quiebre en `--medida-quiebre-*`.
3. Fuentes OFL en `static/fuentes/`, con su archivo de licencia.
4. Si se agregan pares texto-fondo nuevos, sumarlos a la lista de contrastes que comprueba
   `InterfazFitnessTest`.
5. Actualizar `/estilos` y correr:

   ```bash
   mvn clean test -pl cliente-liviano -am
   mvn test -pl cliente-liviano -Pvisual
   ```

6. Una entrada en la [bitácora](../../../docs/interfaz/bitacora.md) por cada decisión.

## 6. Criterio de salida

- [ ] Brief aprobado por el usuario.
- [ ] `tokens.css` con la identidad elegida; tests y comprobación visual en verde.
- [ ] `/estilos` muestra la identidad en todos los componentes.
- [ ] Bitácora con una entrada por tema decidido y capturas en `docs/interfaz/evidencia/`.
