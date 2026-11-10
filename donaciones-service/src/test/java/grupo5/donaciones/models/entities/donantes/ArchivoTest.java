package grupo5.donaciones.models.entities.donantes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ArchivoTest {

  @Test
  void finalizarProcesamiento_sinErrores_deberiaQuedarProcesado() {
    Archivo archivo = new Archivo("donantes.csv");
    archivo.marcarComoProcesando();
    archivo.registrarCreada();

    archivo.finalizarProcesamiento();

    assertEquals(EstadoArchivo.PROCESADO, archivo.getEstado());
  }

  @Test
  void finalizarProcesamiento_conErrores_deberiaQuedarProcesadoConErrores() {
    Archivo archivo = new Archivo("donantes.csv");
    archivo.marcarComoProcesando();
    archivo.registrarCreada();
    archivo.registrarError();

    archivo.finalizarProcesamiento();

    assertEquals(EstadoArchivo.PROCESADO_CON_ERRORES, archivo.getEstado());
  }

  @Test
  void nuevo_sinLeerElPadron_noTieneTotalYNingunaFilaProcesada() {
    Archivo archivo = new Archivo("donantes.csv");

    assertNull(archivo.getTotalFilas());
    assertEquals(0, archivo.getProcesadas());
  }

  @Test
  void registrar_cadaResultado_sumaEnSuContadorYEnProcesadas() {
    Archivo archivo = new Archivo("donantes.csv");
    archivo.iniciarConteo(5);

    archivo.registrarCreada();
    archivo.registrarCreada();
    archivo.registrarActualizada();
    archivo.registrarError();

    assertEquals(5, archivo.getTotalFilas());
    assertEquals(2, archivo.getCreadas());
    assertEquals(1, archivo.getActualizadas());
    assertEquals(1, archivo.getConError());
    assertEquals(4, archivo.getProcesadas());
  }
}
