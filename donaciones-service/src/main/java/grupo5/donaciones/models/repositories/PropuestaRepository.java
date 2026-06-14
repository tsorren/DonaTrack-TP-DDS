package grupo5.donaciones.models.repositories;

import grupo5.donaciones.models.entities.beneficiarios.NecesidadRecurrente;
import java.util.ArrayList;
import java.util.List;

import grupo5.donaciones.models.entities.donaciones.matchmaking.propuestas.Propuesta;
import org.springframework.stereotype.Repository;

// Simulacion de base de datos, despues hay que implementar una enserio
@Repository
public class PropuestaRepository {
    private final List<Propuesta> baseDeDatosFalsa = new ArrayList<>();

    // Método para simular que guardás un comedor
    public void save(Propuesta propuesta) {
        if (!baseDeDatosFalsa.contains(propuesta)) {
            baseDeDatosFalsa.add(propuesta);
        }
    }

    // Método para simular la búsqueda de comedores activos
    public List<Propuesta> findByActivaTrue() {
        return baseDeDatosFalsa.stream().filter(Propuesta:: ? ).toList();
    }
}
