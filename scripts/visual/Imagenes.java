import static java.nio.charset.StandardCharsets.UTF_8;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Stream;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

/**
 * Herramientas de imagen para las capturas de la verificación visual del cliente liviano. Solo la
 * biblioteca estándar de Java 21: se corre con {@code java scripts/visual/Imagenes.java <orden>}.
 * Uso y ejemplos en scripts/visual/README.md.
 */
public class Imagenes {

  private static final String USO =
      """
      Uso: java scripts/visual/Imagenes.java <orden> <argumentos>
        jpg <origen.png|carpeta> <destino.jpg|carpeta> [maxKB=400] [glob=*.png]
        comparar <antes> <después> [glob] [ruido]   sin glob: los PNG de Glassmorphism (sin "--")
        recorte <entrada> <salida.png> <x> <y> <ancho> <alto> [escala=1]
        lado <a> <b> <salida.png> <x> <y> <ancho> <alto> <escala> <rótulo A> <rótulo B>
        tamano <imagen>...
        resumen <guion.txt> <salida.html> [maxKB=400]
      """;

  private static final long KB = 1024;

  /** Lado de la celda de ruido: un cambio a una celda o menos de una zona ruidosa no cuenta. */
  private static final int CELDA = 16;

  public static void main(String[] a) throws IOException {
    System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, UTF_8));
    System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true, UTF_8));
    if (a.length == 0) {
      salirConUso();
    }
    switch (a[0]) {
      case "jpg" -> jpg(a);
      case "comparar" -> System.exit(comparar(a));
      case "recorte" -> recorte(a);
      case "lado" -> lado(a);
      case "tamano" -> tamano(a);
      case "resumen" -> resumen(a);
      default -> salirConUso();
    }
  }

  private static void salirConUso() {
    System.err.print(USO);
    System.exit(2);
  }

  /** Un PNG o todos los de una carpeta (que cumplan el glob) a JPG de maxKB o menos. */
  private static void jpg(String[] a) throws IOException {
    Path origen = Path.of(a[1]);
    Path destino = Path.of(a[2]);
    long max = (a.length > 3 ? Long.parseLong(a[3]) : 400) * KB;
    if (!Files.isDirectory(origen)) {
      escribirJpg(origen, destino, max);
      return;
    }
    Files.createDirectories(destino);
    for (Path png : archivos(origen, a.length > 4 ? a[4] : "*.png")) {
      String nombre = png.getFileName().toString().replaceFirst("\\.[^.]+$", ".jpg");
      escribirJpg(png, destino.resolve(nombre), max);
    }
  }

  private static void escribirJpg(Path origen, Path destino, long max) throws IOException {
    byte[] bytes = comprimir(leer(origen), max, origen);
    Files.write(destino, bytes);
    System.out.printf("%s %d KB%n", destino, bytes.length / KB);
  }

  /** Baja la calidad y, si no alcanza, la escala, hasta que el JPG entra en max bytes. */
  private static byte[] comprimir(BufferedImage origen, long max, Path nombre) throws IOException {
    for (int escala = 100; escala >= 40; escala -= 15) {
      BufferedImage rgb = escalar(origen, escala / 100.0);
      for (int calidad = 85; calidad >= 50; calidad -= 5) {
        byte[] bytes = codificar(rgb, calidad / 100f);
        if (bytes.length <= max) {
          return bytes;
        }
      }
    }
    throw new IllegalStateException("No entra en " + max / KB + " KB: " + nombre);
  }

  private static BufferedImage escalar(BufferedImage origen, double escala) {
    int ancho = (int) (origen.getWidth() * escala);
    int alto = (int) (origen.getHeight() * escala);
    BufferedImage rgb = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = rgb.createGraphics();
    g.setRenderingHint(
        RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    g.drawImage(origen, 0, 0, ancho, alto, null);
    g.dispose();
    return rgb;
  }

  private static byte[] codificar(BufferedImage rgb, float calidad) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    ImageWriter escritor = ImageIO.getImageWritersByFormatName("jpg").next();
    ImageWriteParam parametros = escritor.getDefaultWriteParam();
    parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
    parametros.setCompressionQuality(calidad);
    try (MemoryCacheImageOutputStream salida = new MemoryCacheImageOutputStream(bytes)) {
      escritor.setOutput(salida);
      escritor.write(null, new IIOImage(rgb, null, null), parametros);
    } finally {
      escritor.dispose();
    }
    return bytes.toByteArray();
  }

  /**
   * Compara píxel a píxel los PNG de antes con los de después que se llaman igual; de cada uno
   * distinto, dice la caja donde cambió. Sale con 1 si alguno cambió o falta. Con una carpeta de
   * ruido (otra corrida de la misma base), no cuenta lo que ya difiere entre antes y ruido: las
   * animaciones que la captura no congela.
   */
  private static int comparar(String[] a) throws IOException {
    Path antes = Path.of(a[1]);
    Path despues = Path.of(a[2]);
    Path ruido = a.length > 4 ? Path.of(a[4]) : null;
    List<Path> pngs =
        a.length > 3
            ? archivos(antes, a[3])
            : archivos(antes, "*.png").stream()
                .filter(png -> !png.getFileName().toString().contains("--"))
                .toList();
    int distintas = 0;
    for (Path png : pngs) {
      String nombre = png.getFileName().toString();
      Path otra = despues.resolve(nombre);
      BufferedImage base = leer(png);
      boolean[][] zonas =
          ruido != null && Files.exists(ruido.resolve(nombre))
              ? zonasDeRuido(base, leer(ruido.resolve(nombre)))
              : null;
      String diferencia = Files.exists(otra) ? diferencia(base, leer(otra), zonas) : "FALTA";
      System.out.println((diferencia.isEmpty() ? "IGUAL " : diferencia + " ") + nombre);
      if (!diferencia.isEmpty()) {
        distintas++;
      }
    }
    System.out.println(
        distintas == 0
            ? "SIN CAMBIOS (" + pngs.size() + " capturas" + (ruido == null ? ")" : ", sin el ruido)")
            : "HAY DIFERENCIAS: " + distintas + " de " + pngs.size());
    return distintas == 0 ? 0 : 1;
  }

  /** Las celdas de CELDA px donde dos corridas de la misma base ya difieren (nada si no miden igual). */
  private static boolean[][] zonasDeRuido(BufferedImage x, BufferedImage y) {
    int ancho = x.getWidth();
    boolean[][] zonas = new boolean[x.getHeight() / CELDA + 1][ancho / CELDA + 1];
    if (ancho == y.getWidth() && x.getHeight() == y.getHeight()) {
      int[] px = x.getRGB(0, 0, ancho, x.getHeight(), null, 0, ancho);
      int[] py = y.getRGB(0, 0, ancho, x.getHeight(), null, 0, ancho);
      for (int i = 0; i < px.length; i++) {
        if (px[i] != py[i]) {
          zonas[i / ancho / CELDA][i % ancho / CELDA] = true;
        }
      }
    }
    return zonas;
  }

  /** Si el píxel cae en una celda de ruido o en una vecina. */
  private static boolean enRuido(boolean[][] zonas, int x, int y) {
    for (int f = y / CELDA - 1; f <= y / CELDA + 1; f++) {
      for (int c = x / CELDA - 1; c <= x / CELDA + 1; c++) {
        if (f >= 0 && f < zonas.length && c >= 0 && c < zonas[f].length && zonas[f][c]) {
          return true;
        }
      }
    }
    return false;
  }

  /** Vacío si son iguales (sin contar las zonas de ruido); si no, cuántos píxeles y en qué caja. */
  private static String diferencia(BufferedImage x, BufferedImage y, boolean[][] ruido) {
    int ancho = x.getWidth();
    int alto = x.getHeight();
    if (ancho != y.getWidth() || alto != y.getHeight()) {
      return "DISTINTO TAMAÑO " + ancho + "x" + alto + " vs " + y.getWidth() + "x" + y.getHeight();
    }
    int[] px = x.getRGB(0, 0, ancho, alto, null, 0, ancho);
    int[] py = y.getRGB(0, 0, ancho, alto, null, 0, ancho);
    long distintos = 0;
    int x0 = ancho;
    int y0 = alto;
    int x1 = -1;
    int y1 = -1;
    for (int i = 0; i < px.length; i++) {
      if (px[i] != py[i] && (ruido == null || !enRuido(ruido, i % ancho, i / ancho))) {
        distintos++;
        x0 = Math.min(x0, i % ancho);
        x1 = Math.max(x1, i % ancho);
        y0 = Math.min(y0, i / ancho);
        y1 = Math.max(y1, i / ancho);
      }
    }
    return distintos == 0
        ? ""
        : "DISTINTO " + distintos + " px en " + x0 + "," + y0 + " - " + x1 + "," + y1;
  }

  /** Una zona de la imagen (se acota al borde), opcionalmente escalada. */
  private static void recorte(String[] a) throws IOException {
    BufferedImage zona = zona(leer(Path.of(a[1])), a, 3);
    double escala = a.length > 7 ? Double.parseDouble(a[7]) : 1;
    ImageIO.write(escala == 1 ? zona : escalar(zona, escala), "png", new File(a[2]));
  }

  /** La zona de a[desde..desde+3] = x, y, ancho y alto, acotada al borde de la imagen. */
  private static BufferedImage zona(BufferedImage imagen, String[] a, int desde) {
    int x = Integer.parseInt(a[desde]);
    int y = Integer.parseInt(a[desde + 1]);
    int ancho = Math.min(Integer.parseInt(a[desde + 2]), imagen.getWidth() - x);
    int alto = Math.min(Integer.parseInt(a[desde + 3]), imagen.getHeight() - y);
    return imagen.getSubimage(x, y, ancho, alto);
  }

  /** La misma zona de dos capturas, lado a lado y con su rótulo. */
  private static void lado(String[] a) throws IOException {
    BufferedImage[] lados = {zona(leer(Path.of(a[1])), a, 4), zona(leer(Path.of(a[2])), a, 4)};
    double escala = Double.parseDouble(a[8]);
    int ancho = (int) (Math.min(lados[0].getWidth(), lados[1].getWidth()) * escala);
    int alto = (int) (Math.min(lados[0].getHeight(), lados[1].getHeight()) * escala);
    int banda = 44;
    int separacion = 24;
    BufferedImage salida =
        new BufferedImage(
            ancho * 2 + separacion * 3, alto + banda + separacion, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = salida.createGraphics();
    g.setRenderingHint(
        RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    g.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g.setColor(new Color(0xe3e7ea));
    g.fillRect(0, 0, salida.getWidth(), salida.getHeight());
    g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
    for (int i = 0; i < 2; i++) {
      int izquierda = separacion + i * (ancho + separacion);
      g.setColor(new Color(0x1d2327));
      g.drawString(a[9 + i], izquierda, 30);
      g.drawImage(lados[i], izquierda, banda, ancho, alto, null);
      g.setColor(new Color(0x8a959a));
      g.drawRect(izquierda - 1, banda - 1, ancho + 1, alto + 1);
    }
    g.dispose();
    ImageIO.write(salida, "png", new File(a[3]));
  }

  private static void tamano(String[] a) throws IOException {
    for (int i = 1; i < a.length; i++) {
      BufferedImage imagen = leer(Path.of(a[i]));
      System.out.println(a[i] + " " + imagen.getWidth() + "x" + imagen.getHeight());
    }
  }

  /**
   * Página HTML de un solo archivo (antes y después de una corrida) a partir de un guion de texto:
   * "# título", "## sección", "img ruta | epígrafe" (las seguidas van en una fila) y cualquier
   * otra línea es un párrafo. Las imágenes van adentro, como JPG de maxKB o menos; las rutas
   * relativas salen de la carpeta del guion.
   */
  private static void resumen(String[] a) throws IOException {
    Path guion = Path.of(a[1]);
    long max = (a.length > 3 ? Long.parseLong(a[3]) : 400) * KB;
    StringBuilder cuerpo = new StringBuilder();
    List<String> fila = new ArrayList<>();
    String titulo = "Antes y después";
    for (String linea : Files.readAllLines(guion, UTF_8)) {
      String texto = linea.strip();
      if (texto.startsWith("img ")) {
        String[] partes = texto.substring(4).split("\\|", 2);
        Path imagen = guion.toAbsolutePath().getParent().resolve(partes[0].strip());
        String epigrafe = partes.length > 1 ? partes[1].strip() : "";
        fila.add(figura(imagen, epigrafe, max));
        continue;
      }
      cerrarFila(cuerpo, fila);
      if (texto.startsWith("## ")) {
        cuerpo.append("<h2>").append(escapar(texto.substring(3))).append("</h2>\n");
      } else if (texto.startsWith("# ")) {
        titulo = texto.substring(2);
        cuerpo.append("<h1>").append(escapar(titulo)).append("</h1>\n");
      } else if (!texto.isEmpty()) {
        cuerpo.append("<p>").append(escapar(texto)).append("</p>\n");
      }
    }
    cerrarFila(cuerpo, fila);
    String pagina =
        PAGINA.replace("@TITULO@", escapar(titulo)).replace("@CUERPO@", cuerpo.toString());
    Files.writeString(Path.of(a[2]), pagina, UTF_8);
    System.out.printf("%s %d KB%n", a[2], Files.size(Path.of(a[2])) / KB);
  }

  private static String figura(Path imagen, String epigrafe, long max) throws IOException {
    String datos = Base64.getEncoder().encodeToString(comprimir(leer(imagen), max, imagen));
    return "<figure><div class=\"marco\"><img src=\"data:image/jpeg;base64,"
        + datos
        + "\" alt=\""
        + escapar(epigrafe)
        + "\"></div><figcaption>"
        + escapar(epigrafe)
        + "</figcaption></figure>";
  }

  private static void cerrarFila(StringBuilder cuerpo, List<String> fila) {
    if (!fila.isEmpty()) {
      cuerpo.append("<div class=\"fila\">").append(String.join("", fila)).append("</div>\n");
      fila.clear();
    }
  }

  private static final String PAGINA =
      """
      <!DOCTYPE html><html lang="es-AR"><head><meta charset="utf-8">
      <meta name="viewport" content="width=device-width, initial-scale=1"><title>@TITULO@</title>
      <style>
      :root{--fondo:#eef0f2;--papel:#fff;--tinta:#1d2327;--suave:#5b6469;--filete:#d5dadd}
      @media (prefers-color-scheme:dark){:root{--fondo:#16191b;--papel:#22272a;--tinta:#eef0f2;
        --suave:#aab3b8;--filete:#3a4246}}
      *{box-sizing:border-box}
      body{margin:0;background:var(--fondo);color:var(--tinta);font:17px/1.55 system-ui,sans-serif}
      main{max-width:1280px;margin:0 auto;padding:24px 16px 64px}
      h1{font-size:clamp(1.8rem,1.2rem + 2vw,2.6rem);margin:.2em 0}
      h2{margin:2em 0 .4em;padding-top:.6em;border-top:1px solid var(--filete)}
      p{max-width:72ch}
      .fila{display:grid;gap:16px;grid-template-columns:repeat(auto-fit,minmax(min(100%,300px),1fr))}
      figure{margin:0;background:var(--papel);border:1px solid var(--filete);border-radius:10px;
        padding:10px}
      .marco{max-height:80vh;overflow:auto;border-radius:6px}
      figure img{display:block;width:100%;height:auto}
      figcaption{font-size:.9rem;color:var(--suave);padding-top:6px}
      </style></head><body><main>
      @CUERPO@</main></body></html>
      """;

  private static String escapar(String texto) {
    return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
  }

  private static BufferedImage leer(Path archivo) throws IOException {
    BufferedImage imagen = ImageIO.read(archivo.toFile());
    if (imagen == null) {
      throw new IOException("No es una imagen: " + archivo);
    }
    return imagen;
  }

  /** Los archivos de la carpeta que cumplen el glob, por nombre. */
  private static List<Path> archivos(Path carpeta, String glob) throws IOException {
    PathMatcher patron = FileSystems.getDefault().getPathMatcher("glob:" + glob);
    try (Stream<Path> lista = Files.list(carpeta)) {
      return lista.filter(archivo -> patron.matches(archivo.getFileName())).sorted().toList();
    }
  }
}
