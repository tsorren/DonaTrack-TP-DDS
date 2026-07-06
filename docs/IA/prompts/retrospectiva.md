# Prompt — Retrospectiva de Flujo y Uso de IA

## Cuándo usar este prompt

Usá este prompt después de cerrar una etapa de trabajo del equipo, como:

- el Caso Piloto de Logística;
- una entrega importante;
- una semana de trabajo con el nuevo flujo operativo;
- una serie de PRs de diseño o implementación;
- una retrospectiva específica sobre el uso de IA.

El objetivo es ordenar el feedback del equipo, detectar señales de éxito o alerta, encontrar fricciones reales y proponer mejoras accionables.

Este prompt no debe usarse para evaluar personas individualmente ni para buscar culpables.

---

## Prompt

Actuá como facilitador técnico de retrospectiva para el proyecto DonaTrack.

Necesito analizar el feedback del equipo después de una etapa de trabajo y convertirlo en aprendizajes concretos, mejoras del proceso y acciones para el próximo ciclo.

## Contexto del proyecto

DonaTrack es un proyecto académico desarrollado en equipo. El equipo está trabajando con un modelo operativo dividido en tres grupos:

- Equipo de Diseño y Abstracción.
- Equipo de Ejecución Guiada.
- Equipo de Soporte e Implementación.

El flujo de trabajo separa diseño técnico e implementación, usando issues, sub-issues, Pull Requests de diseño, Pull Requests de implementación, PlantUML, tests y reviews.

También se permite el uso de IA como herramienta de apoyo, pero siempre bajo responsabilidad humana. La IA puede ayudar a analizar issues, diseñar pruebas, revisar PRs, destrabar errores y mejorar documentación, pero no reemplaza el criterio técnico del owner ni del reviewer.

## Objetivo de la retrospectiva

Quiero detectar:

1. Qué funcionó bien.
2. Qué generó fricción.
3. Qué bloqueos aparecieron.
4. Qué parte del flujo ayudó realmente.
5. Qué parte del flujo fue innecesariamente pesada.
6. Cómo se usó la IA.
7. Qué deberíamos ajustar para el próximo ciclo.

## Material disponible

Etapa o caso analizado:
[PEGAR NOMBRE DE LA ETAPA, POR EJEMPLO: Caso Piloto Logística]

Duración:
[PEGAR FECHAS O PERÍODO]

Objetivo de la etapa:
[PEGAR OBJETIVO]

Issues trabajadas:
[PEGAR LISTA DE ISSUES O RESUMEN]

PRs de diseño:
[PEGAR LINKS O RESUMEN]

PRs de implementación:
[PEGAR LINKS O RESUMEN]

Feedback del equipo:
[PEGAR RESPUESTAS DE LA RETROSPECTIVA]

Problemas detectados:
[PEGAR SI YA HAY PROBLEMAS IDENTIFICADOS]

Uso de IA durante la etapa:
[PEGAR CÓMO SE USÓ IA, SI SE SABE]

## Preguntas base de retrospectiva

Analizá el feedback tomando como guía estas preguntas:

1. ¿La descripción y el alcance de las issues fueron lo suficientemente claros antes de empezar a escribir código?
2. ¿Qué tan útil resultó el paso de diseño previo antes de pasar a implementación?
3. ¿Cómo funcionó la comunicación y el acompañamiento cuando alguien se bloqueó?
4. ¿La división de tareas por grupos y prioridades equilibró bien la carga del equipo?
5. ¿Qué partes del flujo aportaron más valor y cuáles agregaron fricción innecesaria?

## Reglas

- No busques culpables individuales.
- No hagas juicios personales sobre integrantes del equipo.
- Analizá el proceso, no a las personas.
- Separá hechos de interpretaciones.
- Marcá patrones repetidos.
- No propongas cambios enormes si no están justificados.
- Priorizá mejoras simples, aplicables y de bajo costo.
- Si el feedback es insuficiente, indicá qué datos faltan.
- Si hay señales contradictorias, explicitalo.
- Si la IA fue usada de forma riesgosa, proponé controles concretos.
- Si la IA fue útil, identificá en qué casos conviene repetir su uso.

