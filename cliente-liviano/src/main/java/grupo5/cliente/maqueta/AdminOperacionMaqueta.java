package grupo5.cliente.maqueta;

import grupo5.cliente.puertos.AdminOperacionPuerto;
import grupo5.cliente.vistas.AdminAsignacionesVista;
import grupo5.cliente.vistas.AdminAsignacionesVista.Decidida;
import grupo5.cliente.vistas.AdminAsignacionesVista.Propuesta;
import grupo5.cliente.vistas.AdminCamionesVista;
import grupo5.cliente.vistas.AdminCamionesVista.Camion;
import grupo5.cliente.vistas.AdminCamionesVista.Chofer;
import grupo5.cliente.vistas.AdminCamionesVista.Rendimiento;
import grupo5.cliente.vistas.AdminCamionesVista.Viaje;
import grupo5.cliente.vistas.AdminDonacionesRegistroVista;
import grupo5.cliente.vistas.AdminDonacionesVista;
import grupo5.cliente.vistas.ConsultaDonaciones;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Los datos de ejemplo de la operación del admin (los que estaban en las plantillas). La pantalla
 * de resultado ({@code accion}) cambia lo que muestra: el camión que se deshabilitó, el que se
 * agregó. Los envíos no guardan nada: en la maqueta los atiende {@code SitioController}.
 */
@Component
@ConditionalOnProperty(
    name = "cliente.datos.admin-operacion",
    havingValue = "maqueta",
    matchIfMissing = true)
class AdminOperacionMaqueta implements AdminOperacionPuerto {

  private static final String DESHABILITADO = "deshabilitado";
  private static final String DISPONIBLE = "disponible";
  private static final String CAMION = "camion";
  private static final String FURGON = "furgon";
  private static final String GRANDE = "3.500 kg · 18 m³";
  private static final String CHICO = "1.500 kg · 9 m³";
  private static final String COMEDOR = "Comedor Barrio Ejemplo";
  private static final String HOGAR = "Hogar Ejemplo Centro";
  private static final String MERENDERO = "Merendero Ejemplo Sur";
  private static final String ESCUELA = "Escuela Ejemplo N.º 3";
  private static final String APROBADA = "Aprobada";
  private static final String COMPLETA = "Cubre la necesidad completa.";
  private static final String HOY = "hoy, 18:36";
  private static final AdminDonacionesRegistroVista.Donante ANA_EJEMPLO =
      new AdminDonacionesRegistroVista.Donante("ana-ejemplo", "Ana Ejemplo", "DNI 20.000.001");
  private static final AdminDonacionesRegistroVista.Donante JUAN_EJEMPLO =
      new AdminDonacionesRegistroVista.Donante("juan-ejemplo", "Juan Ejemplo", "DNI 20.000.002");

  /**
   * Las propuestas y las decididas de ejemplo; la plantilla elige cuáles mostrar según el
   * resultado.
   */
  private static final AdminAsignacionesVista ASIGNACIONES =
      new AdminAsignacionesVista(
          "hoy, 0:00 · generó 2 propuestas",
          List.of(
              new Propuesta(
                  "arroz",
                  COMEDOR,
                  "Arroz, 25 kg",
                  "15 de octubre",
                  "Arroz · 25 kg (de Ana Ejemplo)",
                  COMPLETA,
                  false),
              new Propuesta(
                  "leche",
                  MERENDERO,
                  "Leche en polvo, 12 unidades",
                  "15 de octubre",
                  "Leche en polvo · 10 unidades (de Distribuidora Ejemplo SA)",
                  "Cubre 10 de las 12 unidades; el resto sigue pendiente.",
                  false),
              new Propuesta(
                  "yogur",
                  HOGAR,
                  "Yogur, 24 unidades",
                  HOY,
                  "Yogur · 24 unidades (de Fundación Ejemplo Sur)",
                  COMPLETA,
                  true),
              new Propuesta(
                  "utiles",
                  ESCUELA,
                  "Útiles escolares, 40 unidades",
                  HOY,
                  "Útiles escolares · 30 unidades (de Juan Ejemplo)",
                  "Cubre 30 de las 40 unidades; el resto sigue pendiente.",
                  true)),
          List.of(
              new Decidida(COMEDOR, "Fideos, 20 kg", APROBADA),
              new Decidida(HOGAR, "Aceite, 12 litros", APROBADA),
              new Decidida(MERENDERO, "Azúcar, 15 kg", APROBADA),
              new Decidida(ESCUELA, "Cuadernos, 40 unidades", APROBADA),
              new Decidida(HOGAR, "Ropa de abrigo, 40 prendas", APROBADA),
              new Decidida(COMEDOR, "Galletitas, 5 kg", "Descartada"),
              new Decidida(MERENDERO, "Harina, 10 kg", "Descartada")));

