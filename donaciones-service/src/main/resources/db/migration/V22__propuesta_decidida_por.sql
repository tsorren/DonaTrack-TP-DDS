-- R11-10 (p1-admins): quién aprobó o descartó una propuesta y cuándo. Guarda el id de la cuenta
-- (el sub del token), no el nombre ni el correo: el nombre se lee del proveedor de ingreso al
-- mostrarlo. Las propuestas decididas antes de este cambio quedan en NULL.

ALTER TABLE propuesta ADD COLUMN decidida_por TEXT NULL;
ALTER TABLE propuesta ADD COLUMN fecha_decision TIMESTAMP NULL;
