package grupo5.cliente.rest;

import static grupo5.cliente.rest.Comun.ARGENTINA;
import static grupo5.cliente.rest.Comun.DIA;
import static grupo5.cliente.rest.Comun.HORA;

import grupo5.cliente.puertos.DonanteSeguimientoPuerto;
import grupo5.cliente.vistas.DonanteDonacionVista;
import grupo5.cliente.vistas.DonanteSeguimientoVista;
import grupo5.cliente.vistas.DonanteSeguimientoVista.Punto;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

/**
 * Seguimiento del donante: sus donaciones de Donaciones y, de Logística, las entregas activas
 * ({@code GET /api/entregas/seguimiento/donantes/{id}}, HB-16) con la posición del camión y, de
 * cada una, el destino de la entrega para el mapa. Sin Logística, la lista se muestra sin
 * posiciones.
 */
// @Primary solo para el análisis estático (S9352): la propiedad ya deja un único adaptador activo.
@Component
@Primary
@ConditionalOnProperty(name = "cliente.datos.donante", havingValue = "rest")
class DonanteSeguimientoRest implements DonanteSeguimientoPuerto {

  private final DonacionesApi api;
  private final LogisticaApi logistica;
  private final Clock reloj;

  DonanteSeguimientoRest(DonacionesApi api, LogisticaApi logistica, Clock reloj) {
    this.api = api;
    this.logistica = logistica;
    this.reloj = reloj;
  }

  @Override
  public DonanteSeguimientoVista seguimiento(UUID donante) {
    LocalDateTime ahora = LocalDateTime.now(reloj.withZone(ARGENTINA));
    Map<UUID, LogisticaApi.Seguimiento> seguimientos = seguimientos(logistica, donante);
    List<DonanteDonacionVista> donaciones =
        DonanteDonaciones.de(api, donante, seguimientos, ahora.toLocalDate());
    Map<String, Punto> puntos = new HashMap<>();
    seguimientos
        .values()
        .forEach(s -> punto(s).ifPresent(p -> puntos.put(s.idDonacion().toString(), p)));
    return new DonanteSeguimientoVista(
        donaciones, puntos, "hoy, " + ahora.format(DIA) + ", a las " + ahora.format(HORA));
  }

  /** Las entregas activas del donante por donación; vacío si Logística no responde. */
  static Map<UUID, LogisticaApi.Seguimiento> seguimientos(LogisticaApi cliente, UUID donante) {
    try {
      List<LogisticaApi.Seguimiento> lista = cliente.seguimientoDeDonante(donante);
      return lista == null
          ? Map.of()
          : lista.stream()
              .filter(s -> s.idDonacion() != null)
              .collect(
                  Collectors.toMap(
                      LogisticaApi.Seguimiento::idDonacion, Function.identity(), (a, b) -> a));
    } catch (RestClientException sinLogistica) {
      return Map.of();
    }
  }

  /** La posición del camión y la de la entidad; sin alguna de las dos, la entrega no va al mapa. */
  private Optional<Punto> punto(LogisticaApi.Seguimiento s) {
    LogisticaApi.Posicion camion = s.posicionCamion();
    if (camion == null
        || camion.latitud() == null
        || camion.longitud() == null
        || s.idEntrega() == null) {
      return Optional.empty();
    }
    try {
      LogisticaApi.Entrega entrega = logistica.entrega(s.idEntrega());
      LogisticaApi.Destino destino = entrega == null ? null : entrega.destino();
      if (destino == null || destino.latitud() == null || destino.longitud() == null) {
        return Optional.empty();
      }
      return Optional.of(
          new Punto(camion.latitud(), camion.longitud(), destino.latitud(), destino.longitud()));
    } catch (RestClientException sinEntrega) {
      return Optional.empty();
    }
  }
}
