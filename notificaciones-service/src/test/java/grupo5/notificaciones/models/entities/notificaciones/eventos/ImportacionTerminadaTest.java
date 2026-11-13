package grupo5.notificaciones.models.entities.notificaciones.eventos;

import static org.assertj.core.api.Assertions.assertThat;

import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.notificaciones.TipoEvento;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.entities.personas.TipoPersona;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** El aviso de importación terminada de la Administración (R11-30, ficha §6). */
class ImportacionTerminadaTest {

  private static final LocalDateTime SUBIDO = LocalDateTime.of(2026, 11, 12, 18, 30);
  private static final LocalDateTime FIN = LocalDateTime.of(2026, 11, 12, 18, 45);
  private final Persona administracion =
      new Persona(UUID.randomUUID(), new ArrayList<>(), "Administración", TipoPersona.HUMANA);

  private Notificacion unica(ImportacionTerminada evento) {
    List<Notificacion> notificaciones = evento.generarNotificaciones();
    assertThat(notificaciones).hasSize(1);
    return notificaciones.get(0);
  }

  @Test
  void terminadaSinErrores_avisaALaAdministracionConLasCifrasYLaReferencia() {
    UUID archivoId = UUID.randomUUID();
    ImportacionTerminada evento =
        new ImportacionTerminada(
            administracion,
            false,
            new ImportacionTerminada.Cifras(20000, 19850, 150, 0),
            SUBIDO,
            FIN);
    evento.asociarReferencia(archivoId);

    Notificacion aviso = unica(evento);

    assertThat(aviso.getPersonaId()).isEqualTo(administracion.getId());
    assertThat(aviso.getTipoEvento()).isEqualTo(TipoEvento.IMPORTACION_TERMINADA.name());
    assertThat(aviso.getReferenciaId()).isEqualTo(archivoId);
    assertThat(aviso.getMensaje())
        .isEqualTo(
            "La importación de donantes que se subió el 12/11 a las 18:30 terminó. Se leyeron"
                + " 20.000 filas: 19.850 donantes nuevos y 150 actualizados. Todas las filas se"
                + " importaron sin errores.");
  }

  @Test
  void terminadaConErrores_diceCuantasFilasTuvieronError() {
    ImportacionTerminada evento =
        new ImportacionTerminada(
            administracion,
            false,
            new ImportacionTerminada.Cifras(20000, 19800, 150, 50),
            SUBIDO,
            FIN);

    assertThat(unica(evento).getMensaje())
        .isEqualTo(
            "La importación de donantes que se subió el 12/11 a las 18:30 terminó. Se leyeron"
                + " 20.000 filas: 19.800 donantes nuevos, 150 actualizados y 50 con error.");
    assertThat(evento.tipoEvento()).isEqualTo(TipoEvento.IMPORTACION_TERMINADA);
  }

  @Test
  void cortadaConFilasLeidas_diceQueEsasFilasQuedaronGuardadas() {
    ImportacionTerminada evento =
        new ImportacionTerminada(
            administracion, true, new ImportacionTerminada.Cifras(1234, 1200, 30, 4), SUBIDO, FIN);

    Notificacion aviso = unica(evento);

    assertThat(aviso.getTipoEvento()).isEqualTo(TipoEvento.IMPORTACION_CORTADA.name());
    assertThat(aviso.getMensaje())
        .isEqualTo(
            "La importación de donantes que se subió el 12/11 a las 18:30 se cortó después de"
                + " leer 1.234 filas. Esas filas quedaron guardadas.");
  }

  @Test
  void cortadaSinLeerNinguna_diceQueNoSeImportoNada() {
    ImportacionTerminada evento =
        new ImportacionTerminada(
            administracion, true, new ImportacionTerminada.Cifras(0, 0, 0, 0), SUBIDO, FIN);

    assertThat(unica(evento).getMensaje())
        .isEqualTo(
            "La importación de donantes que se subió el 12/11 a las 18:30 no se pudo leer. No se"
                + " importó ninguna fila.");
  }

  @Test
  void sinFechaDeSubida_noLaInventa() {
    ImportacionTerminada evento =
        new ImportacionTerminada(
            administracion, false, new ImportacionTerminada.Cifras(1, 1, 0, 0), null, FIN);

    assertThat(unica(evento).getMensaje())
        .isEqualTo(
            "La importación de donantes terminó. Se leyó 1 fila: 1 donante nuevo y 0"
                + " actualizados. Todas las filas se importaron sin errores.");
  }
}
