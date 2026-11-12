package grupo5.cliente.rest;

import grupo5.cliente.puertos.AdminOperacionPuerto;
import grupo5.cliente.puertos.DirectorioDeAdministradores;
import grupo5.cliente.vistas.AdminAsignacionesVista;
import grupo5.cliente.vistas.AdminAsignacionesVista.Decidida;
import grupo5.cliente.vistas.AdminAsignacionesVista.Propuesta;
import grupo5.cliente.vistas.AdminCamionesVista;
import grupo5.cliente.vistas.AdminCamionesVista.Camion;
import grupo5.cliente.vistas.AdminDonacionesRegistroVista;
import grupo5.cliente.vistas.AdminDonacionesVista;
import grupo5.cliente.vistas.ConsultaDonaciones;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * La operación del admin desde Logística y Donaciones. Logística da de cada camión la patente, las
 * capacidades y el estado (con los deshabilitados, HB-23); no da nombre, chofer, viaje, motivo ni
 * rendimiento: quedan null y la pantalla no los muestra. Las asignaciones y las donaciones salen de
 * Donaciones (estas, con {@link AdminOperacionDonaciones}).
 */
// @Primary solo para el análisis estático (S9352): la propiedad ya deja un único adaptador activo.
@Component
@Primary
@ConditionalOnProperty(name = "cliente.datos.admin-operacion", havingValue = "rest")
class AdminOperacionRest implements AdminOperacionPuerto {

  private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm", Comun.ES_AR);
  private static final int FURGON = 1500;
  private static final int MEDIANO = 3000;
  private static final String APROBADA = "APROBADA";
  private static final String DESCARTADA = "DESCARTADA";
  private static final Comparator<LocalDateTime> PRIMERO_SIN_FECHA =
      Comparator.nullsFirst(Comparator.naturalOrder());

  private final LogisticaApi logistica;
  private final DonacionesApi donaciones;
  private final Clock reloj;
  private final AdminOperacionRegistro registro;
  private final DirectorioDeAdministradores cuentas;

  /** {@code cuentas} da el nombre de quien hizo cada cambio del historial (R11-10). */
  @Autowired
  AdminOperacionRest(
      LogisticaApi logistica,
      DonacionesApi donaciones,
      Clock reloj,
      ObjectProvider<DirectorioDeAdministradores> cuentas) {
    this(logistica, donaciones, reloj, RestClient.create(), cuentas.getIfAvailable());
  }

  AdminOperacionRest(LogisticaApi logistica, DonacionesApi donaciones, Clock reloj) {
    this(logistica, donaciones, reloj, RestClient.create(), null);
  }

  /** {@code subidas} sube las fotos a la dirección firmada (los tests le pasan uno simulado). */
  AdminOperacionRest(
      LogisticaApi logistica, DonacionesApi donaciones, Clock reloj, RestClient subidas) {
    this(logistica, donaciones, reloj, subidas, null);
  }

  AdminOperacionRest(
      LogisticaApi logistica,
      DonacionesApi donaciones,
      Clock reloj,
      RestClient subidas,
      DirectorioDeAdministradores cuentas) {
    this.logistica = logistica;
    this.donaciones = donaciones;
    this.reloj = reloj;
    this.registro = new AdminOperacionRegistro(donaciones, subidas);
    this.cuentas = cuentas;
  }

  @Override
  public AdminDonacionesRegistroVista registro(String buscar, String donante) {
    return registro.registro(buscar, donante);
  }

