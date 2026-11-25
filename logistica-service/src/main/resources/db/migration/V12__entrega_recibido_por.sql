-- V12__entrega_recibido_por.sql
-- HG-03 (ADR 20261125-nombre-de-quien-recibio-la-entrega): al confirmar, la organización puede
-- decir el nombre de la persona que recibió. Dato personal, opcional y privado: las entregas
-- anteriores quedan sin nombre.

ALTER TABLE entrega ADD COLUMN recibido_por VARCHAR(80);
