package grupo5.donaciones.infrastructure.persistencia.mapper;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Peso y volumen: {@code Double} en el dominio, {@code NUMERIC(12,3)} en la base (D-7). */
final class MedidasPersistencia {

  static final int DECIMALES = 3;

  private MedidasPersistencia() {}

  static BigDecimal aColumna(Double valor) {
    return valor != null
        ? BigDecimal.valueOf(valor).setScale(DECIMALES, RoundingMode.HALF_UP)
        : null;
  }

  static Double aDominio(BigDecimal valor) {
    return valor != null ? valor.doubleValue() : null;
  }
}
