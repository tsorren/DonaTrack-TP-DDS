package grupo5.cliente.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.cliente.maqueta.MaquetaConfig;
import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.DonanteInicioPuerto;
import grupo5.cliente.puertos.DonantePerfilPuerto;
import grupo5.cliente.puertos.DonantePerfilPuerto.Tipo;
import grupo5.cliente.vistas.DonanteAnioVista;
import grupo5.cliente.vistas.DonanteDonacionVista;
import grupo5.cliente.vistas.DonanteDonacionVista.Carta;
import grupo5.cliente.vistas.DonanteInicioVista;
import grupo5.cliente.vistas.DonanteInicioVista.Aviso;
import grupo5.cliente.vistas.EstadoEntrega;
import grupo5.cliente.vistas.PasoRecorrido;
import grupo5.cliente.vistas.PasoRecorrido.Avance;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.StringUtils;

/**
 * El inicio del donante con lo que da el puerto: las fechas de hoy y no las de la maqueta, y la
 * versión del tipo de la cuenta (R11-06).
 */
@WebMvcTest(controllers = {DonanteInicioController.class, PantallasController.class})
@ActiveProfiles("maqueta")
@Import({MaquetaConfig.class, Pantallas.class})
class DonanteInicioControllerTest {

  private static final UUID DONANTE = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000aa");

  private static final DonanteDonacionVista SILLA =
      new DonanteDonacionVista(
          "s1",
          1,
          "Silla plegable",
          "1 unidad",
          null,
          "Mobiliario",
          "Hogar Ejemplo",
          "1 de noviembre de 2026",
          null,
          EstadoEntrega.PENDIENTE,
          "Asignada, espera el camión",
          null,
          null,
          null,
          List.of(),
          List.of(
              new Carta(
                  "Asignamos tu donación a una necesidad de Hogar Ejemplo.",
                  "7 de noviembre, 10:00",
                  "2026-11-07T10:00")));

  private static final DonanteInicioVista CON_SILLA =
      new DonanteInicioVista(
          List.of(SILLA),
          List.of(new Aviso(SILLA, 0)),
          1,
          0,
          "noviembre de 2026",
          "7 de noviembre",
          "7 de noviembre, 19:40",
          "1 de noviembre",
          "2026-11-07",
          "2026-11-06");

  private static final List<String> MESES =
      List.of(
          "enero",
          "febrero",
          "marzo",
          "abril",
          "mayo",
          "junio",
          "julio",
          "agosto",
          "septiembre",
          "octubre",
          "noviembre");

  /** Noviembre con 5 registradas, de las que llega la más reciente; las cifras, como números. */
  private static final DonanteAnioVista ANIO_CON_SILLA = anio(5, List.of(SILLA), true);

  @MockitoBean private DonanteInicioPuerto inicios;
  @MockitoBean private DonantePerfilPuerto perfiles;
  @MockitoBean private CuentaActual cuentas;
  @Autowired private MockMvc mvc;

  /** La que viaja hoy, por el paso 4 de 5, con la posición del camión. */
  private static final DonanteDonacionVista LENTEJAS =
      donacion(
          "l1",
          "Lentejas secas",
          "15 kg",
          "Comedor Barrio Ejemplo",
          EstadoEntrega.EN_CAMINO,
          "Salió hoy a las 14:30",
          null,
          "Posición aproximada, actualizada hoy a las 16:25.",
          3);

  /** La que no pudo llegar: la nota de la maqueta promete una fecha que el backend no da. */
  private static final DonanteDonacionVista UTILES =
      donacion(
          "u1",
          "Útiles escolares",
          "2 cajas",
          "Escuela Ejemplo N.º 12",
          EstadoEntrega.CON_PROBLEMA,
          "No pudo llegar; sale de nuevo el 17 de octubre",
          null,
          "El camino estaba cortado. Última noticia: 6 de noviembre, 16:10.",
          3);

  /** Una confirmada con la foto de ejemplo y otra sin foto, como las del sistema. */
  private static final DonanteDonacionVista ROPA =
      confirmada("r1", "Ropa de abrigo", "12 prendas", "/img/ejemplo-entrega.svg");

  private static final DonanteDonacionVista FIDEOS = confirmada("f1", "Fideos secos", "8 kg", null);

  @BeforeEach
  void cuentaDeEmpresa() {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(perfiles.tipo(DONANTE)).thenReturn(Tipo.EMPRESA);
  }

