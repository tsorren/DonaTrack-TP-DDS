---
name: relevamiento-creativo
description: >-
  Releva con el usuario un encargo creativo para diseñar desde cero en el cliente liviano de
  DonaTrack (un rediseño, una pantalla nueva o una alternativa de identidad): imágenes de
  referencia, mensaje, público, emoción, intensidad, libertades y límites, voz, juego, alcance y
  cantidad de iteraciones. Guarda las referencias con su README y deja un encargo creativo
  autocontenido y aprobado, listo para la skill corrida-creativa. Usar cuando el usuario quiere
  "algo nuevo y creativo", trae imágenes de referencia o pide una corrida de diseño desatendida.
---

# Skill: relevamiento-creativo — El encargo creativo, con el usuario

> **Ámbito:** cliente liviano (E6). Es la única parte que necesita al usuario: después,
> [`corrida-creativa`](../corrida-creativa/SKILL.md) trabaja sola.
> **Alineación:** [`AGENTS.md`](../../../AGENTS.md) y
> [`cliente-liviano/AGENTS.md`](../../../cliente-liviano/AGENTS.md),
> [`docs/interfaz/README.md`](../../../docs/interfaz/README.md) ("Cómo se le pregunta al usuario"),
> [`direccion-de-arte.md`](../../../docs/interfaz/direccion-de-arte.md) ("Libertad creativa" y
> "Oficio") y la plantilla [`encargo-creativo.md`](../../../docs/interfaz/plantillas/encargo-creativo.md).

## 1. Antes de preguntar

1. Leer las reglas para preguntar (README de interfaz), la dirección de arte y el brief de la
   alternativa, si existe (`docs/interfaz/identidad/brief*.md`).
2. Sacar las capturas de lo actual, para mostrarlas y para el "qué no repetir" (las rutas sin la
   barra inicial, por Git Bash):

   ```bash
   mvn test -pl cliente-liviano -Pvisual -Dtest=VerificacionVisualTest -Dsurefire.failIfNoSpecifiedTests=false -Dverificacion.alternativa=<alternativa> -Dverificacion.paginas=<ruta>,<ruta>
   ```

3. **Las referencias.** El usuario las adjunta o da una carpeta. Copiarlas a
   `docs/interfaz/identidad/referencias-<nombre>/` como `NN-descripcion-corta.<ext>` y **mirarlas
   una por una**. Anotar, de cada una, qué rasgo propone (lo que se podría tomar) y qué riesgo trae
   (lo que podría volverse disfraz o juego para chicos). De ahí salen las opciones de la ronda 2.

## 2. Rondas de preguntas

Con `AskUserQuestion`: **3 preguntas por ronda como máximo**, una decisión por pregunta, la opción
recomendada primero, palabras de todos los días y un `preview` cuando ayuda (un texto de muestra,
una combinación). No preguntar lo que el agente puede decidir o comprobar (contraste, tamaños,
cuántos conceptos). Si el usuario corta, lo que falta queda "a criterio de la corrida" y se anota
así. Las opciones de abajo son una guía: se adaptan a lo que mostraron las referencias.

**Ronda 1 — el mensaje**

1. "¿Qué tiene que sentir alguien al verla?" → tres combinaciones de dos o tres palabras sacadas de
   lo que pidió y de las referencias (la más fiel a sus palabras, recomendada).
2. "¿Para quién es, sobre todo?" → "Quien dona, desde el celular (Recomendada)" · "La entidad que
   recibe" · "Quien administra, muchas horas por día" · "Todos por igual".
3. "¿Qué tan lejos de lo que hay vamos?" → "Bien lejos: que sorprenda (Recomendada)" · "Marcado
   pero reconocible" · "Sobrio, con un detalle propio".

**Ronda 2 — las referencias**

