# Encargo creativo: <nombre>

> Plantilla de la skill `relevamiento-creativo`; la lee la skill `corrida-creativa`. Una copia por
> corrida en `docs/interfaz/encargos/<nombre>.md`. Es **autocontenida**: la corrida no le pregunta
> nada al usuario, así que lo que no está acá lo decide la corrida con la
> [dirección de arte](../direccion-de-arte.md) y queda anotado como "a criterio de la corrida".
>
> **Estado:** borrador | aprobado por el usuario el AAAA-MM-DD

## 1. Qué se diseña

- **Alternativa:** `<nombre>` (existente) o `<nombre>` (nueva, con su brief en `identidad/`).
- **Punto de partida:** rediseño de lo que hay | desde cero.
- **Pantalla ancla:** la ruta de la iteración de concepto, por ejemplo `/estilos/armazon` (el
  inicio de una persona donante) o `/prototipo/publico-portada`.
- **Hasta dónde llega:** concepto (una pantalla) | sistema (navegación, pantallas y componentes) |
  flujo completo (el prototipo navegable).
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

Carpeta: `docs/interfaz/identidad/referencias-<nombre>/`, con su README (qué se toma de cada una).
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
| Tipografía y paleta de la alternativa | | | |
| JavaScript propio como mejora | | | |

Fijo siempre, aunque la tabla diga otra cosa: los datos y las acciones, el piso, el stack y la otra
alternativa ([Libertad creativa](../direccion-de-arte.md#libertad-creativa)).

## 7. Voz

- **Tono:** una frase (por ejemplo, "aventura adulta, nunca infantil").
- **Cómo dice algunos títulos y rótulos:**

  | Glassmorphism dice | Esta alternativa dice |
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
| Rama | `E6_<nombre>` (con PR hacia `ENTREGA_6`, el CI exige el prefijo) |
| Al terminar | PR en borrador hacia `<rama base>` · solo la rama, sin push |
| Presupuesto | tope de uso; si se acaba, la corrida cierra la iteración en curso y deja la nota de traspaso |
| Modelo de la sesión | Opus (los roles de la corrida declaran el suyo en la skill) |

## 10. Aprobación

- **Fecha:** AAAA-MM-DD.
- **Lo que dijo el usuario:** "…".
