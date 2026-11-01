# Criterios de UX/UI de la cátedra

> **Fuente:** material oficial de la cátedra, "Introducción al diseño UX/UI 2026" (presentación
> de 255 diapositivas, entregada por el usuario el 2026-11-01). Este documento la resume con
> palabras propias y la convierte en criterios para quien diseña o revisa la interfaz del cliente
> liviano, personas o agentes. El número entre paréntesis es la diapositiva del PDF.
> **Prioridad:** como material de cátedra, estos criterios están por encima de las preferencias de
> estilo. Si una decisión de diseño se aparta de alguno, se dice por qué en la bitácora.

## Cómo se usa

| Momento | Qué criterios mirar | Skill |
|---|---|---|
| Diseñar un flujo | Factores de la experiencia, modelos mentales, arquitectura de la información, wireframes y wireflows | `ux-flujo` |
| Comparar dos variantes | Patrones de navegación e interacción (las dos variantes usan patrones conocidos), lenguaje visual | `iteracion-ab` |
| Construir una vista | Patrones de interacción, tipografía, color, accesibilidad | `vista-ui` |
| Revisar | Las 10 heurísticas, con su "cómo se consigue", y los 7 factores | `revision-ui` |

## 1. UX y UI

- La **UI** es todo lo que se ve y se toca; la **UX**, lo que la persona vive antes, durante y
  después de usar el producto: usabilidad, accesibilidad e interacción (6, 8). No se diseña una sin
  la otra (9).

## 2. Los 7 factores de la experiencia (Peter Morville)

Cada elemento de una pantalla tiene que tener una razón de ser: a veces menos es más (12–19).

| Factor | Pregunta para cada pantalla |
|---|---|
| Útil | ¿Cada elemento sirve a la tarea, o solo llena espacio? |
| Usable | ¿Cualquier persona la usa sin ayuda? |
| Deseable | ¿La imagen y la identidad generan la emoción buscada? |
| Valioso | ¿Le aporta algo a la persona y al objetivo del producto? |
| Ubicable | ¿Se encuentra lo que se busca con la navegación? |
| Accesible | ¿La usa el mayor número de personas, con sus limitaciones y su contexto? |
| Creíble | ¿Lo que dice se puede creer? Perder la confianza es perder a la persona |

Para DonaTrack, **creíble** pesa doble: el producto vende transparencia. Un dato sin respaldo o una
promesa que no se cumple cuesta más que un detalle feo.

## 3. Modelos mentales y patrones

- Un **modelo mental** es la imagen que la persona ya tiene de cómo funciona algo; de ahí salen sus
  expectativas (21–26). Se diseña a favor de ese modelo, no en contra.
- Las personas no leen: escanean buscando palabras clave, y leen en detalle solo después (32–33).
  En pantallas de texto, el recorrido de la mirada sigue una **F**: más atención arriba y a la
  izquierda (34).
- Pasan más tiempo en otros sitios que en el nuestro (36): por eso se usan **patrones conocidos**,
  soluciones ya probadas que bajan la curva de aprendizaje y dan sensación de producto intuitivo
  (29–30). La identidad propia se expresa en la forma, no reinventando la navegación.

## 4. Patrones de navegación

| Patrón | Cuándo se usa |
|---|---|
| Barra de navegación inferior (*bottom navigation*) | En el celular, para **3 a 5 destinos de primer nivel**, accesibles desde cualquier pantalla (43–44, 49–50) |
| Barra de pestañas (*tab bar*) | Pantallas del mismo nivel; como máximo 5 a 7 pestañas (41–42) |
| Pestañas | Destacar la elegida, mantener el orden y la ubicación, y mostrar siempre dónde se está y adónde más se puede ir (45–46) |
| Cajón (*drawer*) | Muchos destinos de primer nivel, o destinos que no se relacionan entre sí (39–40) |
| Barra superior (*top app bar*) | La marca, el título de la pantalla, la navegación y las acciones de esa pantalla (47) |
| Migas de pan | Mostrar el camino desde el inicio y volver a los niveles de arriba (57) |
| Barra de progreso | Procesos con pasos: registros, compras (58) |
| Volver | El "atrás" del navegador tiene que funcionar siempre (51–56) |
| Paginado o desplazamiento sin fin | Paginado para recorrer mucho contenido; sin fin, para contenido corto o social (59) |
| Carrusel | Contenido visual, con control para ir y volver (60) |
| Acordeón | Secciones con mucho texto, como preguntas frecuentes (61) |

