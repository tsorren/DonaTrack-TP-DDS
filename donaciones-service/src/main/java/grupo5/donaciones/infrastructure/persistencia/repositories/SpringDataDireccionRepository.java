package grupo5.donaciones.infrastructure.persistencia.repositories;

import grupo5.donaciones.infrastructure.persistencia.entities.personas.DireccionEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataDireccionRepository extends JpaRepository<DireccionEntity, UUID> {

  // Un parámetro null se traduce a IS NULL, así que piso y departamento ausentes también calzan.
  @SuppressWarnings("java:S107")
  Optional<DireccionEntity>
      findByCalleAndAlturaAndPisoAndDepartamentoAndCodigoPostalAndLocalidadAndProvinciaAndPais(
          String calle,
          Integer altura,
          Integer piso,
          String departamento,
          String codigoPostal,
          String localidad,
          String provincia,
          String pais);
}
