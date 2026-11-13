package grupo5.cliente.rest;

import grupo5.cliente.puertos.PersonaNueva.Humana;
import grupo5.cliente.puertos.SolicitudEntidadPuerto;
import grupo5.cliente.rest.DonacionesApi.DireccionDeLaSolicitud;
import grupo5.cliente.rest.DonacionesApi.Representante;
import grupo5.cliente.rest.DonacionesApi.SolicitudEntidad;
import grupo5.cliente.vistas.SolicitudEntidadVista;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

/**
 * La solicitud que espera la aprobación desde Donaciones, con el token de la cuenta: {@code GET} y
 * {@code PUT /api/personas/{id}/solicitud-entidad} (R11-41). El 409 es {@link YaAprobada}; el 400,
 * {@link DatosRechazados} con los campos del formulario. El tipo se traduce con la misma tabla que
 * Mi perfil de la entidad.
 */
// @Primary solo para el análisis estático (S9352): la propiedad ya deja un único adaptador activo.
@Component
@Primary
@ConditionalOnProperty(name = "cliente.datos.entidad", havingValue = "rest")
class SolicitudEntidadRest implements SolicitudEntidadPuerto {

  private static final Map<String, String> TIPOS =
      Map.of("ONG", "asociacion", "INSTITUCION", "institucion", "GUBERNAMENTAL", "organismo");
  private static final Map<String, String> TIPOS_JURIDICOS =
      Map.of("asociacion", "ONG", "institucion", "INSTITUCION", "organismo", "GUBERNAMENTAL");

  /** El campo de Donaciones y el del formulario que lo escribe. */
  private static final Map<String, String> CAMPOS =
      Map.of(
          "razonSocial", "entidad",
          "rubro", "que-es",
          "tipoJuridico", "tipo",
          "direccion.calle", "calle",
          "direccion.altura", "altura",
          "direccion.codigoPostal", "codigo-postal",
          "direccion.localidad", "localidad",
          "direccion.provincia", "provincia",
          "representante.nombre", "representante",
          "representante.apellido", "representante");

  private final DonacionesApi api;

  SolicitudEntidadRest(DonacionesApi api) {
    this.api = api;
  }

  @Override
  public SolicitudEntidadVista leer(UUID personaId) {
    try {
      return vista(api.solicitudEntidad(personaId));
    } catch (HttpClientErrorException.Conflict yaNoEspera) {
      throw new YaAprobada();
    }
  }

  @Override
  public void corregir(UUID personaId, SolicitudEntidadVista cambios) {
    Humana representante =
        Humana.deNombreCompleto(Objects.requireNonNullElse(cambios.representante(), ""), null);
    SolicitudEntidad cuerpo =
        new SolicitudEntidad(
            cambios.entidad(),
            cambios.queEs(),
            cambios.tipo() == null ? null : TIPOS_JURIDICOS.get(cambios.tipo()),
            new DireccionDeLaSolicitud(
                cambios.calle(),
                cambios.altura(),
                cambios.codigoPostal(),
                cambios.localidad(),
                cambios.provincia()),
            new Representante(representante.nombre(), representante.apellido()),
            null);
    try {
      api.corregirSolicitudEntidad(personaId, cuerpo);
    } catch (HttpClientErrorException.Conflict yaNoEspera) {
      throw new YaAprobada();
    } catch (HttpClientErrorException.BadRequest rechazo) {
      throw new DatosRechazados(campos(rechazo));
    }
  }

  private static List<String> campos(HttpClientErrorException.BadRequest rechazo) {
    Rechazo cuerpo = rechazo.getResponseBodyAs(Rechazo.class);
    if (cuerpo == null || cuerpo.errors() == null) {
      return List.of();
    }
    return cuerpo.errors().stream()
        .map(Campo::field)
        .filter(Objects::nonNull)
        .map(CAMPOS::get)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
  }

  private static SolicitudEntidadVista vista(SolicitudEntidad solicitud) {
    if (solicitud == null) {
      throw new IllegalStateException("Donaciones no devolvió la solicitud");
    }
    DireccionDeLaSolicitud direccion =
        Objects.requireNonNullElse(
            solicitud.direccion(), new DireccionDeLaSolicitud(null, null, null, null, null));
    Representante representante =
        Objects.requireNonNullElse(solicitud.representante(), new Representante(null, null));
    String nombre =
        String.join(
                " ",
                Objects.requireNonNullElse(representante.nombre(), ""),
                Objects.requireNonNullElse(representante.apellido(), ""))
            .trim();
    return new SolicitudEntidadVista(
        solicitud.razonSocial(),
        solicitud.rubro(),
        solicitud.tipoJuridico() == null ? null : TIPOS.get(solicitud.tipoJuridico()),
        direccion.calle(),
        direccion.altura(),
        direccion.codigoPostal(),
        direccion.localidad(),
        direccion.provincia(),
        nombre.isEmpty() ? null : nombre,
        solicitud.correo());
  }

  /** El cuerpo del 400 de Donaciones: un renglón por campo. */
  record Rechazo(List<Campo> errors) {}

  record Campo(String field) {}
}
