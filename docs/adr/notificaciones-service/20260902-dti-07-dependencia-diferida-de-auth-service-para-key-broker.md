# [DTI-07] Dependencia Diferida de auth-service para Key Broker y Solución Interina de Crypto-Shredding

- Status: proposed
- Date: 2026-09-02
- Deciders: Decisión Grupal
- Tags: deuda-tecnica, dti-07, privacidad, crypto-shredding, auth-service, key-broker, persistencia

## Contexto y Problema

El ADR [`20260902-proteccion-de-pii-crypto-shredding-y-desacoplamiento-de-mensajes.md`](./20260902-proteccion-de-pii-crypto-shredding-y-desacoplamiento-de-mensajes.md) formalizó la arquitectura de referencia para la supresión instantánea de datos personales (*Derecho al Olvido* en $O(1)$) mediante **Crypto-Shredding**, estableciendo que la custodia y el ciclo de vida de las claves simétricas (**DEK** - *Data Encryption Key*) se delegan en el microservicio `auth-service` actuando como **Centralized Key Broker**.

Sin embargo, en el estado actual del repositorio, **`auth-service` aún no está implementado** (existe únicamente como un directorio placeholder con `.gitkeep`). No se dispone de sus contratos OpenAPI, endpoints HTTP, esquemas relacionales ni eventos RabbitMQ asociados.

Esta disparidad temporal genera una **deuda técnica arquitectónica crítica (DTI-07)**:
1. `notificaciones-service` (y `donaciones-service`) no pueden establecer dependencias en tiempo de ejecución (`FeignClient`) hacia un servicio inexistente sin romper el arranque de la aplicación y la suite de pruebas automatizadas.
2. Si se pospone completamente la implementación del cifrado de datos personales hasta la entrega en la que se construya `auth-service`, las columnas sensibles (como `notificacion.mensaje` y los medios de contacto) se persistirían temporalmente en texto plano, acumulando pasivos de seguridad e invalidando la protección de copias de seguridad (.dump).

Se requiere formalizar una estrategia interina que permita implementar Crypto-Shredding y Blind Indexing en `notificaciones-service` de manera inmediata, preservando una ruta de migración transparente y sin fricción hacia `auth-service`.

## Atributos de Calidad y Drivers de Decisión

* **Evolutividad y Bajo Acoplamiento:** Diseñar una abstracción de cliente de claves en `common-lib` que desacople la lógica de cifrado de la ubicación física del custodio de claves.
* **Seguridad desde el Diseño (Security by Design):** No postergar la protección criptográfica en reposo; los datos sensibles deben guardarse cifrados desde el primer día en PostgreSQL.
* **Transparencia en la Migración Futura:** La sustitución del proveedor de claves local por `auth-service` debe resolverse mediante configuración e inyección de dependencias de Spring (`@Profile` o `@ConditionalOnProperty`), sin requerir modificaciones en las entidades de dominio, mappers ni convertidores JPA.

## Alternativas Consideradas

### Alternativa 1 (Elegida): Interfaz Port `KeyBrokerClient` en `common-lib` con Adaptador Interino Local (`LocalKeyBrokerAdapter`)
1. **Puerto en Shared Kernel (`common-lib`):**
   Definir la interfaz `KeyBrokerClient`:
   ```java
   public interface KeyBrokerClient {
       SecretKey obtenerOgenerarDek(UUID sujetoId);
       void destruirDek(UUID sujetoId);
   }
   ```
2. **Implementación Interina Local (`LocalKeyBrokerAdapter`):**
   En ausencia de `auth-service`, `notificaciones-service` utiliza una implementación local activa bajo el perfil `postgres` (o cuando `donatrack.crypto.key-broker.remote=false`):
   * Almacena las DEKs por usuario en una tabla local protegida dentro de su propio esquema PostgreSQL:
     ```sql
     CREATE TABLE clave_cifrado_persona (
         persona_id UUID PRIMARY KEY,
         dek_cifrada BYTEA NOT NULL,
         iv_dek BYTEA NOT NULL,
         creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
     );
     ```
   * Las DEKs se cifran utilizando una Clave Maestra (**KEK** - *Key Encryption Key*) inyectada a la aplicación mediante variable de entorno (`DONATRACK_MASTER_KEK_BASE64`).
   * La supresión local se ejecuta mediante `DELETE FROM clave_cifrado_persona WHERE persona_id = :id`.
