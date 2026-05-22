# Estructura Organizacional y Asignación de Roles

- Status: proposed
- Date: 2026-05-09
- Deciders: Decisión Grupal
- Tags: organizacion, gestion-equipo, roles

## Contexto y Problema

Al contar con un equipo numeroso (10 integrantes) para el desarrollo de DonaTrack, existe un alto riesgo de solapamiento
de esfuerzos, falta de apropiación de tareas críticas "invisibles" (como documentar, organizar reuniones o auditar
calidad) y desorganización frente a los plazos académicos de entrega. Es necesario establecer una estructura de trabajo
que asegure que todos los aspectos técnicos y de gestión del proyecto estén cubiertos de manera eficiente y equitativa.

## Atributos de Calidad y Drivers de Decisión

* Eficiencia
* Mantenibilidad

## Alternativas Consideradas

* Organización Ad-Hoc (Sin roles fijos)
* Organización Estructurada Basada en Fortalezas e Intereses

## Resultado de la Decisión

Alternativa elegida: "Organización Estructurada Basada en Fortalezas e Intereses"

Justificación:
A través de un formulario, se evaluó la mayor fortaleza y el nivel de interés (del 1 al 5) de cada integrante en
distintas áreas de gestión y desarrollo. En base a esto, se asignó a cada miembro un rol principal del cual es
responsable, dos áreas en las que actúa como colaborador, y una rutina específica de reporte/acción para las reuniones
troncales de los Jueves o Domingos. Esto garantiza que todas las responsabilidades (desde UX hasta DevOps, pasando por
la agenda) tengan un dueño explícito, maximizando el valor que cada uno puede aportar de forma natural.

### Consecuencias Positivas

* Se definen responsabilidades explícitas y cruzadas, asegurando soporte mutuo y evitando silos de conocimiento o el
  síndrome del "espectador".
* Las reuniones se vuelven más ágiles gracias a las rutinas asignadas: los jueves se enfocan en validaciones técnicas y
  reportes (Valen, Ber, Miranda, Martín, Sofia), mientras que los domingos se enfocan en planificación, asignación y
  salud del equipo (Aylén, Anush, Belén, Nico, Tadeo).
* Se protege la integridad emocional y operativa del equipo mediante el rol de Sinergia (Tadeo), garantizando que las
  cargas de trabajo estén balanceadas.

### Consecuencias Negativas

* Exige una estricta disciplina administrativa; si los integrantes ignoran sus responsabilidades principales por
  enfocarse únicamente en programar, el modelo colapsa.
* La ausencia temporal de un responsable clave (por ejemplo, Revisión de Diseño o Gestión de Requerimientos) puede
  ralentizar el avance en esa área específica si los colaboradores no asumen el liderazgo momentáneo.

## Análisis de Alternativas

### Organización Ad-Hoc (Sin roles fijos)

Las tareas de gestión, revisión de PRs, aseguramiento de calidad y documentación se toman de manera espontánea por quien
esté disponible semana a semana.

#### Pros

* Cero fricción inicial para definir la dinámica de trabajo.
* Máxima flexibilidad para que cualquiera tome cualquier tarea en cualquier momento.

#### Contras

* Altísima probabilidad de que las tareas menos "atractivas" (como mantener ADRs al día o gestionar el tablero Kanban)
  queden huérfanas.
* Riesgo de que la arquitectura y la calidad de código se degraden al no haber una contraparte crítica designada
  formalmente.

### Organización Estructurada Basada en Fortalezas e Intereses

Implementar una matriz de responsabilidades claras donde cada integrante es líder de una disciplina y soporte en otras
dos, con tareas fijas predefinidas para los días de reunión.

#### Pros

* Aprovecha las motivaciones intrínsecas y fortalezas naturales de cada desarrollador.
* Cubre transversalmente todas las necesidades del proyecto (UX, QA, Rendimiento, DevOps, Entregables, Agenda).
* Agiliza la toma de decisiones al haber una autoridad o "dueño" definido por cada área de debate (ej. Arquitectura, QA,
  UI).

#### Contras

* Requiere tiempo y madurez del equipo para respetar y asimilar las jerarquías de responsabilidad.

## Links

[Roles](https://docs.google.com/spreadsheets/d/1tK97FjDmnCVZ8b6iCOdHk4rN_O8sEV_apnGqybvntTg/edit?usp=sharing)