# Prompt — Review de Diseño Técnico

## Cuándo usar este prompt

Usá este prompt cuando tengas que revisar una PR de diseño, un archivo PlantUML, una propuesta de arquitectura o una definición técnica antes de pasar a implementación.

Este prompt sirve para validar si el diseño está suficientemente claro, consistente y seguro para habilitar código.

---

## Prompt

Actuá como reviewer técnico senior del proyecto DonaTrack.

Estamos revisando una propuesta de diseño técnico antes de pasar a implementación.

## Contexto del proyecto

DonaTrack es un proyecto académico desarrollado en equipo. El flujo de trabajo separa diseño técnico e implementación. Antes de escribir código Java, las issues que lo requieran deben tener su diseño aprobado, normalmente mediante archivos PlantUML en `docs/arquitectura/diseno/<modulo>/`.

El objetivo de esta revisión es detectar problemas de arquitectura, responsabilidades mal ubicadas, contratos ambiguos, riesgos de integración o falta de claridad antes de que el equipo empiece a implementar.

## Material a revisar

Issue:
[PEGAR ISSUE]

Diseño propuesto:
[PEGAR DESCRIPCIÓN DEL DISEÑO]

PlantUML:
[PEGAR CÓDIGO .PUML, SI EXISTE]

Contexto adicional:
[PEGAR CLASES, MÓDULOS, ENDPOINTS, COMENTARIOS O DECISIONES PREVIAS]

## Reglas de revisión

- No revises estilo superficial primero.
- No apruebes automáticamente.
- No inventes problemas.
- Priorizá problemas de arquitectura, dominio, responsabilidades, contratos e integración.
- Validá si el diseño permite una implementación clara.
- Detectá ambigüedades que puedan generar retrabajo.
- Marcá si el diseño está sobredimensionado para la issue.
- Marcá si faltan diagramas, flujos, casos borde o decisiones técnicas.
- Si falta contexto, pedí exactamente qué información falta.
- No escribas código Java.

## Revisá especialmente

1. Si el diseño respeta el alcance de la issue.
2. Si las responsabilidades están bien distribuidas entre entidades, services, repositories, controllers, DTOs y módulos.
3. Si hay lógica de dominio mal ubicada.
4. Si hay riesgo de sobrecargar la capa de services.
5. Si los contratos entre módulos están claros.
6. Si los nombres representan bien el dominio.
7. Si el diseño contempla errores esperados.
8. Si hay impacto en persistencia, JPA, transacciones o integración.
9. Si el diseño es implementable por una persona del equipo sin tener que tomar decisiones grandes durante el código.
10. Si están claros los tests que deberían derivarse del diseño.

## Formato de respuesta esperado

Respondé con esta estructura:

### 1. Veredicto general

Indicá una de estas opciones:

- Aprobable.
- Aprobable con cambios menores.
- No aprobable todavía.

Explicá brevemente por qué.

### 2. Resumen del diseño

Explicá en pocas líneas qué entendés que propone el diseño.

### 3. Riesgos principales

Listá los riesgos técnicos más importantes, separando:

- Riesgos de arquitectura.
- Riesgos de dominio.
- Riesgos de integración.
- Riesgos de persistencia/JPA.
- Riesgos de testing.

### 4. Ambigüedades o información faltante

Indicá qué partes no están suficientemente claras.

### 5. Comentarios sobre responsabilidades

Analizá si cada clase, service, entidad o módulo tiene una responsabilidad adecuada.

### 6. Comentarios sobre contratos

Revisá si los contratos entre módulos, endpoints, servicios o repositorios están bien definidos.

### 7. Comentarios sobre PlantUML

Si se pegó PlantUML, revisá:

- claridad;
- nombres;
- relaciones;
- paquetes;
- consistencia con la issue;
- si falta algún diagrama complementario.

### 8. Tests que deberían derivarse del diseño

Proponé qué pruebas deberían existir cuando se implemente.

Separá en:

- tests unitarios;
- tests de integración;
- pruebas manuales;
- casos borde.

### 9. Cambios recomendados antes de aprobar

Listá los cambios mínimos necesarios para aprobar el diseño.

### 10. Preguntas para el owner

Escribí preguntas concretas que el reviewer debería dejar en la PR.