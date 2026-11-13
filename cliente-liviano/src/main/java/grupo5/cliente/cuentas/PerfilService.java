package grupo5.cliente.cuentas;

import grupo5.cliente.puertos.DirectorioDeCuentas;
import grupo5.cliente.puertos.PersonasDeDonaciones;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Completa el perfil de una cuenta recién creada: da de alta a la persona en Donaciones, deja la
 * constancia de los legales vigentes (la casilla del registro es obligatoria) y la anota en la
 * cuenta. Si algo falla después del alta, la persona se da de baja, así que guardar de nuevo no
 * deja duplicados.
 */
@Service
@Profile("!maqueta")
public class PerfilService {

  private final PersonasDeDonaciones donaciones;
  private final DirectorioDeCuentas cuentas;

  PerfilService(PersonasDeDonaciones donaciones, DirectorioDeCuentas cuentas) {
    this.donaciones = donaciones;
    this.cuentas = cuentas;
  }

  public void completar(String cuentaId, Perfil perfil) {
    UUID personaId = donaciones.crear(perfil.persona(cuentas.correoDe(cuentaId)));
    try {
      perfil.darDeAlta(donaciones, personaId);
      donaciones.aceptarLegalesVigentes(personaId);
      cuentas.vincular(cuentaId, perfil.atributos(personaId), perfil.plantilla());
    } catch (RuntimeException falla) {
      donaciones.eliminar(personaId);
      throw falla;
    }
  }
}
