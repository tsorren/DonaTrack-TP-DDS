package grupo5.cliente.maqueta;

import grupo5.cliente.vistas.AdminDonacionesVista;
import grupo5.cliente.vistas.AdminDonacionesVista.Bien;
import grupo5.cliente.vistas.AdminDonacionesVista.Criterio;
import grupo5.cliente.vistas.AdminDonacionesVista.Donacion;
import grupo5.cliente.vistas.ConsultaDonaciones;
import grupo5.cliente.vistas.Tramo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Las donaciones de ejemplo del admin (las que estaban en admin-donaciones.html), con su historial
 * escrito como lo escribía la plantilla. Con {@code vencida}, la leche en polvo ya está vencida.
 */
final class AdminOperacionDonaciones {

  private static final String CENTRAL = "Depósito Central";
  private static final String SUR = "Depósito Sur";
  private static final String NUEVO = "Nuevo";
  private static final String USADO = "Usado";
  private static final String DISTRIBUIDORA = "Distribuidora Ejemplo SA";
  private static final String FUNDACION = "Fundación Ejemplo Sur";
  private static final String JUAN = "Juan Ejemplo";
  private static final String ANA = "Ana Ejemplo";
  private static final String COMEDOR = "Comedor Barrio Ejemplo";
  private static final String HOGAR = "Hogar Ejemplo Centro";
  private static final String DEPOSITO = "deposito";
  private static final String ASIGNADA = "asignada";
  private static final String CATORCE = "14 de octubre";
  private static final String DOCE = "12 de octubre";
  private static final String DIEZ_KG = "10 kg";

  private static final Map<String, List<String>> ORDENES =
      Map.of(
          "reciente",
          List.of(
              "pan",
              "yogur",
              "queso",
              "arroz",
              "leche",
              "frazadas",
              "fideos",
              "utiles",
              "aceite",
              "azucar",
              "cuadernos",
              "abrigo",
              "lentejas",
              "panales"),
          "antigua",
          List.of(
              "panales",
              "lentejas",
              "abrigo",
              "cuadernos",
              "azucar",
              "aceite",
              "utiles",
              "fideos",
              "frazadas",
              "leche",
              "arroz",
              "queso",
              "yogur",
              "pan"),
          "nombre",
          List.of(
              "aceite",
              "arroz",
              "azucar",
              "cuadernos",
              "fideos",
              "frazadas",
              "leche",
              "lentejas",
              "pan",
              "panales",
              "queso",
              "abrigo",
              "utiles",
              "yogur"),
          "donante",
          List.of(
              "arroz",
              "aceite",
              "abrigo",
              "leche",
              "queso",
              "fideos",
              "lentejas",
              "yogur",
              "frazadas",
              "azucar",
              "panales",
              "pan",
              "utiles",
              "cuadernos"));

  private AdminOperacionDonaciones() {}

  /**
   * De a 5 por página en la maqueta (en el sistema, de a 20): con sus 14 donaciones de ejemplo, el
   * paginador provisorio se ve y se puede probar.
   */
  static final int POR_PAGINA = 5;

  private static final List<Criterio> CRITERIOS =
      List.of(
          new Criterio("", "Vencen antes", "Primero, las del depósito que vencen antes."),
          new Criterio(
              "reciente",
              "Registradas más recientes",
              "Primero, las registradas más recientemente."),
          new Criterio(
              "antigua", "Registradas más antiguas", "Primero, las registradas hace más tiempo."),
          new Criterio(
              "nombre",
              "Donación, de la A a la Z",
              "Por el nombre de la donación, de la A a la Z."),
          new Criterio(
              "donante",
              "Donante, de la A a la Z",
              "Por el nombre de quien donó, de la A a la Z."));

