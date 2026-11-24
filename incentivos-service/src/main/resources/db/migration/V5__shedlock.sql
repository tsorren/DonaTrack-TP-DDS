-- V5__shedlock.sql
-- E7-S01d-1 (ADR de despliegue en Docker Swarm, "Tareas programadas"): con varias réplicas, cada
-- tarea @Scheduled toma un lock en esta tabla con @SchedulerLock y corre en una sola. La usa el
-- LockProvider JDBC de common-lib (ShedLockAutoConfiguration). Formato de la documentación de ShedLock.
CREATE TABLE shedlock (
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);
