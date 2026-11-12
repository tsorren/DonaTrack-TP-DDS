package grupo5.cliente.web;

import grupo5.cliente.puertos.AdminPersonasPuerto;
import grupo5.cliente.puertos.PersonaNueva;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Los envíos reales de Donantes, fuera de la maqueta (en la maqueta los atiende SitioController,
 * que vuelve sin guardar). Un archivo que Donaciones no acepta (400) lleva a su pantalla de error.
 */
@Controller
@Profile("!maqueta")
public class AdminDonantesEnvioController {

  private final AdminPersonasPuerto personas;

  AdminDonantesEnvioController(AdminPersonasPuerto personas) {
    this.personas = personas;
  }

  @PostMapping("/admin-donantes-importando")
  String importar(@RequestParam MultipartFile archivo, RedirectAttributes atributos) {
    UUID id;
    try {
      id = personas.importar(archivo);
    } catch (HttpClientErrorException.BadRequest noSeAcepto) {
      return "redirect:/admin-donantes-importar-error";
    }
    String destino =
        UriComponentsBuilder.fromPath("admin-donantes-importando")
            .queryParam("archivo", id)
            .queryParam("nombre", archivo.getOriginalFilename())
            .encode()
            .toUriString();
    return Envios.listo(
        destino,
        "Recibimos el archivo. Podés seguir usando el sitio mientras se importa.",
        atributos);
  }

  /**
   * Registrar donante: una persona (nombre y apellido) o una organización (razón social, tipo y
   * rubro), con el correo y, para la organización, la dirección si se cargó. Donaciones no recibe
   * todavía el documento, la fecha de nacimiento, el género, el CUIT ni el teléfono del alta.
   */
  @PostMapping("/admin-donantes-registrado")
  String registrar(
      @RequestParam(defaultValue = "persona") String tipo,
      @RequestParam(defaultValue = "") String nombre,
      @RequestParam(defaultValue = "") String apellido,
      @RequestParam(defaultValue = "") String razonSocial,
      @RequestParam(required = false) String tipoOrganizacion,
      @RequestParam(required = false) String rubro,
      @RequestParam String correo,
      @RequestParam(required = false) String calle,
      @RequestParam(required = false) String altura,
      @RequestParam(required = false) String localidad,
      @RequestParam(required = false) String provincia,
      @RequestParam(required = false) String pais,
      @RequestParam(required = false) String postal,
      RedirectAttributes atributos) {
    PersonaNueva persona;
    String quien;
    if ("organizacion".equals(tipo)) {
      quien = razonSocial.trim();
      persona =
          new PersonaNueva.Juridica(
              "JURIDICA",
              quien,
              tipoJuridico(tipoOrganizacion),
              vacioANull(rubro),
              direccion(calle, altura, postal, localidad, provincia, pais),
              List.of(new PersonaNueva.Contacto("CORREO", correo.trim(), true)),
              List.of());
    } else {
      persona = new PersonaNueva.Humana(nombre.trim(), apellido.trim(), correo.trim());
      quien = nombre.trim() + " " + apellido.trim();
    }
    personas.registrarDonante(persona);
    return Envios.listo("admin-donantes", "Registraste a " + quien + " como donante.", atributos);
  }

  /**
   * Los tipos del formulario con el nombre del contrato (ONG, EMPRESA, INSTITUCION, GUBERNAMENTAL).
   */
  static String tipoJuridico(String opcion) {
    String tipo =
        Normalizer.normalize(opcion == null ? "" : opcion, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toUpperCase(Locale.ROOT);
    return switch (tipo) {
      case "EMPRESA", "INSTITUCION", "GUBERNAMENTAL" -> tipo;
      default -> "ONG";
    };
  }

  /** La dirección si se cargó la calle; si no, ninguna. La altura, solo si es un número. */
  static PersonaNueva.Direccion direccion(
      String calle, String altura, String postal, String localidad, String provincia, String pais) {
    if (calle == null || calle.isBlank()) {
      return null;
    }
    String numero = altura == null ? "" : altura.trim();
    return new PersonaNueva.Direccion(
        calle.trim(),
        numero.matches("\\d{1,6}") ? Integer.valueOf(numero) : null,
        vacioANull(postal),
        vacioANull(localidad),
        vacioANull(provincia),
        vacioANull(pais));
  }

  static String vacioANull(String texto) {
    return texto == null || texto.isBlank() ? null : texto.trim();
  }
}