  /**
   * La página pedida de las donaciones de ejemplo, filtradas y ordenadas como lo hará el sistema
   * (sin la recién registrada, que es la de Registrar donación).
   */
  static AdminDonacionesVista vista(ConsultaDonaciones pedida, String accion) {
    ConsultaDonaciones consulta =
        new ConsultaDonaciones(
            pedida.estado(),
            pedida.pronto(),
            AdminDonacionesVista.elegido(pedida.orden(), CRITERIOS),
            pedida.pagina());
    List<Donacion> todas = todas(accion).stream().filter(d -> !d.nueva()).toList();
    List<Donacion> ordenadas =
        consulta.orden().isEmpty()
            ? todas
            : ORDENES.get(consulta.orden()).stream()
                .map(clave -> todas.stream().filter(d -> d.clave().equals(clave)).findFirst())
                .flatMap(Optional::stream)
                .toList();
    List<Donacion> filtradas =
        ordenadas.stream()
            .filter(d -> !consulta.pronto() || d.prontaAVencer())
            .filter(d -> consulta.estado().isEmpty() || consulta.estado().equals(d.estado()))
            .toList();
    return new AdminDonacionesVista(
        Tramo.de(filtradas, consulta.pagina(), POR_PAGINA),
        consulta,
        CRITERIOS,
        true,
        null,
        "7 en el depósito · 4 asignadas · 1 en camino · 9 entregas confirmadas en octubre. Al 15"
            + " de octubre de 2026, 18:30.");
  }

  /** La donación de ejemplo con esa clave; si no hay, la primera (la leche en polvo). */
  static Donacion detalle(String clave, String accion) {
    List<Donacion> todas = todas(accion);
    return todas.stream().filter(d -> d.clave().equals(clave)).findFirst().orElse(todas.get(0));
  }

  private static List<Donacion> todas(String accion) {
    boolean vencida = "vencida".equals(accion);
    List<Donacion> todas = new ArrayList<>();
    todas.add(
        new Fila("leche", "Leche en polvo", "10 unidades", "10 u", DISTRIBUIDORA, "10 de octubre")
            .en(
                vencida ? "vencida" : DEPOSITO,
                vencida ? "17 de octubre" : "17 de octubre, en 2 días",
                NUEVO,
                CENTRAL)
            .donacion());
    todas.add(
        new Fila("yogur", "Yogur", "24 unidades", "24 u", FUNDACION, "13 de octubre")
            .en(DEPOSITO, "19 de octubre, en 4 días", NUEVO, CENTRAL)
            .donacion());
    todas.add(
        new Fila("pan", "Pan lactal", "15 unidades", "15 u", JUAN, CATORCE)
            .en(DEPOSITO, "20 de octubre, en 5 días", NUEVO, CENTRAL)
            .donacion());
    todas.add(
        new Fila("queso", "Queso", "6 kg", "6 kg", DISTRIBUIDORA, DOCE)
            .en(DEPOSITO, "21 de octubre, en 6 días", NUEVO, SUR)
            .donacion());
    todas.add(
        new Fila("arroz", "Arroz", "25 kg", "25 kg", ANA, "11 de octubre")
            .en(DEPOSITO, null, NUEVO, CENTRAL)
            .donacion());
    todas.add(
        new Fila("frazadas", "Frazadas", "12 unidades", "12 u", FUNDACION, "9 de octubre")
            .en(DEPOSITO, null, USADO, SUR)
            .donacion());
    todas.add(
        new Fila("utiles", "Útiles escolares", "30 unidades", "30 u", JUAN, "7 de octubre")
            .en(DEPOSITO, null, NUEVO, CENTRAL)
            .donacion());
    todas.add(
        new Fila("fideos", "Fideos", "20 kg", "20 kg", DISTRIBUIDORA, "8 de octubre")
            .en(ASIGNADA, null, NUEVO, CENTRAL)
            .hacia(COMEDOR, CATORCE, null, null)
            .donacion());
    todas.add(
        new Fila("aceite", "Aceite", "12 litros", "12 l", ANA, "6 de octubre")
            .en(ASIGNADA, null, NUEVO, CENTRAL)
            .hacia(HOGAR, CATORCE, null, null)
            .donacion());
    todas.add(
        new Fila("azucar", "Azúcar", "15 kg", "15 kg", FUNDACION, "5 de octubre")
            .en(ASIGNADA, null, NUEVO, SUR)
            .hacia("Merendero Ejemplo Sur", "13 de octubre", null, null)
            .donacion());
    todas.add(
        new Fila("cuadernos", "Cuadernos", "40 unidades", "40 u", JUAN, "3 de octubre")
            .en(ASIGNADA, null, NUEVO, CENTRAL)
            .hacia("Escuela Ejemplo N.º 3", DOCE, null, null)
            .donacion());
    todas.add(
        new Fila("abrigo", "Ropa de abrigo", "40 prendas", "40 prendas", ANA, "2 de octubre")
            .en("camino", null, USADO, CENTRAL)
            .hacia(HOGAR, DOCE, "hoy, 14:30", null)
            .donacion());
    todas.add(
        new Fila("lentejas", "Lentejas", DIEZ_KG, DIEZ_KG, DISTRIBUIDORA, "1 de octubre")
            .en("entregada", null, NUEVO, CENTRAL)
            .hacia(COMEDOR, "6 de octubre", "8 de octubre", "8 de octubre")
            .donacion());
    todas.add(
        new Fila("panales", "Pañales", "6 paquetes", "6 paq.", FUNDACION, "30 de septiembre")
            .en("entregada", null, NUEVO, SUR)
            .hacia(HOGAR, "3 de octubre", "7 de octubre", "7 de octubre")
            .donacion());
    Fila caja =
        new Fila("caja", "Caja de alimentos", "3 bienes", "3 bienes", ANA, "15 de octubre")
            .en(DEPOSITO, null, NUEVO, CENTRAL);
    todas.add(
        new Donacion(
            caja.clave,
            caja.que,
            caja.cantidad,
            caja.corto,
            caja.donante,
            caja.registrada,
            caja.estado,
            null,
            false,
            caja.bien,
            caja.deposito,
            null,
            true,
            List.of(
                new Bien("Arroz", DIEZ_KG, NUEVO, null),
                new Bien("Fideos", DIEZ_KG, NUEVO, null),
                new Bien("Aceite", "10 litros", NUEVO, null)),
            caja.historial()));
    return todas;
  }