## 5. Patrones de interacción

- En el celular, la forma de sostener el teléfono y el dedo con que se toca deciden dónde van los
  controles (63): lo frecuente, al alcance del pulgar.
- **Listas** para contenido estructurado vertical, donde tocar un ítem lleva al detalle (65–66).
- **Diálogos que interrumpen**, solo para lo que no puede esperar (69). **Avisos breves** que no
  interrumpen ni piden nada, para contar cómo salió algo (70, 73).
- **Tarjetas**: un solo tema, con jerarquía clara entre texto, imagen y acciones (75).
- **Campos de texto**: que se note que se puede escribir, qué se pide y cómo corregir (74). Elegir de
  una lista antes que escribir, cuando se pueda (72).
- **Botón de acción flotante**: solo para la acción más importante de la pantalla (76).

## 6. Arquitectura de la información

Organizar, estructurar y etiquetar para que la persona sepa dónde está, qué encontró, qué puede
esperar y qué hay alrededor (79–81). El **75 %** de los problemas de usabilidad vienen de una mala
arquitectura de la información (82). Sus 8 principios (84–106):

1. **Objetos:** el contenido se organiza según su propósito y según cómo cambia con el tiempo.
2. **Elecciones:** pocas opciones a la vez, para no cargar la memoria.
3. **Divulgación progresiva:** primero lo que hay que saber; el detalle, a pedido.
4. **Ejemplos:** mostrar qué hay detrás de un enlace, además de su título.
5. **Puertas delanteras:** se puede llegar a una página profunda sin pasar por el inicio; cada
   página dice dónde está y cómo seguir.
6. **Clasificación múltiple:** más de un camino para encontrar lo mismo (categorías, etiquetas).
7. **Navegación enfocada:** el menú principal lleva solo los destinos principales.
8. **Crecimiento:** la estructura deja lugar para lo que venga.

## 7. Wireframes, wireflows y sistemas de diseño

- **Wireframe:** boceto simple de la estructura de una pantalla: menús, bloques de contenido y cómo
  se relacionan (110). En DonaTrack, el esquema de cada pantalla en su ficha de flujo.
- **Wireflow:** wireframes unidos con flechas que muestran las interacciones (111). En la ficha de
  flujo, el recorrido con las pantallas que toca.
- **Sistema de diseño:** normas, componentes y patrones reutilizables para diseñar a escala (118).
  **Atomic Design** lo ordena en átomos (botón, ícono), moléculas (campo con su etiqueta y su
  botón), organismos (encabezado), plantillas y páginas con contenido real (120–133). En DonaTrack:
  tokens y fragmentos son átomos y moléculas; `layout` y `layout-privado`, organismos y plantillas;
  el catálogo `/estilos` muestra el sistema.

## 8. Lenguaje visual

- **Tipografía:** el 95 % de lo que se comunica es texto (145). La letra transmite un estado de
  ánimo (144). Se juega con tres variables: dirección (normal o cursiva), peso y proporción (de
  condensada a expandida) (148).
- **Largo de línea:** entre **50 y 75 caracteres**, unas 10 palabras; ni todo el ancho de la
  página ni columnas muy angostas con cortes seguidos (149–151).
- **Alineación:** a la izquierda; nunca justificado en la web (152).
- **Jerarquía y énfasis:** tamaño, peso, mayúsculas y color ordenan qué se lee primero (153–156).
- **Color:** pocos colores de marca, usados con consistencia (157–158); colores de respuesta para
  advertencia, éxito y error, pensando en accesibilidad (162). Regla **60-30-10**: 60 % neutro,
  30 % complementario y 10 % de acento (163–167).

