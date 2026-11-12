# Fuentes de las ilustraciones

Los SVG originales de la diseñadora para la piel Huerta con abejas (variante K), tal como los
entregó. El sitio no los sirve: las versiones que usan las pantallas están en
`cliente-liviano/src/main/resources/static/img/ilustraciones/`. Sirven para medir encuadres y
recorridos (por ejemplo, los de las abejas en `static/js/abejas.js` y `componentes/huerta.css`) y para
volver a exportar.

Estaban en `cliente-liviano/assets/`; se movieron acá el 2026-11-10 (revisión de la PR 959), con los
espacios del nombre cambiados por guiones (`Asset 1.svg` → `Asset-1.svg`,
`DonaTrack_Artboard 1-14.svg` → `DonaTrack_Artboard-1-14.svg`).

| Archivos | Qué son |
|---|---|
| `DonaTrack_Abeja1.svg` a `DonaTrack_Abeja3.svg` | Recorridos de las abejas de la portada |
| `DonaTrack_Bee.*`, `DonaTrack-Bee.*` | La abeja: arriba, abajo y dormida, en claro y oscuro |
| `DonaTrack_Paso1-*` a `DonaTrack_Paso4-*` | Los cuatro pasos de "Cómo funciona" |
| `DonaTrack_Orange-*`, `_Cotton-*`, `_Pine-*`, `_Mint-*`, `_ElephantEars-*`, `_Apple.*`, `_Plum.*` | Las plantas (crecidas y en flor) |
| `DonaTrack_Retry-*` | La maceta de reintentar |
| `necesidades.svg` | El ícono de Necesidades del menú de la entidad (la mano con el corazón; usuario, 2026-11-12), en `templates/iconos.html` como `nav-necesidades` |
| `Asset-1.svg` a `Asset-3.svg`, `DonaTrack-33.svg`, `DonaTrack_Artboard-1-14.svg` | Piezas sueltas sin nombre de la entrega |

Decisión de identidad: [ADR de la piel Huerta](../../../adr/cliente-liviano/20261110-piel-huerta-con-abejas-como-identidad-del-cliente-liviano.md) (`proposed`).
