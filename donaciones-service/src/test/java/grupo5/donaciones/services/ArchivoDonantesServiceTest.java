package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import grupo5.donaciones.dto.donantes.ArchivoInputDTO;
import grupo5.donaciones.dto.donantes.ArchivoOutputDTO;
import grupo5.donaciones.models.entities.donantes.Archivo;
import grupo5.donaciones.models.entities.donantes.EstadoArchivo;
import grupo5.donaciones.models.repositories.IArchivoDonantesRepository;
import grupo5.donaciones.services.impl.ArchivoDonantesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ArchivoDonantesServiceTest {

  private IArchivoDonantesRepository archivoRepository;
  private IImportadorService importadorService;
  private ArchivoDonantesService service;

  @BeforeEach
  void setUp() {
    archivoRepository = mock(IArchivoDonantesRepository.class);
    importadorService = mock(IImportadorService.class);
    service = new ArchivoDonantesService(archivoRepository, importadorService);
  }

  @Test
  void cargarArchivoDonantes_deberiaGuardarArchivoYDispararImportacionAsincronica() {
    ArchivoInputDTO input = new ArchivoInputDTO("/ruta/a/donantes.csv");

    ArchivoOutputDTO resultado = service.cargarArchivoDonantes(input);

    assertNotNull(resultado);
    assertNotNull(resultado.id());
    assertEquals("/ruta/a/donantes.csv", resultado.path());
    assertEquals(EstadoArchivo.PENDIENTE.toString(), resultado.estado());

    verify(archivoRepository).save(any(Archivo.class));
    verify(importadorService).procesarImportacionAsincronica(resultado.id());
  }
}
