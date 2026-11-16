package grupo5.donaciones.models.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;

import grupo5.donaciones.fixtures.BienMother;
import grupo5.donaciones.fixtures.DonacionMother;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.ItemDonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.models.repositories.impl.DonacionesIndependientesRepositoryEnMemoria;
import grupo5.donaciones.models.repositories.impl.DonacionesRepositoryEnMemoria;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** HB-02: los conteos del perfil en memoria se comportan como los de JPA. */
class ContadoresEnMemoriaTest {

  @Test
  void idsDeDonantes_devuelveCadaDonanteUnaVez() {
    DonacionesRepositoryEnMemoria donaciones = new DonacionesRepositoryEnMemoria();
    UUID ana = UUID.randomUUID();
    UUID beto = UUID.randomUUID();
    donaciones.save(DonacionMother.simple(ana));
    donaciones.save(DonacionMother.simple(ana));
    donaciones.save(DonacionMother.simple(beto));

    assertEquals(Set.of(ana, beto), donaciones.idsDeDonantes());
  }

  @Test
  void contarEnEstado_cuentaSoloLasDeEseEstado() {
    DonacionesIndependientesRepositoryEnMemoria independientes =
        new DonacionesIndependientesRepositoryEnMemoria();
    independientes.save(
        new DonacionIndependiente(
            UUID.randomUUID(),
            List.of(
                new ItemDonacionIndependiente(
                    UUID.randomUUID(),
                    BienMother.aceptado(BienMother.prendaRopa("Campera"), UUID.randomUUID()),
                    3))));

    assertEquals(1, independientes.contarEnEstado(TipoEstadoDonacion.EN_DEPOSITO));
    assertEquals(0, independientes.contarEnEstado(TipoEstadoDonacion.ENTREGADA));
  }
}
