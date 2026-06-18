package grupo5.donaciones.services.mappers;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.donaciones.dto.direcciones.DireccionInputDTO;
import grupo5.donaciones.dto.donaciones.inputs.DonacionInputDTO;
import grupo5.donaciones.dto.donaciones.inputs.ItemDonacionInputDTO;
import grupo5.donaciones.dto.donaciones.outputs.DonacionOutputDTO;
import grupo5.donaciones.models.entities.donaciones.Deposito;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donaciones.Estado;
import grupo5.donaciones.models.entities.donaciones.EstadoDonacion;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.personas.*;
import java.time.LocalDate;
import java.time.Month;
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
    Humana persona = new Humana("Juan", "Perez", LocalDate.of(1990, Month.JANUARY, 1));
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
    assertNull(mapper.toEntity(null, new Humana("A", "B", LocalDate.of(1990, Month.JANUARY, 1))));
  }

  @Test
  void toOutputDTO_conDonacionValida_deberiaMapearCorrectamente() {
    Humana persona = new Humana("Maria", "Lopez", LocalDate.of(1993, Month.APRIL, 10));
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

  @Test
  void toEntity_conItems_deberiaMapearItems() {
    Humana persona = new Humana("Ana", "Garcia", LocalDate.of(1995, Month.MARCH, 15));
    ItemDonacionInputDTO item1 =
        new ItemDonacionInputDTO(
            "abrigo", "foto.png", LocalDate.of(2027, Month.JANUARY, 1), Estado.NUEVO, 3);
    ItemDonacionInputDTO item2 = new ItemDonacionInputDTO("pantalon", null, null, Estado.USADO, 2);
    DireccionInputDTO dirDTO =
        new DireccionInputDTO(
            "Calle Falsa", 123, null, null, "1000", "CABA", "Buenos Aires", "Argentina");
    DonacionInputDTO dto =
        new DonacionInputDTO(
            UUID.randomUUID(), "ropa de invierno", List.of(item1, item2), "Deposito Sur", dirDTO);

    Donacion donacion = mapper.toEntity(dto, persona);

    assertEquals(2, donacion.getItems().size());
    assertEquals("abrigo", donacion.getItems().get(0).getBien().getDescripcion());
    assertEquals(3, donacion.getItems().get(0).getCantidad());
    assertEquals("pantalon", donacion.getItems().get(1).getBien().getDescripcion());
    assertEquals(2, donacion.getItems().get(1).getCantidad());
  }

  @Test
  void toOutputDTO_conHistorialEstados_deberiaMapearHistorial() {
    Humana persona = new Humana("Carlos", "Ruiz", LocalDate.of(1988, Month.JULY, 20));
    Donante donante = new Donante(persona);
    Deposito deposito = new Deposito("Deposito Norte", crearDireccion());
    Donacion donacion = new Donacion(donante, deposito);
    donacion.marcarNormalizada();
    donacion.marcarSegmentada();

    DonacionOutputDTO output = mapper.toOutputDTO(donacion);

    assertEquals(2, output.historialEstados().size());
    assertEquals(EstadoDonacion.CARGADA, output.historialEstados().get(0).estadoAnterior());
    assertEquals(EstadoDonacion.NORMALIZADA, output.historialEstados().get(0).estadoNuevo());
    assertEquals(EstadoDonacion.NORMALIZADA, output.historialEstados().get(1).estadoAnterior());
    assertEquals(EstadoDonacion.SEGMENTADA, output.historialEstados().get(1).estadoNuevo());
    assertEquals(EstadoDonacion.SEGMENTADA, output.estadoActual());
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