3. **Ruta de Migración a `auth-service` (Resolución de DTI-07):**
   Cuando `auth-service` esté implementado, se desarrollará `RemoteAuthKeyBrokerClient` (usando OpenFeign y escuchando `ClaveUsuarioDestruidaEvent` en RabbitMQ). Bastará alternar la propiedad de configuración a `donatrack.crypto.key-broker.remote=true` para deshabilitar el adaptador local y adoptar el Key Broker centralizado de forma 100% retrocompatible.

### Alternativa 2 (Descartada): Postergar el Cifrado y Mantener la Anonimización Manual (Status Quo de DTI-01)
Continuar guardando `mensaje TEXT`, correos y teléfonos en texto plano y aplicar sobreescritura con `"ANONIMIZADO"` hasta que `auth-service` esté operativo.

*Motivo de descarte:* Viola los principios de privacidad desde el diseño. Deja expuestos todos los backups relacionales que se tomen durante esta etapa y obliga al equipo a rediseñar y migrar el esquema DDL dos veces.

### Alternativa 3 (Descartada): Mock Volátil en Memoria (`ConcurrentHashMap`)
Gestionar las claves DEK en una colección en memoria sin persistencia relacional.

*Motivo de descarte:* Catastrófico. Cada vez que el contenedor de Docker o el microservicio se reinicia, las claves en memoria desaparecen y la totalidad de los datos cifrados en la base de datos se vuelve permanentemente ilegible e irrecuperable.

## Resultado de la Decisión

Se aprueba la **Alternativa 1: Interfaz Port `KeyBrokerClient` en `common-lib` con Adaptador Interino Local (`LocalKeyBrokerAdapter`)** y se cataloga formalmente como **DTI-07**.

### Consecuencias Positivas

* **Seguridad Inmediata:** `notificaciones-service` implementa AES-256-GCM y Blind Indexing en PostgreSQL sin bloquearse por dependencias externas.
* **Cero Impacto en el Dominio:** Los convertidores JPA (`AttributeConverter`) y los agregados consumen exclusivamente el puerto `KeyBrokerClient`.
* **Transición Limpia:** La deuda técnica queda aislada exclusivamente a una clase adaptadora de infraestructura que será reemplazada de forma transparente al implementarse `auth-service`.

### Consecuencias Negativas / Deuda Asumida

* **Supresión Descentralizada Temporal:** Mientras no exista `auth-service`, la baja de un usuario requiere propagar la petición de eliminación a través de `PersonasService.anonimizar()`, borrando la clave local en `notificaciones-service` en lugar de una baja instantánea global en $O(1)$.
* **Migración de Claves Futura:** Al entrar en producción `auth-service`, se requerirá un script de migración para transferir las DEKs locales existentes en `notificaciones.clave_cifrado_persona` hacia la base de datos central de `auth-service`.

## Plan de Cancelación de Deuda Técnica (Exit Criteria)

* **Hito Canónico de Cancelación:** **Entrega 6 — Despliegue, Observabilidad y Seguridad (Semana del 23 de Noviembre 2026)**.
* **Justificación de Alcance:** La Entrega 4 (Septiembre) aborda exclusivamente persistencia relacional ORM y colas de integración. La seguridad criptográfica, hardening y el microservicio `auth-service` tienen su desarrollo programado para la Entrega 6.

DTI-07 se considerará formalmente saldada y cerrada cuando se cumplan las siguientes condiciones:
1. `auth-service` esté implementado en la Entrega 6 y exponga los endpoints `/internal/crypto/keys/{userId}` para emisión y revocación de DEKs.
2. Exista en `common-lib` el cliente `RemoteAuthKeyBrokerClient` con soporte de cache local (Caffeine) y listener RabbitMQ para invalidación.
3. Se desactive `LocalKeyBrokerAdapter` en `notificaciones-service` mediante configuración de entorno (`donatrack.crypto.key-broker.remote=true`).
4. Se ejecute la suite de pruebas de integración distribuida validando la supresión en $O(1)$ a través de `auth-service`.

## Análisis de Alternativas

### Alternativa 1: Port `KeyBrokerClient` + Adaptador Local
* **Pros:** Permite arrancar de inmediato con cifrado real; arquitectura desacoplada; migración sin cambios en el modelo relacional ni de dominio.
* **Contras:** Requiere mantener temporalmente una tabla local de claves y migrarla en el futuro.

### Alternativa 2: Postergar Cifrado (Status Quo)
* **Pros:** Menor código inicial.
* **Contras:** Filtración continua de PII en backups relacionales; retrabajo posterior masivo.

### Alternativa 3: Mock en Memoria
* **Pros:** Simplicidad engañosa.
* **Contras:** Pérdida y corrupción definitiva de datos ante cualquier reinicio de proceso.