## 9. Accesibilidad

- Productos que use el mayor número de personas, con limitaciones visuales, auditivas, motrices o
  neurológicas, o del entorno (170).
- Principios **POUR**: perceptible, operable, comprensible y robusto (171). Pautas **WCAG** del
  W3C (172). En Argentina, la **Ley 26.653 de Accesibilidad Web** (172–173).
- Herramientas: verificador de contraste, simulador de daltonismo (con protanopia, un naranja y un
  verde pueden verse iguales) y paletas generadas según WCAG (176–180). El estado nunca va solo con
  color.

## 10. Evaluación del producto

- Es la fase más importante; se hace al principio, durante y al final (182).
- **Sin usuarios:** la evaluación heurística, la más barata. **Con usuarios:** el test con
  usuarios, el mejor; también entrevistas, encuestas, test A/B y mapas de calor (183).
- La evaluación heurística la hacen **varios evaluadores** contra las heurísticas (185).

### Las 10 heurísticas de Nielsen y cómo se consiguen (186–252)

| # | Heurística | Cómo se consigue |
|---|---|---|
| 1 | Visibilidad del estado del sistema | Toda acción tiene respuesta perceptible y rápida; nada con consecuencias sin avisar antes |
| 2 | Relación entre el sistema y el mundo real | Palabras y metáforas de la persona; formas que ya conoce de su cultura; nada que obligue a buscar una definición |
| 3 | Control y libertad | Cancelar lo que está en curso, deshacer, confirmar lo drástico; saber siempre dónde se está y adónde se puede ir |
| 4 | Consistencia y estándares | Las convenciones de la plataforma; cada pantalla empieza con un título que la describe; las instrucciones y los componentes, en el mismo lugar y con el mismo comportamiento |
| 5 | Prevención de errores | Quitar lo que lleva a error o confirmarlo; buenos valores por defecto; primero los errores costosos |
| 6 | Reconocer antes que recordar | Mostrar opciones, historial, migas de pan y sugerencias, en lugar de pedir que se recuerden |
| 7 | Flexibilidad y eficiencia | Atajos para quien ya sabe, sin estorbar a quien empieza |
| 8 | Diseño estético y minimalista | Solo lo esencial para decidir; cada dato de más compite con los que importan |
| 9 | Reconocer, diagnosticar y recuperarse de errores | Lenguaje claro, el problema exacto, la causa y qué hacer; el sistema se hace cargo del error |
| 10 | Ayuda y documentación | Fácil de encontrar, centrada en la tarea, con los pasos y corta |

## 11. Qué cambia en cómo trabajan los agentes

1. **Patrón conocido primero.** Antes de proponer una navegación, un menú o un control, se nombra el
   patrón de la sección 4 o 5 que sigue. Una variante A/B no compara un patrón conocido contra uno
   inventado: compara dos patrones conocidos, o dos formas de dibujar el mismo.
2. **Los 7 factores en cada ficha.** La revisión de usabilidad de una ficha de flujo suma una línea
   por factor de Morville, con atención especial a **creíble**.
3. **Wireflow en la ficha.** El recorrido de una ficha muestra las pantallas que toca, no solo los
   pasos, y cada pantalla tiene su esquema (wireframe).
4. **Largo de línea medido.** Todo bloque de texto corrido queda entre 50 y 75 caracteres por línea
   en pantalla grande; textos alineados a la izquierda.
5. **60-30-10 a la vista.** Al mostrar una variante, se dice cómo reparte el neutro, el
   complementario y el acento.
6. **Accesibilidad con nombre.** Se citan POUR, WCAG y la Ley 26.653; el estado de algo nunca va
   solo con color.
7. **Evaluación en capas.** La revisión heurística (`revision-ui`) no reemplaza al test con
   usuarios: cuando un flujo queda listo, se anota qué conviene probar con personas reales.
