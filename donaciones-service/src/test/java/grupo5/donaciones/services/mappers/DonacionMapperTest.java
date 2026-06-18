package grupo5.donaciones.services.mappers;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.donaciones.dto.direcciones.DireccionInputDTO;
import grupo5.donaciones.dto.donaciones.inputs.DonacionInputDTO;
import grupo5.donaciones.dto.donaciones.outputs.DonacionOutputDTO;
import grupo5.donaciones.models.entities.donaciones.Deposito;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donaciones.EstadoDonacion;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.personas.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DonacionMapperTest {

  private DonacionMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new DonacionMapper(new DireccionMapper());
  }

  @Test
  void toEntity_conDtoValido_deberiaMapearCorrectamente() {
    Humana persona = new Humana("Juan", "Perez", LocalDate.of(1990, 1, 1));
    DireccionInputDTO dirDTO =
        new DireccionInputDTO(
            "Calle Falsa", 123, null, null, "1000", "CABA", "Buenos Aires", "Argentina");
    DonacionInputDTO dto =
        new DonacionInputDTO(
            UUID.randomUUID(), "descripcion test", List.of(), "Deposito Central", dirDTO);

    Donacion donacion = mapper.toEntity(dto, persona);

    assertNotNull(donacion);
    assertNotNull(donacion.getId());
    assertEquals("descripcion test", donacion.getDescripcion());
    assertNotNull(donacion.getFecha());
    assertEquals(EstadoDonacion.CARGADA, donacion.getEstadoActual());
    assertEquals(persona, donacion.getDonante().getPersona());
    assertEquals("Deposito Central", donacion.getDepositoRecepcion().getNombre());
    assertTrue(donacion.getItems().isEmpty());
  }

  @Test
  void toEntity_conDtoNulo_deberiaRetornarNulo() {
    assertNull(mapper.toEntity(null, new Humana("A", "B", LocalDate.of(1990, 1, 1))));
  }

  @Test
  void toOutputDTO_conDonacionValida_deberiaMapearCorrectamente() {
    Humana persona = new Humana("Maria", "Lopez", LocalDate.of(1993, 4, 10));
    Donante donante = new Donante(persona);
    Deposito deposito = new Deposito("Deposito Test", crearDireccion());
    Donacion donacion = new Donacion(donante, deposito);
    donacion.setDescripcion("una donacion");

    DonacionOutputDTO output = mapper.toOutputDTO(donacion);

    assertNotNull(output);
    assertEquals(donacion.getId(), output.id());
    assertEquals(persona.getId(), output.idDonante());
    assertEquals("una donacion", output.descripcion());
    assertEquals(EstadoDonacion.CARGADA, output.estadoActual());
    assertNotNull(output.deposito());
    assertTrue(output.items().isEmpty());
    assertTrue(output.historialEstados().isEmpty());
  }

  @Test
  void toOutputDTO_conDonacionNula_deberiaRetornarNulo() {
    assertNull(mapper.toOutputDTO(null));
  }

  private Direccion crearDireccion() {
    Pais pais = new Pais();
    pais.setNombre("Argentina");
    Provincia provincia = new Provincia();
    provincia.setNombre("Buenos Aires");
    provincia.setPais(pais);
    Localidad localidad = new Localidad();
    localidad.setNombre("CABA");
    localidad.setProvincia(provincia);
    return new Direccion("Calle Falsa", 123, null, null, "1000", localidad);
  }
}
