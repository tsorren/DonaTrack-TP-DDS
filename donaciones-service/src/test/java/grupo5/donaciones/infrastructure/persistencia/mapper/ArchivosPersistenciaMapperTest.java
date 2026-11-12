package grupo5.donaciones.infrastructure.persistencia.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import grupo5.donaciones.infrastructure.persistencia.entities.donantes.ArchivoEntity;
import grupo5.donaciones.models.entities.donantes.Archivo;
import grupo5.donaciones.models.entities.donantes.UltimoImportado;
import java.util.List;
import org.junit.jupiter.api.Test;

class ArchivosPersistenciaMapperTest {

  private final ArchivosPersistenciaMapper mapper = new ArchivosPersistenciaMapper();

  @Test
  void ultimos_vanYVuelvenDeLaColumnaEnElMismoOrden_R1101() {
    Archivo archivo = new Archivo("padron.csv");
    archivo.registrarCreada("Ana Lopez");
    archivo.registrarActualizada("Empresa Ejemplo SA");

    ArchivoEntity entity = mapper.toEntity(archivo);
    Archivo leido = mapper.toDomain(entity);

    assertEquals(archivo.getUltimos(), leido.getUltimos());
    assertEquals("ACTUALIZADO\tEmpresa Ejemplo SA\nNUEVO\tAna Lopez", entity.getUltimos());
  }

  @Test
  void ultimos_unNombreConTabuladoresOSaltosNoRompeLaColumna() {
    String columna =
        ArchivosPersistenciaMapper.ultimosAColumna(
            List.of(new UltimoImportado("Ana\tde\nla Paz", UltimoImportado.Resultado.NUEVO)));

    assertEquals(
        List.of(new UltimoImportado("Ana de la Paz", UltimoImportado.Resultado.NUEVO)),
        ArchivosPersistenciaMapper.ultimosDeColumna(columna));
  }

  @Test
  void ultimos_sinDonantesProcesados_quedaNuloYSeLeeVacio() {
    assertNull(ArchivosPersistenciaMapper.ultimosAColumna(List.of()));
    assertEquals(List.of(), ArchivosPersistenciaMapper.ultimosDeColumna(null));
  }
}