  @Override
  public void registrarDonacion(DonacionNueva donacion) {
    registro.registrar(donacion);
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

  @Override
  public AdminDonacionesVista donaciones(ConsultaDonaciones consulta, String accion) {
    return new AdminOperacionDonaciones(
            donaciones, LocalDateTime.now(reloj.withZone(Comun.ARGENTINA)))
        .vista(consulta);
  }

  @Override
  public AdminDonacionesVista.Donacion donacion(String clave, String accion) {
    return new AdminOperacionDonaciones(
            donaciones,
            LocalDateTime.now(reloj.withZone(Comun.ARGENTINA)),
            AutoresDelHistorial.con(cuentas))
        .detalle(clave);
  }

  @Override
  public void marcarVencida(String donacion, String actor, String motivo) {
    donaciones.cambiarEstado(
        UUID.fromString(donacion),
        actor,
        new DonacionesApi.Cambiar(
            "VENCIDA",
            motivo == null || motivo.isBlank() ? null : motivo.trim(),
            null,
            null,
            null,
            null));
  }

  @Override
  public AdminAsignacionesVista asignaciones() {
    LocalDate hoy = LocalDate.now(reloj.withZone(Comun.ARGENTINA));
    Pedidos pedidos = new Pedidos();
    List<Propuesta> pendientes = new ArrayList<>();
    List<Decidida> decididas = new ArrayList<>();
    donaciones.propuestas().stream()
        .sorted(Comparator.comparing(DonacionesApi.Propuesta::fechaCreacion, PRIMERO_SIN_FECHA))
        .forEach(
            p -> {
              UUID necesidad = p.necesidad().id();
              String entidad = pedidos.entidadDe(necesidad);
              String unidad = pedidos.unidadDe(necesidad);
              String pide = pide(p.necesidad(), unidad);
              switch (p.estado() == null ? "" : p.estado()) {
                case APROBADA -> decididas.addFirst(new Decidida(entidad, pide, "Aprobada"));
                case DESCARTADA -> decididas.addFirst(new Decidida(entidad, pide, "Descartada"));
                default -> pendientes.add(propuesta(p, entidad, pide, unidad, hoy));
              }
            });
    String ultima =
        donaciones.ejecuciones().stream()
            .filter(e -> e.fechaEjecucion() != null)
            .max(Comparator.comparing(DonacionesApi.Ejecucion::fechaEjecucion))
            .map(e -> cuando(e.fechaEjecucion(), hoy) + " · " + genero(e))
            .orElse(null);
    return new AdminAsignacionesVista(ultima, pendientes, decididas);
  }

  @Override
  public int correrAsignacion() {
    return donaciones.correrAsignacion().size();
  }

  @Override
  public String decidirPropuesta(String propuesta, boolean aprobar, String motivo) {
    UUID id = UUID.fromString(propuesta);
    Pedidos pedidos = new Pedidos();
    String entidad =
        donaciones.propuestas().stream()
            .filter(p -> id.equals(p.id()))
            .findFirst()
            .map(p -> pedidos.entidadDe(p.necesidad().id()))
            .orElse(null);
    donaciones.decidirPropuesta(
        id,
        new DonacionesApi.Decision(
            aprobar ? APROBADA : DESCARTADA,
            motivo == null || motivo.isBlank() ? null : motivo.trim()));
    return entidad;
  }

  private static Propuesta propuesta(
      DonacionesApi.Propuesta p, String entidad, String pide, String unidad, LocalDate hoy) {
    List<DonacionesApi.Fragmento> fragmentos =
        p.fragmentaciones() == null ? List.of() : p.fragmentaciones();
    int cubierto =
        fragmentos.stream()
            .mapToInt(f -> f.cantidadNecesaria() == null ? 0 : f.cantidadNecesaria())
            .sum();
    int pedido = cantidad(p.necesidad().cantidadNecesitada());
    String cubre =
        fragmentos.stream().map(f -> fragmento(f, unidad)).collect(Collectors.joining("; "));
    String alcance =
        cubierto >= pedido
            ? "Cubre la necesidad completa."
            : "Cubre "
                + cubierto
                + " de "
                + Comun.cantidad(pedido, enUnidad(unidad))
                + "; el resto sigue pendiente.";
    boolean nueva = p.fechaCreacion() != null && p.fechaCreacion().toLocalDate().equals(hoy);
    return new Propuesta(
        p.id().toString(), entidad, pide, cuando(p.fechaCreacion(), hoy), cubre, alcance, nueva);
  }

  /** "Arroz · 25 kg (de Ana Ejemplo)"; sin el nombre si el donante lo ocultó. */
  private static String fragmento(DonacionesApi.Fragmento f, String unidad) {
    DonacionesApi.Donacion d = f.donacionIndependiente();
    String nombre = d == null || d.quienDono() == null ? null : d.quienDono().nombreVisible();
    return (d == null ? "" : d.descripcion())
        + " · "
        + Comun.cantidad(cantidad(f.cantidadNecesaria()), enUnidad(unidad))
        + (nombre == null ? "" : " (de " + nombre + ")");
  }

  /** "Arroz, 25 kg". */
  private static String pide(DonacionesApi.NecesidadResumen necesidad, String unidad) {
    return necesidad.descripcion()
        + ", "
        + Comun.cantidad(cantidad(necesidad.cantidadNecesitada()), enUnidad(unidad));
  }

  /** "hoy, 18:36" o "15 de octubre". */
  private static String cuando(LocalDateTime fecha, LocalDate hoy) {
    if (fecha == null) {
      return "";
    }
    return fecha.toLocalDate().equals(hoy)
        ? "hoy, " + Comun.HORA.format(fecha)
        : Comun.DIA.format(fecha);
  }

  private static String genero(DonacionesApi.Ejecucion ejecucion) {
    int n = cantidad(ejecucion.cantidadPropuestasGeneradas());
    if (n == 0) {
      return "no generó propuestas";
    }
    return n == 1 ? "generó 1 propuesta" : "generó " + n + " propuestas";
  }

  private static int cantidad(Integer valor) {
    return valor == null ? 0 : valor;
  }

  /** Comun.cantidad escribe la unidad de una categoría; la de la necesidad es la misma. */
  private static DonacionesApi.Categoria enUnidad(String unidad) {
    return new DonacionesApi.Categoria(null, null, unidad, null, null, null);
  }

  /** La necesidad y el nombre de la entidad de cada propuesta, pedidos una vez por pantalla. */
  private final class Pedidos {

    private final Map<UUID, Optional<DonacionesApi.Necesidad>> necesidades = new HashMap<>();
    private final Map<UUID, Optional<String>> entidades = new HashMap<>();

    /** Una necesidad que ya no está (404) deja la propuesta sin entidad ni unidad. */
    private Optional<DonacionesApi.Necesidad> necesidad(UUID id) {
      return necesidades.computeIfAbsent(
          id,
          clave -> {
            try {
              return Optional.ofNullable(donaciones.necesidad(clave));
            } catch (HttpClientErrorException.NotFound noEsta) {
              return Optional.empty();
            }
          });
    }

    String unidadDe(UUID necesidad) {
      return necesidad(necesidad).map(DonacionesApi.Necesidad::unidad).orElse(null);
    }

    String entidadDe(UUID necesidad) {
      return necesidad(necesidad)
          .map(DonacionesApi.Necesidad::idEntidad)
          .flatMap(
              id ->
                  entidades.computeIfAbsent(
                      id,
                      clave ->
                          Optional.ofNullable(donaciones.entidad(clave).juridica())
                              .map(DonacionesApi.Persona::razonSocial)))
          .orElse(null);
    }
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
