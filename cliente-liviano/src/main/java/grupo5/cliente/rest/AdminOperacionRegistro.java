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
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * Registrar donación desde Donaciones: los donantes activos para elegir y el alta. La foto de cada
 * bien se sube a la dirección firmada que da Donaciones ({@code POST /api/donaciones/fotos}) y el
 * bien guarda su referencia. Lo usa {@link AdminOperacionRest}.
 */
final class AdminOperacionRegistro {

  private final DonacionesApi api;
  private final RestClient subidas;

  AdminOperacionRegistro(DonacionesApi api, RestClient subidas) {
    this.api = api;
    this.subidas = subidas;
  }

  AdminDonacionesRegistroVista registro(String buscar) {
    List<Donante> donantes =
        api.donantes(null).stream()
            .filter(d -> !Boolean.FALSE.equals(d.activo()) && d.idDonante() != null)
            .map(AdminOperacionRegistro::donante)
            .toList();
    String texto = buscar == null ? null : buscar.trim().toLowerCase(Locale.ROOT);
    List<Donante> encontrados =
        texto == null ? List.of() : donantes.stream().filter(d -> coincide(d, texto)).toList();
    return new AdminDonacionesRegistroVista(donantes, encontrados);
  }

  /** Por el nombre, o por el número de documento con o sin puntos y guiones. */
  private static boolean coincide(Donante donante, String texto) {
    if (donante.nombre().toLowerCase(Locale.ROOT).contains(texto)) {
      return true;
    }
    String numero = texto.replaceAll("\\D", "");
    return !numero.isEmpty() && donante.documento().replaceAll("\\D", "").contains(numero);
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
    String documento =
        p == null || p.documento() == null
            ? ""
            : (p.tipoDocumento() == null ? "" : p.tipoDocumento() + " ") + p.documento();
    return new Donante(d.idDonante().toString(), nombre.trim(), documento);
  }
}
