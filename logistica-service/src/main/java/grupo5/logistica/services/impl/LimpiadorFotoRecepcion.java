package grupo5.logistica.services.impl;

import grupo5.logistica.services.FotoRecepcionRechazadaException;
import grupo5.logistica.services.FotoRecepcionRechazadaException.Motivo;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

/**
 * Valida una foto de recepción y la re-codifica sin metadatos (ADR-E6-08, RNF-PRIV-04). En este
 * orden: tope de 5 MB, tipo real por magic bytes (JPEG o PNG), tope de píxeles antes de decodificar
 * (una imagen chica en bytes puede ocupar gigas en memoria) y decodificación. Se escriben solo los
 * píxeles: así se van EXIF, GPS, XMP y miniaturas embebidas.
 */
final class LimpiadorFotoRecepcion {

  static final long TAMANIO_MAXIMO_BYTES = 5L * 1024 * 1024;

  /**
   * 24 megapíxeles: una foto de celular común entra; decodificada ocupa hasta unos 96 MB, que
   * Logística (-Xmx384m en el compose) soporta.
   */
  static final long PIXELES_MAXIMOS = 24_000_000L;

  private static final float CALIDAD_JPEG = 0.9f;
  private static final byte[] MAGIA_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
  private static final byte[] MAGIA_PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

  /** Formato aceptado: nombre de ImageIO, extensión de la clave y tipo de contenido. */
  enum Formato {
    JPEG("jpeg", "jpg", "image/jpeg"),
    PNG("png", "png", "image/png");

    final String nombreImageIo;
    final String extension;
    final String tipoContenido;

    Formato(String nombreImageIo, String extension, String tipoContenido) {
      this.nombreImageIo = nombreImageIo;
      this.extension = extension;
      this.tipoContenido = tipoContenido;
    }
  }

  /** Foto ya limpia, lista para guardar. */
  record FotoLimpia(byte[] contenido, Formato formato) {

    @Override
    public boolean equals(Object otro) {
      return otro instanceof FotoLimpia foto
          && formato == foto.formato
          && Arrays.equals(contenido, foto.contenido);
    }

    @Override
    public int hashCode() {
      return 31 * Arrays.hashCode(contenido) + formato.hashCode();
    }

    @Override
    public String toString() {
      return "FotoLimpia[" + formato + ", " + contenido.length + " bytes]";
    }
  }

  private LimpiadorFotoRecepcion() {}

  static FotoLimpia limpiar(byte[] original) {
    if (original.length > TAMANIO_MAXIMO_BYTES) {
      throw new FotoRecepcionRechazadaException(Motivo.DEMASIADO_GRANDE);
    }
    Formato formato = detectarFormato(original);
    BufferedImage imagen = decodificar(original, formato);
    if (imagen == null) {
      throw new FotoRecepcionRechazadaException(Motivo.DEMASIADO_GRANDE);
    }
    return new FotoLimpia(codificar(imagen, formato), formato);
  }

  private static Formato detectarFormato(byte[] bytes) {
    if (empiezaCon(bytes, MAGIA_JPEG)) {
      return Formato.JPEG;
    }
    if (empiezaCon(bytes, MAGIA_PNG)) {
      return Formato.PNG;
    }
    throw new FotoRecepcionRechazadaException(Motivo.TIPO_NO_PERMITIDO);
  }

  private static boolean empiezaCon(byte[] bytes, byte[] prefijo) {
    return bytes.length >= prefijo.length
        && Arrays.equals(bytes, 0, prefijo.length, prefijo, 0, prefijo.length);
  }

  /**
   * Decodifica con el lector del formato detectado, después de mirar las dimensiones. Devuelve
   * {@code null} si la imagen tiene más de {@link #PIXELES_MAXIMOS} píxeles.
   */
  private static BufferedImage decodificar(byte[] bytes, Formato formato) {
    Iterator<ImageReader> lectores = ImageIO.getImageReadersByFormatName(formato.nombreImageIo);
    ImageReader lector = lectores.next();
    try (ImageInputStream entrada =
        ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
      lector.setInput(entrada, true, true);
      long pixeles = (long) lector.getWidth(0) * lector.getHeight(0);
      return pixeles > PIXELES_MAXIMOS ? null : lector.read(0);
    } catch (IOException | RuntimeException e) {
      // ImageIO informa los archivos corruptos con IOException o con excepciones no chequeadas
      throw new FotoRecepcionRechazadaException(Motivo.ILEGIBLE, e);
    } finally {
      lector.dispose();
    }
  }

  /** Escribe solo los píxeles: sin metadatos (el IIOImage va sin ellos ni miniaturas). */
  private static byte[] codificar(BufferedImage imagen, Formato formato) {
    ImageWriter escritor = ImageIO.getImageWritersByFormatName(formato.nombreImageIo).next();
    ByteArrayOutputStream salida = new ByteArrayOutputStream();
    try (ImageOutputStream destino = ImageIO.createImageOutputStream(salida)) {
      escritor.setOutput(destino);
      ImageWriteParam parametros = escritor.getDefaultWriteParam();
      if (formato == Formato.JPEG) {
        parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        parametros.setCompressionQuality(CALIDAD_JPEG);
      }
      escritor.write(null, new IIOImage(imagen, null, null), parametros);
    } catch (IOException e) {
      throw new FotoRecepcionRechazadaException(Motivo.ILEGIBLE, e);
    } finally {
      escritor.dispose();
    }
    return salida.toByteArray();
  }
}
