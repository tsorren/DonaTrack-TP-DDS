package grupo5.cliente.rest;

import grupo5.cliente.puertos.AdminOperacionPuerto.BienNuevo;
import grupo5.cliente.puertos.AdminOperacionPuerto.DonacionNueva;
import grupo5.cliente.vistas.AdminDonacionesRegistroVista;
import grupo5.cliente.vistas.AdminDonacionesRegistroVista.Donante;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Registrar donación desde Donaciones: la búsqueda de donantes activos para elegir uno y el alta.
 * La foto de cada bien se sube a la dirección firmada que da Donaciones ({@code POST
 * /api/donaciones/fotos}) y el bien guarda su referencia. Lo usa {@link AdminOperacionRest}.
 */
final class AdminOperacionRegistro {

  /** Un buscador para elegir uno: como máximo 10 resultados (ficha, §4.6). */
  static final int A_LA_VISTA = 10;

  private final DonacionesApi api;
  private final RestClient subidas;

  AdminOperacionRegistro(DonacionesApi api, RestClient subidas) {
    this.api = api;
    this.subidas = subidas;
  }

  /**
   * Los donantes activos que coinciden con {@code buscar}, como máximo {@link #A_LA_VISTA}, con
   * cuántos coinciden en total (R11-01: la búsqueda la resuelve Donaciones, sin tildes ni
   * mayúsculas), y el elegido ({@code donante}, su clave), pedido por su identificador.
   */
  AdminDonacionesRegistroVista registro(String buscar, String donante) {
    List<Donante> elegido = elegido(donante);
    String texto = buscar == null ? "" : buscar.strip();
    if (texto.isEmpty()) {
      return new AdminDonacionesRegistroVista(elegido, List.of(), 0);
    }
    MultiValueMap<String, String> filtros = new LinkedMultiValueMap<>();
    filtros.add("q", texto);
    filtros.add("activo", "true");
    filtros.add("orden", "nombre");
    filtros.add("page", "0");
    filtros.add("size", String.valueOf(A_LA_VISTA));
    ResponseEntity<List<DonacionesApi.Donante>> respuesta = api.donantes(filtros);
    List<DonacionesApi.Donante> filas =
        Objects.requireNonNullElse(respuesta.getBody(), List.<DonacionesApi.Donante>of());
    List<Donante> encontrados =
        filas.stream()
            .filter(d -> d.idDonante() != null)
            .map(AdminOperacionRegistro::donante)
            .toList();
    return new AdminDonacionesRegistroVista(
        elegido, encontrados, Paginas.total(respuesta, filas, 1, A_LA_VISTA));
  }

  /** El donante elegido por su clave (su id), o ninguno si no hay o no existe. */
  private List<Donante> elegido(String clave) {
    if (clave == null || clave.isBlank()) {
      return List.of();
    }
    UUID id;
    try {
      id = UUID.fromString(clave.strip());
    } catch (IllegalArgumentException noEsUnId) {
      return List.of();
    }
    try {
      DonacionesApi.Donante d = api.donante(id);
      return d == null || d.idDonante() == null ? List.of() : List.of(donante(d));
    } catch (HttpClientErrorException.NotFound noEsta) {
      return List.of();
    }
  }

  void registrar(DonacionNueva donacion) {
    List<DonacionesApi.ItemNuevo> items =
        donacion.bienes().stream()
            .map(
                b ->
                    new DonacionesApi.ItemNuevo(
                        b.que(), foto(b), b.vence(), b.estado(), null, null, b.cantidad()))
            .toList();
    api.registrarDonacion(
        new DonacionesApi.DonacionNueva(
            UUID.fromString(donacion.donante()),
            donacion.descripcion(),
            items,
            donacion.deposito(),
            null,
            donacion.recepcion() == null ? null : donacion.recepcion().atStartOfDay(),
            null));
  }

  /** Sube la foto a la dirección firmada y devuelve su referencia; sin foto, null. */
  private String foto(BienNuevo bien) {
    if (bien.foto() == null) {
      return null;
    }
    DonacionesApi.SubidaDeFoto subida =
        api.pedirSubidaDeFoto(new DonacionesApi.PedidoDeSubida(bien.tipoFoto()));
    try (InputStream contenido = bien.foto().getInputStream()) {
      subidas
          .method(HttpMethod.valueOf(subida.metodo()))
          .uri(URI.create(subida.uploadUrl()))
          .contentType(MediaType.parseMediaType(subida.tipoContenido()))
          .body(contenido.readAllBytes())
          .retrieve()
          .toBodilessEntity();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return subida.referencia();
  }

  /** Su nombre (o razón social) y su documento: "DNI 20.000.001". */
  private static Donante donante(DonacionesApi.Donante d) {
    DonacionesApi.Persona p = d.persona();
    String nombre;
    if (p == null) {
      nombre = "";
    } else if (p.razonSocial() != null) {
      nombre = p.razonSocial();
    } else {
      nombre =
          (p.nombre() == null ? "" : p.nombre()) + (p.apellido() == null ? "" : " " + p.apellido());
    }
    String documento = "";
    if (p != null && p.documento() != null) {
      documento =
          p.tipoDocumento() == null ? p.documento() : p.tipoDocumento() + " " + p.documento();
    }
    return new Donante(d.idDonante().toString(), nombre.trim(), documento);
  }
}
