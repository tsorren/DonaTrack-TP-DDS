package grupo5.donaciones.infrastructure.persistencia.adapters;

import grupo5.donaciones.models.entities.personas.TipoJuridico;
import grupo5.donaciones.models.entities.personas.TipoPersona;

/**
 * Los fragmentos SQL de búsqueda y orden de una persona, compartidos por los listados de personas y
 * de donantes. Usan los alias {@code p} (persona), {@code h} (humana) y {@code j} (jurídica); hacen
 * lo mismo que {@code PersonaEnListado} en memoria.
 */
final class SqlPersona {

  /** Las uniones con la humana y la jurídica de la persona {@code p}. */
  static final String UNIONES =
      " LEFT JOIN donaciones.humana h ON h.id = p.id LEFT JOIN donaciones.juridica j ON j.id = p.id";

  /** La razón social de la jurídica, o el apellido y el nombre de la humana, para ordenar. */
  static final String NOMBRE_VISIBLE =
      ConsultaListado.sinTildes("coalesce(j.razon_social, h.apellido || ' ' || h.nombre)");

  private SqlPersona() {}

  /** Agrega la búsqueda {@code q} (nombre, apellido, razón social o documento) y los tipos. */
  static ConsultaListado filtrar(
      ConsultaListado consulta, String q, TipoPersona tipo, TipoJuridico tipoJuridico) {
    if (q != null) {
      consulta.donde(
          "("
              + ConsultaListado.contiene("h.nombre", "q")
              + " OR "
              + ConsultaListado.contiene("h.apellido", "q")
              + " OR "
              + ConsultaListado.contiene("j.razon_social", "q")
              + " OR "
              + ConsultaListado.contiene("p.nro_documento", "q")
              + ")",
          "q",
          ConsultaListado.patron(q));
    }
    if (tipo != null) {
      consulta.donde("p.tipo_persona = :tipo", "tipo", tipo.name());
    }
    if (tipoJuridico != null) {
      consulta.donde("j.tipo_juridico = :tipoJuridico", "tipoJuridico", tipoJuridico.name());
    }
    return consulta;
  }
}
