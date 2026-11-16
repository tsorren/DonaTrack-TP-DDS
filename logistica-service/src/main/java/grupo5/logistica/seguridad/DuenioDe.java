package grupo5.logistica.seguridad;

import grupo5.common.seguridad.Actor;
import grupo5.logistica.services.IEntregasService;
import grupo5.logistica.services.IRutasService;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Dueño de cada recurso, para {@code principal.puedeOperarSobre(@duenioDe.entrega(#id))}. Un
 * recurso inexistente no tiene dueño ({@code null}): quien no es admin recibe 403, igual que con
 * uno ajeno, y no puede averiguar qué ids existen.
 */
@Component("duenioDe")
public class DuenioDe {

  private final IEntregasService entregas;
  private final IRutasService rutas;

  public DuenioDe(IEntregasService entregas, IRutasService rutas) {
    this.entregas = entregas;
    this.rutas = rutas;
  }

  /** La entrega es de su beneficiaria. */
  public UUID entrega(UUID id) {
    return entregas.beneficiariaDe(id).orElse(null);
  }

  /** La entrega de una donación es de su donante (solo se conoce si llegó por mensajería). */
  public UUID donacion(UUID idDonacion) {
    return entregas.donanteDeDonacion(idDonacion).orElse(null);
  }

  /** Una ruta es de quien tenga en ella una entrega, como donante o como beneficiaria. */
  public boolean ruta(UUID id, Actor actor) {
    return rutas.organizacionesDe(id).stream().anyMatch(actor::puedeOperarSobre);
  }
}
