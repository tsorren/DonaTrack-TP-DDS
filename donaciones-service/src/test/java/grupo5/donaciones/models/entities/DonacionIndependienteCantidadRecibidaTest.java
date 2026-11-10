package grupo5.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import grupo5.donaciones.dto.comunicaciones.EventoEntregaExitosa;
import grupo5.donaciones.fixtures.BienMother;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.ItemDonacionIndependiente;
import grupo5.donaciones.models.entities.itemsNormalizados.BienNormalizado;
import grupo5.donaciones.models.entities.necesidades.DestinoAsignacion;
import grupo5.donaciones.models.repositories.impl.DonacionesIndependientesRepositoryEnMemoria;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** HB-17: la necesidad cuenta lo que llegó, no lo que salió (DTI-29). */
class DonacionIndependienteCantidadRecibidaTest {

  private final BienNormalizado bien =
      BienMother.aceptado(BienMother.prendaRopa("Campera"), UUID.randomUUID());

  /** 10 + 5 = 15 unidades, en dos ítems. */
  private DonacionIndependiente donacionDe15() {
    return new DonacionIndependiente(
        UUID.randomUUID(),
        List.of(
            new ItemDonacionIndependiente(UUID.randomUUID(), bien, 10),
            new ItemDonacionIndependiente(UUID.randomUUID(), bien, 5)));
  }

  @Test
  void cuentaLoQueLlegoYRedondeaHaciaAbajo() {
    DonacionIndependiente donacion = donacionDe15();

    donacion.registrarCantidadRecibida(12.7);

    assertEquals(15, donacion.getCantidad());
    assertEquals(3, donacion.getCantidadNoRecibida());
    assertEquals(12, donacion.getCantidadContada());
  }

  @Test
  void unaCantidadIgualOMayorQueLaEnviadaNoDescuentaNada() {
    DonacionIndependiente donacion = donacionDe15();

    donacion.registrarCantidadRecibida(15);
    assertNull(donacion.getCantidadNoRecibida());
    donacion.registrarCantidadRecibida(20);
    assertNull(donacion.getCantidadNoRecibida());
    assertEquals(15, donacion.getCantidadContada());
  }

  @Test
  void elRepositorioEnMemoriaSumaLoContado() {
    DonacionesIndependientesRepositoryEnMemoria repo =
        new DonacionesIndependientesRepositoryEnMemoria();
    UUID necesidadId = UUID.randomUUID();
    DonacionIndependiente incompleta = donacionDe15();
    incompleta.asignarDestino(new DestinoAsignacion(necesidadId, null, UUID.randomUUID()));
    incompleta.registrarCantidadRecibida(9);
    DonacionIndependiente completa = donacionDe15();
    completa.asignarDestino(new DestinoAsignacion(necesidadId, null, UUID.randomUUID()));
    repo.save(incompleta);
    repo.save(completa);

    assertEquals(24, repo.sumarCantidadAsignada(necesidadId, null));
  }

  @Test
  void leeElReporteDeEntregaExitosaYToleraCamposQueNoConoce() {
    JsonMapper mapper = JsonMapper.builder().build();
    String id = UUID.randomUUID().toString();

    EventoEntregaExitosa conReporte =
        mapper.readValue(
            """
            {"entregaId":"%s","donacionIndependienteId":"%s","fechaEntrega":"2026-11-08T10:00:00Z",
             "fotosRecepcion":["https://f/1.jpg"],
             "reporte":{"motivo":"INCOMPLETA","detalle":"Faltó una","cantidadRecibida":12.5}}
            """
                .formatted(id, id),
            EventoEntregaExitosa.class);
    EventoEntregaExitosa sinReporte =
        mapper.readValue(
            """
            {"entregaId":"%s","donacionIndependienteId":"%s","fechaEntrega":"2026-11-08T10:00:00Z"}
            """
                .formatted(id, id),
            EventoEntregaExitosa.class);

    assertEquals(
        new EventoEntregaExitosa.Reporte("INCOMPLETA", "Faltó una", 12.5), conReporte.reporte());
    assertNull(sinReporte.reporte());
  }
}
