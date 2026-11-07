# Encargo creativo: <nombre>

> Plantilla de la skill `relevamiento-creativo`; la lee la skill `corrida-creativa`. Una copia por
> corrida en `docs/interfaz/encargos/<nombre>.md`. Es **autocontenida**: la corrida no le pregunta
> nada al usuario, así que lo que no está acá lo decide la corrida con la
> [dirección de arte](../direccion-de-arte.md) y queda anotado como "a criterio de la corrida".
>
> **Estado:** borrador | aprobado por el usuario el AAAA-MM-DD

## 1. Qué se diseña

- **Propuesta:** `<nombre>`, en su propia rama (la rama es la propuesta; se decide con el merge).
  Si es una identidad nueva, su brief reemplaza a `identidad/brief.md` en esa rama.
- **Punto de partida:** rediseño de lo que hay | desde cero.
- **Pantalla ancla:** la ruta de la iteración de concepto, por ejemplo `/donante-inicio` (el
  inicio de una persona donante) o `/publico-portada`.
- **Hasta dónde llega:** concepto (una pantalla) | sistema (layouts, navegación y hojas del sitio) |
  flujo completo (las pantallas del recorrido).
- **Fuera de alcance:** las pantallas que la corrida no toca (la corrida comprueba que sus
  capturas no cambien), o "ninguna".
- **Lo que NO hay que repetir:** las capturas de lo actual (rutas) y, en una frase, por qué no
  sirve.

## 2. En palabras del usuario

> Lo que pidió, textual, sin reinterpretar.

## 3. Mensaje, público y emoción

- **Qué tiene que transmitir:** una frase.
- **Para quién:** los roles y en qué momento la usan.
- **Qué tiene que sentir quien la ve:** dos o tres palabras.
- **Intensidad:** que sorprenda | marcada pero reconocible | sobria con un detalle propio.

## 4. Referencias

Carpeta: `docs/interfaz/identidad/referencias/`, con su README (qué se toma de cada una).
Son de terceros: se toma el estilo, no se copia nada, y no entran al sitio ni al entregable.

| Referencia | Qué se toma | Qué no |
|---|---|---|
| `01-…` | | |

**La guía principal:** `<referencia>` o "todas por igual".

## 5. Lo que no queremos

Además de "Lo que evitamos" de la dirección de arte:

-

## 6. Libertades y límites

| Qué | Libre | Con límite (cuál) | Fijo |
|---|:-:|---|:-:|
| Composición de cada pantalla | | | |
| Navegación | | | |
| Materiales, dibujos propios y movimiento | | | |
| Voz en títulos y rótulos | | | |
| Tipografía y paleta del sitio | | | |
| JavaScript propio como mejora | | | |

Fijo siempre, aunque la tabla diga otra cosa: los datos y las acciones, el piso, el stack, las
pantallas fuera de alcance ([Libertad creativa](../direccion-de-arte.md#libertad-creativa)) y el
[tono común](../tono-y-comunicacion.md).

## 7. Voz

La voz propia vive en títulos y rótulos. Estados, cifras, acciones y errores siguen el
[tono común](../tono-y-comunicacion.md) y no se adornan.

- **Tono:** una frase (por ejemplo, "cercana y concreta, nunca solemne").
- **Cómo dice algunos títulos y rótulos:**

  | Hoy dice | La propuesta dice |
  |---|---|
  | | |

- **Palabras que no van:**

## 8. Juego

- **Cuánto:** nada | se insinúa | bien presente.
- **Con qué piezas:** misiones, rangos, sellos, avance… o ninguna.

## 9. La corrida

| Qué | Valor |
|---|---|
| Iteraciones | 1 (concepto) · 2 (concepto y sistema) · 3 (concepto, sistema y flujo) |
| Conceptos en la iteración 1 | de 2 a 4 (recomendado: 3) |
| Rama | `E6_<nombre>`, propia de la propuesta (con PR hacia `ENTREGA_6`, el CI exige el prefijo) |
| Al terminar | PR en borrador hacia `<rama base>` · solo la rama, sin push |
| Presupuesto | lo propone el agente (agentes, tokens y tiempo, con la corrida de prueba como medida) y se aprueba con el encargo; si se acaba el uso, se retoma desde la última nota de traspaso |
| Modelo de la sesión | Opus (los roles de la corrida declaran el suyo en la skill) |

## 10. Aprobación

- **Fecha:** AAAA-MM-DD.
- **Lo que dijo el usuario:** "…".
