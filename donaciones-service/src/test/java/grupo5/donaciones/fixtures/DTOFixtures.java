package grupo5.donaciones.fixtures;

import grupo5.donaciones.dto.NecesidadDTO;
import grupo5.donaciones.dto.categorias.AliasSubcategoriaInputDTO;
import grupo5.donaciones.dto.categorias.CategoriaInputDTO;
import grupo5.donaciones.dto.categorias.SubcategoriaInputDTO;
import grupo5.donaciones.dto.direcciones.DireccionInputDTO;
import grupo5.donaciones.dto.direcciones.DireccionOutputDTO;
import grupo5.donaciones.dto.donaciones.inputs.DonacionInputDTO;
import grupo5.donaciones.dto.donaciones.inputs.ItemDonacionInputDTO;
import grupo5.donaciones.dto.donacionesIndependientes.CambioEstadoDonacionIndependienteRequestDTO;
import grupo5.donaciones.dto.donantes.ArchivoInputDTO;
import grupo5.donaciones.dto.donantes.DonanteInputDTO;
import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaInputDTO;
import grupo5.donaciones.dto.itemsNormalizados.inputs.ItemDonacionNormalizadoPatchDTO;
import grupo5.donaciones.dto.personas.HumanaInputDTO;
import grupo5.donaciones.dto.personas.JuridicaInputDTO;
import grupo5.donaciones.dto.propuestas.ActualizarEstadoRequestDTO;
import grupo5.donaciones.models.entities.categorias.Unidad;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import grupo5.donaciones.models.entities.itemsNormalizados.EstadoNormalizacion;
import grupo5.donaciones.models.entities.personas.TipoDocumento;
import grupo5.donaciones.models.entities.personas.TipoJuridico;
import grupo5.donaciones.models.entities.personas.TipoPersona;
import grupo5.donaciones.models.entities.propuestas.EstadoPropuesta;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.UUID;

public final class DTOFixtures {

  private DTOFixtures() {}

  public static DireccionInputDTO direccionInput() {
    return new DireccionInputDTO(
        "Av. Corrientes", 1234, 2, "A", "1043", "CABA", "Buenos Aires", "Argentina");
  }

  public static DireccionOutputDTO direccionOutput() {
    return new DireccionOutputDTO(
        "Av. Corrientes", 1234, 2, "A", "1043", "CABA", "Buenos Aires", "Argentina");
  }

  public static HumanaInputDTO humanaInput(String nombre, String apellido, String doc) {
    return new HumanaInputDTO(
        TipoPersona.HUMANA,
        TipoDocumento.DNI,
        doc,
        direccionInput(),
        List.of(),
        nombre,
        apellido,
        null,
        LocalDate.of(1990, Month.JANUARY, 1));
  }

  public static HumanaInputDTO humanaInput() {
    return humanaInput("Juan", "Pérez", "12345678");
  }

  public static JuridicaInputDTO juridicaInput(String razonSocial, TipoJuridico tipoJuridico) {
    return new JuridicaInputDTO(
        TipoPersona.JURIDICA,
        TipoDocumento.CUIT,
        "30112233445",
        direccionInput(),
        List.of(),
        razonSocial,
        tipoJuridico,
        "Alimentos",
        List.of());
  }

  public static JuridicaInputDTO juridicaInput() {
    return juridicaInput("Fundación Donar", TipoJuridico.ONG);
  }

  public static CategoriaInputDTO categoriaInput(String nombre, Unidad unidad) {
    return new CategoriaInputDTO(nombre, false, true, unidad);
  }

  public static CategoriaInputDTO categoriaInput() {
    return categoriaInput("Alimentos", Unidad.KILOGRAMO);
  }

  public static SubcategoriaInputDTO subcategoriaInput(String nombre, UUID categoriaId) {
    return new SubcategoriaInputDTO(nombre, categoriaId, List.of());
  }

  public static SubcategoriaInputDTO subcategoriaInput(UUID categoriaId) {
    return subcategoriaInput("Arroz", categoriaId);
  }

  public static AliasSubcategoriaInputDTO aliasSubcategoriaInput(String alias) {
    return new AliasSubcategoriaInputDTO(alias);
  }

  public static DonanteInputDTO donanteInput(UUID personaId) {
    return new DonanteInputDTO(personaId);
  }

  public static ArchivoInputDTO archivoInput(String path) {
    return new ArchivoInputDTO(path);
  }

  public static ArchivoInputDTO archivoInput() {
    return archivoInput("/data/donantes.csv");
  }

  public static EntidadBeneficiariaInputDTO entidadBeneficiariaInput(UUID juridicaId) {
    return new EntidadBeneficiariaInputDTO(juridicaId);
  }

  public static DonacionInputDTO donacionInput(UUID donanteId) {
    return new DonacionInputDTO(
        donanteId,
        "Donación de alimentos y abrigo",
        List.of(new ItemDonacionInputDTO("Arroz 1kg", null, null, null, 1.0, 0.002, 10)),
        "Depósito Central",
        direccionInput(),
        LocalDateTime.now());
  }

  public static ItemDonacionNormalizadoPatchDTO patchItemNormalizadoInput(
      EstadoNormalizacion estado, UUID subcategoriaId) {
    return new ItemDonacionNormalizadoPatchDTO(estado, subcategoriaId);
  }

  public static ItemDonacionNormalizadoPatchDTO patchItemNormalizadoInput(
      EstadoNormalizacion estado) {
    return patchItemNormalizadoInput(estado, null);
  }

  public static ActualizarEstadoRequestDTO actualizarEstadoPropuestaInput(EstadoPropuesta estado) {
    return new ActualizarEstadoRequestDTO(estado, null);
  }

  public static CambioEstadoDonacionIndependienteRequestDTO cambioEstadoDIInput(
      TipoEstadoDonacion estado) {
    return new CambioEstadoDonacionIndependienteRequestDTO(estado, null, null, null, null, null);
  }

  public static NecesidadDTO necesidadInput(String tipo, UUID subcategoriaId, int cantidad) {
    return new NecesidadDTO(
        null,
        tipo,
        UUID.randomUUID(),
        subcategoriaId,
        cantidad,
        "Necesidad urgente",
        false,
        LocalDate.now(),
        LocalDate.now().plusMonths(1));
  }

  public static NecesidadDTO necesidadInput(UUID subcategoriaId) {
    return necesidadInput("ALIMENTO", subcategoriaId, 50);
  }
}
