package grupo5.donaciones.infrastructure.persistencia.entities.personas;

import grupo5.donaciones.models.entities.personas.TipoDocumento;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Entity
@Table(name = "persona")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "tipo_persona", length = 10)
@Getter
@Setter
public abstract class PersonaEntity {
  @Id private UUID id;

  @ManyToOne
  @JoinColumn(name = "id_direccion")
  private DireccionEntity direccion;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_documento", length = 15)
  private TipoDocumento tipoDocumento;

  @Column(name = "nro_documento")
  private String nroDocumento;

  // SELECT (y no JOIN): con otra colección EAGER en Juridica, dos listas en un mismo join fallan
  @OneToMany(
      mappedBy = "persona",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  @Fetch(FetchMode.SELECT)
  private List<MedioDeContactoEntity> mediosDeContacto = new ArrayList<>();
}
