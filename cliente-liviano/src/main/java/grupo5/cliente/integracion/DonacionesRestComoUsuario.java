package grupo5.cliente.integracion;

import grupo5.cliente.puertos.DonacionesComoUsuario;
import java.util.ArrayList;
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

  private static final int LOTE = 100;

  private final RestClient donaciones;

  DonacionesRestComoUsuario(@Qualifier("donacionesComoUsuario") RestClient donaciones) {
    this.donaciones = donaciones;
  }

  @Override
  public List<NecesidadDeEntidad> necesidadesDe(UUID entidadId) {
    return listar("/api/necesidades?entidadId={id}", new Necesidades(), entidadId);
  }

  /** De a {@link #LOTE} identificadores por pedido, el máximo que acepta Donaciones. */
  @Override
  public List<Juridica> juridicas(List<UUID> ids) {
    List<Juridica> juridicas = new ArrayList<>();
    for (int desde = 0; desde < ids.size(); desde += LOTE) {
      List<UUID> lote = ids.subList(desde, Math.min(ids.size(), desde + LOTE));
      List<Juridica> leidas =
          donaciones
              .get()
              .uri(
                  uri ->
                      uri.path("/api/personas")
                          .queryParam("id", lote.toArray())
                          .queryParam("size", LOTE)
                          .build())
              .retrieve()
              .body(new Juridicas());
      if (leidas != null) {
        juridicas.addAll(leidas);
      }
    }
    return juridicas;
  }

  @Override
  public void registrarEntidad(UUID juridicaId) {
    // origen=SOLICITUD (R11-26): el registro de actividad la anota como "aprobó la solicitud".
    var cuerpo = Map.of("juridicaId", juridicaId, "origen", "SOLICITUD");
    donaciones.post().uri("/api/entidades").body(cuerpo).retrieve().toBodilessEntity();
  }

  private <T> List<T> listar(
      String ruta, ParameterizedTypeReference<List<T>> tipo, Object... variables) {
    return donaciones.get().uri(ruta, variables).retrieve().body(tipo);
  }

  private static class Juridicas extends ParameterizedTypeReference<List<Juridica>> {}

  private static class Necesidades extends ParameterizedTypeReference<List<NecesidadDeEntidad>> {}
}
