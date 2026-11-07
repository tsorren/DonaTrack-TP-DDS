package grupo5.logistica.models.entities.rutas.direccion;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@ToString
public final class Direccion {
  private String calle;
  private Integer altura;
  private Integer piso;
  private String departamento;
  private String codigoPostal;
  private final Localidad localidad;
  private CoordenadasGeograficas coordenadas;

  public Direccion(
      String calle,
      Integer altura,
      Integer piso,
      String departamento,
      String codigoPostal,
      Localidad localidad) {
    this(calle, altura, piso, departamento, codigoPostal, localidad, null);
  }

  public Direccion(
      String calle,
      Integer altura,
      Integer piso,
      String departamento,
      String codigoPostal,
      Localidad localidad,
      CoordenadasGeograficas coordenadas) {
    validarDireccion(calle, altura, codigoPostal, localidad);
    this.calle = calle;
    this.altura = altura;
    this.piso = piso;
    this.departamento = departamento;
    this.codigoPostal = codigoPostal;
    this.localidad = localidad;
    this.coordenadas = coordenadas;
  }

  public String calle() {
    return calle;
  }

  public Integer altura() {
    return altura;
  }

  public Integer piso() {
    return piso;
  }

  public String departamento() {
    return departamento;
  }

  public String codigoPostal() {
    return codigoPostal;
  }

  public Localidad localidad() {
    return localidad;
  }

  public CoordenadasGeograficas coordenadas() {
    return coordenadas;
  }

  public boolean tieneCoordenadas() {
    return coordenadas != null;
  }

  public void actualizarCoordenadas(CoordenadasGeograficas nuevasCoordenadas) {
    this.coordenadas = nuevasCoordenadas;
  }

  private static final String VALOR_ANONIMIZADO = "ANONIMIZADO";

  public void anonimizar() {
    this.calle = VALOR_ANONIMIZADO;
    this.altura = 1;
    this.piso = 0;
    this.departamento = VALOR_ANONIMIZADO;
    this.codigoPostal = VALOR_ANONIMIZADO;
    this.coordenadas = null;
    this.localidad.anonimizar();
  }

  private static void validarDireccion(
      String calle, Integer altura, String codigoPostal, Localidad localidad) {
    if (calle == null || calle.trim().isEmpty()) {
      throw new ValidationException(ErrorCatalog.DIRECCION_CALLE_VACIA);
    }
    if (altura == null || altura <= 0) {
      throw new ValidationException(ErrorCatalog.DIRECCION_ALTURA_INVALIDA);
    }
    if (codigoPostal == null || codigoPostal.trim().isEmpty()) {
      throw new ValidationException(ErrorCatalog.DIRECCION_CODIGO_POSTAL_VACIO);
    }
    if (localidad == null) {
      throw new ValidationException(ErrorCatalog.DIRECCION_LOCALIDAD_NULA);
    }
  }
}
