package grupo5.donaciones.infrastructure.persistencia.repositories;

import grupo5.donaciones.infrastructure.persistencia.entities.personas.PersonaEntity;
import grupo5.donaciones.models.entities.personas.TipoDocumento;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataPersonaRepository extends JpaRepository<PersonaEntity, UUID> {

  Optional<PersonaEntity> findFirstByNroDocumento(String nroDocumento);

  List<PersonaEntity> findByNroDocumento(String nroDocumento);

  Optional<PersonaEntity> findByTipoDocumentoAndNroDocumento(
      TipoDocumento tipoDocumento, String nroDocumento);

  /** Usa el índice funcional {@code ix_correo_direccion_normalizada} (V5). */
  @Query(
      "select distinct c.persona from CorreoEntity c"
          + " where lower(trim(c.direccionCorreo)) = lower(trim(:direccionCorreo))")
  List<PersonaEntity> findByCorreo(@Param("direccionCorreo") String direccionCorreo);

  /** Cuenta si la humana ya representa a una jurídica distinta de {@code juridicaId}. */
  @Query(
      "select count(h) from HumanaEntity h"
          + " where h.id = :humanaId and h.representanteDe is not null"
          + " and h.representanteDe.id <> :juridicaId")
  long contarRepresentacionesEnOtraJuridica(
      @Param("humanaId") UUID humanaId, @Param("juridicaId") UUID juridicaId);
}
