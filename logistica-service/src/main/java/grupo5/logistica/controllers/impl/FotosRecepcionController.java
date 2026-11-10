package grupo5.logistica.controllers.impl;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.responses.ErrorResponse;
import grupo5.logistica.dto.entregas.FotoRecepcionSubidaDTO;
import grupo5.logistica.services.FotoRecepcionRechazadaException;
import grupo5.logistica.services.impl.FotosRecepcionService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Fotos de recepción en almacenamiento de objetos (ADR-E6-08, HB-10): subida multipart y URL
 * estable que redirige a una URL firmada. El {@code PATCH /api/entregas/{id}/fotos} con URLs
 * externas sigue en {@link EntregasController}.
 */
@RestController
@RequestMapping("/api/entregas/{id}/fotos")
public class FotosRecepcionController {

  private final FotosRecepcionService fotosRecepcionService;

  public FotosRecepcionController(FotosRecepcionService fotosRecepcionService) {
    this.fotosRecepcionService = fotosRecepcionService;
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<List<FotoRecepcionSubidaDTO>> subir(
      @PathVariable("id") UUID id, @RequestPart("foto") List<MultipartFile> fotos) {
    List<byte[]> contenidos = fotos.stream().map(FotosRecepcionController::bytes).toList();
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(fotosRecepcionService.subir(id, contenidos));
  }

  @GetMapping("/{fotoId}")
  public ResponseEntity<Void> leer(
      @PathVariable("id") UUID id, @PathVariable("fotoId") UUID fotoId) {
    return ResponseEntity.status(HttpStatus.FOUND)
        .location(URI.create(fotosRecepcionService.urlFirmada(id, fotoId)))
        .build();
  }

  @ExceptionHandler(FotoRecepcionRechazadaException.class)
  public ResponseEntity<ErrorResponse> fotoRechazada(FotoRecepcionRechazadaException ex) {
    HttpStatus status =
        switch (ex.getMotivo()) {
          case DEMASIADO_GRANDE -> HttpStatus.CONTENT_TOO_LARGE;
          case TIPO_NO_PERMITIDO -> HttpStatus.UNSUPPORTED_MEDIA_TYPE;
          case ILEGIBLE -> HttpStatus.BAD_REQUEST;
        };
    return ResponseEntity.status(status).body(new ErrorResponse(ex, ex.getMotivo().detalle()));
  }

  private static byte[] bytes(MultipartFile foto) {
    if (foto.isEmpty()) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
    try {
      return foto.getBytes();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
