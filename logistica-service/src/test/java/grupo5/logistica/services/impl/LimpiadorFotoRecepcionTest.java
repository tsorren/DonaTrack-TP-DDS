package grupo5.logistica.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import grupo5.logistica.services.FotoRecepcionRechazadaException;
import grupo5.logistica.services.FotoRecepcionRechazadaException.Motivo;
import grupo5.logistica.services.impl.LimpiadorFotoRecepcion.Formato;
import grupo5.logistica.services.impl.LimpiadorFotoRecepcion.FotoLimpia;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** Validación y limpieza de las fotos de recepción (ADR-E6-08 §Validación). Fixtures sintéticos. */
class LimpiadorFotoRecepcionTest {

  @Test
  void jpegValido_pasaYSigueSiendoJpeg() throws IOException {
    FotoLimpia limpia = LimpiadorFotoRecepcion.limpiar(imagen("jpeg", 40, 30));

    assertThat(limpia.formato()).isEqualTo(Formato.JPEG);
    BufferedImage leida = ImageIO.read(new ByteArrayInputStream(limpia.contenido()));
    assertThat(leida.getWidth()).isEqualTo(40);
    assertThat(leida.getHeight()).isEqualTo(30);
  }

  @Test
  void pngValido_pasaYSigueSiendoPng() throws IOException {
    FotoLimpia limpia = LimpiadorFotoRecepcion.limpiar(imagen("png", 20, 10));

    assertThat(limpia.formato()).isEqualTo(Formato.PNG);
    assertThat(ImageIO.read(new ByteArrayInputStream(limpia.contenido())).getWidth()).isEqualTo(20);
  }

  @Test
  void pdfConExtensionJpg_esTipoNoPermitido() {
    byte[] pdf = "%PDF-1.7\n1 0 obj\n<<>>\nendobj\n".getBytes(StandardCharsets.US_ASCII);

    assertRechazo(pdf, Motivo.TIPO_NO_PERMITIDO);
  }

  @Test
  void masDeCincoMegabytes_esDemasiadoGrande() {
    byte[] grande = new byte[(int) LimpiadorFotoRecepcion.TAMANIO_MAXIMO_BYTES + 1];
    grande[0] = (byte) 0xFF;
    grande[1] = (byte) 0xD8;
    grande[2] = (byte) 0xFF;

    assertRechazo(grande, Motivo.DEMASIADO_GRANDE);
  }

  @Test
  void cabeceraJpegConBytesAlAzar_esIlegible() {
    byte[] basura = new byte[2048];
    for (int i = 0; i < basura.length; i++) {
      basura[i] = (byte) (i * 31 + 7);
    }
    basura[0] = (byte) 0xFF;
    basura[1] = (byte) 0xD8;
    basura[2] = (byte) 0xFF;

    assertRechazo(basura, Motivo.ILEGIBLE);
  }

  @Test
  void pngConMasPixelesQueElTope_esDemasiadoGrandeSinDecodificar() {
    // Solo la firma y el IHDR de un PNG de 6000 x 5000: el lector ve las dimensiones y no
    // decodifica
    assertRechazo(cabeceraPng(6000, 5000), Motivo.DEMASIADO_GRANDE);
  }

  @Test
  void jpegConExifGps_saleSinSegmentoApp1() throws IOException {
    byte[] conExif = conApp1(imagen("jpeg", 32, 32));
    assertThat(contieneApp1(conExif)).as("el fixture trae APP1").isTrue();

    FotoLimpia limpia = LimpiadorFotoRecepcion.limpiar(conExif);

    assertThat(contieneApp1(limpia.contenido())).isFalse();
    assertThat(new String(limpia.contenido(), StandardCharsets.ISO_8859_1))
        .doesNotContain("Exif")
        .doesNotContain("GPS");
  }

  private static void assertRechazo(byte[] bytes, Motivo motivo) {
    assertThatThrownBy(() -> LimpiadorFotoRecepcion.limpiar(bytes))
        .isInstanceOf(FotoRecepcionRechazadaException.class)
        .extracting(e -> ((FotoRecepcionRechazadaException) e).getMotivo())
        .isEqualTo(motivo);
  }

  static byte[] imagen(String formato, int ancho, int alto) throws IOException {
    BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
    imagen.setRGB(0, 0, 0x336699);
    ByteArrayOutputStream salida = new ByteArrayOutputStream();
    ImageIO.write(imagen, formato, salida);
    return salida.toByteArray();
  }

  /** Inserta después del SOI un APP1 "Exif" con un texto GPS sintético. */
  static byte[] conApp1(byte[] jpeg) {
    byte[] carga =
        "Exif\0\0GPSLatitude=-34.6037;GPSLongitude=-58.3816".getBytes(StandardCharsets.ISO_8859_1);
    int largo = carga.length + 2;
    ByteBuffer buffer = ByteBuffer.allocate(jpeg.length + 4 + carga.length);
    buffer.put(jpeg, 0, 2);
    buffer.put((byte) 0xFF).put((byte) 0xE1).put((byte) (largo >> 8)).put((byte) largo);
    buffer.put(carga);
    buffer.put(jpeg, 2, jpeg.length - 2);
    return buffer.array();
  }

  /** Busca un marcador APP1 (FF E1) entre los segmentos anteriores a los datos de la imagen. */
  static boolean contieneApp1(byte[] jpeg) {
    int i = 2;
    while (i + 4 <= jpeg.length && (jpeg[i] & 0xFF) == 0xFF) {
      int marcador = jpeg[i + 1] & 0xFF;
      if (marcador == 0xE1) {
        return true;
      }
      if (marcador == 0xDA) {
        return false;
      }
      i += 2 + (((jpeg[i + 2] & 0xFF) << 8) | (jpeg[i + 3] & 0xFF));
    }
    return false;
  }

  private static byte[] cabeceraPng(int ancho, int alto) {
    byte[] firma = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    ByteBuffer datos = ByteBuffer.allocate(17);
    datos.put("IHDR".getBytes(StandardCharsets.US_ASCII));
    datos.putInt(ancho).putInt(alto).put((byte) 8).put((byte) 2).put((byte) 0).put((byte) 0);
    datos.put((byte) 0);
    CRC32 crc = new CRC32();
    crc.update(datos.array());
    ByteBuffer png = ByteBuffer.allocate(firma.length + 4 + 17 + 4);
    png.put(firma).putInt(13).put(datos.array()).putInt((int) crc.getValue());
    return png.array();
  }

  @Test
  void fotoLimpia_comparaPorContenidoDelArray() {
    FotoLimpia una = new FotoLimpia(new byte[] {1, 2, 3}, LimpiadorFotoRecepcion.Formato.PNG);
    FotoLimpia igual = new FotoLimpia(new byte[] {1, 2, 3}, LimpiadorFotoRecepcion.Formato.PNG);
    FotoLimpia otra = new FotoLimpia(new byte[] {1, 2, 4}, LimpiadorFotoRecepcion.Formato.PNG);

    assertThat(una).isEqualTo(igual).hasSameHashCodeAs(igual).isNotEqualTo(otra);
    assertThat(una).hasToString("FotoLimpia[PNG, 3 bytes]");
  }
}
