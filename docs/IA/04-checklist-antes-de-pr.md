# Checklist antes de abrir PR con ayuda de IA

## Objetivo

Este checklist debe usarse antes de abrir una Pull Request cuando se haya usado IA para analizar, diseñar, implementar, testear, debuggear o revisar cambios.

La IA puede acelerar el trabajo, pero no reemplaza la responsabilidad del owner.

---

# 1. Comprensión del cambio

Antes de abrir PR confirmo que:

- [ ] Entiendo cada archivo que modifiqué.
- [ ] Entiendo cada bloque de código agregado.
- [ ] Puedo explicar por qué el cambio resuelve la issue.
- [ ] Puedo explicar qué partes del sistema se ven afectadas.
- [ ] Puedo explicar qué alternativas descarté, si las hubo.
- [ ] No acepté código que no entiendo.

---

# 2. Alcance de la issue

Confirmo que:

- [ ] El cambio respeta el alcance de la issue.
- [ ] No agregué funcionalidades que no fueron pedidas.
- [ ] No hice refactors grandes innecesarios.
- [ ] No cambié contratos públicos sin avisar.
- [ ] No modifiqué arquitectura sin aprobación.
- [ ] No mezclé varias issues en una misma PR.

---

# 3. Diseño técnico

Confirmo que:

- [ ] La issue no requiere diseño previo, o el diseño ya fue aprobado.
- [ ] Si había PlantUML asociado, lo revisé.
- [ ] La implementación respeta el diseño aprobado.
- [ ] Las responsabilidades quedaron en las clases correctas.
- [ ] No sobrecargué innecesariamente la capa de services.
- [ ] No dupliqué lógica de negocio existente.
- [ ] No introduje acoplamiento innecesario entre módulos.

---

# 4. Backend

Si la PR toca backend, confirmo que:

- [ ] Revisé reglas de negocio afectadas.
- [ ] Revisé controllers, services, repositories y DTOs involucrados.
- [ ] Validé errores esperados.
- [ ] Validé status codes si aplica.
- [ ] Revisé impacto en entidades JPA.
- [ ] Revisé relaciones, cascades, lazy loading o queries si aplica.
- [ ] Revisé transacciones si aplica.
- [ ] No introduje queries innecesariamente costosas.
- [ ] No expuse datos que no deberían exponerse.

---

# 5. Frontend

Si la PR toca frontend, confirmo que:

- [ ] Probé el flujo principal en pantalla.
- [ ] Probé estados de carga.
- [ ] Probé estados de error.
- [ ] Probé estados vacíos.
- [ ] Validé que no se rompieron componentes compartidos.
- [ ] Validé que los datos enviados al backend respetan el contrato esperado.
- [ ] Revisé mensajes visibles para el usuario.

---

# 6. Testing

Confirmo que:

- [ ] Agregué o actualicé tests relevantes.
- [ ] Corrí los tests del módulo afectado.
- [ ] Probé manualmente el caso principal.
- [ ] Probé al menos un caso borde si corresponde.
- [ ] Probé al menos un caso de error si corresponde.
- [ ] Los tests no solo prueban mocks sin validar comportamiento.
- [ ] Los nombres de tests son claros.
- [ ] Los datos de prueba son entendibles.

---

# 7. Debugging y errores

Si usé IA para resolver un error, confirmo que:

- [ ] Entendí la causa del error.
- [ ] No apliqué cambios al azar.
- [ ] Validé que la solución corrige el problema.
- [ ] Corrí nuevamente el comando o test que fallaba.
- [ ] Dejé comentario en la issue si el bloqueo fue relevante.
- [ ] Escalé al equipo si el error implicaba diseño, contratos o arquitectura.

---

# 8. Uso de IA

Confirmo que:

- [ ] Usé IA con contexto suficiente.
- [ ] Revisé críticamente la respuesta de la IA.
- [ ] No acepté código inventado contra clases inexistentes.
- [ ] No delegué decisiones arquitectónicas.
- [ ] Usé IA también para revisar riesgos, no solo para generar código.
- [ ] Puedo defender la solución sin mencionar “lo hizo la IA”.
- [ ] Si la IA propuso algo dudoso, lo validé con el equipo o lo descarté.

---

# 9. Review previa con IA

Antes de pedir review humana, se recomienda usar:

[`prompts/reviewer-pr-implementacion.md`](./prompts/reviewer-pr-implementacion.md)

Confirmo que:

- [ ] Revisé el diff con mirada crítica.
- [ ] Busqué bugs funcionales.
- [ ] Busqué riesgos de integración.
- [ ] Busqué errores de persistencia/JPA si aplica.
- [ ] Busqué tests faltantes.
- [ ] Revisé casos borde.
- [ ] Corregí o justifiqué los hallazgos relevantes.

---

# 10. GitHub y comunicación

Confirmo que:

- [ ] La issue está actualizada.
- [ ] La PR referencia la issue correspondiente.
- [ ] La descripción de la PR explica qué se cambió.
- [ ] La descripción de la PR explica cómo se probó.
- [ ] La PR es revisable y no demasiado grande.
- [ ] Si hubo decisiones relevantes, quedaron documentadas.
- [ ] Si hubo dudas, quedaron comentadas en canales públicos o en GitHub.

---

# 11. Declaración final

Antes de abrir PR, debería poder afirmar:

> Entiendo el cambio, validé su comportamiento, revisé los riesgos y puedo hacerme responsable técnicamente por esta Pull Request.

Si no puedo afirmar eso, todavía no debería pedir review.