  @Override
  public AdminDonacionesVista donaciones(ConsultaDonaciones consulta, String accion) {
    return AdminOperacionDonaciones.vista(consulta, accion);
  }

  @Override
  public AdminDonacionesVista.Donacion donacion(String clave, String accion) {
    return AdminOperacionDonaciones.detalle(clave, accion);
  }

  @Override
  public void marcarVencida(String donacion, String actor, String motivo) {
    // En la maqueta el envío lo atiende SitioController, sin guardar.
  }

  /** Cualquier búsqueda encuentra a Ana y a Juan, como mostraba la plantilla. */
  @Override
  public AdminDonacionesRegistroVista registro(String buscar, String donante) {
    return new AdminDonacionesRegistroVista(
        List.of(
            ANA_EJEMPLO,
            new AdminDonacionesRegistroVista.Donante(
                "distribuidora", "Distribuidora Ejemplo SA", "CUIT 30-00000001-0"),
            JUAN_EJEMPLO,
            new AdminDonacionesRegistroVista.Donante(
                "pedro-ejemplo", "Pedro Ejemplo", "DNI 20.000.003")),
        buscar == null ? List.of() : List.of(ANA_EJEMPLO, JUAN_EJEMPLO),
        buscar == null ? 0 : 2);
  }

  @Override
  public void registrarDonacion(DonacionNueva donacion) {
    // En la maqueta el envío lo atiende SitioController, sin guardar.
  }

  @Override
  public AdminAsignacionesVista asignaciones() {
    return ASIGNACIONES;
  }

  @Override
  public int correrAsignacion() {
    return 0;
  }

  @Override
  public String decidirPropuesta(String propuesta, boolean aprobar, String motivo) {
    return COMEDOR;
  }

