package grupo5.incentivos.infrastructure.persistencia.entities;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Un {@code EventoDonacion} del historial. Necesita id propio porque sus categorías son una
 * colección (Hibernate no admite colecciones dentro de un embeddable de otra colección).
 */
@Entity
@Table(name = "donante_historial_donacion")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DonanteHistorialDonacionEntity {

  @Id @EqualsAndHashCode.Include private UUID id;

  @Column(name = "orden", nullable = false)
  private int orden;

  @Column(name = "donacion_id")
  private UUID donacionId;

  @Column(name = "cantidad_bienes")
  private Integer cantidadBienes;

  @Column(name = "fecha", nullable = false)
  private LocalDate fecha;

  // FetchType.EAGER justificado: el mapeo a dominio ocurre fuera de la transacción del repo.
  @SuppressWarnings("squid:S1319")
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "donante_historial_donacion_categoria",
      joinColumns = @JoinColumn(name = "historial_donacion_id", nullable = false))
  @OrderColumn(name = "orden")
  @Column(name = "categoria", nullable = false, length = 255)
  private List<String> categorias = new ArrayList<>();
}