  @Test
  void laEmpresaVeElProblemaLaQueViajaLoQueSigueYLoConfirmadoEnEseOrden() throws Exception {
    when(inicios.inicio(DONANTE))
        .thenReturn(inicio(List.of(LENTEJAS, SILLA, UTILES, ROPA, FIDEOS), 2));

    String html =
        mvc.perform(get("/donante-inicio"))
            .andExpect(status().isOk())
            .andExpect(
                content()
                    .string(
                        stringContainsInOrder(
                            "<h1>Hola, ",
                            "class=\"aviso\" data-tipo=\"error\"",
                            "Útiles escolares, 2 cajas: no pudo llegar",
                            "data-papel=\"protagonista\"",
                            ">En camino ahora<",
                            "Lentejas secas, 15 kg",
                            ">Seguir esta donación<",
                            ">También en curso<",
                            "Silla plegable, 1 unidad",
                            ">Ya confirmadas<",
                            "Ropa de abrigo, 12 prendas",
                            "Fideos secos, 8 kg")))
            .andExpect(
                content().string(containsString("href=\"/donante-seguimiento?donacion=l1\"")))
            .andExpect(
                content()
                    .string(containsString("href=\"/donante-donaciones-detalle?donacion=u1\"")))
            .andExpect(
                content()
                    .string(containsString("href=\"/donante-donaciones-detalle?donacion=s1\"")))
            // La de la foto de ejemplo va en la etiqueta; la otra, en una lámina con el mismo
            // sello. Quién recibió no llega (HG-03): la organización.
            .andExpect(
                content()
                    .string(containsString("class=\"etiqueta-entrega etiqueta-entrega--costado\"")))
            .andExpect(content().string(containsString(">Foto de ejemplo<")))
            .andExpect(
                content()
                    .string(
                        containsString(
                            "Recibió: Merendero Ejemplo Sur, el 5 de noviembre de 2026.")))
            .andReturn()
            .getResponse()
            .getContentAsString();

    // Una sola lámina protagonista y una sola acción llena.
    assertThat(cuantas(html, "data-papel=\"protagonista\"")).isEqualTo(1);
    assertThat(cuantas(html, "data-tono=\"principal\"")).isEqualTo(1);
    assertThat(cuantas(html, "Entrega confirmada</span>")).isEqualTo(2);
    // A04 no usa el tablero de antes: solo la lista del inicio.
    verify(inicios, never()).tablero(any(), anyInt(), anyInt(), anyInt());
  }

  @Test
  void laQueViajaDiceDesdeCuandoSinHoraEstimada() throws Exception {
    when(inicios.inicio(DONANTE)).thenReturn(inicio(List.of(LENTEJAS), 0));

    mvc.perform(get("/donante-inicio"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    containsString(
                        "Paso 4 de 5. En camino desde las 14:30. Falta confirmar la entrega: la"
                            + " confirma Comedor Barrio Ejemplo con una foto.")))
        .andExpect(
            content()
                .string(
                    containsString(
                        ">Posición aproximada, actualizada hoy a las 16:25.</figcaption>")))
        .andExpect(content().string(not(containsString("estimada"))))
        .andExpect(content().string(not(containsString("Salió hoy"))));

    // La nota de Donaciones con el día: el texto lo conserva.
    when(inicios.inicio(DONANTE))
        .thenReturn(
            inicio(
                List.of(
                    donacion(
                        "l2",
                        "Arroz",
                        "5 kg",
                        null,
                        EstadoEntrega.EN_CAMINO,
                        "Salió el 6 de noviembre, 9:15",
                        null,
                        null,
                        3)),
                0));
    mvc.perform(get("/donante-inicio"))
        .andExpect(
            content()
                .string(
                    containsString(
                        "En camino desde el 6 de noviembre, 9:15. Falta confirmar la entrega: la"
                            + " confirma la organización con una foto.")))
        .andExpect(content().string(not(containsString("</figcaption>"))));
  }

