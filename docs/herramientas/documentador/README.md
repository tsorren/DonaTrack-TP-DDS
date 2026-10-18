# DonaTrack Document Generator

Generador de documentación (ADRs, Minutas y más) en formato Markdown con modo Crear y modo Editar.

## Características Principales

- **Arquitectura Basada en Plantillas:** Sistema dinámico para múltiples tipos de documentos.
- **Modos de Operación:** Permite crear nuevos documentos o cargar y editar existentes pegando su código Markdown.
- **Interfaz Reactiva:** Renderizado dinámico de formularios según la plantilla seleccionada.
- **Almacenamiento Local:** Auto-guardado de borradores en el navegador.

## Requisitos

- Un navegador moderno (Chrome, Edge, Firefox, Safari)
- No requiere servidor (funciona abriendo `index.html` localmente) aunque se recomienda usar Live Server o GitHub Pages.

## Estructura de Archivos

- `index.html`: Punto de entrada principal.
- `style.css`: Estilos visuales compartidos.
- `script.js`: Orquestador principal de inicialización y eventos.
- `templates.json`: Registro de plantillas disponibles.
- `core/`: Motor de la aplicación.
  - `generator.js`: Orquestador de modos y operaciones.
  - `template-registry.js`: Carga dinámica de plantillas.
  - `ui-engine.js`: Renderizado de formularios y recolección de datos.
  - `utils.js`: Funciones auxiliares.
- `templates/`: Plantillas específicas.
  - `adr.js`: Lógica para generar y parsear ADRs.
  - `minuta.js`: Lógica para generar y parsear Minutas de Reunión.

## Uso

1. **Acceso:** Acceder al generador alojado en GitHub Pages o abrir `index.html` localmente.
2. **Selección de Plantilla:** Elegir el tipo de documento a generar en el selector superior (ej: ADR, Minuta).

### Modo Crear

- Seleccionar el botón "+" en la barra de herramientas.
- Completar los campos requeridos y opcionales.
- Para secciones dinámicas (ej: alternativas, participantes), usar los botones para agregar ítems.
- Usar "Descargar (.md)" para obtener el archivo o "Copiar a portapapeles".

### Modo Editar

1. Cambiar al modo Editar usando el botón con el ícono de lápiz.
2. En la pestaña **Escribir**, pegar el código Markdown completo de un documento existente.
3. Cambiar a la pestaña **Preview**. El formulario se autocompletará con los datos parseados.
4. Realizar las modificaciones necesarias en el formulario.
5. Descargar o copiar el resultado actualizado.

## Notas

- Los archivos generados incluyen una cabecera de metadatos compatible con sistemas como Log4Brains.
- Las convenciones de parseo requieren que los documentos mantengan la estructura básica generada por la herramienta para poder ser editados nuevamente de forma fiable.

