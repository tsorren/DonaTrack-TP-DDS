package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.comunicaciones.EventoPersonaSincronizadaV1;
import grupo5.donaciones.dto.personas.SolicitudEntidadDTO;
import grupo5.donaciones.dto.personas.SolicitudEntidadDTO.Direccion;
import grupo5.donaciones.dto.personas.SolicitudEntidadDTO.Representante;
import grupo5.donaciones.fixtures.PersonaMother;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.personas.Correo;
import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.entities.personas.TipoDocumento;
import grupo5.donaciones.models.entities.personas.TipoJuridico;
import grupo5.donaciones.models.repositories.IDonantesRepository;
import grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.services.impl.NotificacionesAsyncService;
import grupo5.donaciones.services.impl.SolicitudEntidadService;
import grupo5.donaciones.services.mappers.DireccionMapper;
import grupo5.donaciones.services.mappers.MedioDeContactoMapper;
import grupo5.donaciones.services.mappers.PersonaMapper;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SolicitudEntidadServiceTest {

  private static final String CORREO = "rosa@merendero-ejemplo.test";

  @Mock private IPersonasRepository personas;
  @Mock private IEntidadesBeneficiariasRepository entidades;
  @Mock private IDonantesRepository donantes;
  @Mock private NotificacionesAsyncService notificaciones;

  private SolicitudEntidadService service;
  private Juridica juridica;
  private UUID id;

  @BeforeEach
  void setUp() {
    service =
        new SolicitudEntidadService(
            personas,
            entidades,
            donantes,
            new PersonaMapper(new DireccionMapper(), new MedioDeContactoMapper()),
            notificaciones);
    Humana rosa = new Humana("Rosa", "Ejemplo", null);
    rosa.agregarMedioDeContacto(correo(CORREO));
    juridica = new Juridica(rosa, "Merendero Ejemplo Oeste", TipoJuridico.ONG, "Merendero");
    juridica.actualizarDocumento(TipoDocumento.CUIT, "30-11111111-9");
    juridica.actualizarDireccion(PersonaMother.direccionValida());
    juridica.agregarMedioDeContacto(correo("merendero@ejemplo.test"));
    id = juridica.getId();
  }

  private static Correo correo(String direccion) {
    Correo correo = new Correo();
    correo.setDireccionCorreo(direccion);
    return correo;
  }

  private void pendiente() {
    when(personas.findById(id)).thenReturn(Optional.of(juridica));
    when(entidades.findById(id)).thenReturn(Optional.empty());
    when(donantes.findById(id)).thenReturn(Optional.empty());
  }

  private static SolicitudEntidadDTO corregida(TipoJuridico tipo) {
    return new SolicitudEntidadDTO(
        "Merendero Ejemplo Este",
        "Comedor",
        tipo,
        new Direccion("Calle Nueva", 99, "2000", "Localidad Nueva", "Provincia Nueva"),
        new Representante("Rosa María", "Ejemplar"),
        "otro@ejemplo.test");
  }

  @Test
  void consultar_unaJuridicaPendiente_devuelveLoQueEnvioConElCorreoDelRepresentante() {
    pendiente();

    SolicitudEntidadDTO solicitud = service.consultar(id);

    assertEquals("Merendero Ejemplo Oeste", solicitud.razonSocial());
    assertEquals("Merendero", solicitud.rubro());
    assertEquals(TipoJuridico.ONG, solicitud.tipoJuridico());
    assertEquals(juridica.getDireccion().calle(), solicitud.direccion().calle());
    assertEquals(juridica.getDireccion().altura(), solicitud.direccion().altura());
    assertEquals(
        juridica.getDireccion().localidad().provincia().nombre(),
        solicitud.direccion().provincia());
    assertEquals(new Representante("Rosa", "Ejemplo"), solicitud.representante());
    assertEquals(CORREO, solicitud.correo());
  }

  @Test
  void consultar_siNoExiste_responde404() {
    when(personas.findById(id)).thenReturn(Optional.empty());

    assertThrows(RecursoNoEncontradoException.class, () -> service.consultar(id));
  }

  @Test
  void consultar_siYaEsEntidad_responde409() {
    when(personas.findById(id)).thenReturn(Optional.of(juridica));
    when(entidades.findById(id)).thenReturn(Optional.of(new EntidadBeneficiaria(id, true)));

    BusinessStateException falla =
        assertThrows(BusinessStateException.class, () -> service.consultar(id));
    assertEquals(ErrorCatalog.SOLICITUD_ENTIDAD_NO_PENDIENTE, falla.getError());
  }

  @Test
  void consultar_siEsDonante_responde409() {
    when(personas.findById(id)).thenReturn(Optional.of(juridica));
    when(entidades.findById(id)).thenReturn(Optional.empty());
    when(donantes.findById(id)).thenReturn(Optional.of(new Donante(juridica)));

    assertThrows(BusinessStateException.class, () -> service.consultar(id));
  }

  @Test
  void consultar_siEsUnaHumana_responde409() {
    Humana humana = PersonaMother.juanPerez();
    when(personas.findById(humana.getId())).thenReturn(Optional.of(humana));

    UUID humanaId = humana.getId();
    assertThrows(BusinessStateException.class, () -> service.consultar(humanaId));
  }

  @Test
  void consultar_siEstaAnonimizada_responde409() {
    juridica.anonimizar();
    when(personas.findById(id)).thenReturn(Optional.of(juridica));

    assertThrows(BusinessStateException.class, () -> service.consultar(id));
  }

  @Test
  void corregir_cambiaSoloLosDatosDeLaSolicitudYSincroniza() {
    pendiente();
    when(personas.save(any(Persona.class))).thenAnswer(guardar -> guardar.getArgument(0));
    var medios = juridica.getMediosDeContacto();
    String pais = juridica.getDireccion().localidad().provincia().pais().nombre();

    SolicitudEntidadDTO guardada = service.corregir(id, corregida(TipoJuridico.INSTITUCION));

    assertEquals("Merendero Ejemplo Este", juridica.getRazonSocial());
    assertEquals("Comedor", juridica.getRubro());
    assertEquals(TipoJuridico.INSTITUCION, juridica.getTipo());
    assertEquals("Calle Nueva", juridica.getDireccion().calle());
    assertEquals(99, juridica.getDireccion().altura());
    assertEquals("2000", juridica.getDireccion().codigoPostal());
    assertEquals("Localidad Nueva", juridica.getDireccion().localidad().nombre());
    assertEquals("Provincia Nueva", juridica.getDireccion().localidad().provincia().nombre());
    assertEquals(pais, juridica.getDireccion().localidad().provincia().pais().nombre());
    Humana representante = juridica.getRepresentantes().getFirst();
    assertEquals("Rosa María", representante.getNombre());
    assertEquals("Ejemplar", representante.getApellido());
    assertEquals(medios, juridica.getMediosDeContacto());
    assertEquals("30-11111111-9", juridica.getDocumento());
    assertEquals(CORREO, guardada.correo());
    verify(personas).save(representante);
    verify(personas).save(juridica);
    verify(notificaciones).sincronizarPersona(any(EventoPersonaSincronizadaV1.class));
  }

  @Test
  void corregir_siYaEsEntidad_noCambiaNada() {
    when(personas.findById(id)).thenReturn(Optional.of(juridica));
    when(entidades.findById(id)).thenReturn(Optional.of(new EntidadBeneficiaria(id, true)));

    SolicitudEntidadDTO cambios = corregida(TipoJuridico.ONG);
    assertThrows(BusinessStateException.class, () -> service.corregir(id, cambios));

    assertEquals("Merendero Ejemplo Oeste", juridica.getRazonSocial());
    verify(personas, never()).save(any());
    verify(notificaciones, never()).sincronizarPersona(any());
  }

  @Test
  void corregir_comoEmpresa_responde400() {
    pendiente();

    SolicitudEntidadDTO cambios = corregida(TipoJuridico.EMPRESA);
    assertThrows(ValidationException.class, () -> service.corregir(id, cambios));

    assertEquals(TipoJuridico.ONG, juridica.getTipo());
    verify(personas, never()).save(any());
  }
}
