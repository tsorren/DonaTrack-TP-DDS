package grupo5.donaciones.models.repositories;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.donaciones.models.entities.donaciones.Deposito;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.personas.*;
import grupo5.donaciones.models.repositories.impl.DonacionRepositoryEnMemoria;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DonacionRepositoryTest {

  private IDonacionesRepository repository;

  @BeforeEach
  void setUp() {
    repository = new DonacionRepositoryEnMemoria();
  }

  @Test
  void save_deberiaPersistirDonacion() {
    Donacion donacion = crearDonacion();

    Donacion saved = repository.save(donacion);

    assertNotNull(saved);
    assertEquals(donacion.getId(), saved.getId());
    assertTrue(repository.existsById(donacion.getId()));
  }

  @Test
  void findById_deberiaRetornarDonacionSiExiste() {
    Donacion donacion = crearDonacion();
    repository.save(donacion);

    Optional<Donacion> found = repository.findById(donacion.getId());

    assertTrue(found.isPresent());
    assertEquals(donacion.getId(), found.get().getId());
  }

  @Test
  void findById_deberiaRetornarVacioSiNoExiste() {
    Optional<Donacion> found = repository.findById(UUID.randomUUID());

    assertTrue(found.isEmpty());
  }

  @Test
  void delete_deberiaRemoverDonacion() {
    Donacion donacion = crearDonacion();
    repository.save(donacion);

    repository.delete(donacion);

    assertFalse(repository.existsById(donacion.getId()));
  }

  @Test
  void findAll_deberiaRetornarTodasLasDonaciones() {
    repository.save(crearDonacion());
    repository.save(crearDonacion());

    List<Donacion> all = repository.findAll();

    assertEquals(2, all.size());
  }

  private Donacion crearDonacion() {
    Humana humana = new Humana("Juan", "Perez", LocalDate.of(1990, Month.JANUARY, 1));
    Donante donante = new Donante(humana);
    Pais pais = new Pais();
    pais.setNombre("Argentina");
    Provincia provincia = new Provincia();
    provincia.setNombre("Buenos Aires");
    provincia.setPais(pais);
    Localidad localidad = new Localidad();
    localidad.setNombre("CABA");
    localidad.setProvincia(provincia);
    Direccion direccion = new Direccion("Calle Falsa", 123, null, null, "1000", localidad);
    Deposito deposito = new Deposito("Deposito Test", direccion);
    return new Donacion(donante, deposito);
  }
}
