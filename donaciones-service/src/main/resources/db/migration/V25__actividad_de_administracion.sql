-- R11-26 (p2-actividad): el registro de actividad de administración. Cada fila es una acción ya
-- hecha: quién (el sub del token, nunca el nombre ni el correo), qué, sobre qué y cuándo (en la hora
-- de Buenos Aires, el Clock del servicio, como fecha_alta). detalle es un rótulo sin datos de una
-- persona humana: la razón social de una organización o de un donante persona jurídica, el nombre
-- del archivo importado, la patente de un camión o la cantidad de propuestas nuevas. Las filas no se
-- editan ni se borran.

CREATE TABLE actividad_administracion (
    id UUID PRIMARY KEY,
    ocurrio_en TIMESTAMP NOT NULL,
    autor TEXT NOT NULL,
    accion VARCHAR(40) NOT NULL CHECK (accion IN (
        'PROPUESTA_APROBADA', 'PROPUESTA_DESCARTADA', 'ASIGNACION_CORRIDA',
        'DONACION_REGISTRADA', 'DONACION_VENCIDA', 'DONANTE_REGISTRADO', 'IMPORTACION_SUBIDA',
        'ORGANIZACION_APROBADA', 'ORGANIZACION_REGISTRADA', 'ADMINISTRADOR_DADO_DE_ALTA',
        'CONTRASENA_PROVISORIA_NUEVA', 'CAMION_AGREGADO', 'CAMION_DESHABILITADO',
        'CAMION_HABILITADO')),
    objeto_id TEXT NULL,
    detalle VARCHAR(200) NULL
);

CREATE INDEX ix_actividad_ocurrio_en ON actividad_administracion (ocurrio_en DESC, id);
CREATE INDEX ix_actividad_autor ON actividad_administracion (autor, ocurrio_en DESC);
CREATE INDEX ix_actividad_accion ON actividad_administracion (accion, ocurrio_en DESC);

-- Lo de antes de este cambio no se inventa: solo las propuestas aprobadas o descartadas guardan
-- quién y cuándo (V22). Esas entran al registro, con la razón social de hoy de su organización (la
-- única que existe). Las de SISTEMA o sin fecha de decisión no tienen autor real y no se copian.
INSERT INTO actividad_administracion (id, ocurrio_en, autor, accion, objeto_id, detalle)
SELECT gen_random_uuid(),
       p.fecha_decision,
       p.decidida_por,
       CASE p.estado WHEN 'APROBADA' THEN 'PROPUESTA_APROBADA' ELSE 'PROPUESTA_DESCARTADA' END,
       p.id::text,
       left(j.razon_social, 200)
FROM propuesta p
LEFT JOIN necesidad n ON n.id = p.id_necesidad_que_satisface
LEFT JOIN juridica j ON j.id = n.entidad_beneficiaria_id
WHERE p.estado IN ('APROBADA', 'DESCARTADA')
  AND p.decidida_por IS NOT NULL
  AND p.decidida_por <> 'SISTEMA'
  AND p.fecha_decision IS NOT NULL;
