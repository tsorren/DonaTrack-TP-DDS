# Encargo creativo: Raíces cálida

> Plantilla de la skill `relevamiento-creativo`; la lee la skill `corrida-creativa`. Es
> **autocontenida**: la corrida no le pregunta nada al usuario, así que lo que no está acá lo decide
> la corrida con la [dirección de arte](../direccion-de-arte.md) y queda anotado como "a criterio de
> la corrida".
>
> **Estado:** aprobado por el usuario el 2026-11-06

## 1. Qué se diseña

- **Propuesta:** `raices-calida`, en la rama `E6_raices_calida` (la rama es la propuesta; se decide
  con el merge). No es una identidad nueva: ajusta Raíces y actualiza su
  [brief](../identidad/brief.md) en esa rama.
- **Punto de partida:** rediseño de lo que hay, en **dos etapas, en orden**:
  1. **Etapa 1, sobria (con el usuario, antes de la corrida).** Un A/B de color y acabado con la
     skill `iteracion-ab` en modo exploración (tema `color-y-acabado`), en la portada y en el
     inicio del donante. Entran tres cosas:
     - la tierra, más tibia y más clara (marrón humus o verde tibio, con lo oscuro solo en lo más
       hondo). **Una opción conserva la tierra casi negra de hoy como control**: al usuario le
       gusta;
     - esquinas suaves en el papel y en los botones;
     - una serif cálida en los títulos, de libro de jardinería, de trazo firme y sin floreo, que
       no se lea "demasiado femenina"; Archivo sigue en el texto, los datos y las cifras.
       Candidatas con licencia OFL, a criterio del A/B: Fraunces (sin el eje blando), Young Serif o
       Literata.

     Lo que el usuario elige se aplica a todo el sitio (`tokens.css` y las hojas que hagan falta),
     reemplaza a Huerta de noche y se anota en la bitácora y en el brief. La etapa 1 no la corre el
     workflow: la conduce la sesión, una decisión por vez.
  2. **Etapa 2, marcada pero reconocible (la corrida desatendida),** sobre lo elegido en la
     etapa 1. Arranca solo cuando la etapa 1 está aplicada y commiteada en la rama.
- **Pantalla ancla:** `/donante-inicio`, el inicio de la persona que dona.
- **Hasta dónde llega:** dos vueltas: concepto (el inicio del donante) y sistema (lo común del
  sitio: tokens, layouts, piezas compartidas y la firma de las abejas).
- **Fuera de alcance:** la composición de las demás pantallas (donante, entidad, admin y
  públicas, también la portada). Pueden cambiar solo por lo que heredan del sistema; la corrida
  comprueba que no se rompan. La navegación (menú y cabecera, aprobados por el grupo) queda fija.
  También quedan afuera los servicios backend, ADR y DTI nuevos, y la huerta que cambia con los
  meses o las estaciones (el usuario la sacó).
- **Lo que NO hay que repetir:** `/donante-inicio` y `/` con Huerta de noche (capturas de
  `cliente-liviano/target/capturas/donante-inicio-1280.png` y `publico-portada-1280.png`, del
  2026-11-06). La tierra casi negra ocupa la mitad de la portada y el bloque de cifras del inicio;
  con las esquinas rectas, las sombras de tinta y una sola grotesca de afiche, Raíces perdió la
  calidez: sirve para la empresa, no le resulta atractiva a la persona que dona sola.

## 2. En palabras del usuario

