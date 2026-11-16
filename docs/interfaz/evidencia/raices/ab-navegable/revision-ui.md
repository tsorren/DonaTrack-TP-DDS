# Revisión `revision-ui` de la corrida A/B de Raíces

> UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5
> **2026-11-02 · registro de veredictos** de la [corrida A/B](../../../encargos/raices-ab-navegable.md)
> (encargo, sección 9, y plan de la corrida: "`revision-ui` PASS en A y en B" y fichas "con
> `revision-ui` en PASS"). Lo pidió la auditoría de cierre, ronda 1, porque el veredicto no estaba
> registrado en ningún lado.

Cada revisión corrió como `INDEPENDENT_REVIEW`, en un contexto aparte (skill `revision-ui`, ejecución
en fork), en modo SOURCE_READ_ONLY, sobre el árbol de trabajo de `E6_raices` antes del commit
"E6: Raíces A/B, arreglos de la auditoría (ronda 1)". El revisor recibió solo los artefactos: fichas,
plantillas, hojas, controller, test, capturas y reporte de axe.

## Veredictos finales

| Qué | Modo | Rondas | Veredicto final |
|---|---|---|---|
| Sitio A (`/prototipo/raices/`) | pantalla (U1 a U9) | 1 | **PASS** |
| Sitio B (`/prototipo/raices-b/`) | pantalla (U1 a U9) | 1 | **PASS** |
| [Ficha del donante](../../../flujos/raices-donante-navegable.md) | flujo (U1, U5, U6, U8, U9) | 1 | **PASS** |
| [Ficha de la entidad y el admin](../../../flujos/raices-entidad-navegable.md) | flujo | 2 | **PASS** |
| [Ficha pública](../../../flujos/raices-publico-navegable.md) | flujo | 4 | **PASS** |

## Hallazgos bloqueantes y cómo se arreglaron

| Ronda | Ficha | Hallazgo | Arreglo |
|---|---|---|---|
| 1 | Pública | Recuperar la contraseña (`publico-recuperar` y `-enviado`) estaba dibujada en A y B sin esquema, estados ni textos en la ficha | Sección 4.8, sus filas en Estados y en Textos, nivel 3 en la sección 2 y el diagrama |
| 1 | Entidad | "Biblioteca Popular Ejemplo" figuraba entre las entidades que piden sumarse y, a la vez, como activa con pedidos y entregas | Pasa a "Merendero Ejemplo Este" en la ficha y en `admin-inicio` de A y de B |
| 1 | Entidad | El admin decía "42 entregas confirmadas en octubre" y la ficha pública, con los mismos datos, 9 | El admin dice 9, en la ficha y en `admin-inicio` de A y de B |
| 2 | Pública | El aviso de Recuperar decía "Te mandamos un enlace." aunque no hubiera cuenta (da por hecho un envío) | "Si hay una cuenta con ese correo, te mandamos un enlace.", en la ficha y en A y B |
| 3 | Pública | El aviso prometía "el enlace vence en una hora", un plazo que nadie fijó (RF-AUTH-13 sugiere 30 min; D8 pendiente) | "el enlace sirve una sola vez y por tiempo limitado"; pregunta abierta 4 sobre el plazo |
| 3 | Pública | Recuperar no tenía su línea de huecos de backend | Línea nueva en la ficha y en [la lista de la corrida](../../../flujos/raices-ab-huecos-backend.md) |

Un test nuevo cubre el aviso de Recuperar en los dos sitios
(`PrototipoControllerTest.enLosDosSitiosDeRaicesRecuperarLaContrasenaAvisaSinRevelarCuentas`).

## Avisos que quedan (no bloquean)

- A: la barra fija de abajo (celular) no tiene `scroll-padding-bottom`; con Tab, un campo de un
  formulario largo puede quedar debajo (WCAG 2.2, 2.4.11). Sin verificar con teclado.
- A: "sin dibujar" ofrece solo "Verla en Cozy"; B pone primero "Volver al inicio".
- B: en 360 px la cuarta estación de la línea baja a un ramal; el nombre corto "Entregas" de la
  entidad choca con el de la línea pública. `entidad-notificaciones` llama `al-dia` al estado que A
  llama `nada-sin-leer`. `publico-donaciones` no tiene `?escenario=sin-foto`.
- Fichas: ubicaciones de algunas entidades distintas entre la ficha del donante y la pública; el
  hogar "de día" que pide frazadas "para las camas"; la palabra "juego" en el inicio del donante
  (el encargo fija juego "nada"); las unidades de la leche entre `viaje-datos` y la ficha pública;
  "Filtro sin resultados" de la entidad, que no se alcanza en la maqueta; el error del servidor de
  Recuperar, descrito y no dibujado.
- Al elegir A, revisar si el acceso a Notificaciones y la navegación pública pasan también a las
  pantallas aprobadas: hoy faltan porque A no las toca (encargo, sección 1).
