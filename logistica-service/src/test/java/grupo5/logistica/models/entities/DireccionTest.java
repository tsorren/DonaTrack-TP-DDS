package grupo5.logistica.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;

import grupo5.logistica.models.entities.rutas.direccion.Direccion;
import grupo5.logistica.models.entities.rutas.direccion.Localidad;
import grupo5.logistica.models.entities.rutas.direccion.Pais;
import grupo5.logistica.models.entities.rutas.direccion.Provincia;
import org.junit.jupiter.api.Test;

class DireccionTest {

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
}