  @Test
  void laRaizDeLaQueViajaMarcaLoHechoElPasoActualYLoQueFalta() throws Exception {
    when(inicios.inicio(DONANTE)).thenReturn(inicio(List.of(LENTEJAS), 0));

    String html =
        mvc.perform(get("/donante-inicio"))
            .andExpect(status().isOk())
            .andExpect(
                content()
                    .string(
                        containsString(
                            "data-forma=\"acostada\" aria-label=\"El recorrido de esta donación:"
                                + " va por el paso 4 de 5\"")))
            .andExpect(content().string(not(containsString("data-paso=\"pendiente\""))))
            // El mapa es el dibujo de la guía, decorativo: la posición va en el texto.
            .andExpect(content().string(containsString("mapa-recorrido.svg#mapa-recorrido")))
            .andExpect(content().string(not(containsString("role=\"img\""))))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(cuantas(html, "data-paso=\"hecho\"")).isEqualTo(3);
    assertThat(cuantas(html, "data-paso=\"actual\" aria-current=\"step\"")).isEqualTo(1);
    assertThat(cuantas(html, "data-paso=\"falta\"")).isEqualTo(1);
  }

  @Test
  void tambienEnCursoMuestraHastaTresConSuPasoYLlevaADonaciones() throws Exception {
    List<DonanteDonacionVista> todas = new ArrayList<>();
    todas.add(LENTEJAS);
    IntStream.rangeClosed(1, 5)
        .mapToObj(
            i ->
                donacion(
                    "p" + i,
                    "Pendiente " + i,
                    i + " kg",
                    i == 1 ? null : "Hogar Ejemplo",
                    EstadoEntrega.PENDIENTE,
                    "En el depósito, espera destino",
                    null,
                    i == 1 ? null : "Sale del depósito el 9 de noviembre.",
                    i == 1 ? 0 : 1))
        .forEach(todas::add);
    when(inicios.inicio(DONANTE)).thenReturn(inicio(todas, 0));

    mvc.perform(get("/donante-inicio"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    stringContainsInOrder(
                        "Pendiente 1, 1 kg",
                        "data-estado=\"registrada\"",
                        "Registrada</span>",
                        "Paso 1 de 5. Todavía sin organización. En el depósito, espera destino.",
                        "Pendiente 2, 2 kg",
                        "data-estado=\"asignada\"",
                        "Asignada</span>",
                        "Paso 2 de 5. Para Hogar Ejemplo. Sale del depósito el 9 de noviembre.",
                        "Pendiente 3, 3 kg")))
        .andExpect(content().string(not(containsString("Pendiente 4, 4 kg"))))
        .andExpect(
            content()
                .string(
                    containsString(
                        "6 donaciones en curso, contando la que viaja ahora. Actualizado hoy, 7 de"
                            + " noviembre, 19:40.")))
        .andExpect(content().string(containsString("href=\"/donante-donaciones\">Ver todas en")));
  }

  @Test
  void elAvisoDelProblemaNoPrometeUnaNuevaSalida() throws Exception {
    when(inicios.inicio(DONANTE)).thenReturn(inicio(List.of(UTILES), 0));

    mvc.perform(get("/donante-inicio"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    stringContainsInOrder(
                        "<p class=\"aviso-titulo\"><a"
                            + " href=\"/donante-donaciones-detalle?donacion=u1\">Útiles escolares,"
                            + " 2 cajas: no pudo llegar</a></p>",
                        "El camino estaba cortado. Última noticia: 6 de noviembre, 16:10.",
                        "Todavía sin fecha de",
                        "nueva salida.")))
        .andExpect(content().string(not(containsString("sale de nuevo"))))
        .andExpect(content().string(not(containsString("17 de octubre"))));
  }

  @Test
  void sinNadaEnCaminoLaLaminaLoDiceYNoEsProtagonista() throws Exception {
    when(inicios.inicio(DONANTE)).thenReturn(inicio(List.of(SILLA), 0));

    mvc.perform(get("/donante-inicio"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString(">Nada en camino ahora<")))
        .andExpect(
            content()
                .string(containsString("Hoy, 7 de noviembre, ningún camión lleva tus donaciones.")))
        .andExpect(content().string(containsString("1 donación en curso. Actualizado hoy")))
        .andExpect(content().string(not(containsString("data-papel=\"protagonista\""))))
        .andExpect(content().string(not(containsString("Seguir esta donación"))));
  }

