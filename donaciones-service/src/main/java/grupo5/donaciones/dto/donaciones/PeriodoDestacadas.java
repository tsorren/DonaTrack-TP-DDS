package grupo5.donaciones.dto.donaciones;

/** Ventana de las destacadas (HB-01): el boceto ofrece la última semana, el último mes o el año. */
public enum PeriodoDestacadas {
  SEMANA(7),
  MES(30),
  ANIO(365);

  private final int dias;

  PeriodoDestacadas(int dias) {
    this.dias = dias;
  }

  public int dias() {
    return dias;
  }
}