1. "¿Qué es lo que más querés llevarte de las referencias?" (`multiSelect`) → los tres o cuatro
   rasgos que más se repiten (por ejemplo, "El dibujo a tinta", "Los objetos con cuerpo", "El orden
   editorial").
2. "¿Qué no queremos, aunque aparezca en alguna?" (`multiSelect`) → los riesgos que se vieron (por
   ejemplo, "Que parezca un juego para chicos", "Disfraz: textura en todo", "Letra antigua en todo").
3. "¿Alguna es la guía principal?" → las dos o tres más fuertes · "Ninguna: todas por igual".

**Ronda 3 — libertades, voz y juego**

1. "¿Qué puede cambiar la corrida?" (`multiSelect`) → "La composición de cada pantalla
   (Recomendada)" · "La navegación" · "La voz en títulos y rótulos" · "JavaScript como mejora".
   Datos, acciones, piso, stack y la otra alternativa no se preguntan: son fijos.
2. "¿Cómo habla?" → dos o tres tonos con un título de muestra cada uno en el `preview` (la tabla
   "Glassmorphism dice / esta dice").
3. "¿Cuánto juego?" → "Se insinúa: misiones, sellos, rango (Recomendada)" · "Nada de juego" ·
   "Bien presente".

**Ronda 4 — la corrida**

1. "¿Con qué pantalla empezamos?" → la más importante para el público elegido (recomendada) y dos
   más. Es la pantalla ancla de la iteración de concepto.
2. "¿Hasta dónde llega?" → "Tres vueltas: concepto, sistema y flujo completo (Recomendada)" · "Dos:
   concepto y sistema" · "Una: solo conceptos de una pantalla".
3. "¿Abro un PR al terminar?" → "Sí, en borrador (Recomendada)" · "No, dejo la rama".

## 3. Armar el encargo

1. Copiar la plantilla a `docs/interfaz/encargos/<nombre>.md` y completarla: cada sección con lo que
   dijo el usuario (sus palabras textuales en la sección 2) y lo que decidió el agente marcado "a
   criterio de la corrida". Sin "a definir": si algo queda abierto, se dice quién lo decide.
2. Escribir el README de las referencias (qué se toma de cada una y qué no), con el formato de
   `docs/interfaz/identidad/referencias-cozy-medieval/README.md`.
3. Si la alternativa es nueva, armar su brief con la plantilla
   [`brief-identidad.md`](../../../docs/interfaz/plantillas/brief-identidad.md) en
   `docs/interfaz/identidad/brief-<nombre>.md`.
4. Completar la sección 9 (la corrida): conceptos (3 por defecto), rama (`E6_<nombre>` si hay PR),
   presupuesto y el tamaño estimado en unidades contadas (iteraciones × conceptos, más críticos y
   arreglos).

## 4. Aprobación

Mostrar un resumen de 5 a 8 líneas (qué se diseña, el mensaje, lo que se toma y lo que no, hasta
dónde llega, PR sí o no, tamaño) y preguntar una sola cosa: "¿Arranco la corrida con este encargo?"
→ "Sí, arrancá (Recomendada)" · "Cambiar algo" · "Guardarlo y arrancar después".

Con el sí:

1. Marcar el encargo "aprobado por el usuario el AAAA-MM-DD" con su frase.
2. Una entrada en la [bitácora](../../../docs/interfaz/bitacora.md):
   `AAAA-MM-DD · proceso (<alternativa>) · Encargo creativo: <frase>`, con "Quién decidió: usuario".
3. Un commit con el encargo, las referencias, su README y el brief si es nuevo:
   `E6: <alternativa>, encargo creativo`.
4. Seguir con `corrida-creativa`. El "arrancá" del usuario es el pedido de correr el workflow.

## 5. Criterio de salida

- [ ] Referencias en `docs/interfaz/identidad/referencias-<nombre>/` con su README.
- [ ] Encargo en `docs/interfaz/encargos/<nombre>.md`, autocontenido y aprobado, sin "a definir".
- [ ] Entrada en la bitácora y commit.
