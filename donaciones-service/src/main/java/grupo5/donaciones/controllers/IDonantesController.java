package grupo5.donaciones.controllers;

import grupo5.donaciones.dto.donantes.ArchivoOutputDTO;
import grupo5.donaciones.dto.donantes.DonanteInputDTO;
import grupo5.donaciones.dto.donantes.DonanteOutputDTO;
import grupo5.donaciones.dto.donantes.FilaConErrorDTO;
import grupo5.donaciones.dto.donantes.FiltroArchivos;
import grupo5.donaciones.dto.donantes.FiltroDonantes;
import grupo5.donaciones.dto.donantes.PreferenciasDonanteDTO;
import grupo5.donaciones.dto.publico.EntidadParaDonanteDTO;
import grupo5.donaciones.services.ParametrosListado;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface IDonantesController {
  ResponseEntity<DonanteOutputDTO> crearDonante(DonanteInputDTO dto);

  ResponseEntity<List<DonanteOutputDTO>> listarDonantes(
      FiltroDonantes filtro, ParametrosListado listado);

  ResponseEntity<DonanteOutputDTO> obtenerDonante(UUID id);

  ResponseEntity<List<EntidadParaDonanteDTO>> listarEntidades(
      UUID id, String nombre, UUID categoriaId, boolean soloDonadas, Integer page, Integer size);

  ResponseEntity<DonanteOutputDTO> actualizarPreferencias(UUID id, PreferenciasDonanteDTO dto);

  ResponseEntity<Void> eliminarDonante(UUID id);

  ResponseEntity<ArchivoOutputDTO> cargarArchivoDonantes(MultipartFile archivo) throws IOException;

  ResponseEntity<ArchivoOutputDTO> obtenerArchivoDonantes(UUID id);

  ResponseEntity<List<ArchivoOutputDTO>> listarArchivosDonantes(
      FiltroArchivos filtro, ParametrosListado listado);

  ResponseEntity<List<FilaConErrorDTO>> obtenerFilasConError(UUID id, Integer page, Integer size);
}
