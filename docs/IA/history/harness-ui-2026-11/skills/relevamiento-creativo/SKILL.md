---
name: relevamiento-creativo
description: >-
  Releva con el usuario un encargo creativo para diseñar desde cero en el cliente liviano de
  DonaTrack (un rediseño, una pantalla nueva o una identidad nueva): imágenes de
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

1. Leer las reglas para preguntar (README de interfaz), la dirección de arte, el
   [tono y comunicación](../../../docs/interfaz/tono-y-comunicacion.md) (común a todo el sitio:
   no se vuelve a preguntar) y el brief vigente de la identidad
   (`docs/interfaz/identidad/brief.md`).
2. Sacar las capturas de lo actual, para mostrarlas y para el "qué no repetir" (las rutas sin la
   barra inicial, por Git Bash):

   ```bash
   mvn test -pl cliente-liviano -Pvisual -Dtest=VerificacionVisualTest -Dsurefire.failIfNoSpecifiedTests=false -Dverificacion.paginas=<ruta>,<ruta>
   ```

3. **Las referencias.** El usuario las adjunta o da una carpeta. Copiarlas a
   `docs/interfaz/identidad/referencias/` como `NN-descripcion-corta.<ext>` (si la propuesta es una
   identidad nueva, reemplazan a las que hay, en su rama; si no, siguen la numeración) y **mirarlas
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
   rasgos que más se repiten, dichos como se ven (por ejemplo, "La letra grande", "Los colores
   planos", "El orden en columnas").
2. "¿Qué no queremos, aunque aparezca en alguna?" (`multiSelect`) → los riesgos que se vieron (por
   ejemplo, "Que parezca un juego para chicos", "Disfraz: el adorno en todo", "Que cueste leer").
3. "¿Alguna es la guía principal?" → las dos o tres más fuertes · "Ninguna: todas por igual".

**Ronda 3 — libertades, voz y juego**

1. "¿Qué puede cambiar la corrida?" (`multiSelect`) → "La composición de cada pantalla
   (Recomendada)" · "La navegación" · "La voz en títulos y rótulos" · "Movimiento e interacción
   encima de lo que ya funciona" (es JavaScript propio como mejora; la página sigue andando sin él).
   Datos, acciones, piso y stack no se preguntan: son fijos.
2. "¿Cómo habla en títulos y rótulos?" → dos o tres tonos con un título de muestra cada uno en el
   `preview` (la tabla "Hoy dice / La propuesta dice"). Solo títulos y rótulos: el resto de la
   voz es el tono común, y los títulos de muestra también lo pasan (sin prometer impacto).
3. "¿Cuánto juego?" → "Se insinúa: misiones, sellos, rango (Recomendada)" · "Nada de juego" ·
   "Bien presente". En cualquier caso, el juego celebra hechos confirmados y no presiona
   ([tono común](../../../docs/interfaz/tono-y-comunicacion.md#juego-y-celebración)).

**Ronda 4 — la corrida**

1. "¿Con qué pantalla empezamos?" → la más importante para el público elegido (recomendada) y dos
   más. Es la pantalla ancla de la iteración de concepto.
2. "¿Hasta dónde llega?" → "Tres vueltas: concepto, sistema y flujo completo (Recomendada)" · "Dos:
   concepto y sistema" · "Una: solo conceptos de una pantalla".
3. "Al terminar, ¿dejo el trabajo listo para revisar en GitHub?" → "Sí, como borrador para revisar
   (Recomendada)" (un PR en borrador) · "No, lo dejo guardado en una rama".

## 3. Armar el encargo

1. Copiar la plantilla a `docs/interfaz/encargos/<nombre>.md` y completarla: cada sección con lo que
   dijo el usuario (sus palabras textuales en la sección 2) y lo que decidió el agente marcado "a
   criterio de la corrida". Sin "a definir": si algo queda abierto, se dice quién lo decide.
2. Escribir el README de las referencias: una fila por imagen, con el archivo, qué se toma y qué
   no.
3. Si la propuesta es una identidad nueva, armar su brief con la plantilla
   [`brief-identidad.md`](../../../docs/interfaz/plantillas/brief-identidad.md): reemplaza a
   `docs/interfaz/identidad/brief.md` en la rama de la corrida.
4. Completar la sección 9 (la corrida): conceptos (3 por defecto), rama propia (`E6_<nombre>` si
   hay PR; la rama es la propuesta), las pantallas fuera de alcance (las que no se tocan),
   el tamaño en unidades contadas y el presupuesto. El presupuesto lo propone el agente, no se
   pregunta: unos 10 agentes por iteración con 2 conceptos (director, constructores, integración,
   3 críticos, decisión, arreglos y cierre), uno de preparación y de 4 a 7 entre el cierre, la
   auditoría y el PR. La corrida de prueba (1 iteración y 2 conceptos) usó 15 agentes, unos 2
   millones de tokens de los subagentes y 74 minutos (`corrida-creativa`, sección 2). Se aprueba
   junto con el encargo.

## 4. Aprobación

Mostrar un resumen de 5 a 8 líneas (qué se diseña, el mensaje, lo que se toma y lo que no, hasta
dónde llega, si queda listo para revisar en GitHub, el tamaño y el presupuesto) y preguntar una
sola cosa: "¿Arranco la corrida con este encargo?"
→ "Sí, arrancá (Recomendada)" · "Cambiar algo" · "Guardarlo y arrancar después".

Con el sí:

1. Marcar el encargo "aprobado por el usuario el AAAA-MM-DD" con su frase.
2. Una entrada en la [bitácora](../../../docs/interfaz/bitacora.md):
   `AAAA-MM-DD · proceso (<nombre>) · Encargo creativo: <frase>`, con "Quién decidió: usuario".
3. En la rama de la corrida, un commit con el encargo, las referencias, su README y el brief si es
   nuevo: `E6: <nombre>, encargo creativo`.
4. Seguir con `corrida-creativa`. El "arrancá" del usuario es el pedido de correr el workflow.

## 5. Criterio de salida

- [ ] Referencias en `docs/interfaz/identidad/referencias/` con su README.
- [ ] Encargo en `docs/interfaz/encargos/<nombre>.md`, autocontenido y aprobado, sin "a definir".
- [ ] Entrada en la bitácora y commit.
