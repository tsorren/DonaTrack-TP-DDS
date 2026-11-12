package grupo5.donaciones.models.repositories.listados;

import grupo5.donaciones.models.entities.personas.TipoJuridico;
import grupo5.donaciones.models.entities.personas.TipoPersona;
import java.util.UUID;

/**
 * Búsqueda de donantes (R11-01). Un campo nulo no filtra. {@code q} busca en nombre, apellido,
 * razón social y número de documento. {@code categoriaId}: donó alguna vez algo de esa categoría.
 * {@code canal} (CORREO, TELEFONO o WHATSAPP): solo los activos, no anonimizados y con ese medio,
 * como hacía el listado para contactar.
 */
public record CriterioDonantes(
    String q,
    TipoPersona tipo,
    TipoJuridico tipoJuridico,
    Boolean activo,
    UUID categoriaId,
    String canal,
    OrdenDonantes orden,
    Paginado paginado) {}
