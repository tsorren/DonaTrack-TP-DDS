# Entregable 5 — Documento de arquitectura (Entrega 4)

Informe que se entrega a la cátedra: `arquitectura-sistema-e4.docx` (y su exportación `arquitectura-sistema-e4.pdf`).
El respaldo técnico verificado contra el código, con nombres de exchanges, colas y rutas, es [`../arquitectura-sistema.md`](../arquitectura-sistema.md).

| Archivo | Qué es |
|---|---|
| `arquitectura-sistema-e4.docx` | Entregable, generado. No editar a mano: se pisa al regenerar. |
| `arquitectura-sistema-e4.pdf` | Exportación de Word del `.docx`, con el índice actualizado. |
| `contenido.md` | Fuente del texto. Es lo que se edita. |
| `figuras/*.puml` · `*.png` | Las 5 figuras del cuerpo (mapa, capas, broker de integración, datos, despliegue). |
| `construir_docx.py` | Arma el `.docx`: portada, índice, pie, tablas, recuadros de decisión. Requiere python-docx. |

## Reglas de redacción del `contenido.md`

- Sin identificadores de implementación: ni clases, ni tablas, ni colas, ni endpoints, ni rutas. Tecnologías y patrones sí.
- Sin códigos internos (IDs de deuda, de requisitos o de decisión) ni rótulos de estado: todo se dice en palabras.
- Cada tabla y cada figura se citan en el texto antes de aparecer. La numeración es literal.
- Recuadro de decisión: bloque `:::decision | Título` con las líneas `Decisión:`, `Alternativas:`, `Por qué:`, `Costo:` y `Referencia:`.
- Anchos de tabla: línea `{anchos: 4.0, 11.0}` (en cm, suman 15) después del epígrafe `Tabla N. ...`.

## Regenerar

```bash
python construir_docx.py contenido.md arquitectura-sistema-e4.docx
java -jar plantuml.jar -tpng figuras/<figura>.puml
```

Para el PDF y el índice con números de página, abrir el `.docx` en Word, actualizar los campos y exportar a PDF.
Las figuras usan `../diagramas/estilo-e4.iuml` y no superan 730 px de ancho, para que la letra impresa quede en 7 pt o más.

## Pendiente

- El cuerpo ocupa 23 páginas: las Figuras 1 y 3 son altas y dejan blancos.
