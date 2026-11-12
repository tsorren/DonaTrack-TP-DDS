package grupo5.cliente.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import grupo5.cliente.puertos.DirectorioDeAdministradores;
import grupo5.cliente.puertos.DirectorioDeAdministradores.Administrador;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;

/** Quién hizo cada cambio del historial, como lo lee el admin (R11-10, C8). */
class AutoresDelHistorialTest {

  private static final String CUENTA = "6f1c2c1e-0000-4000-8000-0000000000aa";

  private final DirectorioDeAdministradores cuentas = mock(DirectorioDeAdministradores.class);

  @Test
  void sinAutorNoDiceQuien() {
    AutoresDelHistorial autores = AutoresDelHistorial.con(cuentas);

    assertEquals("", autores.de(null));
    assertEquals("", autores.de(" "));
    assertEquals("", autores.de("SISTEMA"));
    verifyNoInteractions(cuentas);
  }

  @Test
  void unTextoQueNoEsUnaCuentaSeMuestraComoEsta() {
    assertEquals("Leo Ejemplo", AutoresDelHistorial.con(cuentas).de(" Leo Ejemplo "));
    verifyNoInteractions(cuentas);
  }

  @Test
  void unaCuentaSinNombreCargadoSeMuestraConSuCorreo() {
    when(cuentas.cuenta(CUENTA))
        .thenReturn(
            Optional.of(new Administrador(CUENTA, null, " ", "admin@ejemplo.org", null, true)));

    assertEquals("admin@ejemplo.org", AutoresDelHistorial.con(cuentas).de(CUENTA));
  }

  @Test
  void unaCuentaQueYaNoEstaOUnProveedorQueNoRespondeNoMuestranElCodigo() {
    when(cuentas.cuenta(anyString())).thenReturn(Optional.empty());
    assertEquals(AutoresDelHistorial.SIN_NOMBRE, AutoresDelHistorial.con(cuentas).de(CUENTA));

    when(cuentas.cuenta(anyString())).thenThrow(new RestClientException("ejemplo"));
    assertEquals(AutoresDelHistorial.SIN_NOMBRE, AutoresDelHistorial.con(cuentas).de(CUENTA));

    assertEquals(AutoresDelHistorial.SIN_NOMBRE, AutoresDelHistorial.con(null).de(CUENTA));
  }
}
