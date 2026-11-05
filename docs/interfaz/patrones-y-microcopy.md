# Patrones, estados y textos

> UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5
> **Versión 1 · 2026-10-30 · parte del [proceso de diseño](README.md)**

Cubre RNF-UI-01 a RNF-UI-06 de [`requerimientos-e6.md`](../requerimientos/requerimientos-e6.md).
La cátedra pide **patrones** conocidos (Material Design, Human Interface Guidelines), no una
librería: cada componente del catálogo `/estilos` cita el patrón que sigue.

## Componentes y su patrón

| Componente | Patrón | Cuándo se usa | Accesibilidad |
|---|---|---|---|
| Botón | Material: Buttons (filled, outlined, text) | Una acción principal por pantalla; las otras, secundarias | `<button>` real, texto que dice qué hace |
| Campo de formulario | Material: Text fields | Siempre con etiqueta visible arriba; la ayuda, debajo | `<label for>`, error enlazado con `aria-describedby` |
| Carga de fotos | HIG: Photo pickers | Adjuntar imágenes desde la cámara o la galería | `<input type="file">` real; el arrastre es un extra |
| Aviso breve (toast) | Material: Snackbar | Confirmar una acción que salió bien | Región `aria-live="polite"`; no se va antes de 5 s |
| Banner | Material: Banners | Algo que afecta a toda la pantalla (error del servidor, sin conexión) | `role="alert"` solo si es urgente |
| Diálogo | Material: Dialogs / HIG: Alerts | Confirmar algo irreversible | `<dialog>` nativo; el foco entra y vuelve |
| Esqueleto | Material: Progress indicators | Listas o tarjetas que tardan más de 300 ms | `aria-busy="true"` en la región |
| Indicador de carga | Material: Progress indicators | Acciones que tardan más de 300 ms | Texto "Enviando…" además del ícono |
| Estado vacío | Material: Empty states | Una lista sin datos | Invita a la acción posible |
| Tarjeta | Material: Cards | Una donación o una entrega con foto | El título es el enlace principal |
| Tabla o lista | Material: Data tables / Lists | Escritorio: tabla. Celular: cada fila como tarjeta | `<table>` con `<th scope>` |
| Navegación por rol | Material: Navigation bar / Tabs | Secciones del rol; la actual, marcada | `<nav>` con `aria-current="page"` |
| Mapa | — | Siempre acompañado de una lista con los mismos datos | RF-CL-06; "última actualización" visible (RF-CL-07) |

## Estados que toda pantalla contempla

| Estado | Cuándo | Qué ve el usuario |
|---|---|---|
| Con datos | El caso normal | El contenido |
| Vacío | No hay datos todavía | Qué significa y qué puede hacer ("Todavía no tenés entregas para confirmar.") |
| Cargando | Más de 300 ms de espera | Esqueleto o indicador con texto |
| Éxito | Una acción salió bien | Aviso breve que dice qué pasó |
| Error de datos | El formulario tiene algo mal | El error junto al campo, en palabras simples, y cómo arreglarlo |
| Error del servidor | Algo falló de nuestro lado | Banner con disculpa breve, qué hacer y código de seguimiento (`traceId`) |
| Sin permiso | El rol no puede ver esto | Mensaje claro y un camino de vuelta |
| Deshabilitado | Falta un paso previo | El control se ve inactivo y se explica por qué |

## Qué aviso usar

```text
¿La acción salió bien?           → aviso breve (toast)
¿Falló algo de un campo?         → error junto al campo
¿Falló algo que afecta todo?     → banner arriba
¿Va a hacer algo irreversible?   → diálogo de confirmación, antes de hacerlo
```

## Voz y tono

- **Voseo**, como en los bocetos ("Sumate", "Podés"). Se confirma en la ronda de identidad.
- Cercano y concreto. Trata al usuario como alguien que hace algo valioso, sin exagerar.
- Oraciones cortas. Mayúscula solo al inicio de la oración, no en cada palabra.

## Reglas para los textos

1. El botón dice **qué pasa al tocarlo**: "Confirmar recepción", no "Enviar" ni "Aceptar".
2. La misma acción se llama igual en todo el flujo.
3. Los errores dicen qué pasó y cómo seguir, sin culpar: "La foto pesa más de 5 MB. Probá con una
   más liviana."
4. Sin jerga técnica ni códigos, salvo el código de seguimiento para soporte.
5. Etiquetas visibles siempre. El texto de ayuda dentro del campo no reemplaza a la etiqueta.
6. Los estados vacíos invitan a hacer algo.
7. Las fechas y las cantidades, en formato argentino: "18 de mayo de 2026", "1.234".
8. Nada de signos de exclamación en los errores.
9. Una ayuda (tooltip o texto chico) solo donde la duda es real, no en cada campo.
10. Si se pide algo sensible (fotos, datos personales), se dice para qué se usa.

## Ayudas por flujo crítico (RNF-UI-04)

Cada ficha de flujo completa esta lista:

- [ ] Cada campo tiene etiqueta visible.
- [ ] El ejemplo de dato válido está donde se duda (formato, tamaño, tipo de archivo).
- [ ] Hay una pista de primer uso donde el flujo no es obvio.
- [ ] Se explica para qué se piden los datos sensibles.
- [ ] Cada acción tiene su texto de éxito y su texto de error.

## Las 10 heurísticas, como preguntas

Cada ficha de flujo contesta estas preguntas (RNF-UI-01):

| # | Heurística | Pregunta |
|---|---|---|
| 1 | Visibilidad del estado | ¿El usuario sabe siempre qué está pasando y si su acción funcionó? |
| 2 | Lenguaje del usuario | ¿Las palabras son las de una entidad o un donante, no las del sistema? |
| 3 | Control y libertad | ¿Puede deshacer, volver o cancelar sin perder lo cargado? |
| 4 | Consistencia | ¿Lo que se parece se comporta igual en todo el sitio? |
| 5 | Prevención de errores | ¿El diseño evita el error antes de tener que avisarlo? |
| 6 | Reconocer antes que recordar | ¿Lo necesario está a la vista y no hay que recordarlo de otra pantalla? |
| 7 | Flexibilidad | ¿Quien lo usa seguido tiene atajos y quien lo usa por primera vez no se pierde? |
| 8 | Diseño minimalista | ¿Hay algo en pantalla que no ayude a la tarea? |
| 9 | Recuperarse de errores | ¿El error dice qué pasó y cómo arreglarlo? |
| 10 | Ayuda | ¿Hay ayuda donde hace falta, breve y en contexto? |