## Analizá especialmente

1. Claridad de las issues.
2. Calidad del diseño previo.
3. Tamaño y revisabilidad de las PRs.
4. Funcionamiento del Project Board.
5. Uso de PlantUML.
6. Calidad de los tests.
7. Velocidad de reviews.
8. Bloqueos técnicos.
9. Comunicación pública en issues o canales del equipo.
10. Uso correcto o incorrecto de IA.
11. Distribución de carga entre los tres equipos.
12. Señales de éxito o alerta para el próximo ciclo.

## Formato de respuesta esperado

Respondé con esta estructura:

### 1. Resumen ejecutivo

Explicá en pocas líneas cómo funcionó la etapa analizada.

Indicá si el resultado general parece:

- positivo;
- mixto;
- problemático;
- inconcluso por falta de datos.

### 2. Señales de éxito detectadas

Listá los aspectos que muestran que el nuevo flujo ayudó.

Ejemplos:

- menos bloqueos largos;
- PRs más chicas;
- mejor diseño previo;
- mejor distribución de tareas;
- más claridad antes de implementar;
- mejor uso de tests;
- mejor comunicación en GitHub;
- menor dependencia del grupo de Diseño y Abstracción.

### 3. Señales de alerta detectadas

Listá problemas o riesgos del proceso.

Ejemplos:

- issues ambiguas;
- diseño resuelto tarde;
- PRs demasiado grandes;
- bloqueos no comunicados;
- uso de IA sin revisión;
- tests insuficientes;
- reviews lentas;
- tareas bajas demasiado ambiguas;
- sobrecarga del equipo de Diseño y Abstracción.

### 4. Análisis por dimensión

Separá el análisis en estas categorías:

#### Claridad de issues

Evaluá si las issues llegaron suficientemente claras a diseño o implementación.

#### Diseño previo

Evaluá si las PRs de diseño y PlantUML ayudaron o agregaron fricción.

#### Implementación

Evaluá si la implementación fue más ordenada, revisable y alineada al diseño.

#### Testing

Evaluá si los tests fueron suficientes y útiles.

#### Comunicación

Evaluá si los bloqueos, avances y dudas se comunicaron bien.

#### Uso de IA

Evaluá cómo se usó IA durante la etapa.

Indicá:

- usos positivos;
- usos riesgosos;
- oportunidades de mejora;
- prompts o guías que convendría agregar.

#### Distribución por equipos

Evaluá si la división entre Alta, Media y Baja funcionó bien.

### 5. Problemas raíz

Identificá las causas de fondo detrás de los problemas detectados.

No te quedes solo con síntomas.

Ejemplo:

- Síntoma: PRs grandes.
- Posible causa raíz: issues mal divididas o diseño incompleto.

### 6. Mejoras recomendadas

Proponé mejoras concretas para el próximo ciclo.

Separalas en:

#### Cambios inmediatos

Acciones simples que pueden aplicarse desde la próxima issue.

#### Cambios de proceso

Ajustes al flujo de trabajo, tablero, reviews o división de tareas.

#### Cambios en documentación

Archivos, prompts, checklists o guías que habría que agregar o modificar.

#### Cambios en uso de IA

Mejoras específicas para usar IA de forma más segura y útil.

### 7. Acciones propuestas

Convertí las mejoras en tareas accionables.

Usá este formato:

- Acción:
- Responsable sugerido:
- Prioridad:
- Impacto esperado:
- Costo estimado:
- Criterio de éxito:

### 8. Decisiones que debería tomar el equipo

Listá decisiones abiertas que no debería resolver una IA sola.

Ejemplos:

- cambiar o no el flujo de PRs de diseño;
- ajustar criterios de prioridad;
- modificar responsabilidades entre equipos;
- exigir checklist obligatorio antes de PR;
- sumar o quitar prompts del manual de IA.

### 9. Riesgos para el próximo ciclo

Indicá qué podría salir mal si no se corrigen las señales de alerta.

### 10. Recomendación final

Dame una conclusión breve y práctica sobre cómo debería avanzar el equipo en la próxima etapa.