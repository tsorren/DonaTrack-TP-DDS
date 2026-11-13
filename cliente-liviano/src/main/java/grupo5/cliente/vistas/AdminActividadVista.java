package grupo5.cliente.vistas;

import grupo5.cliente.vistas.AdminDonantesVista.Opcion;
import java.util.ArrayList;
import java.util.List;

/**
 * El registro de actividad de administración (R11-26), de a una página. {@code al}, cuándo se leyó
 * ("12 de noviembre de 2026, 18:34"); {@code acciones}, la página que se muestra, ya filtrada y con
 * su total (el de Donaciones); {@code consulta}, lo que pidió la dirección; {@code filtrada}, si a
 * Donaciones se le pidió con algún filtro (una búsqueda que no encontró cuentas no cuenta); {@code
 * avisos}, lo que no se entendió o no se pudo aplicar, en palabras; {@code quienes}, {@code grupos}
 * y {@code fechas}, las opciones de cada filtro (la primera es "todas", valor '').
 */
public record AdminActividadVista(
    String al,
    Tramo<Fila> acciones,
    ConsultaActividad consulta,
    boolean filtrada,
    List<String> avisos,
    List<Opcion> quienes,
    List<Opcion> grupos,
    List<Opcion> fechas) {

  /** Las opciones de Acción (ficha §6). */
  public static final List<Opcion> GRUPOS =
      List.of(
          new Opcion("", "Todas"),
          new Opcion("propuestas", "Propuestas y asignación"),
          new Opcion("donaciones", "Donaciones"),
          new Opcion("donantes", "Donantes e importaciones"),
          new Opcion("organizaciones", "Organizaciones"),
          new Opcion("cuentas", "Cuentas de administración"),
          new Opcion("camiones", "Camiones"));

  public AdminActividadVista {
    avisos = List.copyOf(avisos);
    quienes = List.copyOf(quienes);
    grupos = List.copyOf(grupos);
    fechas = List.copyOf(fechas);
  }

  /**
   * La vista con las opciones de siempre. {@code deQuien} es el nombre de la cuenta elegida en la
   * dirección ("Bruno Díaz"), para que la pastilla diga "De: Bruno Díaz"; null si no hay una.
   */
  public static AdminActividadVista de(
      String al,
      Tramo<Fila> acciones,
      ConsultaActividad consulta,
      boolean filtrada,
      String deQuien,
      List<String> avisos) {
    List<Opcion> quienes =
        new ArrayList<>(
            List.of(new Opcion("", "Todos"), new Opcion(ConsultaActividad.YO, "Solo lo mío")));
    if (consulta.deUnaCuenta()) {
      quienes.add(
          new Opcion(
              consulta.autor(),
              "De: " + (deQuien == null ? "una cuenta de administración" : deQuien)));
    }
    return new AdminActividadVista(
        al,
        acciones,
        consulta,
        filtrada,
        avisos,
        quienes,
        GRUPOS,
        AdminImportacionesVista.subidas(consulta.comoFechas()));
  }

  /**
   * Una acción. {@code cuando} ("12 nov 2026, 18:20") y {@code cuandoIso}, para {@code time};
   * {@code quien}, el nombre de la cuenta (con "(vos)" si es la propia) o lo que se sabe de ella;
   * {@code filtrarPor}, el id de la cuenta para ver solo sus acciones (null si la cuenta ya no
   * existe); {@code que}, la frase en pasado; {@code accion} ("Ver la donación"), {@code enlace} y
   * {@code lectorEnlace}, null si no hay a dónde ir.
   */
  public record Fila(
      String cuando,
      String cuandoIso,
      String quien,
      String filtrarPor,
      String que,
      String accion,
      String enlace,
      String lectorEnlace) {}
}
