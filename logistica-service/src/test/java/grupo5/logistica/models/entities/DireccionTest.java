package grupo5.logistica.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.models.entities.rutas.direccion.CoordenadasGeograficas;
import grupo5.logistica.models.entities.rutas.direccion.Direccion;
import grupo5.logistica.models.entities.rutas.direccion.Localidad;
import grupo5.logistica.models.entities.rutas.direccion.Pais;
import grupo5.logistica.models.entities.rutas.direccion.Provincia;
import java.util.List;
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
    assertNull(direccion.coordenadas());
    assertFalse(direccion.tieneCoordenadas());
  }

  @Test
  void constructorConCoordenadasValidasConservaCoordenadasYOrdenGeoJson() {
    Localidad localidad = localidadValida();
    CoordenadasGeograficas coords = new CoordenadasGeograficas(-34.5986, -58.4201);
    Direccion direccion = new Direccion("Medrano", 951, null, null, "C1179", localidad, coords);

    assertTrue(direccion.tieneCoordenadas());
    assertEquals(coords, direccion.coordenadas());
    assertEquals(List.of(-58.4201, -34.5986), coords.toGeoJsonCoordinates());
  }

  @Test
  void coordenadasGeograficasPermiteOrigenCeroCeroYLamitesExtremos() {
    CoordenadasGeograficas origen = new CoordenadasGeograficas(0.0, 0.0);
    assertEquals(0.0, origen.latitud());
    assertEquals(0.0, origen.longitud());

    CoordenadasGeograficas extremo = new CoordenadasGeograficas(-90.0, 180.0);
    assertEquals(List.of(180.0, -90.0), extremo.toGeoJsonCoordinates());
  }

  @Test
  void coordenadasGeograficasRechazaValoresFueraDeRangoONoFinitos() {
    assertThrows(ValidationException.class, () -> new CoordenadasGeograficas(-90.001, 0.0));
    assertThrows(ValidationException.class, () -> new CoordenadasGeograficas(90.001, 0.0));
    assertThrows(ValidationException.class, () -> new CoordenadasGeograficas(0.0, -180.001));
    assertThrows(ValidationException.class, () -> new CoordenadasGeograficas(0.0, 180.001));
    assertThrows(ValidationException.class, () -> new CoordenadasGeograficas(Double.NaN, 0.0));
    assertThrows(
        ValidationException.class, () -> new CoordenadasGeograficas(0.0, Double.POSITIVE_INFINITY));
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
  void anonimizarReemplazaLosDatosDeTodaLaJerarquiaYLimpiacoordenadas() {
    Pais pais = new Pais("Argentina");
    Provincia provincia = new Provincia("Buenos Aires", pais);
    Localidad localidad = new Localidad("CABA", provincia);
    CoordenadasGeograficas coords = new CoordenadasGeograficas(-34.6037, -58.3816);
    Direccion direccion = new Direccion("Calle", 123, 4, "B", "1000", localidad, coords);

    direccion.anonimizar();

    assertEquals("ANONIMIZADO", direccion.calle());
    assertEquals(1, direccion.altura());
    assertEquals(0, direccion.piso());
    assertEquals("ANONIMIZADO", direccion.departamento());
    assertEquals("ANONIMIZADO", direccion.codigoPostal());
    assertNull(direccion.coordenadas());
    assertFalse(direccion.tieneCoordenadas());
    assertEquals("ANONIMIZADO", localidad.nombre());
    assertEquals("ANONIMIZADO", provincia.nombre());
    assertEquals("ANONIMIZADO", pais.nombre());
  }

  private static Localidad localidadValida() {
    return new Localidad("CABA", new Provincia("Buenos Aires", new Pais("Argentina")));
  }
}
