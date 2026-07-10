package grupo5.logistica.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.repositories.ICamionRepository;
import grupo5.logistica.services.impl.ValidadorPatentes;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ValidadorPatentesTest {

  private ICamionRepository camionRepository;
  private ValidadorPatentes validadorPatentes;

  @BeforeEach
  void setUp() {

    camionRepository = mock(ICamionRepository.class);

    validadorPatentes = new ValidadorPatentes(camionRepository);
  }

  // =====================================================
  // formato válido
  // =====================================================

  @Test
  void validar_deberiaAceptarPatenteFormatoViejo() {

    when(camionRepository.findAll()).thenReturn(List.of());

    assertDoesNotThrow(() -> validadorPatentes.validar("ABC123"));
  }

  @Test
  void validar_deberiaAceptarPatenteFormatoNuevo() {

    when(camionRepository.findAll()).thenReturn(List.of());

    assertDoesNotThrow(() -> validadorPatentes.validar("AB123CD"));
  }

  // =====================================================
  // normalización
  // =====================================================

  @Test
  void validar_deberiaAceptarPatenteConEspaciosYMinusculas() {

    when(camionRepository.findAll()).thenReturn(List.of());

    assertDoesNotThrow(() -> validadorPatentes.validar(" ab-123-cd "));
  }

  // =====================================================
  // formato inválido
  // =====================================================

  @Test
  void validar_deberiaFallarCuandoPatenteEsNull() {

    assertThrows(ValidationException.class, () -> validadorPatentes.validar(null));

    verifyNoInteractions(camionRepository);
  }

  @Test
  void validar_deberiaFallarCuandoPatenteEstaVacia() {

    assertThrows(ValidationException.class, () -> validadorPatentes.validar("   "));

    verifyNoInteractions(camionRepository);
  }

  @Test
  void validar_deberiaFallarCuandoFormatoEsIncorrecto() {

    when(camionRepository.findAll()).thenReturn(List.of());

    assertThrows(ValidationException.class, () -> validadorPatentes.validar("123ABC"));
  }

  @Test
  void validar_deberiaFallarCuandoTieneCantidadIncorrectaDeCaracteres() {

    when(camionRepository.findAll()).thenReturn(List.of());

    assertThrows(ValidationException.class, () -> validadorPatentes.validar("AB12CD"));
  }

  // =====================================================
  // unicidad
  // =====================================================

  @Test
  void validar_deberiaFallarCuandoPatenteYaExiste() {

    Camion camion = mock(Camion.class);

    when(camion.getPatente()).thenReturn("AB123CD");

    when(camionRepository.findAll()).thenReturn(List.of(camion));

    assertThrows(BusinessStateException.class, () -> validadorPatentes.validar("AB123CD"));
  }

  @Test
  void validar_deberiaAceptarCuandoPatenteNoExiste() {

    Camion camion = mock(Camion.class);

    when(camion.getPatente()).thenReturn("XY999ZZ");

    when(camionRepository.findAll()).thenReturn(List.of(camion));

    assertDoesNotThrow(() -> validadorPatentes.validar("AB123CD"));
  }
}
