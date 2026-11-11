package grupo5.cliente.rest;

import grupo5.cliente.puertos.PublicoPuerto;
import grupo5.cliente.vistas.PublicoCifra;
import java.text.NumberFormat;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Las páginas públicas con los datos de Donaciones, sin sesión: los contadores (HB-02). Lo que
 * Donaciones no cuenta (las donaciones en camino ahora y desde cuándo se cuenta) no se muestra; la
 * tercera cifra de la portada pasa a ser la de donantes, como en Nosotros.
 */
// @Primary solo para el análisis estático (S9352): la propiedad ya deja un único adaptador activo.
@Component
@Primary
@ConditionalOnProperty(name = "cliente.datos.publico", havingValue = "rest")
class PublicoRest implements PublicoPuerto {

  private static final String ENTREGAS = "Entregas confirmadas";
  private static final String ENTIDADES = "Entidades en el mapa";
  private static final String DONANTES = "Donantes con al menos una donación";

  private final DonacionesApi donaciones;

  PublicoRest(DonacionesApi donaciones) {
    this.donaciones = donaciones;
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
