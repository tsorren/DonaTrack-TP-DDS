package grupo5.cliente.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
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
import grupo5.cliente.vistas.DonanteTableroVista;
import grupo5.cliente.vistas.EstadoEntrega;
import grupo5.cliente.vistas.Tramo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

  /** El tablero con la silla en "En camino o por salir" y las otras dos columnas vacías. */
  private static final DonanteTableroVista TABLERO_CON_SILLA =
      new DonanteTableroVista(
          new Tramo<>(List.of(), 0, 1, 1),
          new Tramo<>(List.of(SILLA), 1, 1, 1),
          new Tramo<>(List.of(), 0, 1, 1),
          0);

  @BeforeEach
  void cuentaDeEmpresa() {
    when(cuentas.donante(any())).thenReturn(Optional.of(DONANTE));
    when(perfiles.tipo(DONANTE)).thenReturn(Tipo.EMPRESA);
    when(inicios.tablero(eq(DONANTE), anyInt(), anyInt(), anyInt())).thenReturn(TABLERO_CON_SILLA);
  }

  /** Una columna de {@code total} donaciones en la página {@code pagina}, con la silla. */
  private static Tramo<DonanteDonacionVista> columna(long total, int pagina) {
    return new Tramo<>(
        List.of(SILLA), total, pagina, Tramo.paginas(total, DonanteTableroVista.POR_COLUMNA));
  }

  @Test
  void elTableroVaEnElOrdenDelBocetoConLaCifraDeCadaColumna() throws Exception {
    when(inicios.inicio(DONANTE)).thenReturn(CON_SILLA);
    when(inicios.tablero(DONANTE, 1, 1, 1))
        .thenReturn(new DonanteTableroVista(columna(1, 1), columna(5, 1), columna(1284, 1), 2));

    mvc.perform(get("/donante-inicio"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    matchesPattern(
                        Pattern.compile(
                            "(?s).*Para tener en cuenta · 1<.*En camino o por salir · 5<"
                                + ".*Ya llegó · 1.284<.*"))))
        .andExpect(content().string(containsString("Ya llegó, 1.284 donaciones, página 1 de 428")))
        .andExpect(content().string(containsString("Para tener en cuenta, 1 donación<")))
        .andExpect(content().string(containsString("Mostrando 1 a 3 de 5.")))
        .andExpect(content().string(containsString("Mostrando 1 a 3 de 1.284.")))
        .andExpect(content().string(containsString("aria-label=\"Páginas de Ya llegó\"")))
        .andExpect(
            content().string(containsString("aria-label=\"Páginas de En camino o por salir\"")))
        .andExpect(content().string(not(containsString("Páginas de Para tener en cuenta"))))
        .andExpect(
            content().string(containsString("href=\"/donante-inicio?llego=2#tablero-llego\"")))
        .andExpect(content().string(containsString("2 donaciones esperan destino en el depósito.")))
        .andExpect(content().string(containsString("id=\"tablero-llego\"")))
        // Las cifras de arriba salen de los totales de las columnas: 1 con problema + 5 en curso.
        .andExpect(
            content()
                .string(
                    matchesPattern(
                        Pattern.compile("(?s).*<b>6</b>\\s*<span>entregas en curso hoy.*"))))
        .andExpect(content().string(not(containsString("autofocus"))));
  }

  @Test
  void cadaColumnaLeeSuPaginaDeLaDireccionYConservaLasOtras() throws Exception {
    when(inicios.inicio(DONANTE)).thenReturn(CON_SILLA);
    when(inicios.tablero(DONANTE, 1, 2, 1))
        .thenReturn(new DonanteTableroVista(columna(1, 1), columna(5, 2), columna(48, 1), 0));

    mvc.perform(get("/donante-inicio?camino=2&llego=abc"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    containsString("href=\"/donante-inicio?camino=2&amp;llego=2#tablero-llego\"")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "href=\"/donante-inicio?llego=abc&amp;camino=1#tablero-camino\"")))
        .andExpect(content().string(containsString("Mostrando 4 a 5 de 5.")));
    verify(inicios).tablero(DONANTE, 1, 2, 1);
  }

  @Test
  void conHtmxLaColumnaTocadaTomaElFocoYNoLlevaElAncla() throws Exception {
    when(inicios.inicio(DONANTE)).thenReturn(CON_SILLA);
    when(inicios.tablero(DONANTE, 1, 1, 2))
        .thenReturn(new DonanteTableroVista(columna(1, 1), columna(5, 1), columna(48, 2), 0));

    mvc.perform(
            get("/donante-inicio?llego=2")
                .header("HX-Request", "true")
                .header("X-Tablero", "llego"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    matchesPattern(
                        Pattern.compile(
                            "(?s).*<h3 id=\"llego-titulo\" tabindex=\"-1\" autofocus.*"))))
        .andExpect(content().string(not(containsString("id=\"tablero-llego\""))))
        .andExpect(content().string(containsString("data-columna=\"llego\"")));
  }

  @Test
  void conHtmxLaRespuestaRenuevaLosPiesDeLasOtrasColumnasConLasPaginasVigentes() throws Exception {
    when(inicios.inicio(DONANTE)).thenReturn(CON_SILLA);
    when(inicios.tablero(DONANTE, 1, 2, 1))
        .thenReturn(new DonanteTableroVista(columna(1, 1), columna(5, 2), columna(48, 1), 0));

    // Se tocó "›" en "En camino": el pie de "Ya llegó" que viene en la respuesta (y que HTMX pone
    // en lugar del viejo con hx-select-oob) ya lleva camino=2.
    mvc.perform(
            get("/donante-inicio?camino=2")
                .header("HX-Request", "true")
                .header("X-Tablero", "camino"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("id=\"pie-llego\"")))
        .andExpect(content().string(containsString("id=\"pie-camino\"")))
        .andExpect(
            content()
                .string(
                    containsString("href=\"/donante-inicio?camino=2&amp;llego=2#tablero-llego\"")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "hx-select-oob=\"#pie-tener,#pie-llego,#reintentar-tener,#reintentar-llego\"")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "hx-select-oob=\"#pie-tener,#pie-camino,#reintentar-tener,#reintentar-camino\"")));
  }

  @Test
  void elVolverAIntentarDeUnaColumnaQueNoCargoLlevaLaDireccionVigente() throws Exception {
    when(inicios.inicio(DONANTE)).thenReturn(CON_SILLA);
    when(inicios.tablero(DONANTE, 1, 2, 1))
        .thenReturn(new DonanteTableroVista(columna(1, 1), columna(5, 2), null, 0));

    mvc.perform(
            get("/donante-inicio?camino=2")
                .header("HX-Request", "true")
                .header("X-Tablero", "camino"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    matchesPattern(
                        Pattern.compile(
                            "(?s).*href=\"/donante-inicio\\?camino=2\"\\s+id=\"reintentar-llego\".*"))));
  }

  @Test
  void unaColumnaQueNoCargaNoTumbaLasOtras() throws Exception {
    when(inicios.inicio(DONANTE)).thenReturn(CON_SILLA);
    when(inicios.tablero(DONANTE, 1, 1, 1))
        .thenReturn(new DonanteTableroVista(columna(1, 1), columna(5, 1), null, 0));

    mvc.perform(get("/donante-inicio"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("No pudimos cargar esta columna.")))
        .andExpect(content().string(containsString("Volver a intentar")))
        .andExpect(content().string(containsString("En camino o por salir · 5<")))
        .andExpect(content().string(matchesPattern(Pattern.compile("(?s).*>Ya llegó</span>.*"))));
  }

  @Test
  void lasColumnasVaciasLoDicenSinCelebrar() throws Exception {
    when(inicios.inicio(DONANTE)).thenReturn(CON_SILLA);
    Tramo<DonanteDonacionVista> ninguna = new Tramo<>(List.of(), 0, 1, 1);
    when(inicios.tablero(DONANTE, 1, 1, 1))
        .thenReturn(new DonanteTableroVista(ninguna, ninguna, ninguna, 1));

    mvc.perform(get("/donante-inicio"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Ninguna donación con problema.")))
        .andExpect(content().string(containsString("1 donación espera destino en el depósito.")))
        .andExpect(
            content().string(containsString("Ninguna en camino ni por salir hoy, 7 de noviembre.")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "Todavía no llegó ninguna. Cuando una organización confirme una entrega, la"
                            + " vas a ver acá.")))
        .andExpect(content().string(not(containsString("Mostrando"))));

    when(inicios.tablero(DONANTE, 1, 1, 1))
        .thenReturn(new DonanteTableroVista(ninguna, ninguna, ninguna, 0));
    mvc.perform(get("/donante-inicio"))
        .andExpect(content().string(containsString("Nada para tener en cuenta por ahora.")));
  }

  @Test
  void muestraLasCifrasYLasNoticiasDeHoy() throws Exception {
    when(inicios.inicio(DONANTE)).thenReturn(CON_SILLA);

    mvc.perform(get("/donante-inicio"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("en noviembre de 2026. Actualizado hoy,")))
        .andExpect(content().string(containsString("Noviembre de 2026")))
        .andExpect(content().string(containsString("entregas en curso hoy, 7 de noviembre")))
        .andExpect(content().string(containsString("desde el 1 de noviembre")))
        .andExpect(content().string(containsString("Va a Hogar Ejemplo.")))
        .andExpect(content().string(containsString("Hoy, 10:00")))
        .andExpect(content().string(containsString("Ver todas (1 sin leer)")));
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

  @Test
  void laPersonaVeSuAnioConLaHuertaArribaDeTodo() throws Exception {
    when(perfiles.tipo(DONANTE)).thenReturn(Tipo.PERSONA);
    when(inicios.anio(DONANTE)).thenReturn(ANIO_CON_SILLA);

    mvc.perform(get("/donante-inicio"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Este es tu 2026, mes a mes")))
        .andExpect(content().string(containsString("Noviembre, este mes · 5 donaciones")))
        .andExpect(content().string(containsString("Silla plegable, 1 unidad")))
        .andExpect(content().string(containsString("Donaste 5 veces en 2026.")))
        .andExpect(content().string(containsString("5 donaciones registradas")))
        .andExpect(content().string(containsString("<dd>2</dd>")))
        .andExpect(content().string(containsString("href=\"/donante-donaciones?mes=2026-11\"")))
        .andExpect(content().string(containsString("Ver todas las de noviembre (5)")))
        .andExpect(content().string(containsString("Ver todas (1 sin leer)")))
        .andExpect(
            content()
                .string(
                    matchesPattern(
                        Pattern.compile(
                            "(?s).*Ver mi huerta.*id=\"anio-meses\".*", Pattern.DOTALL))))
        .andExpect(content().string(containsString(">Misiones<")))
        .andExpect(content().string(not(containsString("planta sos vos"))))
        .andExpect(content().string(not(containsString("Tu próxima misión"))))
        .andExpect(content().string(not(containsString("cuenta=persona"))))
        .andExpect(content().string(not(containsString("tu empresa"))));
    // La persona no pide la lista entera del inicio de la empresa (regla 1 del backlog R11).
    verify(inicios, never()).inicio(any());
    verify(inicios, never()).tablero(any(), anyInt(), anyInt(), anyInt());
  }

  @Test
  void sinTipoEsPersonaSinHuertaNiMisiones() throws Exception {
    when(perfiles.tipo(DONANTE)).thenReturn(Tipo.SIN_TIPO);
    when(inicios.anio(DONANTE)).thenReturn(ANIO_CON_SILLA);

    mvc.perform(get("/donante-inicio"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Este es tu 2026, mes a mes")))
        .andExpect(content().string(not(containsString("Ver mi huerta"))))
        .andExpect(content().string(not(containsString("href=\"/donante-misiones\""))))
        .andExpect(content().string(not(containsString("tu empresa"))));
  }

  @Test
  void laPersonaSinDonacionesVeElEstadoVacio() throws Exception {
    when(perfiles.tipo(DONANTE)).thenReturn(Tipo.PERSONA);
    when(inicios.anio(DONANTE)).thenReturn(anio(0, List.of(), false));

    mvc.perform(get("/donante-inicio"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Todavía no tenés donaciones.")))
        .andExpect(content().string(containsString("Ver mi huerta")))
        .andExpect(content().string(not(containsString("id=\"anio-meses\""))));
  }

  @Test
  void laPersonaDeEjemploDeLaMaquetaLeeSuAnioYLaEmpresaNoLoPide() throws Exception {
    when(inicios.inicio(DONANTE)).thenReturn(CON_SILLA);
    when(inicios.anio(DONANTE)).thenReturn(ANIO_CON_SILLA);

    mvc.perform(get("/donante-inicio"))
        .andExpect(status().isOk())
        .andExpect(content().string(not(containsString("mes a mes"))));
    verify(inicios, never()).anio(any());

    mvc.perform(get("/donante-inicio?cuenta=persona"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Noviembre, este mes · 5 donaciones")));
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
        .andExpect(content().string(not(containsString("Lo que <em>ya dio fruto.</em>"))));
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
