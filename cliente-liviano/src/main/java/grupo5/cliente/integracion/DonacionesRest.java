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
class DonacionesRest implements PersonasDeDonaciones {

  private record Creada(UUID id) {}

  private final RestClient donaciones;

  DonacionesRest(@Qualifier("donacionesComoServicio") RestClient donaciones) {
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

  @Override
  public void eliminar(UUID personaId) {
    donaciones.delete().uri("/api/personas/{id}", personaId).retrieve().toBodilessEntity();
  }

  private void enviar(String ruta, Object cuerpo) {
    donaciones.post().uri(ruta).body(cuerpo).retrieve().toBodilessEntity();
  }
}
