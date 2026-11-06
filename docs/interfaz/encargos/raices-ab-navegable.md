# Encargo creativo: Raíces A/B navegable

> Plantilla de la skill `relevamiento-creativo`; la lee la skill `corrida-creativa`. Es
> **autocontenida**: la corrida no le pregunta nada al usuario, así que lo que no está acá lo decide
> la corrida con la [dirección de arte](../direccion-de-arte.md) y queda anotado como "a criterio de
> la corrida".
>
> **Estado:** aprobado por el usuario el 2026-11-02

## 1. Qué se diseña

- **Alternativa:** `raices` (existente, con su brief en
  [`identidad/brief-raices.md`](../identidad/brief.md)). Se arman dos sitios navegables de la
  misma alternativa: **A** en `/prototipo/raices/` y **B** en `/prototipo/raices-b/`.
- **Punto de partida:** rediseño de lo que hay. Las siete pantallas aprobadas de Raíces (portada,
  Seguimiento, Necesidades, Confirmar recepción, Contacto, Legales y Mi perfil) son el punto de
  partida; A no las toca y B puede rehacerlas.
- **Pantalla ancla:** `/prototipo/raices/publico-portada` (y `/prototipo/raices-b/publico-portada`
  para B). Es donde empiezan los tres recorridos.
- **Hasta dónde llega:** flujo completo (el prototipo navegable), en dos versiones paralelas, A y B,
  que no compiten por un ganador: se entregan las dos y las elige el equipo.
- **Lo que NO hay que repetir:** las pantallas actuales de Raíces
  (`/prototipo/raices/publico-portada`, `donante-seguimiento`, `entidad-necesidades`,
  `entidad-seguimiento-confirmar`, `publico-contacto`, `publico-legales`, `donante-perfil`). Un
  hallazgo de la preparación: la portada de Raíces enlazaba a rutas de Cozy y cortaba el recorrido
  público; en los dos sitios todo enlace va a pantallas de Raíces.

## 2. En palabras del usuario

> Aprender el sistema de Raíces y plantear (y luego correr) una corrida desatendida que deje dos
> sitios navegables, A y B. Los recorridos Público "entrar", Donante "donar y seguir" y Entidad
> "recibir" se recorren de punta a punta en A y en B, sin enlaces muertos; lo demás enlaza a "sin
> dibujar". Admin con inicio y menú; el resto de sus pantallas, "sin dibujar". A fiel: extiende las
> piezas actuales y no toca las siete aprobadas. B audaz: mismo ADN (paleta Oliva, risografía,
> metáfora de raíces), otra navegación y composición, y puede rediseñar las aprobadas. Fuera de
> alcance: servicios backend, ADR y DTI nuevos. Entrega en commits sobre `E6_raices`, sin push.
> Los huecos de backend se resuelven con datos sintéticos de maqueta y una lista al final.
>
> (Relevamiento de 2 rondas, 2026-11-02, y plan aprobado el mismo día: "Sí, la corrida".)

## 3. Mensaje, público y emoción

- **Qué tiene que transmitir:** lo que se dona llega a quien lo necesita y queda a la vista, paso a
  paso, con todo lo que lo prueba (arraigada y transparente, según el brief).
- **Para quién:**
  - Público: quien llega sin cuenta, entra, mira donaciones realizadas, el mapa de entregadas y las
    entidades con necesidades, y se registra como donante o como entidad.
  - Donante (la cuenta de ejemplo es una empresa, Distribuidora Ejemplo SA): dona y sigue sus
    donaciones, mira entidades y notificaciones. Foco de Raíces: la persona de responsabilidad
    social de la empresa, en escritorio.
  - Entidad: recibe, publica necesidades, ve las donaciones recibidas y confirma la recepción.
  - Admin: solo el inicio y su menú.
- **Qué tiene que sentir quien la ve:** arraigada, clara, de confianza; cálida por la imagen y
  ordenada por la trazabilidad.
- **Intensidad:** A, marcada pero reconocible (fiel a lo aprobado). B, que sorprenda dentro del
  mismo ADN.

## 4. Referencias

Carpeta: [`docs/interfaz/identidad/referencias-raices/`](../identidad/referencias/README.md),
con su README. Son de terceros: se toma el estilo, no se copia nada, y no entran al sitio ni al
entregable. Además, las pantallas actuales de Raíces (listadas en la sección 1) son la referencia
interna de oficio, y el [brief de Raíces](../identidad/brief.md) fija el ADN.

| Referencia | Qué se toma | Qué no |
|---|---|---|
| `referencias-raices/` (las del README de la carpeta) | La textura de impresión, la red que es mapa, la lámina de campo y el corte de suelo | Cualquier imagen o texto de terceros |
| Pantallas actuales de Raíces | Piezas (`layout-privado`, `layout-publico`, `pie`, `iconos`, `sellos.css`, `formularios.css`), etiquetas de herbario, frutos, una sola imagen de raíz por pantalla en el título | Nada del prototipo de Cozy |
| `brief-raices.md` | Paleta Oliva, Archivo ancha, risografía solo en el fondo, esquina suave con sombra plana corrida, la voz | Lo que el brief marca como evitar |

**La guía principal:** el brief de Raíces y las pantallas aprobadas.

## 5. Lo que no queremos

Además de "Lo que evitamos" de la dirección de arte:

