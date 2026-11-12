package grupo5.cliente.rest;

import grupo5.cliente.puertos.PublicoPuerto;
import grupo5.cliente.vistas.PublicoCifra;
import grupo5.cliente.vistas.PublicoEntregas;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Las páginas públicas con los datos de Donaciones, sin sesión: los contadores (HB-02) y las
 * entregas destacadas (HB-01). Lo que Donaciones no cuenta (las donaciones en camino ahora y desde
 * cuándo se cuenta) no se muestra; la tercera cifra de la portada pasa a ser la de donantes, como
 * en Nosotros.
 */
// @Primary solo para el análisis estático (S9352): la propiedad ya deja un único adaptador activo.
@Component
@Primary
@ConditionalOnProperty(name = "cliente.datos.publico", havingValue = "rest")
class PublicoRest implements PublicoPuerto {

  private static final String ENTREGAS = "Entregas confirmadas";
  private static final String ENTIDADES = "Entidades en el mapa";
  private static final String DONANTES = "Donantes con al menos una donación";

  private static final int DIAS_DEL_MES = 30;
  private static final int DIAS_DE_LA_SEMANA = 7;
  private static final DateTimeFormatter ACTUALIZADO =
      DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, HH:mm", Comun.ES_AR);
  private static final DateTimeFormatter SOLO_DIA = DateTimeFormatter.ofPattern("d", Comun.ES_AR);
  private static final DateTimeFormatter ANIO = DateTimeFormatter.ofPattern("yyyy", Comun.ES_AR);

  private final DonacionesApi donaciones;
  private final Clock reloj;

  PublicoRest(DonacionesApi donaciones, Clock reloj) {
    this.donaciones = donaciones;
    this.reloj = reloj.withZone(Comun.ARGENTINA);
  }

  /**
   * Las destacadas del mes (HB-01, el máximo que da Donaciones: 50), con su primera foto. La
   * destacada no trae el barrio ni el texto de la foto: el lugar es la entidad y el texto
   * alternativo se arma con lo entregado.
   */
  // ponytail: cuenta las que trae (hasta 50); si hacen falta más, el total está en X-Total-Count.
  @Override
  public PublicoEntregas entregasConfirmadas() {
    MultiValueMap<String, String> filtros = new LinkedMultiValueMap<>();
    filtros.add("periodo", "MES");
    filtros.add("limite", "50");
    LocalDateTime ahora = LocalDateTime.now(reloj);
    LocalDate hoy = ahora.toLocalDate();
    LocalDate semana = hoy.minusDays(DIAS_DE_LA_SEMANA);
    List<PublicoEntregas.Entrega> lista =
        donaciones.destacadas(filtros).stream()
            .filter(d -> d.fechaEntrega() != null && d.fotos() != null && !d.fotos().isEmpty())
            .sorted(Comparator.comparing(DonacionesApi.Destacada::fechaEntrega).reversed())
            .map(d -> entrega(d, !d.fechaEntrega().toLocalDate().isBefore(semana)))
            .toList();
    return new PublicoEntregas(
        lista,
        rango(hoy.minusDays(DIAS_DEL_MES), hoy),
        rango(semana, hoy),
        ACTUALIZADO.format(ahora));
  }

  private static PublicoEntregas.Entrega entrega(DonacionesApi.Destacada d, boolean semana) {
    String nombre = d.subcategoria() != null ? d.subcategoria() : d.categoria();
    String cantidad =
        d.cantidad() == null
            ? null
            : Comun.cantidad(
                d.cantidad(),
                new DonacionesApi.Categoria(null, null, d.unidad(), null, null, null));
    String que = cantidad == null ? nombre : nombre + ", " + cantidad;
    String icono = d.categoria() == null ? null : Comun.icono(d.categoria());
    LocalDate dia = d.fechaEntrega().toLocalDate();
    return new PublicoEntregas.Entrega(
        que,
        icono == null ? null : icono.substring("cat-".length()),
        d.categoria(),
        d.entidad(),
        d.entidad(),
        Comun.DIA.format(dia),
        ANIO.format(dia),
        dia.toString(),
        semana,
        d.fotos().getFirst(),
        "Foto de " + que + " que subió " + d.entidad() + " al confirmar la entrega");
  }

  /** "del 8 al 15 de octubre de 2026", "del 16 de septiembre al 15 de octubre de 2026". */
  private static String rango(LocalDate desde, LocalDate hasta) {
    DateTimeFormatter inicio;
    if (desde.getYear() != hasta.getYear()) {
      inicio = Comun.DIA_Y_ANIO;
    } else if (desde.getMonthValue() != hasta.getMonthValue()) {
      inicio = Comun.DIA;
    } else {
      inicio = SOLO_DIA;
    }
    return "del " + inicio.format(desde) + " al " + Comun.DIA_Y_ANIO.format(hasta);
  }

  @Override
  public List<PublicoCifra> cifrasDePortada() {
    return cifras();
  }

  @Override
  public List<PublicoCifra> cifrasDeNosotros() {
    return cifras();
  }

  private List<PublicoCifra> cifras() {
    DonacionesApi.Contadores contadores = donaciones.contadores();
    return List.of(
        new PublicoCifra(numero(contadores.donacionesRealizadas()), ENTREGAS),
        new PublicoCifra(numero(contadores.beneficiariosActivos()), ENTIDADES),
        new PublicoCifra(numero(contadores.donantesActivos()), DONANTES));
  }

  /** "1.284", como se escribe en la Argentina; sin dato, 0. */
  private static String numero(Long valor) {
    return NumberFormat.getIntegerInstance(Comun.ES_AR).format(valor == null ? 0 : valor);
  }
}
