package grupo5.notificaciones.infrastructure.persistencia.entities;

import grupo5.notificaciones.models.entities.personas.TipoPersona;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "persona")
@Getter
@Setter
@NoArgsConstructor
public class PersonaEntity {
  @Id private UUID id;

  @Column(name = "denominacion", nullable = false, length = 150)
  private String denominacion;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_persona", nullable = false, length = 20)
  private TipoPersona tipoPersona;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
  @JoinColumn(name = "persona_id", nullable = false)
  private List<MedioDeContactoEntity> mediosDeContacto = new ArrayList<>();
}