- Un panfleto "eco": planeta, "be eco", hojitas sueltas, lavado verde.
- Urgencia, culpa y las palabras "eco", "florecer", "regá", "cosechá" y "sostenible" usadas de
  adorno.
- Enlaces a Cozy desde los recorridos: todo enlace va a pantallas de Raíces o a "sin dibujar".
- Misiones e incentivos: la cuenta donante es una empresa y RF-07 los oculta.
- Dibujo de cuento ante una empresa; acercarse a Cozy o a Glassmorphism.
- Datos reales: todo dato de maqueta es sintético.

## 6. Libertades y límites

| Qué | Libre | Con límite (cuál) | Fijo |
|---|:-:|---|:-:|
| Composición de cada pantalla | B | A extiende las piezas actuales sin tocar las siete aprobadas | |
| Navegación | B | A conserva la de Raíces; el menú de cada rol no suma Incentivos | |
| Materiales, dibujos propios y movimiento | | Los de Raíces; una sola imagen de raíz por pantalla; respeta "reducir movimiento" | |
| Voz en títulos y rótulos | | La de Raíces: "echa raíces en tu comunidad", "Quiero donar", "Lo que está confirmado" | |
| Tipografía y paleta de la alternativa | | | X |
| JavaScript propio como mejora | | Solo como mejora sobre enlaces y formularios reales (Server Side Rendering) | |

Fijo siempre, aunque la tabla diga otra cosa: los datos y las acciones, el piso, el stack, la otra
alternativa ([Libertad creativa](../direccion-de-arte.md#libertad-creativa)) y el
[tono común](../tono-y-comunicacion.md).

Nombres de pantalla fijos para los dos sitios (así se enlazan entre grupos): `publico-donaciones`,
`publico-mapa`, `publico-entidades`, `publico-ingresar`, `publico-registro`,
`publico-registro-donante`, `publico-registro-entidad`, `donante-inicio`, `donante-donaciones`,
`donante-donaciones-detalle`, `donante-entidades`, `donante-notificaciones`, `entidad-inicio`,
`entidad-recibidas`, `entidad-notificaciones`, `admin-inicio` y las ya existentes.

Recorridos que tienen que quedar navegables de punta a punta, en A y en B:

- Público "entrar": portada, donaciones, mapa o entidades, ingresar o registrarse, registro de
  donante o de entidad.
- Donante "donar y seguir": inicio, mis donaciones, detalle, seguimiento; entidades; notificaciones.
- Entidad "recibir": inicio, necesidades, donaciones recibidas, confirmar recepción;
  notificaciones.
- Ingresar, en la maqueta, lleva al inicio de cada rol con su cuenta de ejemplo (donante, la empresa
  Distribuidora Ejemplo SA; entidad; admin).
- Admin: solo `admin-inicio`; sus secciones caen en `sin-dibujar`.

Huecos de backend: datos de maqueta y una línea en la lista de huecos (rol, pantalla, dato que
falta, por qué); sin DTI ni ADR nuevos.

## 7. Voz

La voz propia vive en títulos y rótulos. Estados, cifras, acciones y errores siguen el
[tono común](../tono-y-comunicacion.md) y no se adornan.

- **Tono:** arraigado y transparente; lo pendiente se dice pendiente, las cifras van con período y
  fecha, sin urgencia ni culpa.
- **Cómo dice algunos títulos y rótulos:**

  | Glassmorphism dice | Raíces dice |
  |---|---|
  | Portada de inicio | "Echa raíces en tu comunidad" |
  | Donar | "Quiero donar" |
  | Cifras de impacto | "Lo que está confirmado" |

- **Palabras que no van:** eco, florecer, regá, cosechá y sostenible de adorno; las que marcan
  urgencia o culpa.

## 8. Juego

- **Cuánto:** nada. Los sellos de estado y los frutos son trazabilidad, no juego.
- **Con qué piezas:** ninguna. Misiones e incentivos quedan fuera (RF-07).

## 9. La corrida

| Qué | Valor |
|---|---|
| Iteraciones | 3 (flujo completo), con A y B en paralelo y sin elegir un ganador |
| Conceptos en la iteración 1 | No aplica: A y B son los dos sitios, no conceptos que compitan |
| Rama | `E6_raices` (la rama ya existe) |
| Al terminar | Solo commits en `E6_raices`; sin PR y sin push |
| Presupuesto | Propuesto en el plan aprobado: unos 22 agentes, de 3 a 5 millones de tokens de subagentes y de 3 a 5 horas ([INFERRED] a partir de la corrida de prueba de Cozy); si se acaba el uso, se retoma desde la última nota de traspaso |
| Modelo de la sesión | Opus (los roles de la corrida declaran el suyo en la skill) |

Cada sitio tiene 16 pantallas nuevas (público 7, donante 5, entidad 3, admin 1); B además rehace las
siete aprobadas. B comparte `tokens.css` con Raíces (paleta y letra) y suma sus hojas en
`static/css/prototipo/raices-b/`. Como la regla "Ninguna pantalla sin su flujo aprobado" no tiene
fichas para las pantallas nuevas, la corrida escribe fichas compartidas por A y B en
`docs/interfaz/flujos/` con `revision-ui` en PASS; quedan "pendiente de aprobación" hasta que se
elija A o B.

## 10. Aprobación

- **Fecha:** 2026-11-02.
- **Lo que dijo el usuario:** "Sí, la corrida" (sobre el plan de la corrida desatendida A/B de
  Raíces, en la sesión del 2026-11-02).
