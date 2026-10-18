package grupo5.donaciones.models.entities.donantes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ArchivoTest {

  @Test
  void finalizarProcesamiento_sinErrores_deberiaQuedarProcesado() {
    Archivo archivo = new Archivo("donantes.csv");
    archivo.marcarComoProcesando();

    archivo.finalizarProcesamiento(0);

    assertEquals(EstadoArchivo.PROCESADO, archivo.getEstado());
  }

  @Test
  void finalizarProcesamiento_conErrores_deberiaQuedarProcesadoConErrores() {
    Archivo archivo = new Archivo("donantes.csv");
    archivo.marcarComoProcesando();

    archivo.finalizarProcesamiento(3);

    assertEquals(EstadoArchivo.PROCESADO_CON_ERRORES, archivo.getEstado());
  }
}
