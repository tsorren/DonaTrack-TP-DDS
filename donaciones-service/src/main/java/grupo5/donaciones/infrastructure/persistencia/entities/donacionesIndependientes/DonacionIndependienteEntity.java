package grupo5.donaciones.infrastructure.persistencia.entities.donacionesIndependientes;

import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "donacion_independiente")
@Getter
@Setter
public class DonacionIndependienteEntity {
  @Id private UUID id;

  // FK a donacion en la base; en el dominio es una referencia por id a otro agregado
  @Column(name = "id_donacion", nullable = false)
  private UUID donacionId;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_actual", nullable = false, length = 25)
  private TipoEstadoDonacion estadoActual;

  @Column(name = "fecha_registro", nullable = false)
  private LocalDateTime fechaRegistro;

  // D3: el destino de la asignación, por id (DTI-06)
  @Column(name = "id_necesidad")
  private UUID necesidadId;

  @Column(name = "id_periodo")
  private UUID periodoId;

  @Column(name = "id_beneficiario_asignado")
  private UUID entidadBeneficiariaId;

  // A2: sube en cada guardado (lo fuerza el adapter, porque agregar o quitar ítems no la cambia)
  @Version
  @Column(name = "version", nullable = false)
  private Long version;

  // A9: el dominio consume los ítems en orden, la base tiene que devolverlos igual
  @OneToMany(mappedBy = "donacion", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("orden ASC")
  private List<ItemDonacionIndependienteEntity> items = new ArrayList<>();

  @OneToMany(mappedBy = "donacion", cascade = CascadeType.ALL)
  private List<EstadoDonacionIndependienteEntity> estados = new ArrayList<>();

  @OneToMany(mappedBy = "donacion", cascade = CascadeType.ALL)
  private List<CambioEstadoDonacionIndependienteEntity> historial = new ArrayList<>();
}
