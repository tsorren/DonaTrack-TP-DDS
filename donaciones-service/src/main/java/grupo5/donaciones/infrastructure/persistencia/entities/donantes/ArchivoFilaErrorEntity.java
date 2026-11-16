package grupo5.donaciones.infrastructure.persistencia.entities.donantes;

import grupo5.donaciones.models.entities.donantes.MotivoErrorFila;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Fila del padrón que no se importó (HB-14). Id generado por la base: con un id asignado, {@code
 * saveAll} haría un SELECT por fila antes de insertar.
 */
@Entity
@Table(name = "archivo_fila_error")
@Getter
@Setter
public class ArchivoFilaErrorEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "id_archivo", nullable = false)
  private UUID archivoId;

  @Column(name = "linea", nullable = false)
  private int linea;

  @Enumerated(EnumType.STRING)
  @Column(name = "motivo", nullable = false, length = 40)
  private MotivoErrorFila motivo;

  @Column(name = "codigo", length = 20)
  private String codigo;
}
