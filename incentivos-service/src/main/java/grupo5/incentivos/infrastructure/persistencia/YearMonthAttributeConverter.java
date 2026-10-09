package grupo5.incentivos.infrastructure.persistencia;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.YearMonth;

/** Persiste {@link YearMonth} como texto {@code yyyy-MM} (p. ej. {@code 2026-08}). */
@Converter
public class YearMonthAttributeConverter implements AttributeConverter<YearMonth, String> {

  @Override
  public String convertToDatabaseColumn(YearMonth attribute) {
    return attribute == null ? null : attribute.toString();
  }

  @Override
  public YearMonth convertToEntityAttribute(String dbData) {
    return dbData == null ? null : YearMonth.parse(dbData);
  }
}
