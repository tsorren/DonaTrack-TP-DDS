package grupo5.cliente.integracion;

import grupo5.cliente.puertos.DonacionesComoUsuario;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Profile("!maqueta")
class DonacionesRestComoUsuario implements DonacionesComoUsuario {

  private final RestClient donaciones;

  DonacionesRestComoUsuario(@Qualifier("donacionesComoUsuario") RestClient donaciones) {
    this.donaciones = donaciones;
  }

  @Override
  public List<DonacionDelDonante> donacionesDe(UUID donanteId) {
    return listar("/donaciones-independientes?donanteId={id}", new Donaciones(), donanteId);
  }

  @Override
  public List<NecesidadDeEntidad> necesidadesDe(UUID entidadId) {
    return listar("/api/necesidades?entidadId={id}", new Necesidades(), entidadId);
  }

  @Override
  public List<Juridica> juridicas() {
    return listar("/api/personas?tipo=JURIDICA", new Juridicas());
  }

  @Override
  public void registrarEntidad(UUID juridicaId) {
    var cuerpo = Map.of("juridicaId", juridicaId);
    donaciones.post().uri("/api/entidades").body(cuerpo).retrieve().toBodilessEntity();
  }

  private <T> List<T> listar(
      String ruta, ParameterizedTypeReference<List<T>> tipo, Object... variables) {
    return donaciones.get().uri(ruta, variables).retrieve().body(tipo);
  }

  private static class Juridicas extends ParameterizedTypeReference<List<Juridica>> {}

  private static class Donaciones extends ParameterizedTypeReference<List<DonacionDelDonante>> {}

  private static class Necesidades extends ParameterizedTypeReference<List<NecesidadDeEntidad>> {}
}
