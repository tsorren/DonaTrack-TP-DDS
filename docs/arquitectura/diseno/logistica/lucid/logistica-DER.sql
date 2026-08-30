CREATE TABLE "Pais" (
  "id_pais" UUID,
  "nombre" string,
  PRIMARY KEY ("id_pais")
);

CREATE TABLE "Provincia" (
  "id_provincia" UUID,
  "id_pais" UUID,
  "nombre" string,
  PRIMARY KEY ("id_provincia"),
  CONSTRAINT "FK_Provincia_id_pais"
    FOREIGN KEY ("id_pais")
      REFERENCES "Pais"("id_pais")
);

CREATE TABLE "Localidad" (
  "id_localidad" UUID,
  "id_provincia" UUID,
  "nombre" string,
  PRIMARY KEY ("id_localidad"),
  CONSTRAINT "FK_Localidad_id_provincia"
    FOREIGN KEY ("id_provincia")
      REFERENCES "Provincia"("id_provincia")
);

CREATE TABLE "Cambio_estado_camion" (
  "id_cambio_estado_camion" UUID,
  "id_camion" UUID,
  "estado_anterior" enum,
  "estado_nuevo" enum,
  "timestamp" int,
  "" <type>,
  PRIMARY KEY ("id_cambio_estado_camion")
);

CREATE TABLE "Camion" (
  "id_camion" UUID,
  "patente" varchar,
  "capacidad_volumen" float,
  "capacidad_peso" float,
  "estado" enum,
  PRIMARY KEY ("id_camion"),
  CONSTRAINT "FK_Camion_patente"
    FOREIGN KEY ("patente")
      REFERENCES "Cambio_estado_camion"("id_camion")
);

CREATE TABLE "Cambio_estado_chofer" (
  "id_cambio_estado_entrega" UUID,
  "id_chofer" UUID,
  "estado_anterior" enum,
  "estado_nuevo" enum,
  "timestamp" int,
  "" <type>,
  PRIMARY KEY ("id_cambio_estado_entrega")
);

CREATE TABLE "Direccion" (
  "id_direccion" UUID,
  "id_localidad" UUID,
  "calle" varchar,
  "altura" int,
  "piso" int,
  "departamento" varchar,
  "codigo_postal" int,
  PRIMARY KEY ("id_direccion"),
  CONSTRAINT "FK_Direccion_id_localidad"
    FOREIGN KEY ("id_localidad")
      REFERENCES "Localidad"("id_localidad")
);

CREATE TABLE "Cambio_estado_ruta" (
  "id_cambio_estado_ruta" UUID,
  "id_ruta" UUID,
  "estado_anterior" enum,
  "estado_nuevo" enum,
  "timestamp" int,
  "" <type>,
  PRIMARY KEY ("id_cambio_estado_ruta")
);

CREATE TABLE "Cambio_estado_entrega" (
  "id_cambio_estado_entrega" UUID,
  "id_entrega" UUID,
  "estado_anterior" enum,
  "estado_nuevo" enum,
  "timestamp" int,
  "actor" <type>,
  PRIMARY KEY ("id_cambio_estado_entrega")
);

CREATE TABLE "Chofer" (
  "id_chofer" UUID,
  "nombre" string,
  "apellido" string,
  "habilitado" bool,
  "telefono" int,
  "estado" enum,
  PRIMARY KEY ("id_chofer"),
  CONSTRAINT "FK_Chofer_nombre"
    FOREIGN KEY ("nombre")
      REFERENCES "Cambio_estado_chofer"("id_chofer")
);

CREATE TABLE "Ruta" (
  "id_ruta" UUID,
  "id_chofer" UUID,
  "id_camion" UUID,
  "fecha" int,
  "estado" enum,
  "hora_inicio_estimada" int,
  "hora_fin_estimada" int,
  PRIMARY KEY ("id_ruta"),
  CONSTRAINT "FK_Ruta_id_chofer"
    FOREIGN KEY ("id_chofer")
      REFERENCES "Chofer"("id_chofer"),
  CONSTRAINT "FK_Ruta_id_camion"
    FOREIGN KEY ("id_camion")
      REFERENCES "Camion"("patente"),
  CONSTRAINT "FK_Ruta_id_ruta"
    FOREIGN KEY ("id_ruta")
      REFERENCES "Cambio_estado_ruta"("id_ruta")
);

CREATE TABLE "Entrega" (
  "id_entrega" UUID,
  "id_ruta" UUID,
  "id_direccion" UUID,
  "id_donacion" UUID,
  "id_beneficiario" UUID,
  "estado" enum,
  "hora_arribo" int,
  "hora_salida" int,
  "foto_recepcion_url" varchar,
  "volumen_total_m3" float,
  "peso_total_kg" float,
  PRIMARY KEY ("id_entrega"),
  CONSTRAINT "FK_Entrega_id_entrega"
    FOREIGN KEY ("id_entrega")
      REFERENCES "Cambio_estado_entrega"("id_entrega"),
  CONSTRAINT "FK_Entrega_id_direccion"
    FOREIGN KEY ("id_direccion")
      REFERENCES "Direccion"("id_direccion"),
  CONSTRAINT "FK_Entrega_id_ruta"
    FOREIGN KEY ("id_ruta")
      REFERENCES "Ruta"("id_ruta")
);

