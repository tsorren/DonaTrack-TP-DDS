package grupo5.logistica.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.models.entities.rutas.direccion.Direccion;
import grupo5.logistica.models.entities.rutas.direccion.Localidad;
import grupo5.logistica.models.entities.rutas.direccion.Pais;
import grupo5.logistica.models.entities.rutas.direccion.Provincia;
import org.junit.jupiter.api.Test;

class DireccionTest {

  @Test
  void constructorConDatosValidosConservaLosValores() {
    Localidad localidad = localidadValida();
    Direccion direccion = new Direccion("Corrientes", 1234, 5, "A", "C1043", localidad);
    assertEquals("Corrientes", direccion.calle());
    assertEquals(1234, direccion.altura());
    assertEquals(5, direccion.piso());
    assertEquals("A", direccion.departamento());
    assertEquals("C1043", direccion.codigoPostal());
    assertEquals(localidad, direccion.localidad());
  }

  @Test
  void constructorRechazaCalleNulaOVacia() {
    Localidad localidad = localidadValida();
    assertThrows(
        ValidationException.class, () -> new Direccion(null, 1, null, null, "1000", localidad));
    assertThrows(
        ValidationException.class, () -> new Direccion(" ", 1, null, null, "1000", localidad));
  }

  @Test
  void constructorRechazaAlturaNulaONoPositiva() {
    Localidad localidad = localidadValida();
    assertThrows(
        ValidationException.class,
        () -> new Direccion("Calle", null, null, null, "1000", localidad));
    assertThrows(
        ValidationException.class, () -> new Direccion("Calle", 0, null, null, "1000", localidad));
    assertThrows(
        ValidationException.class, () -> new Direccion("Calle", -1, null, null, "1000", localidad));
  }

  @Test
  void constructorRechazaCodigoPostalNuloOVacio() {
    Localidad localidad = localidadValida();
    assertThrows(
        ValidationException.class, () -> new Direccion("Calle", 1, null, null, null, localidad));
    assertThrows(
        ValidationException.class, () -> new Direccion("Calle", 1, null, null, " ", localidad));
  }

  @Test
  void constructorRechazaLocalidadNula() {
    assertThrows(
        ValidationException.class, () -> new Direccion("Calle", 1, null, null, "1000", null));
  }

  @Test
  void anonimizarReemplazaLosDatosDeTodaLaJerarquia() {
    Pais pais = new Pais("Argentina");
    Provincia provincia = new Provincia("Buenos Aires", pais);
    Localidad localidad = new Localidad("CABA", provincia);
    Direccion direccion = new Direccion("Calle", 123, 4, "B", "1000", localidad);

    direccion.anonimizar();

    assertEquals("ANONIMIZADO", direccion.calle());
    assertEquals(1, direccion.altura());
    assertEquals(0, direccion.piso());
    assertEquals("ANONIMIZADO", direccion.departamento());
    assertEquals("ANONIMIZADO", direccion.codigoPostal());
    assertEquals("ANONIMIZADO", localidad.nombre());
    assertEquals("ANONIMIZADO", provincia.nombre());
    assertEquals("ANONIMIZADO", pais.nombre());
  }

  private static Localidad localidadValida() {
    return new Localidad("CABA", new Provincia("Buenos Aires", new Pais("Argentina")));
  }
}
