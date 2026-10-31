import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Graba un recorrido del sitio según un guion y lo deja en MP4 (Playwright + ffmpeg).
 *
 * <p>Uso: java -cp <jars de Playwright y gson> Recorrido.java guion.txt salida.mp4 http://localhost:8090
 *
 * <p>Guion, una orden por línea (# es comentario): "pantalla <ancho> <alto>" abre un tramo nuevo
 * (la primera es obligatoria), "cartel <segundos> | <título> | <texto>", "ir <ruta>", "esperar
 * <segundos>", "bajar <px> <segundos>", "subir", "clic <nombre accesible del enlace o botón>",
 * "foco <selector CSS>" (lo desplaza a la vista).
 */
public class Recorrido {

  static final String CARTEL =
      """
      ([titulo, texto]) => {
        const d = document.createElement('div');
        d.id = 'cartel-recorrido';
        d.style.cssText = 'position:fixed;inset:0;z-index:2147483647;display:grid;place-content:center;'
          + 'gap:16px;padding:48px;background:rgba(38,36,44,.92);color:#FBF4E6;text-align:center;'
          + 'font-family:"Baloo 2",Nunito,system-ui,sans-serif;opacity:0;transition:opacity .4s';
        d.innerHTML = '<div style="font-size:clamp(28px,5vw,56px);font-weight:800;line-height:1.1"></div>'
          + '<div style="font-family:Nunito,system-ui,sans-serif;font-size:clamp(16px,2.2vw,24px);max-width:36em;margin:auto;line-height:1.4"></div>';
        d.children[0].textContent = titulo;
        d.children[1].textContent = texto;
        document.body.appendChild(d);
        requestAnimationFrame(() => d.style.opacity = '1');
      }
      """;

  public static void main(String[] args) throws Exception {
    List<String> guion = Files.readAllLines(Path.of(args[0]));
    Path salida = Path.of(args[1]).toAbsolutePath();
    String base = args[2];
    Path tramos = Files.createTempDirectory("recorrido");
    List<Path> videos = new ArrayList<>();
    List<Double> inicios = new ArrayList<>();
    try (Playwright pw = Playwright.create();
        Browser navegador = pw.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))) {
      BrowserContext contexto = null;
      Page pagina = null;
      long creada = 0;
      boolean cargada = false;
      for (String linea : guion) {
        String l = linea.strip();
        if (l.isEmpty() || l.startsWith("#")) {
          continue;
        }
        String orden = l.split("\\s+", 2)[0];
        String resto = l.length() > orden.length() ? l.substring(orden.length()).strip() : "";
        switch (orden) {
          case "pantalla" -> {
            if (contexto != null) {
              videos.add(cerrar(contexto, pagina));
            }
            String[] t = resto.split("\\s+");
            int ancho = Integer.parseInt(t[0]);
            int alto = Integer.parseInt(t[1]);
            contexto =
                navegador.newContext(
                    new Browser.NewContextOptions()
                        .setViewportSize(ancho, alto)
                        .setDeviceScaleFactor(1)
                        .setIsMobile(ancho < 768)
                        .setHasTouch(ancho < 768)
                        .setRecordVideoDir(tramos)
                        .setRecordVideoSize(ancho, alto));
            pagina = contexto.newPage();
            creada = System.nanoTime();
            cargada = false;
          }
          case "ir" -> {
            pagina.navigate(base + resto);
            pagina.waitForLoadState();
            if (!cargada) {
              pagina.waitForTimeout(400);
              inicios.add((System.nanoTime() - creada) / 1e9);
              cargada = true;
            }
          }
          case "esperar" -> pagina.waitForTimeout(segundos(resto));
          case "cartel" -> {
            String[] p = resto.split("\\|");
            pagina.evaluate(CARTEL, List.of(p[1].strip(), p.length > 2 ? p[2].strip() : ""));
            pagina.waitForTimeout(segundos(p[0].strip()));
            pagina.evaluate("document.getElementById('cartel-recorrido')?.remove()");
            pagina.waitForTimeout(300);
          }
          case "bajar" -> {
            String[] p = resto.split("\\s+");
            int px = Integer.parseInt(p[0]);
            double s = segundos(p[1]);
            int pasos = (int) Math.max(1, s / 40);
            for (int i = 0; i < pasos; i++) {
              pagina.mouse().wheel(0, (double) px / pasos);
              pagina.waitForTimeout(40);
            }
          }
          case "subir" -> {
            pagina.evaluate("window.scrollTo({top:0,behavior:'smooth'})");
            pagina.waitForTimeout(800);
          }
          case "foco" -> {
            pagina.locator(resto).first().scrollIntoViewIfNeeded();
            pagina.waitForTimeout(600);
          }
          case "clic" -> {
            Locator destino =
                pagina
                    .getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(resto))
                    .or(pagina.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(resto)))
                    .first();
            destino.scrollIntoViewIfNeeded();
            destino.hover();
            pagina.waitForTimeout(500);
            destino.click();
            pagina.waitForLoadState();
          }
          default -> throw new IllegalArgumentException("Orden desconocida: " + l);
        }
      }
      videos.add(cerrar(contexto, pagina));
    }
    componer(videos, inicios, salida);
    System.out.println(salida);
  }

  static double segundos(String s) {
    return Double.parseDouble(s) * 1000;
  }

  static Path cerrar(BrowserContext contexto, Page pagina) {
    Path video = pagina.video().path();
    contexto.close();
    return video;
  }

  /** Cada tramo a 1280×720 (el del celular, centrado sobre tinta), unidos en un MP4 H.264. */
  static void componer(List<Path> videos, List<Double> inicios, Path salida) throws Exception {
    List<String> cmd = new ArrayList<>(List.of("ffmpeg", "-y", "-loglevel", "error"));
    StringBuilder filtro = new StringBuilder();
    for (int i = 0; i < videos.size(); i++) {
      cmd.addAll(List.of("-ss", String.valueOf(inicios.get(i)), "-i", videos.get(i).toString()));
      filtro
          .append('[')
          .append(i)
          .append(":v]scale=1280:720:force_original_aspect_ratio=decrease,")
          .append("pad=1280:720:(ow-iw)/2:(oh-ih)/2:color=0x26242C,setsar=1,fps=30[v")
          .append(i)
          .append("];");
    }
    for (int i = 0; i < videos.size(); i++) {
      filtro.append("[v").append(i).append(']');
    }
    filtro.append("concat=n=").append(videos.size()).append(":v=1:a=0[out]");
    cmd.addAll(
        List.of(
            "-filter_complex", filtro.toString(), "-map", "[out]", "-c:v", "libx264", "-crf", "20",
            "-preset", "slow", "-pix_fmt", "yuv420p", "-movflags", "+faststart",
            salida.toString()));
    Process p = new ProcessBuilder(cmd).inheritIO().start();
    if (p.waitFor() != 0) {
      throw new IllegalStateException("ffmpeg falló");
    }
  }
}
