package grupo5.logistica.infrastructure.persistencia.mongo.documents; // NOSONAR

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

@SuppressWarnings("java:S120")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DireccionSubdocument {

  private String calle;
  private Integer altura;
  private Short piso;
  private String departamento;
  private String codigoPostal;
  private String localidad;
  private String provincia;
  private String pais;

  /** GeoJSON Point: {@code coordinates = [longitud, latitud]}. */
  private GeoJsonPoint ubicacion;
}
