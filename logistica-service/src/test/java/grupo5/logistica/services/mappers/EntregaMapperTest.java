package grupo5.logistica.services.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.dto.entregas.ConfirmarRecepcionRequestDTO;
import grupo5.logistica.dto.entregas.CrearEntregaRequestDTO;
import grupo5.logistica.dto.entregas.RegresarAlDepositoRequestDTO;
import grupo5.logistica.dto.entregas.ReportarNoRecepcionRequestDTO;
import grupo5.logistica.dto.rutas.DireccionDTO;
import grupo5.logistica.models.entities.entregas.Entrega;
import grupo5.logistica.models.entities.rutas.direccion.CoordenadasGeograficas;
import grupo5.logistica.testutils.EntregaMother;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EntregaMapperTest {
  private final EntregaMapper mapper = new EntregaMapper(new DireccionMapper());

  @Test
  void mapeaRequestADominioYDominioAResponse() {
    DireccionDTO direccion =
        new DireccionDTO("Calle", 123, null, null, "1000", "CABA", "Buenos Aires", "Argentina");
    CrearEntregaRequestDTO dto =
        new CrearEntregaRequestDTO(UUID.randomUUID(), UUID.randomUUID(), direccion, 10f, 2f);
    Entrega entrega = mapper.toEntity(dto);
    var response = mapper.toResponseDTO(entrega);
    assertEquals(dto.idDonacion(), response.idDonacion());
    assertEquals(dto.idBeneficiaria(), response.idBeneficiaria());
    assertEquals(direccion, response.destino());
    assertEquals(dto.pesoTotalKG(), response.pesoTotalKG());
  }

  @Test
  void mapeaLasTresSolicitudesDeCambio() {
    Entrega entrega = EntregaMother.pendiente();
    var confirmacion =
        mapper.toSolicitud(
            entrega, new ConfirmarRecepcionRequestDTO("beneficiaria", List.of("https://f1.jpg")));
    assertEquals("beneficiaria", confirmacion.actor());
    assertEquals(List.of("https://f1.jpg"), confirmacion.fotosRecepcion());
    assertTrue(
        mapper
            .toSolicitud(
                entrega, new ReportarNoRecepcionRequestDTO("beneficiaria", "cerrado", null))
            .replanificable());
    assertFalse(
        mapper
            .toSolicitud(
                entrega, new ReportarNoRecepcionRequestDTO("beneficiaria", "cerrado", false))
            .replanificable());
    assertEquals(
        "admin", mapper.toSolicitud(entrega, new RegresarAlDepositoRequestDTO("admin")).actor());
  }

  @Test
  void mapeaEntregaParaMapaPublicoSoloSiTieneCoordenadas() {
    Entrega sinCoords = EntregaMother.pendiente();
    assertNull(mapper.toMapaPublicoResponseDTO(sinCoords));

    sinCoords.getDestino().actualizarCoordenadas(new CoordenadasGeograficas(-34.5986, -58.4201));
    sinCoords.iniciarRuta("Chofer");
    sinCoords.confirmarEntrega("Beneficiaria", "https://foto.jpg");

    var mapaDto = mapper.toMapaPublicoResponseDTO(sinCoords);
    assertNotNull(mapaDto);
    assertEquals(sinCoords.getId(), mapaDto.idEntrega());
    assertEquals(-34.5986, mapaDto.latitud());
    assertEquals(-58.4201, mapaDto.longitud());
    assertEquals("CABA", mapaDto.localidad());
    assertNotNull(mapaDto.fechaEntrega());
  }

  @Test
  void contemplaNulosYMagnitudesAusentes() {
    assertNull(mapper.toEntity(null));
    assertNull(mapper.toResponseDTO(null));
    assertNull(mapper.toMapaPublicoResponseDTO(null));
    assertNull(mapper.toCambioEstadoResponseDTO(null));
    CrearEntregaRequestDTO dto =
        new CrearEntregaRequestDTO(UUID.randomUUID(), UUID.randomUUID(), null, null, 2f);
    assertThrows(ValidationException.class, () -> mapper.toEntity(dto));
  }
}
