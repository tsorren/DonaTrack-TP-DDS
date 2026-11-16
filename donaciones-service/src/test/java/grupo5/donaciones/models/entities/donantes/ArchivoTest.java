package grupo5.donaciones.models.entities.donantes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
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

  @Test
  void registrar_recuerdaLosUltimosCincoDonantes_elMasRecientePrimero_R1101() {
    Archivo archivo = new Archivo("donantes.csv");
    for (int i = 1; i <= 6; i++) {
      if (i % 2 == 0) {
        archivo.registrarActualizada("Donante " + i);
      } else {
        archivo.registrarCreada("Donante " + i);
      }
    }

    assertEquals(
        List.of(
            new UltimoImportado("Donante 6", UltimoImportado.Resultado.ACTUALIZADO),
            new UltimoImportado("Donante 5", UltimoImportado.Resultado.NUEVO),
            new UltimoImportado("Donante 4", UltimoImportado.Resultado.ACTUALIZADO),
            new UltimoImportado("Donante 3", UltimoImportado.Resultado.NUEVO),
            new UltimoImportado("Donante 2", UltimoImportado.Resultado.ACTUALIZADO)),
        archivo.getUltimos());
    assertEquals(3, archivo.getCreadas());
    assertEquals(3, archivo.getActualizadas());
  }

  @Test
  void registrarError_noEntraEntreLosUltimos() {
    Archivo archivo = new Archivo("donantes.csv");
    archivo.registrarCreada("Ana");
    archivo.registrarError();

    assertEquals(
        List.of(new UltimoImportado("Ana", UltimoImportado.Resultado.NUEVO)), archivo.getUltimos());
  }
}
