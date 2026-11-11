package grupo5.cliente.maqueta;

import grupo5.cliente.puertos.AdminPersonasPuerto;
import grupo5.cliente.puertos.PersonaNueva;
import grupo5.cliente.vistas.AdminDonantesVista;
import grupo5.cliente.vistas.AdminDonantesVista.Donante;
import grupo5.cliente.vistas.AdminDonantesVista.FilaConError;
import grupo5.cliente.vistas.AdminDonantesVista.Importacion;
import grupo5.cliente.vistas.AdminEntidadesVista;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Los donantes y las entidades de ejemplo del admin, coherentes con la ficha
 * (docs/interfaz/flujos/raices-admin-navegable.md) y con las páginas públicas. Los estados (vacío,
 * lento, error) los elige la plantilla con {@code ?escenario=}.
 */
@Component
@ConditionalOnProperty(
    name = "cliente.datos.admin-personas",
    havingValue = "maqueta",
    matchIfMissing = true)
class AdminPersonasMaqueta implements AdminPersonasPuerto {

  private static final String PERSONA = "persona";
  private static final String PERSONA_TEXTO = "Persona";
  private static final String EMPRESA = "empresa";
  private static final String EMPRESA_TEXTO = "Empresa";
  private static final String ORGANIZACION = "organizacion";
  private static final String ORGANIZACION_TEXTO = "Organización";
  private static final String ACTIVO = "activo";
  private static final String PENDIENTE = "pendiente";
  private static final String ALIMENTOS = "alimentos";
  private static final String ALIMENTOS_TEXTO = "Alimentos";
  private static final String ESCOLARES = "escolares";
  private static final String ESCOLARES_TEXTO = "Útiles escolares";
  private static final String MUEBLES = "muebles";
  private static final String ROPA = "ropa";
  private static final String AL = "15 de octubre de 2026, 18:30";

  private static final List<Donante> DONANTES =
      List.of(
          new Donante(
              "lucia",
              "Lucía Ejemplo",
              PERSONA,
              PERSONA_TEXTO,
              "DNI 20.000.004",
              "15 de octubre",
              PENDIENTE,
              ALIMENTOS,
              ALIMENTOS_TEXTO),
          new Donante(
              "ana",
              "Ana Ejemplo",
              PERSONA,
              PERSONA_TEXTO,
              "DNI 20.000.001",
              "14 de octubre",
              ACTIVO,
              ALIMENTOS,
              ALIMENTOS_TEXTO),
          new Donante(
              "club",
              "Asociación Vecinal Ejemplo",
              ORGANIZACION,
              ORGANIZACION_TEXTO,
              "CUIT 30-00000005-0",
              "12 de octubre",
              PENDIENTE,
              MUEBLES,
              "Muebles"),
          new Donante(
              "juan",
              "Juan Ejemplo",
              PERSONA,
              PERSONA_TEXTO,
              "DNI 20.000.002",
              "3 de septiembre",
              ACTIVO,
              ESCOLARES,
              ESCOLARES_TEXTO),
          new Donante(
              "super",
              "Supermercado Ejemplo SRL",
              EMPRESA,
              EMPRESA_TEXTO,
              "CUIT 30-00000003-0",
              "28 de agosto",
              ACTIVO,
              ALIMENTOS,
              ALIMENTOS_TEXTO),
          new Donante(
              "fundacion",
              "Fundación Ejemplo Sur",
              ORGANIZACION,
              ORGANIZACION_TEXTO,
              "CUIT 30-00000002-0",
              "21 de julio",
              ACTIVO,
              ROPA,
              "Ropa"),
          new Donante(
              "libreria",
              "Librería Ejemplo SA",
              EMPRESA,
              EMPRESA_TEXTO,
              "CUIT 30-00000004-0",
              "9 de junio",
              ACTIVO,
              ESCOLARES,
              ESCOLARES_TEXTO),
          new Donante(
              "pedro",
              "Pedro Ejemplo",
              PERSONA,
              PERSONA_TEXTO,
              "DNI 20.000.003",
              "5 de mayo",
              "baja",
              ROPA,
              "Ropa"),
          new Donante(
              "carla",
              "Carla Ejemplo",
              PERSONA,
              PERSONA_TEXTO,
              "DNI 20.000.005",
              "1 de abril",
              ACTIVO,
              MUEBLES,
              "Muebles"),
          new Donante(
              "distribuidora",
              "Distribuidora Ejemplo SA",
              EMPRESA,
              EMPRESA_TEXTO,
              "CUIT 30-00000001-0",
              "2 de marzo",
              ACTIVO,
              ALIMENTOS,
              ALIMENTOS_TEXTO));

  private static final List<String> NUEVOS =
      List.of(
          "lucia",
          "ana",
          "club",
          "juan",
          "super",
          "fundacion",
          "libreria",
          "pedro",
          "carla",
          "distribuidora");

  private static final List<String> POR_NOMBRE =
      List.of(
          "ana",
          "club",
          "carla",
          "distribuidora",
          "fundacion",
          "juan",
          "libreria",
          "lucia",
          "pedro",
          "super");

  /** La importación de la ficha: 20.000 filas, con su avance y su resultado. */
  private static final Importacion IMPORTACION =
      new Importacion(
          "importando",
          "donantes-octubre.csv",
          "20.000",
          "8.400",
          "15 de octubre de 2026, 18:42",
          "20.000 filas leídas: 19.100 donantes nuevos, 754 actualizados, 20 sin cambios porque el"
              + " donante está dado de baja y 126 filas con error.",
          "126",
          List.of(
              new FilaConError("Fila 15", "Falta el apellido."),
              new FilaConError("Fila 202", "El correo ya es de otra persona, con otro documento."),
              new FilaConError("Fila 3.410", "Tiene 6 columnas y el encabezado tiene 8.")),
          "/admin-donantes-importado");

  @Override
  public AdminDonantesVista donantes() {
    return new AdminDonantesVista(
        86, AL, DONANTES, NUEVOS, NUEVOS.reversed(), POR_NOMBRE, "la lista de ejemplo");
  }

  @Override
  public Importacion importacion(UUID archivo, String nombre) {
    return IMPORTACION;
  }

  /** En la maqueta el formulario lo atiende SitioController y no sube nada. */
  @Override
  public UUID importar(MultipartFile archivo) {
    throw new UnsupportedOperationException("La maqueta no importa archivos");
  }

  /** En la maqueta el formulario lo atiende SitioController y no guarda nada. */
  @Override
  public void registrarDonante(PersonaNueva persona) {
    throw new UnsupportedOperationException("La maqueta no registra donantes");
  }

  /** En la maqueta el formulario lo atiende SitioController y no guarda nada. */
  @Override
  public void registrarEntidad(PersonaNueva.Juridica juridica) {
    throw new UnsupportedOperationException("La maqueta no registra entidades");
  }

  @Override
  public AdminEntidadesVista entidades() {
    return AdminPersonasEntidadesDeEjemplo.ENTIDADES;
  }

  /** En la maqueta el formulario lo atiende SitioController y no aprueba nada. */
  @Override
  public String aprobarEntidad(String clave) {
    throw new UnsupportedOperationException("La maqueta no aprueba entidades");
  }
}
