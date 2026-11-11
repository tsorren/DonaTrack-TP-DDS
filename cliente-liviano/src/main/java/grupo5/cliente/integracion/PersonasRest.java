package grupo5.cliente.integracion;

import grupo5.cliente.puertos.PersonaNueva;
import grupo5.cliente.puertos.PersonasDeDonaciones;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Profile("!maqueta")
class PersonasRest implements PersonasDeDonaciones {

  private record Creada(UUID id) {}

  private record Legales(Integer versionVigente) {}

  private static final String LEGALES = "/api/personas/{id}/legales";

  private final RestClient donaciones;

  PersonasRest(@Qualifier("donacionesComoServicio") RestClient donaciones) {
    this.donaciones = donaciones;
  }

  @Override
  public UUID crear(PersonaNueva persona) {
    return donaciones.post().uri("/api/personas").body(persona).retrieve().body(Creada.class).id();
  }

  @Override
  public void registrarDonante(UUID personaId) {
    enviar("/api/donantes", Map.of("idPersona", personaId));
  }

  /** La versión vigente la sabe Donaciones: se lee y se acepta esa. */
  @Override
  public void aceptarLegalesVigentes(UUID personaId) {
    Legales legales = donaciones.get().uri(LEGALES, personaId).retrieve().body(Legales.class);
    donaciones
        .put()
        .uri(LEGALES, personaId)
        .body(Map.of("version", legales.versionVigente()))
        .retrieve()
        .toBodilessEntity();
  }

  @Override
  public void eliminar(UUID personaId) {
    donaciones.delete().uri("/api/personas/{id}", personaId).retrieve().toBodilessEntity();
  }

  private void enviar(String ruta, Object cuerpo) {
    donaciones.post().uri(ruta).body(cuerpo).retrieve().toBodilessEntity();
  }
}
