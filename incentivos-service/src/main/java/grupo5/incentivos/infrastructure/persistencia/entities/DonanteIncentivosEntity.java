package grupo5.incentivos.infrastructure.persistencia.entities;

import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

/**
 * Raíz persistida de {@code DonanteIncentivos}. Las métricas escalares se aplanan en esta tabla.
 *
 * <p>Todas las colecciones son EAGER (los adapters no son transaccionales y el mapeo a dominio
 * ocurre fuera de la transacción del repo) y usan SUBSELECT para evitar el producto cartesiano.
 * Ninguna es un bag: se usan {@link Set} o {@link OrderColumn}, así que no hay
 * MultipleBagFetchException.
 *
 * <p>Deuda técnica: historialDonaciones crece sin límite y se carga completo en cada lectura.
 */
@Entity
@Table(name = "donante_incentivos")
@Getter
@Setter
@NoArgsConstructor
public class  DonanteIncentivosEntity {

  @Id private UUID id;

  @Column(name = "persona_id", nullable = false)
  private UUID personaId;

  @Column(name = "nombre", length = 255)
  private String nombre;

  @Enumerated(EnumType.STRING)
  @Column(name = "categoria", nullable = false, length = 20)
  private CategoriaDonante categoria;

  @Column(name = "fecha_registro", nullable = false)
  private LocalDate fechaRegistro;

  @Column(name = "total_donaciones_historicas", nullable = false)
  private Integer totalDonacionesHistoricas = 0;

  @Column(name = "total_donaciones_exitosas", nullable = false)
  private Integer totalDonacionesExitosas = 0;

  @Column(name = "ultima_donacion")
  private LocalDate ultimaDonacion;

  // FetchType.EAGER justificado: el mapeo a dominio ocurre fuera de la transacción del repo.
  // El orden cronológico importa, por eso @OrderColumn (lista indexada, no bag).
  @SuppressWarnings("squid:S1319")
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "donante_historial_categoria",
      joinColumns = @JoinColumn(name = "donante_id", nullable = false))
  @OrderColumn(name = "orden")
  @Fetch(FetchMode.SUBSELECT)
  private List<CambioCategoriaEmbeddable> historialCategorias = new ArrayList<>();

  // FetchType.EAGER justificado: el mapeo a dominio ocurre fuera de la transacción del repo.
  @SuppressWarnings("squid:S1319")
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "donante_insignia_ganada",
      joinColumns = @JoinColumn(name = "donante_id", nullable = false))
  @OrderColumn(name = "orden")
  @Fetch(FetchMode.SUBSELECT)
  private List<InsigniaGanadaEmbeddable> insignias = new ArrayList<>();

  // FetchType.EAGER justificado: el mapeo a dominio ocurre fuera de la transacción del repo.
  // Set: el orden lo reconstruye el mapper por numeroMision. Id estable => merge actualiza.
  @SuppressWarnings("squid:S1319")
  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
  @JoinColumn(name = "donante_id", nullable = false)
  @Fetch(FetchMode.SUBSELECT)
  private Set<MisionEntity> misiones = new LinkedHashSet<>();

  // FetchType.EAGER justificado: el mapeo a dominio ocurre fuera de la transacción del repo.
  // Set: el orden lo reconstruye el mapper por la columna orden.
  @SuppressWarnings("squid:S1319")
  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
  @JoinColumn(name = "donante_id", nullable = false)
  @Fetch(FetchMode.SUBSELECT)
  private Set<DonanteHistorialDonacionEntity> historialDonaciones = new LinkedHashSet<>();

  // FetchType.EAGER justificado: el mapeo a dominio ocurre fuera de la transacción del repo.
  @SuppressWarnings("squid:S1319")
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "donante_organizacion_ayudada",
      joinColumns = @JoinColumn(name = "donante_id", nullable = false))
  @Column(name = "organizacion_id", nullable = false)
  @Fetch(FetchMode.SUBSELECT)
  private Set<UUID> organizacionesAyudadas = new LinkedHashSet<>();
}
