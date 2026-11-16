package grupo5.donaciones.services.impl;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.personas.SolicitudEntidadDTO;
import grupo5.donaciones.dto.personas.SolicitudEntidadDTO.Representante;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.entities.ubicaciones.Direccion;
import grupo5.donaciones.models.entities.ubicaciones.Localidad;
import grupo5.donaciones.models.entities.ubicaciones.Pais;
import grupo5.donaciones.models.entities.ubicaciones.Provincia;
import grupo5.donaciones.models.repositories.IDonantesRepository;
import grupo5.donaciones.models.repositories.IEntidadesBeneficiariasRepository;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.services.ISolicitudEntidadService;
import grupo5.donaciones.services.mappers.PersonaMapper;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * La solicitud que espera la aprobación (R11-41). "Pendiente" lo deciden los datos de Donaciones,
 * no el atributo de la cuenta: una empresa donante, que también opera sobre su jurídica, recibe
 * 409.
 */
@Service
public class SolicitudEntidadService implements ISolicitudEntidadService {

  /** El país de la dirección, si la jurídica no tenía una: el del registro. */
  private static final String PAIS = "Argentina";

  private final IPersonasRepository personas;
  private final IEntidadesBeneficiariasRepository entidades;
  private final IDonantesRepository donantes;
  private final PersonaMapper mapper;
  private final NotificacionesAsyncService notificaciones;

  public SolicitudEntidadService(
      IPersonasRepository personas,
      IEntidadesBeneficiariasRepository entidades,
      IDonantesRepository donantes,
      PersonaMapper mapper,
      NotificacionesAsyncService notificaciones) {
    this.personas = personas;
    this.entidades = entidades;
    this.donantes = donantes;
    this.mapper = mapper;
    this.notificaciones = notificaciones;
  }

  @Override
  public SolicitudEntidadDTO consultar(UUID personaId) {
    return aDto(pendiente(personaId));
  }

  @Override
  public SolicitudEntidadDTO corregir(UUID personaId, SolicitudEntidadDTO solicitud) {
    Juridica juridica = pendiente(personaId);
    EntidadBeneficiaria.validarRequisitos(solicitud.tipoJuridico(), true);
    Humana representante = juridica.getRepresentantes().getFirst();
    representante.actualizar(
        solicitud.representante().nombre(),
        solicitud.representante().apellido(),
        representante.getFechaNacimiento(),
        representante.getGenero());
    juridica.actualizar(solicitud.razonSocial(), solicitud.tipoJuridico(), solicitud.rubro());
    juridica.actualizarDireccion(direccion(juridica.getDireccion(), solicitud.direccion()));
    personas.save(representante);
    Juridica guardada = (Juridica) personas.save(juridica);
    notificaciones.sincronizarPersona(mapper.toEventoPersonaSincronizadaV1(guardada));
    return aDto(guardada);
  }

  private Juridica pendiente(UUID personaId) {
    var persona =
        personas.findById(personaId).orElseThrow(() -> new RecursoNoEncontradoException(personaId));
    if (!(persona instanceof Juridica juridica)
        || juridica.estaAnonimizada()
        || juridica.getRepresentantes().isEmpty()
        || entidades.findById(personaId).isPresent()
        || donantes.findById(personaId).isPresent()) {
      throw new BusinessStateException(ErrorCatalog.SOLICITUD_ENTIDAD_NO_PENDIENTE);
    }
    return juridica;
  }

  /** La dirección nueva conserva el piso, el departamento y el país de la anterior. */
  private static Direccion direccion(Direccion anterior, SolicitudEntidadDTO.Direccion nueva) {
    String pais = anterior == null ? PAIS : anterior.localidad().provincia().pais().nombre();
    return new Direccion(
        nueva.calle(),
        nueva.altura(),
        anterior == null ? null : anterior.piso(),
        anterior == null ? null : anterior.departamento(),
        nueva.codigoPostal(),
        new Localidad(nueva.localidad(), new Provincia(nueva.provincia(), new Pais(pais))));
  }

  private static SolicitudEntidadDTO aDto(Juridica juridica) {
    Humana representante = juridica.getRepresentantes().getFirst();
    Direccion direccion = juridica.getDireccion();
    return new SolicitudEntidadDTO(
        juridica.getRazonSocial(),
        juridica.getRubro(),
        juridica.getTipo(),
        direccion == null
            ? null
            : new SolicitudEntidadDTO.Direccion(
                direccion.calle(),
                direccion.altura(),
                direccion.codigoPostal(),
                direccion.localidad().nombre(),
                direccion.localidad().provincia().nombre()),
        new Representante(representante.getNombre(), representante.getApellido()),
        representante.direccionesDeCorreo().stream()
            .findFirst()
            .or(() -> juridica.direccionesDeCorreo().stream().findFirst())
            .orElse(null));
  }
}
