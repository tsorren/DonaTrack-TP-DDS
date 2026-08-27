package grupo5.donaciones.models.entities.personas;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.ubicaciones.Direccion;
import grupo5.donaciones.models.entities.ubicaciones.Localidad;
import grupo5.donaciones.models.entities.ubicaciones.Pais;
import grupo5.donaciones.models.entities.ubicaciones.Provincia;
import grupo5.donaciones.models.privacidad.Anonimizable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
public final class Juridica extends Persona {
  @Getter(AccessLevel.NONE)
  private final List<Humana> representantes = new ArrayList<>();

  private String razonSocial;
  private TipoJuridico tipo;
  private String rubro;

  public Juridica(
      Humana representanteInicial, String razonSocial, TipoJuridico tipo, String rubro) {
    super();
    if (representanteInicial == null) {
      throw new ValidationException(ErrorCatalog.JURIDICA_SIN_REPRESENTANTE_INICIAL);
    }
    validarJuridica(razonSocial, tipo, rubro);
    this.representantes.add(representanteInicial);
    this.razonSocial = razonSocial;
    this.tipo = tipo;
    this.rubro = rubro;
  }

  public List<Humana> getRepresentantes() {
    return Collections.unmodifiableList(representantes);
  }

  public void actualizar(String razonSocial, TipoJuridico tipo, String rubro) {
    validarJuridica(razonSocial, tipo, rubro);
    this.razonSocial = razonSocial;
    this.tipo = tipo;
    this.rubro = rubro;
  }

  private static void validarJuridica(String razonSocial, TipoJuridico tipo, String rubro) {
    if (razonSocial == null || razonSocial.trim().isEmpty()) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    if (tipo == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    if (rubro == null || rubro.trim().isEmpty()) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
  }

  public void agregarRepresentante(Humana representante) {
    if (representante == null) {
      throw new ValidationException(ErrorCatalog.JURIDICA_AGREGAR_REPRESENTANTE_NULO);
    }
    this.representantes.add(representante);
  }

  public void quitarRepresentante(Humana representante) {
    if (!this.representantes.contains(representante)) {
      throw new ValidationException(ErrorCatalog.JURIDICA_QUITAR_REPRESENTANTE_INEXISTENTE);
    }
    if (this.representantes.size() == 1) {
      throw new BusinessStateException(ErrorCatalog.JURIDICA_SIN_REPRESENTANTES_RESTANTES);
    }
    this.representantes.remove(representante);
  }

  public void limpiarRepresentantes() {
    this.representantes.clear();
  }

  @Override
  public TipoPersona getTipoPersona() {
    return TipoPersona.JURIDICA;
  }

  @Override
  public String getNombreCompleto() {
    return this.razonSocial;
  }

  @Override
  public void anonimizar() {
    this.razonSocial = Anonimizable.VALOR_STRING;
    this.representantes.forEach(Anonimizable::anonimizar);
    this.actualizarDocumento(null, null);
    if (this.getDireccion() != null) {
      this.actualizarDireccion(
          new Direccion(
              Anonimizable.VALOR_STRING,
              1,
              0,
              Anonimizable.VALOR_STRING,
              Anonimizable.VALOR_STRING,
              new Localidad(
                  Anonimizable.VALOR_STRING,
                  new Provincia(Anonimizable.VALOR_STRING, new Pais(Anonimizable.VALOR_STRING)))));
    }
  }
}
