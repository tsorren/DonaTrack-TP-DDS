# Bocetos de la Entrega 1 — inventario

> UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5
> **Versión 1 · 2026-10-30 · insumo del proceso de diseño ([README](README.md))**

Fuente: [`DonaTrack-Bocetos.pdf`](../entregas/1/interfaz/DonaTrack-Bocetos.pdf) (31 páginas, exportado de
[`DonaTrack.fig`](../entregas/1/interfaz/DonaTrack.fig)) y [`Mapa del Sitio.pdf`](../entregas/1/interfaz/Mapa%20del%20Sitio.pdf).

## Cómo usar este inventario

- Los bocetos son el **punto de partida**, no un molde. El equipo decidió que se pueden cambiar.
  Cada cambio importante se anota en la [bitácora](bitacora.md) con su porqué.
- Son bocetos en **escala de grises** (negro, blanco y grises de Figma). Muestran estructura y
  contenido, no la identidad visual. La única pieza en color son las insignias ilustradas.
- Las páginas están a 1280 px de ancho (escritorio). No hay versión móvil: esa la diseña el
  proceso.

## Páginas

| Pág. | Pantalla | Quién la ve | Qué muestra | Para revisar al diseñar |
|---|---|---|---|---|
| 1 | Landing | Público | Menú superior, título "Multiplicando el impacto de tu solidaridad", botón "Donar Ahora", 3 contadores en círculos, carrusel de emergencias con "AYUDAR", donaciones destacadas (filtro semana/mes/año, tarjetas con ícono de caja), "Únete a nuestra comunidad" (Soy donante / Soy beneficiario), pie de 4 columnas | El carrusel esconde contenido y cuesta en móvil; el ícono de caja repetido no muestra la donación real (la foto sí) |
| 2 | Donaciones realizadas | Público | Mapa mundial coloreado por cantidad, galería en mosaico, "Unirse a la comunidad" | El mapa necesita una alternativa en lista o tabla (RF-CL-06) |
| 3 | Nosotros | Público | Contadores, "Quiénes somos" con imagen, "Nuestra solución", "Nuestros valores" | Texto largo: cuidar el largo de línea y la jerarquía |
| 4 | Iniciar sesión | Público | Usuario, contraseña, botón, enlace a registrarse | Los campos usan el texto de ayuda como etiqueta (se pierde al escribir) |
| 5 | Registrarse — elegir | Público | Dos tarjetas: Soy donante / Soy beneficiario, con ícono | — |
| 6 | Notificaciones | Donante | Menú del donante en píldoras, tabla Estado / Detalle / Fecha, filtro | Una tabla de 3 columnas no entra en 360 px |
| 7–8 | (piezas sueltas) | — | Marcos vacíos de trabajo en Figma | No son pantallas |
| 9–10 | Carrusel de emergencias | Público | Variantes del carrusel con flechas y puntos | Ídem pág. 1 |
| 11 | Registro de entidad | Público | Razón social, tipo jurídico, rubro, dirección completa, representantes (se pueden agregar), "Enviar" | Formulario largo: conviene partirlo en pasos o secciones claras |
| 12 | (navegación del boceto) | — | Botones Vista admin / donante / beneficiario | Aclara que no es parte del sitio |
| 13 | Inicio del admin | Admin | Menú: Registrar donantes, Registrar donaciones, Donaciones, Camiones, Donantes; saludo | El admin tiene 9 secciones en total (§6.2): hay que agruparlas |
| 14 | Menú de usuario | Todos | Desplegable: Cambiar de vista, Cerrar sesión, Ajustes | "Cambiar de vista" = cambiar de rol (RF-CL-03) |
| 15 | Inicio del donante | Donante | Saludo, métricas en 3 anillos (meta mensual, ayuda respecto del mes anterior, necesidades satisfechas), notificaciones pendientes ("Nada que mostrar!") | El estado vacío ya está pensado |
| 16 | Mis donaciones | Donante | Tabla Título / Estado / Receptores / Fecha / Detalle, filtro | Ídem pág. 6 en móvil |
| 17 | Beneficiarios | Donante | Tarjetas por donación con entidades beneficiadas | — |
| 18 | Misiones e insignias | Donante | Insignias obtenidas (ilustraciones en color), misiones con progreso en torta | **Única pieza con identidad**: ilustración plana geométrica en verde agua, rojo y lima |
| 19 | Seguir entregas | Donante | Mapa con recorrido punteado y marcadores | Necesita alternativa accesible y "última actualización" (RF-CL-07) |
| 20–22 | Detalle de misión | Donante | Torta con "Pendiente" / "Objetivo" | — |
| 23 | Mapas por región | Público | Mapa mundial → Argentina → provincia; detalle "Donación #6354" con contenidos | — |
| 24 | Inicio de la entidad | Entidad | Saludo, métricas, notificaciones pendientes ("Entregas sin verificar >") | Atajo directo al flujo piloto |
| 25 | Métricas | Entidad | Los 3 anillos sueltos | — |
| 26 | Necesidades | Entidad | Tabla Necesidad / Recurrencia / Motivo / Estado / Detalle, botón "+" | — |
| 27 | Donaciones recibidas | Entidad | Tarjetas por donación con donante y fecha | — |
| 28 | Seguir entregas y confirmar | Entidad | Mapa con recorrido, botones "<Estado>" y "Reportar problema", **"Entregas a confirmar"**: por entrega, fecha, hora, código y contenido, más una zona "Suelte los archivos aquí / Elegir los archivos" ("Adjuntar al menos 1 imagen"); casilla "Disclaimer legal"; botón "Confirmar Entregas" | **Base del flujo piloto.** "<Estado>" y "Disclaimer legal" están sin definir; confirmar varias entregas juntas complica los errores |
| 29–30 | Detalle de necesidad | Entidad | Línea de tiempo por período con colores rojo / amarillo / verde | El color solo no alcanza: falta texto o ícono |
| 31 | Leyenda | Entidad | Qué significa cada color de período | — |

## Patrones que se repiten

- **Encabezado:** logo a la izquierda; a la derecha, campana de notificaciones y menú de usuario.
- **Menú por rol:** fila de píldoras debajo del encabezado; la sección actual va rellena.
- **Pie:** marca + lema, enlaces rápidos, legal y privacidad, contacto.
- **Tarjetas** de donación con título, persona o entidad, fecha y lugar.
- **Tablas** con filas alternadas y un enlace "Detalle".
- **Métricas** en anillos con porcentaje.
- **Mapa** con recorrido punteado y marcadores.

## Pistas para la identidad (sin decidir)

- Las insignias (pág. 18) ya tienen un estilo: ilustración plana, geométrica, con colores
  saturados. Es la pieza con más carácter de los bocetos.
- El recorrido punteado del mapa (págs. 19 y 28) puede volverse un motivo gráfico: el camino de
  una donación.
- Los textos ya usan voseo ("Sumate", "Podés", "Rastreá"). Se confirma en la ronda de voz y tono.