  /** Arma una fila de ejemplo con su historial, como lo escribía la plantilla. */
  private static final class Fila {

    private static final String LEO = "Leo, de Administración.";

    private final String clave;
    private final String que;
    private final String cantidad;
    private final String corto;
    private final String donante;
    private final String registrada;
    private String estado;
    private String vence;
    private String bien;
    private String deposito;
    private String entidad;
    private String asignadaEl;
    private String salioEl;
    private String entregadaEl;

    Fila(
        String clave,
        String que,
        String cantidad,
        String corto,
        String donante,
        String registrada) {
      this.clave = clave;
      this.que = que;
      this.cantidad = cantidad;
      this.corto = corto;
      this.donante = donante;
      this.registrada = registrada;
    }

    Fila en(String estado, String vence, String bien, String deposito) {
      this.estado = estado;
      this.vence = vence;
      this.bien = bien;
      this.deposito = deposito;
      return this;
    }

    Fila hacia(String entidad, String asignadaEl, String salioEl, String entregadaEl) {
      this.entidad = entidad;
      this.asignadaEl = asignadaEl;
      this.salioEl = salioEl;
      this.entregadaEl = entregadaEl;
      return this;
    }

    List<String> historial() {
      List<String> historial = new ArrayList<>();
      historial.add(registrada + ": llegó al " + deposito + ". La registró " + LEO);
      if (asignadaEl != null) {
        historial.add(asignadaEl + ": quedó asignada a " + entidad + ". La aprobó " + LEO);
      }
      if (salioEl != null) {
        historial.add(salioEl + ": salió en camino.");
      }
      if (entregadaEl != null) {
        historial.add(entregadaEl + ": " + entidad + " confirmó que la recibió.");
      }
      if ("vencida".equals(estado)) {
        historial.add(
            "15 de octubre, 18:35: la marcaste como vencida. Dejó de estar disponible para"
                + " asignar.");
      }
      return historial;
    }

    Donacion donacion() {
      return new Donacion(
          clave,
          que,
          cantidad,
          corto,
          donante,
          registrada,
          estado,
          vence,
          vence != null,
          bien,
          deposito,
          entidad,
          false,
          null,
          historial());
    }
  }
}
