package grupo5.cliente.maqueta;

import grupo5.cliente.puertos.DonanteInicioPuerto;
import grupo5.cliente.vistas.DonanteAnioVista;
import grupo5.cliente.vistas.DonanteDonacionVista;
import grupo5.cliente.vistas.DonanteInicioVista;
import grupo5.cliente.vistas.DonanteInicioVista.Aviso;
import grupo5.cliente.vistas.EstadoEntrega;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * El inicio de la empresa de ejemplo: las siete donaciones de viaje-datos, las tres noticias más
 * nuevas (las de datos-donante) y las fechas de la maqueta ("hoy" es el 15 de octubre de 2026). El
 * año de la persona sale de las mismas siete, todas de octubre: el mes muestra 3 y el total.
 */
@Component
@ConditionalOnProperty(
    name = "cliente.datos.donante",
    havingValue = "maqueta",
    matchIfMissing = true)
class DonanteInicioMaqueta implements DonanteInicioPuerto {

  private static final DonanteInicioVista INICIO =
      new DonanteInicioVista(
          DonanteDonacionesDeEjemplo.DONACIONES,
          List.of(
              new Aviso(donacion("lentejas"), 0),
              new Aviso(donacion("arroz"), 0),
              new Aviso(donacion("leche"), 0)),
          4,
          2,
          "octubre de 2026",
          "15 de octubre",
          "15 de octubre, 16:25",
          "1 de octubre",
          "2026-10-15",
          "2026-10-14");

  private static final List<String> MESES =
      List.of(
          "enero",
          "febrero",
          "marzo",
          "abril",
          "mayo",
          "junio",
          "julio",
          "agosto",
          "septiembre",
          "octubre");

  private static final DonanteAnioVista ANIO =
      new DonanteAnioVista(
          2026,
          MESES.size(),
          IntStream.rangeClosed(1, MESES.size()).mapToObj(DonanteInicioMaqueta::mes).toList(),
          INICIO.donaciones().size(),
          contar(EstadoEntrega.ENTREGADA),
          contar(EstadoEntrega.EN_CAMINO),
          contar(EstadoEntrega.PENDIENTE),
          true,
          INICIO.avisos(),
          INICIO.sinLeer(),
          INICIO.hoy(),
          INICIO.actualizado(),
          INICIO.hoyIso(),
          INICIO.ayerIso());

  private static DonanteAnioVista.Mes mes(int numero) {
    String nombre = MESES.get(numero - 1);
    List<DonanteDonacionVista> delMes =
        INICIO.donaciones().stream().filter(d -> d.donada().endsWith(nombre + " de 2026")).toList();
    return new DonanteAnioVista.Mes(
        numero,
        nombre,
        "2026-%02d".formatted(numero),
        delMes.size(),
        delMes.stream().limit(DonanteAnioVista.POR_MES).toList());
  }

  private static long contar(EstadoEntrega estado) {
    return INICIO.donaciones().stream().filter(d -> d.estado() == estado).count();
  }

  private static DonanteDonacionVista donacion(String clave) {
    return DonanteDonacionesDeEjemplo.DONACIONES.stream()
        .filter(d -> d.clave().equals(clave))
        .findFirst()
        .orElseThrow();
  }

  @Override
  public DonanteInicioVista inicio(UUID donante) {
    return INICIO;
  }

  @Override
  public DonanteAnioVista anio(UUID donante) {
    return ANIO;
  }
}
