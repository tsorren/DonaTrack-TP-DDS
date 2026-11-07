package grupo5.donaciones.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

/**
 * Buckets, límites y vigencias del almacenamiento de objetos ({@code donaciones.almacenamiento.*}).
 * Valen igual con MinIO y en memoria.
 *
 * @param bucketPadrones bucket privado de los padrones CSV de donantes
 * @param bucketFotos bucket privado de las fotos de bienes (se accede con URLs prefirmadas)
 * @param csvTamanioMaximo tamaño máximo de un padrón CSV
 * @param fotoTamanioMaximo tamaño máximo de una foto de bien
 * @param fotoTiposPermitidos tipos de contenido aceptados para las fotos
 * @param vigenciaSubida cuánto dura la URL para subir una foto
 * @param vigenciaDescarga cuánto dura la URL para ver una foto
 */
@ConfigurationProperties("donaciones.almacenamiento")
public record AlmacenamientoProperties(
    @DefaultValue("donaciones-padrones") String bucketPadrones,
    @DefaultValue("donaciones-fotos-bienes") String bucketFotos,
    @DefaultValue("50MB") DataSize csvTamanioMaximo,
    @DefaultValue("10MB") DataSize fotoTamanioMaximo,
    @DefaultValue({"image/jpeg", "image/png", "image/webp"}) List<String> fotoTiposPermitidos,
    @DefaultValue("15m") Duration vigenciaSubida,
    @DefaultValue("2h") Duration vigenciaDescarga) {}
