# Bitácora de diseño de la interfaz

> UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5
> **Se actualiza en cada decisión · parte del [proceso de diseño](README.md)**

Cada decisión de diseño queda acá, de la más nueva a la más vieja, con el mismo formato que usa el
entregable para la cátedra. Las comparaciones A/B agregan sus capturas en `evidencia/<tema>/`.

## Formato

```markdown
### AAAA-MM-DD · <tema> · <decisión en una línea>

- **Decisión:** qué se eligió.
- **Alternativas:** qué otras opciones hubo (en un A/B, la variante descartada).
- **Por qué:** el motivo, en palabras simples.
- **Costo:** qué se resigna o qué queda pendiente.
- **Quién decidió:** usuario, diseñadora o equipo.
- **Evidencia:** capturas o enlace (si hay).
```

## Decisiones

### 2026-10-30 · proceso · Flujo piloto: confirmar recepción con fotos

- **Decisión:** el proceso se prueba de punta a punta con el flujo "Confirmar recepción con fotos"
  de la entidad (RF-12, boceto pág. 28).
- **Alternativas:** registrar donación (admin), registro de cuentas e importar CSV.
- **Por qué:** es el que más exige a la interfaz (celular, cámara, carga de archivos, estados,
  avisos). Además es el momento con más valor emocional del sistema, y su decisión técnica (D27)
  ya está tomada.
- **Costo:** la integración real con Logística (subida multipart, HB-10) queda para después. El
  piloto usa datos de ejemplo.
- **Quién decidió:** equipo, al aprobar el plan.

### 2026-10-30 · proceso · Anchos de referencia: 360, 1280 y 1920 px

- **Decisión:** se diseña, se captura y se compara en 360 (celular), 1280 (escritorio) y 1920
  (pantalla grande).
- **Alternativas:** 360, 768 y 1280, como proponía RNF-UI-07 en su forma de verificar.
- **Por qué:** cubre desde el celular hasta las pantallas grandes, que hoy son comunes. La tableta
  sigue cubierta por una comprobación automática de anchos intermedios.
- **Costo:** ninguna captura fija de tableta para revisar a ojo.
- **Quién decidió:** usuario.

### 2026-10-30 · proceso · Los bocetos de E1 son punto de partida, no molde

- **Decisión:** los bocetos de E1 orientan estructura y contenido, y se pueden mejorar. Cada
  cambio importante se anota acá.
- **Alternativas:** implementarlos tal cual (lectura literal de RQ-E6-06).
- **Por qué:** la cátedra no es estricta con eso y los bocetos tienen problemas de uso en celular
  (tablas anchas, carrusel, etiquetas dentro de los campos).
- **Costo:** hay que justificar cada desvío en el entregable.
- **Quién decidió:** usuario.
