package grupo5.notificaciones.infrastructure.persistencia.entities;

import grupo5.notificaciones.models.entities.contacto.EstadoMensajeContacto;
import grupo5.notificaciones.models.entities.contacto.TemaContacto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "mensaje_contacto")
@Getter
@Setter
@NoArgsConstructor
public class MensajeContactoEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "numero")
  private Long numero;

  @Column(name = "clave_envio", nullable = false, unique = true)
  private UUID claveEnvio;

  @Enumerated(EnumType.STRING)
  @Column(name = "tema", nullable = false, length = 20)
  private TemaContacto tema;

  @Column(name = "texto", nullable = false, columnDefinition = "TEXT")
  private String texto;

  @Column(name = "codigo_error", length = 64)
  private String codigoError;

  @Column(name = "correo", nullable = false, length = 254)
  private String correo;

  @Column(name = "ip_origen", length = 45)
  private String ipOrigen;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado", nullable = false, length = 20)
  private EstadoMensajeContacto estado;

  @Column(name = "recibido_en", nullable = false)
  private LocalDateTime recibidoEn;

  @Column(name = "respuesta", columnDefinition = "TEXT")
  private String respuesta;

  @Column(name = "respondido_en")
  private LocalDateTime respondidoEn;
}
