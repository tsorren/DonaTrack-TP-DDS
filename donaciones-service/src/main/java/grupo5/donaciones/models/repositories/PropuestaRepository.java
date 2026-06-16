package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.BaseRepositoryEnMemoria;
import grupo5.donaciones.models.entities.donaciones.matchmaking.propuestas.Propuesta;
import org.springframework.stereotype.Repository;

// Simulacion de base de datos, despues hay que implementar una enserio
@Repository
public class PropuestaRepository extends BaseRepositoryEnMemoria<Propuesta> {

  // Metodo para simular la busqueda de propuestas activas
  // public List<Propuesta> findByActivaTrue() {return
  // findAll().stream().filter(Propuesta::estaActiva).toList();}
}
