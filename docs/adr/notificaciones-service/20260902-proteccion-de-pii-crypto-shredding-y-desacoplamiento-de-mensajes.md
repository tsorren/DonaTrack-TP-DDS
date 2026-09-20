# Protección de Datos Sensibles (PII), Crypto-Shredding y Desacoplamiento de Notificaciones

- Status: proposed
- Date: 2026-09-02
- Deciders: Decisión Grupal
- Tags: privacidad, pii, crypto-shredding, blind-indexing, notificaciones, auth-service, gdpr, ley-25326

## Contexto y Problema

En `notificaciones-service`, la privacidad de los usuarios y el cumplimiento de la Ley 25.326 de Protección de Datos Personales (y regulaciones equivalentes como GDPR Art. 17) se resolvieron originalmente mediante la interfaz `Anonimizable`, aplicando una sobreescritura manual en caliente con `"ANONIMIZADO"` sobre la persona y sus notificaciones vinculadas ([`PersonasService.java`](../../../notificaciones-service/src/main/java/grupo5/notificaciones/services/impl/PersonasService.java#L50-L64)).

Una auditoría técnica en profundidad de la persistencia relacional en PostgreSQL y de la construcción de mensajes reveló tres vulnerabilidades arquitectónicas críticas:

1. **Persistencia de PII en Texto Plano en `Notificacion.mensaje`:**
   La columna `mensaje TEXT` en la tabla `notificacion` ([`V1__init_notificaciones.sql`](../../../notificaciones-service/src/main/resources/db/migration/V1__init_notificaciones.sql#L32-L38)) almacena textos que contienen datos personales identificatorios directos:
   * Credenciales temporales de acceso y contraseñas de bienvenida en [`DonanteRegistrado.java`](../../../notificaciones-service/src/main/java/grupo5/notificaciones/models/entities/notificaciones/eventos/DonanteRegistrado.java#L20-L24) (`"Bienvenido a DonaTrack " + credencialesDeAcceso`).
   * Denominaciones y nombres completos de donantes y beneficiarios.
   * Enlaces y tokens de geolocalización en tiempo real en [`DonacionEnCamino.java`](../../../notificaciones-service/src/main/java/grupo5/notificaciones/models/entities/notificaciones/eventos/DonacionEnCamino.java#L24-L28).
   En los backups periódicos de base de datos (`pg_dump`), en réplicas de lectura y en los logs transaccionales (WAL), esta información permanece expuesta indefinidamente en claro.

2. **Fuga Crítica de PII Cruzado entre Agregados (*Cross-Aggregate Leakage*):**
   Eventos como [`EntregaFallida.java`](../../../notificaciones-service/src/main/java/grupo5/notificaciones/models/entities/notificaciones/eventos/EntregaFallida.java#L31-L70) generan alertas simultáneas para el donante, el beneficiario y el administrador. En la notificación persistida para el Administrador (`persona_id = adminId`), se quema textualmente el nombre del donante:
   ```java
   "Entrega fallida — donante: " + getPersona().getDenominacion() + ...
   ```
   Cuando el donante solicita la supresión de sus datos (*Derecho al Olvido*), la consulta `findByPersonaId(donanteId)` solo localiza las notificaciones dirigidas a la casilla del donante. La notificación en la casilla del administrador conserva el nombre y apellido del donante en texto plano de por vida, violando el derecho de supresión.

3. **Inmunidad Nula en Respaldos Históricos (*Cold Backups*):**
   La sobreescritura manual en la base de datos de producción mediante sentencias `UPDATE` no altera los respaldos históricos ya archivados en almacenamiento secundario. Si se restaura un backup de hace 3 meses, los datos personales del usuario dado de baja reaparecen en el sistema.

4. **Incompatibilidad de Cifrado Probabilístico con Búsquedas Relacionales:**
   Al cifrar campos sensibles como `direccion_correo` o `numero` de teléfono con algoritmos probabilísticos seguros (AES-256-GCM con Vector de Inicialización aleatorio), cada operación genera un ciphertext distinto para el mismo valor, imposibilitando búsquedas exactas (`WHERE email = ?`) sin descifrar masivamente todas las filas en memoria (*Full Table Scan*).

## Atributos de Calidad y Drivers de Decisión

* **Privacidad y Cumplimiento Normativo (Privacy by Design):** Garantizar la supresión efectiva de datos personales conforme al Art. 2 y 16 de la Ley 25.326 y Art. 17 del GDPR.
* **Inmunidad en Respaldos (NIST SP 800-88 Cryptographic Erase):** Asegurar que al ejercer el Derecho al Olvido, los datos personales queden irrevocablemente destruidos en la base de datos viva, en réplicas y en backups históricos sin necesidad de alterar archivos de respaldo.
* **Integridad y Trazabilidad Histórica:** Conservar los registros de notificaciones, estados (`ENVIADA`, `FALLIDA`), timestamps y métricas operativas sin romper claves foráneas ni recurrir a borrados destructivos en cascada (`DELETE CASCADE`).
* **Eficiencia de Consulta ($O(\log N)$):** Permitir búsquedas por canales de contacto exactos mediante índices B-Tree estándar.

## Alternativas Consideradas

### Alternativa 1 (Elegida): Arquitectura Integral de Protección de PII con Crypto-Shredding Centralizado, Blind Indexing y Plantillas Seudonimizadas
Se implementa una solución coordinada en cuatro ejes técnicos:

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                     auth-service                                       │
│                    (Custodio Central de Identidad y Claves DEK)                        │
│               DELETE /api/keys/users/{id}  ──► Destruye DEK en O(1)                    │
│                                            ──► Emite ClaveUsuarioDestruidaEvent        │
└─────────────────────────────────────────┬──────────────────────────────────────────────┘
                                          │ RabbitMQ (Fanout)
                   ┌──────────────────────┴──────────────────────┐
                   ▼                                             ▼
        donaciones-service                            notificaciones-service
┌────────────────────────────────┐            ┌────────────────────────────────┐
│ DEK Cache (Caffeine TTL 10m)   │            │ DEK Cache (Caffeine TTL 10m)   │
│ Invalida cache ante evento     │            │ Invalida cache ante evento     │
│ Ciphertext en PostgreSQL       │            │ Mensajes y canales cifrados    │
│ 404 Key Shredded ──► ANONIMIZADO│           │ 404 Key Shredded ──► ANONIMIZADO│
└────────────────────────────────┘            └────────────────────────────────┘
```

1. **Centralized Key Broker en `auth-service`:**
   * `auth-service` asume la responsabilidad de custodiar las Claves de Cifrado de Datos (**DEK** - *Data Encryption Key*, AES-256) asociadas a cada `persona_id`, almacenadas cifradas con una KEK maestra de infraestructura (*Envelope Encryption*).
   * `notificaciones-service` consulta la DEK vía cliente interno y la almacena en una cache en memoria local (Caffeine Cache con TTL de 10 minutos).
   * **Supresión en $O(1)$:** Al solicitar la baja, `auth-service` destruye la DEK de la persona (`DELETE FROM user_encryption_keys WHERE user_id = :id`) y publica `ClaveUsuarioDestruidaEvent` en RabbitMQ. Los microservicios purgan la clave de su cache local al recibir el evento. A partir de ese nanosegundo, el contenido cifrado de ese usuario en todas las bases de datos y en todos los backups históricos queda convertido matemáticamente en ruido irrecuperable.
2. **Cifrado de `Notificacion.mensaje` y Canales de Contacto:**
   * En `notificaciones-service`, la columna `mensaje` en `NotificacionEntity` y las columnas `denominacion`, `direccion_correo` y `numero` en `PersonaEntity`/`MedioDeContactoEntity` se cifran en la JVM utilizando un `AttributeConverter` JPA con **AES-256-GCM**.
   * El mensaje se cifra con la DEK del destinatario de la notificación (`personaId`). Si la DEK fue destruida, el convertidor devuelve automáticamente `"ANONIMIZADO"`.
3. **Erradicación del PII Cruzado mediante Plantillas Seudonimizadas:**
   * Se prohíbe quemar texto plano con nombres de terceros en el cuerpo de las alertas generadas en `eventos/`.
   * Para alertas a administradores o beneficiarios, el mensaje se persiste almacenando identificadores o tokens de plantilla:
     ```text
     "Entrega fallida — donante: {persona_id: 8f2a...}, donación: {detalle}"
     ```
   * En tiempo de lectura o despacho, el nombre se resuelve dinámicamente consultando la entidad `Persona`. Si el donante fue suprimido (crypto-shredded), la resolución devuelve automáticamente `"ANONIMIZADO"`, garantizando que ninguna notificación de terceros retenga datos personales residuales.
4. **Blind Indexing (Índices Ciegos) con HMAC-SHA256:**
   * Para permitir búsquedas eficientes de medios de contacto por correo o teléfono sin romper el cifrado probabilístico, se incorporan columnas auxiliares `correo_bidx` y `numero_bidx` calculadas mediante:
     $$\text{bidx} = \text{Hex}(\text{HMAC-SHA256}(\text{normalizar}(\text{valor}), \text{BLIND\_INDEX\_SECRET\_SALT}))$$
   * Estas columnas se indexan con índices B-Tree estándar en PostgreSQL, permitiendo consultas $O(\log N)$. Al anonimizar a la persona, estas columnas de índice ciego se actualizan a `NULL`.

### Alternativa 2 (Descartada): Cryptography-as-a-Service (CaaS / Transit Engine Puro sin Cache Local)
Enviar cada dato sensible vía HTTP/REST hacia `auth-service` para que este lo cifre y descifre en cada operación.

*Motivo de descarte:* Inviable por latencia de red. Si una consulta recupera 50 notificaciones de un usuario, se dispararían 50 llamadas HTTP hacia `auth-service`, multiplicando por 10x los tiempos de respuesta y creando un Punto Único de Fallo (SPOF) crítico.

### Alternativa 3 (Descartada): Sobrescritura Manual de Cadenas en Caliente (Status Quo)
Mantener `Anonimizable` sobrescribiendo campos con `"ANONIMIZADO"` mediante sentencias `UPDATE`.

*Motivo de descarte:* No protege copias de seguridad (.dump) ni logs WAL históricos, y falla completamente ante el PII cruzado incrustado en notificaciones dirigidas a otros usuarios.

### Alternativa 4 (Descartada): Cifrado Determinístico Directo (AES-ECB / AES-SIV)
Cifrar las columnas sin sal ni IV aleatorio para permitir cláusulas `WHERE` directas en SQL.

*Motivo de descarte:* Vulnerable a ataques de frecuencia y tablas arcoíris (*Rainbow Tables*). Un atacante con acceso a la base de datos puede descifrar números de teléfono y nombres deduciendo patrones estadísticos.

### Alternativa 5 (Descartada): Borrado Físico en Cascada (`DELETE CASCADE`)
Eliminar físicamente las filas de `notificacion` y `persona` de la base de datos.

*Motivo de descarte:* Destruye las auditorías de entrega, las estadísticas de volumen de donaciones y las métricas de monitoreo histórico, violando las invariantes de negocio de la plataforma.

## Resultado de la Decisión

Se aprueba la **Alternativa 1: Arquitectura Integral de Protección de PII con Crypto-Shredding Centralizado, Blind Indexing y Plantillas Seudonimizadas**.

### Consecuencias Positivas

* **Cumplimiento Legal Absoluto (GDPR Art. 17 / Ley 25.326):** Destrucción irreversible de la identidad en la base de datos viva y en todos los respaldos pasados y futuros.
* **Eliminación Total del PII Cruzado:** Ninguna casilla de administración ni de terceros retiene nombres de personas dadas de baja gracias a la resolución dinámica de plantillas.
* **Operación Atómica Instantánea en $O(1)$:** La baja de un usuario se efectiviza eliminando un único registro de clave en `auth-service`, sin requerir transacciones distribuidas complejas ni barridos masivos de tablas.
* **Consultas de Alta Velocidad:** El uso de Blind Indexing preserva búsquedas indexadas en $O(\log N)$ para validación de medios de contacto.

### Consecuencias Negativas

* **Sobrecarga Computacional Mínima:** El cifrado/descifrado AES-256-GCM consume ciclos de CPU en la JVM (mitigado por aceleración por hardware AES-NI).
* **Dependencia de `auth-service` para la Primera Carga de Clave:** Los microservicios requieren conectividad con `auth-service` para obtener la DEK inicialmente (mitigado por el cacheo local en Caffeine).
* **Complejidad Adicional en Mapeo y DDL:** Requiere columnas auxiliares `*_bidx` y migraciones Flyway correspondientes.

## Hito Canónico de Implementación y Plan de Ejecución

* **Target de Implementación:** **Entrega 6 — Despliegue, Observabilidad y Seguridad (Semana del 23 de Noviembre 2026)**.
* **Justificación de Desacoplamiento Temporal:** La Entrega 4 está estrictamente acotada al modelado relacional y colas de integración. El desarrollo de capacidades de seguridad criptográfica en reposo, Blind Indexing y Key Broker se ejecutará de forma integrada en la Entrega 6 junto con el despliegue del microservicio `auth-service`.
* **Régimen Interino en Entrega 4:** Durante la Entrega 4 se mantiene la anonimización en caliente en base de datos viva mediante `Anonimizable` (`"ANONIMIZADO"`). Las columnas de datos personales se persisten en texto plano como deuda técnica catalogada ([`DTI-07`](../DEUDA_TECNICA.md)).

## Validación

1. **Test de Supresión Instantánea de Notificaciones:** En `RepositoriosJpaTest`, persistir una persona y 3 notificaciones con texto confidencial; simular la revocación de la DEK en el Key Broker; verificar que las consultas posteriores devuelven `"ANONIMIZADO"` en la denominación y en el mensaje de todas las notificaciones.
2. **Test de No-Fuga en Notificaciones a Terceros:** Test de integración con `EntregaFallida`: verificar que al darse de baja el donante, la notificación almacenada en la casilla del administrador muestra `"Entrega fallida — donante: ANONIMIZADO"`.
3. **Test de Blind Index:** Verificar que la búsqueda por correo o teléfono utiliza el índice `_bidx` y retorna la entidad correcta antes de la baja, y que tras la anonimización el blind index queda en `NULL`.

## Análisis de Alternativas

### Alternativa 1: Crypto-Shredding con Key Broker + Blind Index + Plantillas
* **Pros:** Cumplimiento normativo integral; protección de backups; erradica PII cruzado; búsquedas indexadas en $O(\log N)$; operación de baja atómica en $O(1)$.
* **Contras:** Requiere incorporar componentes criptográficos en `common-lib` y coordinar con `auth-service`.

### Alternativa 2: CaaS / Transit Engine Puro
* **Pros:** Claves nunca salen del servicio de auth.
* **Contras:** Latencia inaceptable de red por cada campo leído/escrito; saturación de conexiones.

### Alternativa 3: Sobrescritura Manual (Status Quo)
* **Pros:** Cero complejidad criptográfica.
* **Contras:** Infracción regulatoria por backups expuestos y fuga de datos en casillas de terceros.

### Alternativa 4: Cifrado Determinístico
* **Pros:** Permite SQL WHERE sin columnas auxiliares.
* **Contras:** Falla de seguridad criptográfica crítica frente a análisis de frecuencias.

### Alternativa 5: Borrado Físico
* **Pros:** Elimina los datos de las tablas vivas.
* **Contras:** Destruye balances históricos, auditoría contable y métricas del sistema.
