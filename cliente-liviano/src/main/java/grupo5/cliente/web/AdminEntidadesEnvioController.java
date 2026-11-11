package grupo5.cliente.web;

import grupo5.cliente.puertos.AdminPersonasPuerto;
import grupo5.cliente.puertos.PersonaNueva;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Los envíos reales de Entidades, fuera de la maqueta (en la maqueta los atiende SitioController,
 * que vuelve sin guardar).
 */
@Controller
@Profile("!maqueta")
public class AdminEntidadesEnvioController {

  private static final String ENTIDADES = "admin-entidades";

  private final AdminPersonasPuerto personas;

  AdminEntidadesEnvioController(AdminPersonasPuerto personas) {
    this.personas = personas;
  }

  /**
   * Aprobar a una entidad que pidió sumarse ({@code entidad} es su cuenta): la da de alta en
   * Donaciones y habilita su cuenta (SolicitudesDeEntidad). La dirección es de un solo tramo para
   * que la alcance la regla de acceso del área de administración.
   */
  @PostMapping("/admin-entidades-aprobada")
  String aprobar(@RequestParam String entidad, RedirectAttributes atributos) {
    String nombre = personas.aprobarEntidad(entidad);
    return Envios.listo(
        ENTIDADES,
        "Aprobaste a " + nombre + ". Ya puede publicar sus necesidades y recibir donaciones.",
        atributos);
  }

  /**
   * Registrar entidad: la organización con su dirección y su responsable, que queda activa. El CUIT
   * no viaja todavía: PersonaNueva no lo tiene.
   */
  @PostMapping("/admin-entidades-registrada")
  String registrar(
      @RequestParam String razonSocial,
      @RequestParam String tipo,
      @RequestParam String calle,
      @RequestParam String altura,
      @RequestParam String localidad,
      @RequestParam String provincia,
      @RequestParam String pais,
      @RequestParam String responsable,
      @RequestParam String correo,
      RedirectAttributes atributos) {
    String nombre = razonSocial.trim();
    personas.registrarEntidad(
        new PersonaNueva.Juridica(
            nombre,
            AdminDonantesEnvioController.tipoJuridico(tipo),
            null,
            AdminDonantesEnvioController.direccion(calle, altura, null, localidad, provincia, pais),
            PersonaNueva.Humana.deNombreCompleto(responsable, correo.trim()),
            correo.trim()));
    return Envios.listo(
        ENTIDADES,
        "Registraste a " + nombre + ". Queda activa: la registraste vos, no necesita aprobación.",
        atributos);
  }
}