> Qué mejoras le podemos aplicar al diseño de raices sin que sea tan serio? la alternativa actual
> perdió demasiada calidez y, si bien nos gusta para empresas, no resulta atractivo para los
> donantes individuales.
>
> Pensemos algo inspirandonos en estas respuestas, pero sin ser gimmicky (comienzan desde la 4)
>
> Huerta con abejas · respuestas
>
> 4. ¿Qué tan saturados querés los colores? 4
> 5. En los primeros cinco segundos, ¿qué tiene que sentir alguien que entra por primera vez?
>    Seriedad: es un sistema prolijo, institucional. Nota: Confianza: 30, Seriedad: 30, Calidez:
>    25, Ganas de Sumarme: 15
> 6. ¿Quién tiene que verse reflejado primero? Una persona que dona / Una entidad que recibe
>    (comedor, escuela, hogar)
> 7. ¿Qué no tiene que transmitir nunca? Juego infantil / Burocracia fría
> 8. ¿Qué es la huerta en esta historia? Cada donante tiene su propia huerta personal
> 9. ¿Qué hacen las abejas? Son los donantes, que polinizan sin ver todo el recorrido
> 10. ¿Cómo aparecen las abejas chicas? Un enjambre chiquito, disperso por la página
> 11. ¿Cómo se ve una donación que llegó? Un fruto maduro en la planta
> 12. ¿Cómo se ve algo que salió mal (vencida, entrega fallida)? Sin metáfora: un aviso claro y
>     nada más
> 13. Las palabras de la metáfora (semilla, cantero, fruto), ¿dónde van? Solo en lo visual: el
>     texto dice «donación», «entrega»
> 14. ¿Cuánta ilustración querés? 4
> 15. ¿Hacia qué edad apunta el tono visual? 4
> 16. ¿Cuánto se mueve la página? 3
> 17. ¿Qué letra te imaginás para los títulos? La de ahora: Archivo ancha y pesada, de afiche.
>     Nota: Podría ser una serif cálida, de libro de jardinería si se mantiene el profesionalismo y
>     no es demasiado femenina
> 18. ¿La huerta cambia con el tiempo para mostrar progreso? Sí, con estaciones o meses en toda la
>     página
>
> (Relevamiento de 4 rondas, 2026-11-06. Respuestas: la emoción, "la encuesta tal cual"; el
> alcance, "todo el sitio"; la distancia, "primero sobrio, con un detalle, luego marcado,
> reconocible"; etapa 1, "probar tierra tibia y más clara, aunque nos gusta el negro actual",
> esquinas suaves y serif cálida; etapa 2, la huerta de cada donante, las abejas y más ilustración
> de personas, sin las estaciones; abejas "pocas, como firma".)

## 3. Mensaje, público y emoción

- **Qué tiene que transmitir:** lo que donás llega y se ve; detrás hay personas, no un trámite.
- **Para quién:** primero la persona que dona sola, desde el celular o la computadora, cuando
  entra a ver qué pasó con lo suyo; también se tiene que ver reflejada la entidad que recibe. La
  empresa usa las mismas pantallas y tiene que seguir encontrando el orden y el detalle.
- **Qué tiene que sentir quien la ve:** la mezcla de la encuesta: confianza 30, seriedad 30,
  calidez 25 y ganas de sumarse 15. Hoy sobra seriedad y falta calidez.
- **Intensidad:** marcada pero reconocible: siguen el corte de suelo, las raíces y los frutos.

## 4. Referencias

Sin imágenes nuevas. La referencia es la encuesta "Huerta con abejas" (sección 2), de una
integrante del equipo. Siguen vigentes las de
[`identidad/referencias/`](../identidad/referencias/README.md), que fundaron Raíces (textura de
impresión, la red que es mapa, la lámina de campo y el corte de suelo).

| Referencia | Qué se toma | Qué no |
|---|---|---|
| Encuesta, 8 y 11 | La huerta de cada donante en su inicio: una planta por donación y el fruto maduro cuando la entidad confirma | Las palabras de la metáfora en el texto (13) |
| Encuesta, 9 y 10 | Las abejas chicas como firma de la casa | Un enjambre que se mueve sin parar o que tapa datos |
| Encuesta, 6 y 14 | Más ilustración (4 de 5), con personas: quien dona y quien recibe | Dibujo de cuento ante una empresa |
| Encuesta, 7, 12 y 15 | Un registro adulto (4 de 5): ni juego infantil ni burocracia fría; lo que sale mal, un aviso claro sin metáfora | — |
| Encuesta, 16 | Movimiento medio (3 de 5) | Movimiento sin fin sin pausa |

**La guía principal:** la encuesta, con lo que el usuario eligió en este relevamiento.

## 5. Lo que no queremos

Además de "Lo que evitamos" de la dirección de arte:

