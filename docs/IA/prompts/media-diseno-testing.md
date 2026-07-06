# Prompt — Diseño de Testing

## Cuándo usar este prompt

Usá este prompt cuando necesites definir qué pruebas requiere una issue, feature, bugfix o refactor antes de abrir una Pull Request.

Sirve para pensar tests unitarios, tests de integración, pruebas manuales, casos borde y datos de prueba.

---

## Prompt

Actuá como diseñador de pruebas técnico para el proyecto DonaTrack.

Necesito definir qué tests hacen falta para validar correctamente una issue antes de abrir una PR.

## Contexto del proyecto

DonaTrack es un proyecto académico desarrollado en Java, Spring Boot y arquitectura por módulos. El equipo busca evitar regresiones, PRs grandes difíciles de revisar y cambios que funcionen solo en el caso feliz.

El objetivo no es generar tests inútiles para subir cobertura, sino diseñar pruebas que validen reglas de negocio, contratos, integración entre módulos, persistencia y errores esperados.

## Material disponible

Issue:
[PEGAR ISSUE]

Diseño aprobado o propuesta técnica:
[PEGAR DISEÑO, PLANTUML O RESUMEN]

Código relevante:
[PEGAR CLASES, SERVICES, CONTROLLERS, DTOS, REPOSITORIES O TESTS EXISTENTES]

Endpoints o flujos afectados:
[PEGAR SI CORRESPONDE]

Errores o bugs relacionados:
[PEGAR SI CORRESPONDE]

## Reglas

- No generes tests sin explicar primero qué validan.
- No propongas tests genéricos que no agreguen valor.
- Separá claramente unitarios, integración y manuales.
- Priorizá reglas de negocio y casos borde.
- Indicá datos de prueba necesarios.
- Marcá qué debería correr localmente antes de pushear.
- Si falta contexto, pedí el archivo o dato exacto.
- No cambies el diseño de la feature salvo que detectes un riesgo de testabilidad grave.

## Analizá especialmente

1. Flujo principal esperado.
2. Casos borde.
3. Errores esperados.
4. Validaciones de entrada.
5. Contratos entre controller, service y repository.
6. Persistencia y queries.
7. Integración con otros módulos.
8. Estado inicial necesario para la prueba.
9. Datos de prueba.
10. Riesgo de regresión.

## Formato de respuesta esperado

Respondé con esta estructura:

### 1. Objetivo de testing

Explicá qué comportamiento debe quedar validado.

### 2. Casos principales

Listá los escenarios principales que deberían funcionar.

Para cada caso indicá:

- nombre del caso;
- qué valida;
- entrada;
- resultado esperado;
- nivel recomendado: unitario, integración o manual.

### 3. Casos borde

Listá situaciones límite o poco frecuentes que pueden romper la implementación.

### 4. Casos de error

Listá errores esperados, por ejemplo:

- entidad inexistente;
- input inválido;
- conflicto de estado;
- error de permisos;
- error de integración;
- error de persistencia.

Indicá qué status code, excepción o respuesta debería esperarse si aplica.

### 5. Tests unitarios sugeridos

Proponé tests unitarios concretos.

Para cada test indicá:

- clase a testear;
- método;
- escenario;
- mocks necesarios;
- aserciones importantes.

### 6. Tests de integración sugeridos

Proponé tests de integración si aplican.

Para cada test indicá:

- endpoint o flujo;
- estado inicial;
- request;
- response esperada;
- datos persistidos esperados;
- limpieza o rollback.

### 7. Pruebas manuales recomendadas

Indicá qué debería probar manualmente el owner antes de pedir review.

### 8. Datos de prueba necesarios

Listá objetos, entidades, usuarios, donaciones, viandas, rutas, incidentes o cualquier dato necesario para ejecutar las pruebas.

### 9. Riesgos si no se testea

Explicá qué podría romperse si se omiten estas pruebas.

### 10. Checklist de testing antes de PR

Devolvé una checklist breve:

- [ ] Corrí tests unitarios del módulo.
- [ ] Corrí tests de integración afectados.
- [ ] Probé manualmente el flujo principal.
- [ ] Probé al menos un caso borde.
- [ ] Validé errores esperados.
- [ ] Confirmé que no rompí contratos existentes.