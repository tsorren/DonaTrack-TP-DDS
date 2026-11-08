package grupo5.cliente.puertos;

import static grupo5.cliente.vistas.EstadoEntrega.CON_PROBLEMA;
import static grupo5.cliente.vistas.EstadoEntrega.ENTREGADA;
import static grupo5.cliente.vistas.EstadoEntrega.EN_CAMINO;
import static grupo5.cliente.vistas.EstadoEntrega.PENDIENTE;

import grupo5.cliente.vistas.EstadoEntrega;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Lo que el sitio lee y hace en Donaciones con el token de quien tiene la sesión. */
public interface DonacionesComoUsuario {

  List<DonacionDelDonante> donacionesDe(UUID donanteId);

  List<NecesidadDeEntidad> necesidadesDe(UUID entidadId);

  /** Las personas jurídicas registradas, con su razón social; las lee quien administra. */
  List<Juridica> juridicas();

  /** Da de alta la entidad beneficiaria de una persona jurídica; la hace quien administra. */
  void registrarEntidad(UUID juridicaId);

  record DonacionDelDonante(
      UUID id, String descripcion, String estadoActual, LocalDateTime fechaRegistro, int cantidad) {

    private static final Map<String, EstadoEntrega> ESTADOS =
        Map.of(
            "EN_TRASLADO", EN_CAMINO,
            "ENTREGADA", ENTREGADA,
            "ENTREGA_FALLIDA", CON_PROBLEMA,
            "VENCIDA", CON_PROBLEMA);

    public EstadoEntrega estado() {
      return ESTADOS.getOrDefault(estadoActual, PENDIENTE);
    }
  }

  record Juridica(UUID id, String razonSocial) {}

  record NecesidadDeEntidad(
      UUID id,
      String tipo,
      int cantidadNecesitada,
      String descripcion,
      boolean estaSatisfecha,
      LocalDate fechaInicio,
      LocalDate fechaFin) {}
}
