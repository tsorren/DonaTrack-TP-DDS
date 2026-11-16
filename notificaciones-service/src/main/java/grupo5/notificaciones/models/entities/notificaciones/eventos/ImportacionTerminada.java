package grupo5.notificaciones.models.entities.notificaciones.eventos;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.notificaciones.TipoEvento;
import grupo5.notificaciones.models.entities.personas.Persona;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import lombok.Getter;

/**
 * Terminó o se cortó una importación de donantes (R11-30): un solo aviso, para la persona de la
 * Administración. El mensaje no lleva el nombre del archivo (puede traer datos personales): lleva
 * cuándo se subió y las cifras que dio Donaciones.
 */
@Getter
public class ImportacionTerminada extends EventoNotificable {

  private static final Locale ES_AR = Locale.forLanguageTag("es-AR");
  private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM");
  private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

  private final boolean cortada;
  private final Cifras cifras;
  private final LocalDateTime subidoEn;

  /** Las filas leídas y cómo terminó cada una; las da Donaciones, acá no se calcula ninguna. */
  public record Cifras(int procesadas, int creadas, int actualizadas, int conError) {}

  public ImportacionTerminada(
      Persona administracion,
      boolean cortada,
      Cifras cifras,
      LocalDateTime subidoEn,
      LocalDateTime fecha) {
    super(administracion, fecha);
    if (cifras == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    this.cortada = cortada;
    this.cifras = cifras;
    this.subidoEn = subidoEn;
  }

  @Override
  public TipoEvento tipoEvento() {
    return cortada ? TipoEvento.IMPORTACION_CORTADA : TipoEvento.IMPORTACION_TERMINADA;
  }

  @Override
  protected List<Notificacion> crearNotificaciones() {
    return List.of(new Notificacion(getPersona().getId(), mensaje(), getFecha()));
  }

  String mensaje() {
    String importacion =
        subidoEn == null
            ? "La importación de donantes"
            : "La importación de donantes que se subió el "
                + subidoEn.format(DIA)
                + " a las "
                + subidoEn.format(HORA);
    if (cortada) {
      return importacion + cortadaDespuesDe();
    }
    return importacion
        + " terminó. "
        + (cifras.procesadas() == 1 ? "Se leyó " : "Se leyeron ")
        + cantidad(cifras.procesadas(), "fila", "filas")
        + ": "
        + resultadoPorFila();
  }

  private String cortadaDespuesDe() {
    int leidas = cifras.procesadas();
    if (leidas == 0) {
      return " no se pudo leer. No se importó ninguna fila.";
    }
    return " se cortó después de leer "
        + cantidad(leidas, "fila", "filas")
        + (leidas == 1 ? ". Esa fila quedó guardada." : ". Esas filas quedaron guardadas.");
  }

  private String resultadoPorFila() {
    String nuevos = cantidad(cifras.creadas(), "donante nuevo", "donantes nuevos");
    String actualizados = cantidad(cifras.actualizadas(), "actualizado", "actualizados");
    if (cifras.conError() == 0) {
      return nuevos + " y " + actualizados + ". Todas las filas se importaron sin errores.";
    }
    return nuevos + ", " + actualizados + " y " + numero(cifras.conError()) + " con error.";
  }

  private static String cantidad(int n, String singular, String plural) {
    return numero(n) + " " + (n == 1 ? singular : plural);
  }

  /**
   * Con punto de miles, como pide el tono del sitio ("1.234 filas"). Explícito: el formato de
   * {@code es-AR} no agrupa los números de cuatro cifras.
   */
  private static String numero(int n) {
    DecimalFormatSymbols simbolos = DecimalFormatSymbols.getInstance(ES_AR);
    simbolos.setGroupingSeparator('.');
    return new DecimalFormat("#,##0", simbolos).format(n);
  }
}