  @Test
  void sinOtrasEnCursoNiConfirmadasLoDiceSinCelebrar() throws Exception {
    when(inicios.inicio(DONANTE)).thenReturn(inicio(List.of(LENTEJAS), 0));

    mvc.perform(get("/donante-inicio"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("No hay otras donaciones en curso.")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "<strong>Todavía no hay entregas confirmadas</strong> en noviembre.")))
        .andExpect(content().string(containsString("Cuando una organización confirme una entrega")))
        .andExpect(content().string(not(containsString("class=\"huerta-frutos\""))))
        .andExpect(content().string(not(containsString("Ver las confirmadas"))));
  }

  @Test
  void muestraUnFrutoPorEntregaConfirmadaDelMes() throws Exception {
    when(inicios.inicio(DONANTE)).thenReturn(inicio(List.of(ROPA, FIDEOS), 2));

    mvc.perform(get("/donante-inicio"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    containsString(
                        "<strong>2 entregas confirmadas</strong> en noviembre. Cada fruto es"
                            + " una.")))
        .andExpect(
            content()
                .string(
                    matchesPattern(
                        Pattern.compile(
                            "(?s).*<ul class=\"huerta-frutos\" role=\"list\""
                                + " aria-hidden=\"true\">\\s*<li></li>\\s*<li></li>\\s*</ul>.*"))))
        .andExpect(
            content()
                .string(
                    containsString(
                        "href=\"/donante-donaciones?estado=entregada\">Ver las confirmadas")));
  }

  @Test
  void elMenuDeLaEmpresaEsInicioDonacionesOrganizacionesYComoDonar() throws Exception {
    when(inicios.inicio(DONANTE)).thenReturn(inicio(List.of(SILLA), 0));

    String html =
        mvc.perform(get("/donante-inicio"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    // Sin Seguimiento (DA-03a) y con Cómo donar, que es Para donantes (DA-05).
    assertThat(menu(html))
        .containsExactly(
            "/donante-inicio Inicio",
            "/donante-donaciones Donaciones",
            "/donante-entidades Organizaciones",
            "/publico-para-donantes Cómo donar");
  }

  @Test
  void elMenuDeLaPersonaConIngresoSumaMisionesAntesDeComoDonar() throws Exception {
    when(perfiles.tipo(DONANTE)).thenReturn(Tipo.PERSONA);
    when(inicios.inicio(DONANTE)).thenReturn(CON_SILLA);
    when(inicios.anio(DONANTE)).thenReturn(ANIO_CON_SILLA);

    String html =
        mvc.perform(get("/donante-inicio"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    // MENU_PERSONA de Pantallas: el del login real, sin la cuenta de ejemplo de la maqueta.
    assertThat(menu(html))
        .containsExactly(
            "/donante-inicio Inicio",
            "/donante-donaciones Donaciones",
            "/donante-entidades Organizaciones",
            "/donante-misiones Misiones",
            "/publico-para-donantes Cómo donar");
  }

  private static final Pattern MENU =
      Pattern.compile("<nav[^>]*id=\"menu\"[^>]*>(.*?)</nav>", Pattern.DOTALL);
  private static final Pattern ENTRADA =
      Pattern.compile("<a href=\"([^\"]+)\"[^>]*>.*?<span>([^<]+)</span></a>", Pattern.DOTALL);

  /**
   * Las entradas del menú de la página (el {@code nav#menu} del layout privado), cada una como
   * "dirección texto", en orden.
   */
  static List<String> menu(String html) {
    Matcher nav = MENU.matcher(html);
    assertThat(nav.find()).as("el menú de la página").isTrue();
    Matcher entrada = ENTRADA.matcher(nav.group(1));
    List<String> entradas = new ArrayList<>();
    while (entrada.find()) {
      entradas.add(entrada.group(1) + " " + entrada.group(2));
    }
    return entradas;
  }

  /** Cuántas veces aparece {@code texto} en la página. */
  private static int cuantas(String html, String texto) {
    return StringUtils.countOccurrencesOf(html, texto);
  }

  private static DonanteInicioVista inicio(List<DonanteDonacionVista> donaciones, int delMes) {
    return new DonanteInicioVista(
        donaciones,
        List.of(),
        0,
        delMes,
        "noviembre de 2026",
        "7 de noviembre",
        "7 de noviembre, 19:40",
        "1 de noviembre",
        "2026-11-07",
        "2026-11-06");
  }

  /**
   * Una donación por el paso {@code hechos + 1}: los anteriores, hechos; los que siguen, faltan.
   */
  @SuppressWarnings("java:S107") // Una donación de ejemplo, con sus datos.
  private static DonanteDonacionVista donacion(
      String clave,
      String titulo,
      String cantidad,
      String destino,
      EstadoEntrega estado,
      String nota,
      String foto,
      String seguimiento,
      int hechos) {
    List<PasoRecorrido> pasos =
        IntStream.range(0, 5)
            .mapToObj(
                i ->
                    new PasoRecorrido(
                        "Paso " + (i + 1),
                        i < hechos ? "1 de noviembre" : "Pendiente",
                        avance(i, hechos)))
            .toList();
    return new DonanteDonacionVista(
        clave,
        1,
        titulo,
        cantidad,
        null,
        null,
        destino,
        "1 de noviembre de 2026",
        estado == EstadoEntrega.ENTREGADA ? "5 de noviembre de 2026" : null,
        estado,
        nota,
        foto,
        foto == null ? null : "Cajas junto a la puerta",
        seguimiento,
        pasos,
        List.of());
  }

  private static Avance avance(int paso, int hechos) {
    if (paso < hechos) {
      return Avance.HECHO;
    }
    return paso == hechos ? Avance.ACTUAL : Avance.PENDIENTE;
  }

  private static DonanteDonacionVista confirmada(
      String clave, String titulo, String cantidad, String foto) {
    return donacion(
        clave,
        titulo,
        cantidad,
        "Merendero Ejemplo Sur",
        EstadoEntrega.ENTREGADA,
        "Entrega confirmada el 5 de noviembre",
        foto,
        null,
        5);
  }

  @Test
  void laEmpresaNoVeLaHuertaNiQuePlantaSos() throws Exception {
    when(inicios.inicio(DONANTE)).thenReturn(CON_SILLA);

    mvc.perform(get("/donante-inicio"))
        .andExpect(status().isOk())
        .andExpect(content().string(not(containsString("mes a mes"))))
        .andExpect(content().string(not(containsString("Ver mi huerta"))))
        .andExpect(content().string(not(containsString("planta sos vos"))))
        .andExpect(content().string(not(containsString("href=\"/donante-misiones\""))));
  }

  /**
   * DA-03c y DA-08: la persona deriva de A04 (saludo, en camino, también en curso, ya confirmadas y
   * novedades), con la huerta como tarjeta chica debajo y sin lo que es solo de la empresa. Pide lo
   * mismo que la empresa, más su año.
   */
  @Test
  void laPersonaVeSuInicioComoA04ConLaHuertaYSinLoDeLaEmpresa() throws Exception {
    when(perfiles.tipo(DONANTE)).thenReturn(Tipo.PERSONA);
    when(inicios.inicio(DONANTE))
        .thenReturn(conAvisos(List.of(LENTEJAS, SILLA, ROPA, FIDEOS), 2, 4));
    when(inicios.anio(DONANTE)).thenReturn(ANIO_CON_SILLA);

    String html =
        mvc.perform(get("/donante-inicio"))
            .andExpect(status().isOk())
            .andExpect(
                content()
                    .string(
                        stringContainsInOrder(
                            "<h1>Hola, ",
                            "data-papel=\"protagonista\"",
                            ">En camino ahora<",
                            "Lentejas secas, 15 kg",
                            "data-tono=\"principal\" href=\"/donante-seguimiento?donacion=l1\"",
                            ">Seguir esta donación<",
                            ">También en curso<",
                            "Silla plegable, 1 unidad",
                            ">Ya confirmadas<",
                            "<strong>2 entregas confirmadas</strong> del 1 al 7 de noviembre.",
                            "Ropa de abrigo, 12 prendas",
                            "id=\"anio-huerta\"",
                            "data-tono=\"secundario\" href=\"/donante-misiones\">Ver mi huerta<",
                            "id=\"anio-meses\"",
                            ">Últimas novedades<",
                            "href=\"/donante-notificaciones\">Ver todas</a>")))
            .andExpect(content().string(containsString("Recibió: Merendero Ejemplo Sur")))
            .andExpect(content().string(containsString(">Misiones<")))
            .andExpect(content().string(not(containsString("Ver las 6 en una tabla"))))
            .andExpect(content().string(not(containsString("tu empresa"))))
            .andExpect(content().string(not(containsString("planta sos vos"))))
            .andExpect(content().string(not(containsString("Tu próxima misión"))))
            .andExpect(content().string(not(containsString("cuenta=persona"))))
            .andReturn()
            .getResponse()
            .getContentAsString();

    // La cabecera sin botones (DA-03a): Seguimiento se abre desde la donación.
    assertThat(html).doesNotContain(">Mis donaciones</a>").doesNotContain(">Ver seguimiento<");
    // Una sola lámina protagonista y una sola acción llena.
    assertThat(cuantas(html, "data-papel=\"protagonista\"")).isEqualTo(1);
    assertThat(cuantas(html, "data-tono=\"principal\"")).isEqualTo(1);
    // Las 3 últimas novedades, no las 4.
    assertThat(cuantas(html, "<li class=\"noticia")).isEqualTo(3);
    // La persona pide lo mismo que la empresa, más su año; nunca el tablero.
    verify(inicios).inicio(DONANTE);
    verify(inicios).anio(DONANTE);
    verify(inicios, never()).tablero(any(), anyInt(), anyInt(), anyInt());
  }

  /** DON-P3: sin nada en camino, la huerta es la protagonista; con una en camino, una tarjeta. */
  @Test
  void sinNadaEnCaminoLaHuertaEsLaProtagonistaYConUnaEnCaminoEsUnaTarjeta() throws Exception {
    when(perfiles.tipo(DONANTE)).thenReturn(Tipo.PERSONA);
    when(inicios.anio(DONANTE)).thenReturn(ANIO_CON_SILLA);

    when(inicios.inicio(DONANTE)).thenReturn(inicio(List.of(SILLA, ROPA), 1));
    String sinCamino = pagina("/donante-inicio");
    assertThat(sinCamino)
        .contains(
            "id=\"anio-huerta\" class=\"tarjeta huerta-caja\" data-papel=\"protagonista\"",
            "data-tono=\"principal\" href=\"/donante-misiones\">Ver mi huerta<")
        .doesNotContain("Nada en camino ahora")
        .doesNotContain("Seguir esta donación");
    assertThat(cuantas(sinCamino, "data-papel=\"protagonista\"")).isEqualTo(1);
    assertThat(cuantas(sinCamino, "data-tono=\"principal\"")).isEqualTo(1);
    assertThat(sinCamino.indexOf("id=\"anio-huerta\""))
        .as("la huerta, arriba de lo que sigue en curso")
        .isLessThan(sinCamino.indexOf(">También en curso<"));

    when(inicios.inicio(DONANTE)).thenReturn(inicio(List.of(LENTEJAS, SILLA), 0));
    String conCamino = pagina("/donante-inicio");
    assertThat(conCamino)
        .contains(
            "data-tono=\"principal\" href=\"/donante-seguimiento?donacion=l1\"",
            "id=\"anio-huerta\" class=\"tarjeta huerta-caja\" aria-labelledby",
            "data-tono=\"secundario\" href=\"/donante-misiones\">Ver mi huerta<");
    assertThat(cuantas(conCamino, "data-papel=\"protagonista\"")).isEqualTo(1);
    assertThat(cuantas(conCamino, "data-tono=\"principal\"")).isEqualTo(1);
  }

  /** DA-08: con una donación con problema, el aviso va arriba de la lámina y no promete fecha. */
  @Test
  void conUnaConProblemaElAvisoVaArribaDeLaLamina() throws Exception {
    when(perfiles.tipo(DONANTE)).thenReturn(Tipo.PERSONA);
    when(inicios.inicio(DONANTE)).thenReturn(inicio(List.of(UTILES, LENTEJAS), 0));
    when(inicios.anio(DONANTE)).thenReturn(ANIO_CON_SILLA);

    mvc.perform(get("/donante-inicio"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    stringContainsInOrder(
                        "<h1>Hola, ",
                        "class=\"aviso\" data-tipo=\"error\"",
                        "href=\"/donante-donaciones-detalle?donacion=u1\"",
                        "Útiles escolares, 2 cajas: no pudo llegar",
                        "El camino estaba cortado.",
                        "Todavía sin fecha de nueva salida.",
                        "data-papel=\"protagonista\"",
                        ">En camino ahora<")))
        .andExpect(content().string(not(containsString("sale de nuevo"))));
  }

  /**
   * DON-P2: el almanaque chico, con los 12 meses, la cuenta de cada mes y un fruto por entrega
   * confirmada; sin plantas, sin lista mes por mes y sin marca de falta en el mes sin donaciones.
   */
  @Test
  void elAlmanaqueTieneDoceMesesConFrutosYSinListaMesPorMes() throws Exception {
    when(perfiles.tipo(DONANTE)).thenReturn(Tipo.PERSONA);
    when(inicios.inicio(DONANTE)).thenReturn(inicio(List.of(LENTEJAS, ROPA, FIDEOS), 2));
    when(inicios.anio(DONANTE)).thenReturn(anio(5, List.of(LENTEJAS, ROPA, FIDEOS), true));

    String html = pagina("/donante-inicio");
    String almanaque =
        html.substring(
            html.indexOf("<ol class=\"almanaque\""),
            html.indexOf("</ol>", html.indexOf("<ol class=\"almanaque\"")));

    assertThat(cuantas(almanaque, "<li data-mes=")).isEqualTo(12);
    assertThat(cuantas(almanaque, "data-mes=\"con\"")).isEqualTo(1);
    assertThat(cuantas(almanaque, "data-mes=\"sin\"")).isEqualTo(10);
    assertThat(cuantas(almanaque, "data-mes=\"por-venir\"")).isEqualTo(1);
    assertThat(almanaque)
        .contains("href=\"/donante-donaciones?mes=2026-11\"")
        .contains(">Este mes<")
        .contains(">5<")
        .contains(" donaciones, 2 con la entrega confirmada entre las 3 más recientes")
        .contains(">+2<")
        .doesNotContain("planta-")
        .doesNotContain("<img");
    // Un fruto por cada una de las 2 confirmadas de noviembre.
    assertThat(almanaque)
        .containsPattern(
            "<ul class=\"huerta-frutos\" data-plantas role=\"list\" aria-hidden=\"true\">"
                + "\\s*<li></li>\\s*<li></li>\\s*</ul>");
    assertThat(html).doesNotContain("id=\"mes-").doesNotContain("mes a mes");
    // El mes sin donaciones, tranquilo: sin marca de falta ni racha.
    assertThat(almanaque)
        .contains("Sin donaciones")
        .doesNotContain("racha")
        .doesNotContain("falta");
  }

  /** DON-P2: un mes sin confirmadas entre las que trae no lleva frutos ni dice de cuáles cuenta. */
  @Test
  void unMesSinConfirmadasNoLlevaFrutos() throws Exception {
    when(perfiles.tipo(DONANTE)).thenReturn(Tipo.PERSONA);
    when(inicios.inicio(DONANTE)).thenReturn(inicio(List.of(LENTEJAS, SILLA), 0));
    when(inicios.anio(DONANTE)).thenReturn(anio(4, List.of(LENTEJAS, SILLA, UTILES), true));

    String html = pagina("/donante-inicio");
    int desde = html.indexOf("<ol class=\"almanaque\"");
    String almanaque = html.substring(desde, html.indexOf("</ol>", desde));

    assertThat(almanaque)
        .containsPattern("class=\"solo-lectores\"\\s*> donaciones</span>")
        .contains(">+1<")
        .doesNotContain("huerta-frutos")
        .doesNotContain("entre las");
  }

  @Test
  void sinTipoEsPersonaSinHuertaNiMisiones() throws Exception {
    when(perfiles.tipo(DONANTE)).thenReturn(Tipo.SIN_TIPO);
    when(inicios.inicio(DONANTE)).thenReturn(inicio(List.of(SILLA), 0));
    when(inicios.anio(DONANTE)).thenReturn(ANIO_CON_SILLA);

    mvc.perform(get("/donante-inicio"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("id=\"anio-meses\"")))
        // Sin la huerta, la variante b no aplica: la lámina dice que no hay nada en camino.
        .andExpect(content().string(containsString(">Nada en camino ahora<")))
        .andExpect(content().string(not(containsString("data-papel=\"protagonista\""))))
        .andExpect(content().string(not(containsString("Ver mi huerta"))))
        .andExpect(content().string(not(containsString("href=\"/donante-misiones\""))))
        .andExpect(content().string(not(containsString("tu empresa"))));
  }

  @Test
  void laPersonaSinDonacionesVeElEstadoVacio() throws Exception {
    when(perfiles.tipo(DONANTE)).thenReturn(Tipo.PERSONA);
    when(inicios.inicio(DONANTE)).thenReturn(vacio());
    when(inicios.anio(DONANTE)).thenReturn(anio(0, List.of(), false));

    String html = pagina("/donante-inicio");
    assertThat(html)
        .containsSubsequence(
            "<h1>Hola, ",
            "data-papel=\"protagonista\"",
            "data-tono=\"principal\" href=\"/donante-misiones\">Ver mi huerta<",
            "Todavía no tenés donaciones.",
            "Cuando lleves algo al depósito y lo registren, aparece acá.",
            "data-tono=\"secundario\" href=\"/donante-entidades\">Conocer las organizaciones<")
        .doesNotContain("id=\"anio-meses\"")
        .doesNotContain("En camino ahora");
    assertThat(cuantas(html, "data-tono=\"principal\"")).isEqualTo(1);
  }

  @Test
  void laPersonaDeEjemploDeLaMaquetaLeeSuAnioYLaEmpresaNoLoPide() throws Exception {
    when(inicios.inicio(DONANTE)).thenReturn(CON_SILLA);
    when(inicios.anio(DONANTE)).thenReturn(ANIO_CON_SILLA);

    mvc.perform(get("/donante-inicio"))
        .andExpect(status().isOk())
        .andExpect(content().string(not(containsString("id=\"anio-meses\""))));
    verify(inicios, never()).anio(any());

    mvc.perform(get("/donante-inicio?cuenta=persona"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Hola, Martina Ruiz")))
        .andExpect(content().string(containsString("href=\"/donante-donaciones?mes=2026-11\"")));
  }

  private String pagina(String ruta) throws Exception {
    return mvc.perform(get(ruta))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  /** El inicio con {@code noticias} avisos, uno por carta de la silla repetida. */
  private static DonanteInicioVista conAvisos(
      List<DonanteDonacionVista> donaciones, int delMes, int noticias) {
    DonanteInicioVista base = inicio(donaciones, delMes);
    return new DonanteInicioVista(
        donaciones,
        IntStream.range(0, noticias).mapToObj(i -> new Aviso(SILLA, 0)).toList(),
        1,
        delMes,
        base.mes(),
        base.hoy(),
        base.actualizado(),
        base.desde(),
        base.hoyIso(),
        base.ayerIso());
  }

  @Test
  void sinCuentaMandaAIngresar() throws Exception {
    when(cuentas.donante(any())).thenReturn(Optional.empty());

    mvc.perform(get("/donante-inicio")).andExpect(redirectedUrl("/publico-ingresar"));
  }

  @Test
  void unaCuentaSinDonacionesVeElEstadoVacio() throws Exception {
    when(inicios.inicio(DONANTE)).thenReturn(vacio());

    mvc.perform(get("/donante-inicio"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Todavía no hay donaciones de tu empresa.")))
        .andExpect(content().string(containsString("href=\"/donante-entidades\"")))
        .andExpect(content().string(not(containsString("En camino ahora"))))
        .andExpect(content().string(not(containsString("También en curso"))));
  }

  private static DonanteInicioVista vacio() {
    return new DonanteInicioVista(
        List.of(),
        List.of(),
        0,
        0,
        "noviembre de 2026",
        "7 de noviembre",
        "7 de noviembre, 19:40",
        "7 de noviembre",
        "2026-11-07",
        "2026-11-06");
  }

  /**
   * El año hasta noviembre: {@code enNoviembre} registradas, de las que llegan {@code primeras}.
   */
  private static DonanteAnioVista anio(
      long enNoviembre, List<DonanteDonacionVista> primeras, boolean algunaVez) {
    List<DonanteAnioVista.Mes> meses =
        IntStream.rangeClosed(1, MESES.size())
            .mapToObj(
                m ->
                    m == MESES.size()
                        ? new DonanteAnioVista.Mes(
                            m, MESES.get(m - 1), "2026-11", enNoviembre, primeras)
                        : new DonanteAnioVista.Mes(
                            m, MESES.get(m - 1), "2026-%02d".formatted(m), 0, List.of()))
            .toList();
    return new DonanteAnioVista(
        2026,
        11,
        meses,
        enNoviembre,
        enNoviembre == 0 ? 0 : 1,
        enNoviembre == 0 ? 0 : 2,
        enNoviembre == 0 ? 0 : 2,
        algunaVez,
        primeras.isEmpty() ? List.of() : List.of(new Aviso(SILLA, 0)),
        1,
        "7 de noviembre",
        "7 de noviembre, 19:40",
        "2026-11-07",
        "2026-11-06");
  }
}
