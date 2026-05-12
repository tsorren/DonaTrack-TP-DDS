# ADR Generator

Generador de ADRs en formato Markdown con modo Crear y modo Editar.

## Requisitos

- Un navegador moderno (Chrome, Edge, Firefox)
- Archivos en una misma carpeta

## Estructura

- adr-generator.html
- style.css
- script.js
- template.md

## Uso rápido

1. Acceder a https://tsorren.github.io/DonaTrack-TP-DDS/adr-generator en el navegador.
2. Completar el formulario en modo Crear.
3. Usar "Descargar ADR (.md)" para generar el archivo.

## Modo crear

- Usar el botón "+" para activar el modo Crear.
- Completar los campos de identificación, contexto, validación y alternativas.
- Agregar drivers, alternativas y links con los botones correspondientes.
- Elegir la alternativa final y completar la justificación.
- Descargar o copiar el Markdown.

## Modo editar

1. Cambiar a modo Editar (botón con lápiz).
2. En la pestaña Escribir, pegar el Markdown del ADR.
3. Cambiar a la pestaña Preview para cargar el formulario.
4. Editar los campos y usar "Copiar a portapapeles" o descargar.

## Salida

- Se genera un archivo con nombre `yyyymmdd-titulo.md`.
- El contenido sigue la estructura de `template.md`.

## Notas

- La "Alternativa elegida" debe coincidir con una alternativa listada.
- Si no coincide, se muestra un aviso y no se crea una alternativa nueva.