- gimmick: la metáfora en todo, abejas y plantas en botones, datos o errores;
- juego infantil (encuesta, 7): dibujos de cuento, colores de juguete, caritas;
- burocracia fría (encuesta, 7): lo que hoy sobra en Raíces;
- las palabras de la metáfora en el texto ("semilla", "cantero", "fruto", "huerta", "cosecha"): el
  texto dice "donación" y "entrega";
- la metáfora en lo que sale mal: vencida o entrega fallida es un aviso claro, nada más;
- que la empresa sienta que entró a un sitio para chicos.

## 6. Libertades y límites

| Qué | Libre | Con límite (cuál) | Fijo |
|---|:-:|---|:-:|
| Composición de cada pantalla | | Solo `/donante-inicio`; las demás, solo por herencia del sistema | |
| Navegación | | | ✔ |
| Materiales, dibujos propios y movimiento | | Abejas pocas y como firma (cabecera, portada, estados vacíos; como mucho unas pocas por pantalla), sin dato detrás y nunca en botones, datos ni errores. Movimiento 3 de 5, con pausa y "reducir movimiento" | |
| Voz en títulos y rótulos | ✔ | | |
| Tipografía y paleta del sitio | | Las de la etapa 1; la corrida no las cambia | |
| JavaScript propio como mejora | | Solo para movimiento, sobre lo que ya funciona sin él | |

Fijo siempre, aunque la tabla diga otra cosa: los datos y las acciones, el piso, el stack, las
pantallas fuera de alcance ([Libertad creativa](../direccion-de-arte.md#libertad-creativa)) y el
[tono común](../tono-y-comunicacion.md).

## 7. Voz

La voz propia vive en títulos y rótulos. Estados, cifras, acciones y errores siguen el
[tono común](../tono-y-comunicacion.md) y no se adornan.

- **Tono:** cercana y concreta: habla de la persona y de quien recibe, con hechos.
- **Cómo dice algunos títulos y rótulos** (muestras que eligió el usuario; las demás, a criterio de
  la corrida):

  | Hoy dice | La propuesta dice |
  |---|---|
  | Lo que está confirmado | Lo que ya recibieron |
  | En curso | Lo que está viajando |
  | Conocer las entidades | Conocé a quién le llegó |

- **Palabras que no van:** las de la metáfora ("semilla", "cantero", "fruto", "huerta",
  "cosecha", "polinizar") y las del brief ("eco", "huella verde", "florecer", "regá", "cosechá" y
  "sostenible" puesto de adorno).

## 8. Juego

- **Cuánto:** se insinúa.
- **Con qué piezas:** la huerta de cada donante crece con cada entrega confirmada y marca hitos de
  lo acumulado, sin medallas ni puntos. Celebra hechos confirmados y no presiona
  ([tono común](../tono-y-comunicacion.md#juego-y-celebración)).

## 9. La corrida

| Qué | Valor |
|---|---|
| Iteraciones | 2 (concepto y sistema) |
| Conceptos en la iteración 1 | 3, a criterio de la corrida cómo divergen (por ejemplo: la huerta como lámina grande, como fila de plantas por donación, o integrada a las cifras) |
| Rama | `E6_raices_calida`, desde `E6_raices`; la etapa 1 y la corrida van en la misma rama |
| Al terminar | PR en borrador hacia `E6_raices` |
| Tamaño | Etapa 1: 3 temas en un A/B (tierra, esquinas y letra) sobre 2 pantallas, más la aplicación en `tokens.css`. Etapa 2: 1 pantalla ancla con 3 conceptos y el sistema del sitio |
| Presupuesto | Etapa 2: unos 26 agentes (1 de preparación, 10 por iteración y 5 entre el cierre, la auditoría y el PR), unos 3,5 millones de tokens de los subagentes y de 2 a 2,5 horas [INFERRED, con la corrida de prueba: 15 agentes, 2 millones y 74 minutos]. Si se acaba el uso, se retoma desde la última nota de traspaso |
| Modelo de la sesión | Opus (los roles de la corrida declaran el suyo en la skill) |

## 10. Aprobación

- **Fecha:** 2026-11-06.
- **Lo que dijo el usuario:** "Sí, arrancá", a la pregunta "¿Apruebo el encargo «Raíces cálida» y
  arranco por la etapa 1 (el A/B de tierra, esquinas y serif, con vos)?".
