package grupo5.donaciones.services;

import static grupo5.donaciones.models.entities.donantes.EstadoArchivo.PROCESADA;

import grupo5.donaciones.models.entities.donantes.Archivo;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.repositories.IArchivoDonantesRepository;
import grupo5.donaciones.models.repositories.IDonantesRepository;
import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ArchivoDonantesService {

  private static final Logger log = LoggerFactory.getLogger(ArchivoDonantesService.class);

  private final IDonantesRepository donantesRepository;
  private final IArchivoDonantesRepository archivoDonantesRepository;

  public ArchivoDonantesService(
      IDonantesRepository donantesRepository,
      IArchivoDonantesRepository archivoDonantesRepository) {
    this.donantesRepository = donantesRepository;
    this.archivoDonantesRepository = archivoDonantesRepository;
  }

  public Archivo registrarArchivoInicial(String path) {
    Archivo archivo = new Archivo(path);
    return archivoDonantesRepository.save(archivo);
  }

  @Async
  public void procesarArchivoMasivo(Archivo archivoAuditoria) {
    log.info("Iniciando lectura de fondo para el archivo en: {}", archivoAuditoria.getPath());

    List<Donante> donantesExistentes = donantesRepository.findAll().stream().toList();
    int filasContadas = 0;

    try (BufferedReader br = Files.newBufferedReader(Paths.get(archivoAuditoria.getPath()))) {
      String linea;
      boolean esEncabezado = true;

      while ((linea = br.readLine()) != null) {
        if (esEncabezado) {
          esEncabezado = false; // Salteamos la primera fila de títulos del CSV
          continue;
        }

        String[] columnas = linea.split(","); // O ";" según el formato del CSV de prueba
        if (columnas.length >= 4) {
          String tipoPersona = columnas[0].trim();
          String tipoDoc = columnas[1].trim();
          String documento = columnas[2].trim();
          String canalContacto = columnas[3].trim();

          donantesExistentes.stream()
              .filter(
                  d -> d.getPersona() != null && documento.equals(d.getPersona().getDocumento()))
              .findFirst()
              .ifPresentOrElse(
                  donante -> {
                    donantesRepository.save(donante);
                  },
                  () -> {});
        }

        filasContadas++;
        if (filasContadas % 5000 == 0) {
          log.info("Progreso de importación: {} filas leídas...", filasContadas);
        }
      }

      archivoAuditoria.setEstado(PROCESADA);
      archivoDonantesRepository.save(archivoAuditoria);
      log.info("¡Archivo procesado con éxito! Total: {} filas registradas.", filasContadas);

    } catch (Exception e) {

      log.error("Error al procesar el lote masivo de donantes: {}", e.getMessage());
    }
  }
}
