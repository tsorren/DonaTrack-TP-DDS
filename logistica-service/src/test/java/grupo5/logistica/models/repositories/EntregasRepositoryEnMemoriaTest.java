package grupo5.logistica.models.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.repositories.impl.EntregasRepository;
import grupo5.logistica.testutils.EntregaMother;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EntregasRepositoryEnMemoriaTest {

  @Test
  void findByDonanteId_devuelveSoloLasEntregasDelDonanteEnOrdenDeAlta() {
    EntregasRepository repositorio = new EntregasRepository();
    UUID donanteId = UUID.randomUUID();
    Entrega primera = EntregaMother.pendiente();
    primera.registrarDonante(donanteId);
    Entrega deOtro = EntregaMother.pendiente();
    deOtro.registrarDonante(UUID.randomUUID());
    Entrega segunda = EntregaMother.pendiente();
    segunda.registrarDonante(donanteId);
    repositorio.save(primera);
    repositorio.save(deOtro);
    repositorio.save(EntregaMother.pendiente());
    repositorio.save(segunda);

    assertEquals(
        List.of(primera.getId(), segunda.getId()),
        repositorio.findByDonanteId(donanteId).stream().map(Entrega::getId).toList());
    assertTrue(repositorio.findByDonanteId(null).isEmpty());
  }
}
