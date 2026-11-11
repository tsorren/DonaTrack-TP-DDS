package grupo5.cliente.rest;

import grupo5.cliente.puertos.AdminOperacionPuerto;
import grupo5.cliente.vistas.AdminCamionesVista;
import grupo5.cliente.vistas.AdminCamionesVista.Camion;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * La operación del admin desde Logística y Donaciones. Logística da de cada camión la patente, las
 * capacidades y el estado (con los deshabilitados, HB-23); no da nombre, chofer, viaje, motivo ni
 * rendimiento: quedan null y la pantalla no los muestra.
 */
// @Primary solo para el análisis estático (S9352): la propiedad ya deja un único adaptador activo.
@Component
@Primary
@ConditionalOnProperty(name = "cliente.datos.admin-operacion", havingValue = "rest")
class AdminOperacionRest implements AdminOperacionPuerto {

  private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm", Comun.ES_AR);
  private static final int FURGON = 1500;
  private static final int MEDIANO = 3000;

  private final LogisticaApi logistica;
  private final Clock reloj;

  AdminOperacionRest(LogisticaApi logistica, Clock reloj) {
    this.logistica = logistica;
    this.reloj = reloj;
  }

  @Override
  public AdminCamionesVista camiones(String accion) {
    ZonedDateTime ahora = ZonedDateTime.now(reloj).withZoneSameInstant(Comun.ARGENTINA);
    return new AdminCamionesVista(
        logistica.camiones(true).stream().map(AdminOperacionRest::camion).toList(),
        "Al " + Comun.DIA_Y_ANIO.format(ahora) + ", " + HORA.format(ahora) + ".",
        null,
        null);
  }

  @Override
  public String agregarCamion(String patente, float capacidadKg, float volumen, float altura) {
    return logistica
        .agregarCamion(new LogisticaApi.CamionNuevo(patente, volumen, altura, capacidadKg))
        .id()
        .toString();
  }

  @Override
  public String cambiarCamion(String camion, boolean habilitado, String motivo) {
    return logistica
        .cambiarEstadoDeCamion(
            UUID.fromString(camion),
            new LogisticaApi.CambioDeCamion(
                habilitado ? "DISPONIBLE" : "DESHABILITADO",
                motivo == null || motivo.isBlank() ? null : motivo.trim()))
        .patente();
  }

  private static Camion camion(LogisticaApi.Camion c) {
    int kg = c.capacidadKG() == null ? 0 : Math.round(c.capacidadKG());
    String estado =
        switch (c.estado() == null ? "" : c.estado()) {
          case "EN_RUTA" -> "ruta";
          case "DESHABILITADO" -> "deshabilitado";
          default -> "disponible";
        };
    return new Camion(
        c.id().toString(),
        c.patente(),
        null,
        kg <= FURGON ? "furgon" : "camion",
        cifra(kg) + " kg · " + cifra(c.capacidadVolumen()) + " m³",
        kg,
        tamano(kg),
        estado,
        "ruta".equals(estado) ? "En ruta" : "En el depósito",
        null,
        null,
        null,
        null,
        null,
        false);
  }

  /** Los tamaños del filtro de capacidad: hasta 1.500 kg, hasta 3.000 y más. */
  private static String tamano(int kg) {
    if (kg <= FURGON) {
      return "chico";
    }
    return kg <= MEDIANO ? "mediano" : "grande";
  }

  /** "3.500", "12,5": la cifra como se escribe en la Argentina. */
  private static String cifra(Number valor) {
    DecimalFormat formato =
        new DecimalFormat("#,##0.#", DecimalFormatSymbols.getInstance(Comun.ES_AR));
    return formato.format(valor == null ? 0 : valor);
  }
}
