package grupo5.cliente.maqueta;

import grupo5.cliente.puertos.PublicoPuerto;
import grupo5.cliente.vistas.PublicoCifra;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Los datos de ejemplo de las páginas públicas (ficha raices-publico-navegable.md, 6.3), los que
 * antes estaban escritos en las plantillas. Los estados ({@code ?escenario=}) los sigue mostrando
 * cada plantilla: acá no se leen, porque Contacto y Entregas usan nombres que {@link Escenario} no
 * conoce.
 */
@Component
@ConditionalOnProperty(
    name = "cliente.datos.publico",
    havingValue = "maqueta",
    matchIfMissing = true)
class PublicoMaqueta implements PublicoPuerto {

  private static final String ENTREGAS = "1.284";
  private static final String ENTIDADES = "46";

  @Override
  public List<PublicoCifra> cifrasDePortada() {
    return List.of(
        new PublicoCifra(ENTREGAS, "Entregas confirmadas"),
        new PublicoCifra(ENTIDADES, "Entidades en el mapa"),
        new PublicoCifra("32", "Donaciones en camino ahora"));
  }

  @Override
  public List<PublicoCifra> cifrasDeNosotros() {
    return List.of(
        new PublicoCifra(ENTREGAS, "Entregas confirmadas desde marzo de 2026"),
        new PublicoCifra(ENTIDADES, "Entidades en el mapa"),
        new PublicoCifra("318", "Donantes con al menos una donación desde marzo de 2026"));
  }
}
