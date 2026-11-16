package grupo5.donaciones.models.repositories.listados;

/** Orden del listado de donantes: por fecha de alta o por nombre visible. Desempata por id. */
public enum OrdenDonantes {
  NUEVOS,
  ANTIGUOS,
  NOMBRE
}
