# Categorización de Bienes

- Status: accepted
- Date: 2026-06-13
- Deciders: Decisión Grupal
- Tags: categorizacion, normalizacion

## Contexto y Problema

El sistema DonaTrack debe procesar bienes donados cargados por usuarios humanos. Estos registros contienen descripciones informales e inconsistentes (e.g. "campera", "parka", "camperón"). Para poder segmentar, ejecutar el matchmaking y mantener el stock logístico ordenado, es fundamental clasificar estos bienes bajo una categoría y subcategoría estándar.

La decisión principal consiste en definir la estrategia o método para clasificar y categorizar estas descripciones.

## Atributos de Calidad y Drivers de Decisión

* Mantenibilidad
* Escalabilidad
* Precisión

## Alternativas Consideradas

* **Categorización Manual:** Cada bien donado requiere que un operador humano revise la descripción y le asigne manualmente la categoría y subcategoría correspondiente.
* **Categorización Automática con Revisión:** Un pipeline de procesamiento de texto/matching realiza una clasificación automática. Si el nivel de confianza de la clasificación es bajo, la categorización queda en estado pendiente de revisión para que un operador humano la confirme o corrija.

## Resultado de la Decisión

Alternativa elegida: "Categorización Automática con Revisión"

Justificación:
Automatizar el proceso reduce drásticamente el tiempo de procesamiento y el esfuerzo administrativo, permitiendo escalar el sistema. Dado que las entradas humanas son impredecibles y los algoritmos automáticos iniciales pueden fallar, el estado de revisión humana para casos de baja confianza asegura la precisión del stock físico sin bloquear el flujo operativo principal.

### Consecuencias Positivas

* Alta velocidad de procesamiento para descripciones comunes y claras.
* Evita detener el flujo logístico ante casos ambiguos, encolándolos para revisión.
* Disminuye la carga operativa de los administradores.

### Consecuencias Negativas

* Requiere diseñar e implementar un pipeline interno de normalización y matching.
* Exige el desarrollo de una interfaz de usuario para que los operadores gestionen la bandeja de revisión.

### Validación

Se testearán los algoritmos de matching semántico con descripciones reales para medir la precisión y calibrar el umbral de confianza a partir del cual se requiere revisión humana.

## Análisis de Alternativas

### Categorización Manual

Los operadores humanos asignan individualmente la categoría a cada donación cargada en el sistema.

#### Pros

* 100% de precisión inicial en las categorías.
* No requiere el desarrollo de algoritmos de limpieza ni matching semántico.

#### Contras

* No escala ante grandes volúmenes de donaciones.
* Genera retrasos y cuellos de botella en el flujo logístico de stock.

### Categorización Automática con Revisión

Un algoritmo realiza el matching y genera una normalización automática, requiriendo validación o corrección humana solo en casos de baja confianza.

#### Pros

* Alta velocidad de procesamiento para el flujo común.
* Los casos dudosos no detienen el flujo general, sino que se encolan para revisión diferida.

#### Contras

* Requiere el desarrollo de algoritmos de matching (exact match, alias, matching semántico).
* Exige diseñar una bandeja de entrada de revisiones humanas.
