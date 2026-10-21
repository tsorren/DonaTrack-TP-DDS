package grupo5.donaciones.infrastructure.persistencia.entities.personas;

import grupo5.donaciones.models.entities.personas.TipoJuridico;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Entity
@Table(name = "juridica")
@DiscriminatorValue("JURIDICA")
@Getter
@Setter
public class JuridicaEntity extends PersonaEntity {
  @Column(name = "razon_social", nullable = false)
  private String razonSocial;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_juridico", nullable = false, length = 20)
  private TipoJuridico tipoJuridico;

  @Column(name = "rubro", nullable = false)
  private String rubro;

  // Lado inverso: la FK vive en humana.representante_de. Sin cascade: quitar un representante
  // lo desliga de la jurídica pero no borra a la humana.
  @OneToMany(mappedBy = "representanteDe", fetch = FetchType.EAGER)
  @Fetch(FetchMode.SELECT)
  private List<HumanaEntity> representantes = new ArrayList<>();
}