  @Override
  public AdminCamionesVista camiones(String accion) {
    boolean sinCambios = accion.isEmpty() || "error".equals(accion);
    List<Camion> camiones = new ArrayList<>();
    camiones.add(
        new Camion(
            "ab123cd",
            "AB 123 CD",
            "Camión Norte",
            CAMION,
            GRANDE,
            3500,
            "grande",
            "ruta",
            "En ruta a Comedor Barrio Ejemplo",
            null,
            null,
            new Chofer(
                "Martín Ejemplo",
                "ME",
                "+541155550101",
                "11 5555-0101",
                "martin.chofer@ejemplo.org"),
            new Viaje(
                2,
                18,
                42,
                "Salió del depósito hoy a las 14:30",
                "Av. Ejemplo al 2300",
                "Comedor Barrio Ejemplo",
                4,
                2,
                -34.6215,
                -58.4342,
                -34.6092,
                -58.4588,
                75,
                "2.625 de 3.500 kg",
                "13 de 18 m³",
                54,
                List.of("alimentos", "ropa", "escolares")),
            new Rendimiento(
                38, "94 %", "12 min", 1, barras(3, 5, 4, 6, 2, 5, 7, 4, 6, 5, 3, 6, 7, 5, 8)),
            false));
    boolean sur = DESHABILITADO.equals(accion);
    camiones.add(
        new Camion(
            "ac456ef",
            "AC 456 EF",
            "Camión Sur",
            CAMION,
            GRANDE,
            3500,
            "grande",
            sur ? DESHABILITADO : DISPONIBLE,
            "En el depósito desde ayer, 17:10",
            sur ? "Service de frenos. Desde hoy, 18:35." : null,
            "14 de octubre · 5 paradas · 36 km",
            new Chofer(
                "Lucía Ejemplo", "LE", "+541155550102", "11 5555-0102", "lucia.chofer@ejemplo.org"),
            null,
            new Rendimiento(
                41, "97 %", "6 min", 0, barras(4, 6, 5, 7, 6, 4, 5, 8, 6, 7, 5, 6, 4, 7, 6)),
            false));
    camiones.add(
        new Camion(
            "ad789gh",
            "AD 789 GH",
            "Furgón Centro",
            FURGON,
            CHICO,
            1500,
            "chico",
            DISPONIBLE,
            "En el depósito desde hoy, 12:40",
            null,
            "15 de octubre · 3 paradas · 14 km",
            new Chofer(
                "Sofía Ejemplo", "SE", "+541155550103", "11 5555-0103", "sofia.chofer@ejemplo.org"),
            null,
            new Rendimiento(
                22, "91 %", "15 min", 1, barras(2, 3, 4, 2, 5, 3, 4, 3, 2, 4, 5, 3, 4, 2, 4)),
            false));
    camiones.add(
        new Camion(
            "ae012ij",
            "AE 012 IJ",
            "Furgón Oeste",
            FURGON,
            CHICO,
            1500,
            "chico",
            DISPONIBLE,
            "En el depósito desde el 13 de octubre",
            null,
            "13 de octubre · 4 paradas · 21 km",
            new Chofer(
                "Diego Ejemplo", "DE", "+541155550104", "11 5555-0104", "diego.chofer@ejemplo.org"),
            null,
            new Rendimiento(
                17, "100 %", "0 min", 0, barras(1, 2, 3, 2, 3, 1, 2, 4, 3, 2, 3, 2, 1, 3, 2)),
            false));
    boolean este = "habilitado".equals(accion);
    camiones.add(
        new Camion(
            "aa999zz",
            "AA 999 ZZ",
            "Furgón Este",
            FURGON,
            CHICO,
            1500,
            "chico",
            este ? DISPONIBLE : DESHABILITADO,
            "En el depósito desde hoy, 18:35",
            este ? null : "En el taller desde el 12 de octubre.",
            "11 de octubre · 2 paradas · 9 km",
            new Chofer(
                "Carla Ejemplo", "CE", "+541155550105", "11 5555-0105", "carla.chofer@ejemplo.org"),
            null,
            new Rendimiento(
                9, "89 %", "18 min", 2, barras(3, 2, 4, 3, 2, 3, 4, 2, 3, 1, 0, 0, 0, 0, 0)),
            false));
    if ("agregado".equals(accion)) {
      camiones.add(
          new Camion(
              "af345kl",
              "AF 345 KL",
              "Camión Mediano",
              CAMION,
              "2.000 kg · 12 m³",
              2000,
              "mediano",
              DISPONIBLE,
              "En el depósito, recién agregado",
              null,
              null,
              null,
              null,
              null,
              true));
    }
    return new AdminCamionesVista(
        camiones,
        sinCambios ? "Al 15 de octubre de 2026, 18:30." : "Al 15 de octubre de 2026, 18:35.",
        127,
        "16 de septiembre al 15 de octubre");
  }

  @Override
  public String agregarCamion(String patente, float capacidadKg, float volumen, float altura) {
    return "af345kl";
  }

  @Override
  public String cambiarCamion(String camion, boolean habilitado, String motivo) {
    return camion;
  }

  private static List<Integer> barras(Integer... valores) {
    return List.of(valores);
  }
}
