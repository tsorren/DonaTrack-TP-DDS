package grupo5.donaciones.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.donaciones.fixtures.DonacionIndependienteMother;
import grupo5.donaciones.fixtures.NecesidadMother;
import grupo5.donaciones.models.algoritmos.DatosDeAsignacion;
import grupo5.donaciones.models.entities.necesidades.NecesidadExtraordinaria;
import grupo5.donaciones.models.entities.necesidades.NecesidadRecurrente;
import grupo5.donaciones.models.repositories.impl.DonacionesIndependientesRepositoryEnMemoria;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** DTI-06: lo asignado se calcula sumando las donaciones con ese destino. */
class AcumuladosDeAsignacionTest {

  private DonacionesIndependientesRepositoryEnMemoria donaciones;
  private AcumuladosDeAsignacion acumulados;

  @BeforeEach
  void setUp() {
    donaciones = new DonacionesIndependientesRepositoryEnMemoria();
    acumulados = new AcumuladosDeAsignacion(donaciones);
  }

  @Test
  void asignadoA_extraordinaria_sumaSusDonacionesYNoLasDeOtras() {
    NecesidadExtraordinaria necesidad =
        NecesidadMother.extraordinaria(UUID.randomUUID(), UUID.randomUUID(), 30);
    NecesidadExtraordinaria otra =
        NecesidadMother.extraordinaria(UUID.randomUUID(), UUID.randomUUID(), 30);
    donaciones.save(DonacionIndependienteMother.asignada(10, necesidad));
    donaciones.save(DonacionIndependienteMother.asignada(25, necesidad));
    donaciones.save(DonacionIndependienteMother.asignada(7, otra));

    assertEquals(35, acumulados.asignadoA(necesidad));
    assertTrue(acumulados.estaSatisfecha(necesidad));
    assertFalse(acumulados.estaSatisfecha(otra));
  }

  @Test
  void asignadoA_recurrente_cuentaSoloElPeriodoActual() {
    NecesidadRecurrente necesidad =
        NecesidadMother.recurrenteSemanal(UUID.randomUUID(), UUID.randomUUID(), 50);
    donaciones.save(DonacionIndependienteMother.asignada(50, necesidad));
    assertTrue(acumulados.estaSatisfecha(necesidad));

    necesidad.generarNuevoPeriodo();
    donaciones.save(DonacionIndependienteMother.asignada(20, necesidad));

    assertEquals(20, acumulados.asignadoA(necesidad));
    assertFalse(acumulados.estaSatisfecha(necesidad));
  }

  @Test
  void datosPara_traeLoAsignadoPorNecesidadYLasRecibidasPorEntidad() {
    UUID entidad = UUID.randomUUID();
    NecesidadExtraordinaria necesidad =
        NecesidadMother.extraordinaria(entidad, UUID.randomUUID(), 30);
    NecesidadExtraordinaria sinDonaciones =
        NecesidadMother.extraordinaria(UUID.randomUUID(), UUID.randomUUID(), 30);
    donaciones.save(DonacionIndependienteMother.asignada(10, necesidad));
    donaciones.save(DonacionIndependienteMother.asignada(5, necesidad));

    DatosDeAsignacion datos = acumulados.datosPara(List.of(necesidad, sinDonaciones));

    assertEquals(15, datos.asignadoA(necesidad.getId()));
    assertEquals(0, datos.asignadoA(sinDonaciones.getId()));
    assertEquals(2, datos.recibidasPor(entidad));
    assertEquals(0, datos.recibidasPor(sinDonaciones.getEntidadId()));
  }
}
