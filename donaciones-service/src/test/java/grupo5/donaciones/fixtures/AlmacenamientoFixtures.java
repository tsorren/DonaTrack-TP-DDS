package grupo5.donaciones.fixtures;

import grupo5.donaciones.config.AlmacenamientoProperties;
import grupo5.donaciones.infrastructure.almacenamiento.StorageEnMemoria;
import grupo5.donaciones.models.ports.StoragePort;
import grupo5.donaciones.services.impl.FotosBienesService;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.springframework.util.unit.DataSize;

/** Propiedades de almacenamiento con los mismos valores por defecto que la aplicación. */
public final class AlmacenamientoFixtures {

  public static final String BUCKET_PADRONES = "donaciones-padrones";
  public static final String BUCKET_FOTOS = "donaciones-fotos-bienes";

  private AlmacenamientoFixtures() {}

  public static AlmacenamientoProperties propiedades() {
    return new AlmacenamientoProperties(
        BUCKET_PADRONES,
        BUCKET_FOTOS,
        DataSize.ofMegabytes(50),
        DataSize.ofMegabytes(10),
        List.of("image/jpeg", "image/png", "image/webp"),
        Duration.ofMinutes(15),
        Duration.ofHours(2));
  }

  /** Servicio de fotos real sobre un storage en memoria vacío. */
  public static FotosBienesService fotosBienes() {
    return fotosBienes(new StorageEnMemoria());
  }

  public static FotosBienesService fotosBienes(StoragePort storage) {
    return new FotosBienesService(storage, propiedades(), Clock.systemUTC());
  }
}
