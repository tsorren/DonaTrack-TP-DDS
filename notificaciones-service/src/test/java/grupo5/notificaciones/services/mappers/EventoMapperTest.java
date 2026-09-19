package grupo5.notificaciones.services.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import grupo5.common.exceptions.ValidationException;
import grupo5.notificaciones.dto.input.*;
import grupo5.notificaciones.models.entities.notificaciones.eventos.*;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.entities.personas.TipoPersona;
import grupo5.notificaciones.models.repositories.IPersonaRepository;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventoMapperTest {

  private static final LocalDateTime TEST_DATE_TIME =
      LocalDateTime.of(2026, Month.JULY, 2, 12, 0, 0);

  @Mock private IPersonaRepository personaRepository;

  private EventoMapper mapper;

  private Persona donante;
  private Persona beneficiario;
  private Persona admin;

  @BeforeEach
  void setUp() {
    mapper = new EventoMapper(personaRepository);

    donante = new Persona(UUID.randomUUID(), new ArrayList<>(), "Juan", TipoPersona.HUMANA);
    beneficiario =
        new Persona(
            UUID.randomUUID(), new ArrayList<>(), "Comedor Esperanza", TipoPersona.JURIDICA);
    admin = new Persona(UUID.randomUUID(), new ArrayList<>(), "Admin", TipoPersona.HUMANA);

    lenient().when(personaRepository.findById(donante.getId())).thenReturn(Optional.of(donante));
    lenient()
        .when(personaRepository.findById(beneficiario.getId()))
        .thenReturn(Optional.of(beneficiario));
    lenient().when(personaRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
  }

  @Test
  void toEntity_donacionAsignada_deberiaMapearDonanteYBeneficiarioCorrectamente() {
    EventoDonacionAsignadaDTO dto =
        new EventoDonacionAsignadaDTO(
            UUID.randomUUID(),
            donante.getId(),
            TEST_DATE_TIME,
            beneficiario.getId(),
            "10kg de arroz");

    EventoNotificable evento = mapper.toEntity(dto);

    DonacionAsignada resultado = assertInstanceOf(DonacionAsignada.class, evento);
    assertEquals(donante.getId(), resultado.getPersona().getId());
    assertEquals(beneficiario.getId(), resultado.getEntidadBeneficiaria().getId());
    assertEquals("10kg de arroz", resultado.getDetalleDonacion());
  }

  @Test
  void toEntity_donacionAsignadaV1_deberiaMapearDonanteBeneficiarioYDetalleCompleto() {
    UUID donacionId = UUID.randomUUID();
    DestinoEventoDTO destino =
        new DestinoEventoDTO(
            "Av. Corrientes", 1234, 3, "B", "C1043", "San Nicolás", "CABA", "Argentina");
    EventoDonacionAsignadaV1 eventoV1 =
        new EventoDonacionAsignadaV1(
            donacionId,
            donante.getId(),
            donante.getId(),
            TEST_DATE_TIME,
            beneficiario.getId(),
            "Ropa de invierno y frazadas",
            destino,
            12.5,
            0.4);

    DonacionAsignada resultado = mapper.toEntity(eventoV1);

    assertEquals(donante.getId(), resultado.getPersona().getId());
    assertEquals(beneficiario.getId(), resultado.getEntidadBeneficiaria().getId());
    assertEquals(TEST_DATE_TIME, resultado.getFecha());

    String detalle = resultado.getDetalleDonacion();
    assertTrue(detalle.contains("Ropa de invierno y frazadas"));
    assertTrue(detalle.contains(donacionId.toString()));
    assertTrue(detalle.contains("Peso: 12.5 kg"));
    assertTrue(detalle.contains("Volumen: 0.4 m³"));
    assertTrue(
        detalle.contains(
            "Av. Corrientes 1234, Piso 3, Dpto B, San Nicolás, CABA (CP C1043), Argentina"));
  }

  /** Evento V1 mínimo para los casos en los que sólo importa qué persona no se encuentra. */
  private EventoDonacionAsignadaV1 eventoV1Con(UUID donanteId, UUID beneficiarioId) {
    DestinoEventoDTO destino =
        new DestinoEventoDTO(
            "Calle Falsa", 123, null, null, "1234", "La Plata", "Buenos Aires", "Argentina");
    return new EventoDonacionAsignadaV1(
        UUID.randomUUID(),
        donanteId,
        donanteId,
        TEST_DATE_TIME,
        beneficiarioId,
        "Alimentos",
        destino,
        5.0,
        0.1);
  }

  @Test
  void toEntity_donacionAsignadaV1_conDonanteInexistente_deberiaLanzarExcepcion() {
    UUID donanteInexistente = UUID.randomUUID();
    when(personaRepository.findById(donanteInexistente)).thenReturn(Optional.empty());

    EventoDonacionAsignadaV1 eventoV1 = eventoV1Con(donanteInexistente, beneficiario.getId());

    assertThrows(ValidationException.class, () -> mapper.toEntity(eventoV1));
  }

  @Test
  void toEntity_donacionAsignadaV1_conBeneficiarioInexistente_deberiaLanzarExcepcion() {
    UUID beneficiarioInexistente = UUID.randomUUID();
    when(personaRepository.findById(beneficiarioInexistente)).thenReturn(Optional.empty());

    EventoDonacionAsignadaV1 eventoV1 = eventoV1Con(donante.getId(), beneficiarioInexistente);

    assertThrows(ValidationException.class, () -> mapper.toEntity(eventoV1));
  }

  @Test
  void toEntity_donacionRecibida_deberiaMapearPatenteDeCamion() {
    EventoDonacionRecibidaDTO dto =
        new EventoDonacionRecibidaDTO(
            UUID.randomUUID(),
            donante.getId(),
            TEST_DATE_TIME,
            beneficiario.getId(),
            "ropa",
            "AB123CD");

    EventoNotificable evento = mapper.toEntity(dto);

    DonacionRecibida resultado = assertInstanceOf(DonacionRecibida.class, evento);
    assertEquals(donante.getId(), resultado.getPersona().getId());
    assertEquals(beneficiario.getId(), resultado.getEntidadBeneficiaria().getId());
    assertEquals("AB123CD", resultado.getPatenteCamion());
  }

  @Test
  void toEntity_donacionEnCamino_deberiaMapearEnlaceDeSeguimiento() {
    EventoDonacionEnCaminoDTO dto =
        new EventoDonacionEnCaminoDTO(
            UUID.randomUUID(),
            donante.getId(),
            TEST_DATE_TIME,
            beneficiario.getId(),
            "ropa",
            "https://donatrack.app/mapa/123");

    EventoNotificable evento = mapper.toEntity(dto);

    DonacionEnCamino resultado = assertInstanceOf(DonacionEnCamino.class, evento);
    assertEquals(donante.getId(), resultado.getPersona().getId());
    assertEquals(beneficiario.getId(), resultado.getEntidadBeneficiaria().getId());
    assertEquals("https://donatrack.app/mapa/123", resultado.getEnlaceSeguimiento());
  }

  @Test
  void toEntity_entregaFallida_deberiaMapearDonanteBeneficiarioYAdminSinConfundirlos() {
    EventoEntregaFallidaDTO dto =
        new EventoEntregaFallidaDTO(
            UUID.randomUUID(),
            donante.getId(),
            TEST_DATE_TIME,
            beneficiario.getId(),
            "ropa",
            admin.getId(),
            "Nadie respondió",
            true);

    EventoNotificable evento = mapper.toEntity(dto);

    EntregaFallida resultado = assertInstanceOf(EntregaFallida.class, evento);
    assertEquals(donante.getId(), resultado.getPersona().getId());
    assertEquals(beneficiario.getId(), resultado.getEntidadBeneficiaria().getId());
    assertEquals(admin.getId(), resultado.getAdministracion().getId());
    assertEquals("Nadie respondió", resultado.getMotivo());
    assertEquals(true, resultado.isReplanificable());
  }

  @Test
  void toEntity_donanteRegistrado_deberiaMapearCredenciales() {
    EventoDonanteRegistradoDTO dto =
        new EventoDonanteRegistradoDTO(
            UUID.randomUUID(), donante.getId(), TEST_DATE_TIME, "usuario: Juan");

    EventoNotificable evento = mapper.toEntity(dto);

    DonanteRegistrado resultado = assertInstanceOf(DonanteRegistrado.class, evento);
    assertEquals(donante.getId(), resultado.getPersona().getId());
  }

  @Test
  void toEntity_donanteInactivo_deberiaMapearDiasInactivo() {
    EventoDonanteInactivoDTO dto =
        new EventoDonanteInactivoDTO(UUID.randomUUID(), donante.getId(), TEST_DATE_TIME, 21);

    EventoNotificable evento = mapper.toEntity(dto);

    assertInstanceOf(DonanteInactivo.class, evento);
  }

  @Test
  void toEntity_misionCumplida_deberiaMapearNombreYRecompensa() {
    EventoMisionCumplidaDTO dto =
        new EventoMisionCumplidaDTO(
            UUID.randomUUID(), donante.getId(), TEST_DATE_TIME, "Racha", "Insignia Oro");

    EventoNotificable evento = mapper.toEntity(dto);

    assertInstanceOf(MisionCumplida.class, evento);
  }

  @Test
  void toEntity_subioCategoria_deberiaMapearCategorias() {
    EventoSubioCategoriaDTO dto =
        new EventoSubioCategoriaDTO(
            UUID.randomUUID(), donante.getId(), TEST_DATE_TIME, "Sostenedor", "Colaborador");

    EventoNotificable evento = mapper.toEntity(dto);

    assertInstanceOf(SubioCategoria.class, evento);
  }

  @Test
  void toEntity_personaNoEncontrada_deberiaLanzarExcepcion() {
    UUID idInexistente = UUID.randomUUID();
    when(personaRepository.findById(idInexistente)).thenReturn(Optional.empty());

    EventoDonanteInactivoDTO dto =
        new EventoDonanteInactivoDTO(UUID.randomUUID(), idInexistente, TEST_DATE_TIME, 21);

    assertThrows(ValidationException.class, () -> mapper.toEntity(dto));
  }

  @Test
  void toEntity_donacionVencida_deberiaMapearCorrectamente() {
    EventoDonacionVencidaDTO dto =
        new EventoDonacionVencidaDTO(
            UUID.randomUUID(),
            donante.getId(),
            TEST_DATE_TIME,
            admin.getId(),
            "10 kg de arroz",
            "Expiró plazo de acopio");

    EventoNotificable evento = mapper.toEntity(dto);

    assertInstanceOf(DonacionVencida.class, evento);
    DonacionVencida dv = (DonacionVencida) evento;
    assertEquals(donante.getId(), dv.getPersona().getId());
    assertEquals(admin.getId(), dv.getAdministracion().getId());
    assertEquals("10 kg de arroz", dv.getDetalleDonacion());
    assertEquals("Expiró plazo de acopio", dv.getMotivo());
    assertEquals(TEST_DATE_TIME, dv.getFecha());
  }

  @Test
  void toEntity_donacionVencida_conAdminInexistente_deberiaLanzarExcepcion() {
    UUID adminInexistente = UUID.randomUUID();
    when(personaRepository.findById(adminInexistente)).thenReturn(Optional.empty());

    EventoDonacionVencidaDTO dto =
        new EventoDonacionVencidaDTO(
            UUID.randomUUID(),
            donante.getId(),
            TEST_DATE_TIME,
            adminInexistente,
            "10 kg de arroz",
            "Expiró plazo de acopio");

    assertThrows(ValidationException.class, () -> mapper.toEntity(dto));
  }
}
