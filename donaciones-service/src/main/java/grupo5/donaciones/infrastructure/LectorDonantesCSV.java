package grupo5.donaciones.infrastructure;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.InfrastructureException;
import grupo5.donaciones.models.entities.donantes.DonanteParser;
import grupo5.donaciones.models.ports.CargadorDonantes;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LectorDonantesCSV implements CargadorDonantes {

  private static final Logger log = LoggerFactory.getLogger(LectorDonantesCSV.class);
  public static final String BOM =
      "﻿"; // Carácter BOM invisible al inicio de algunos archivos UTF-8

  private final DonanteParser donanteParser;

  public LectorDonantesCSV() {
    this.donanteParser = new DonanteParser();
  }

  @Override
  public int contarFilas(InputStream contenido) {
    // Cerrar el reader cierra también el stream; quien lo abrió puede volver a cerrarlo sin efecto.
    try (BufferedReader reader = abrir(contenido)) {
      if (reader.readLine() == null) {
        return 0; // Archivo vacío
      }
      int filas = 0;
      String linea;
      while ((linea = reader.readLine()) != null) {
        if (!linea.isBlank()) {
          filas++;
        }
      }
      return filas;
    } catch (IOException e) {
      throw new InfrastructureException(ErrorCatalog.CSV_READ_ERROR, e);
    }
  }

  @Override
  public void leerPorTandas(
      InputStream contenido, int tamanioTanda, Consumer<List<FilaPadron>> alLeerTanda) {
    if (tamanioTanda < 1) {
      throw new IllegalArgumentException("El tamaño de la tanda tiene que ser al menos 1");
    }
    try (BufferedReader reader = abrir(contenido)) {
      String cabeceraStr = reader.readLine();
      if (cabeceraStr == null) {
        return; // Archivo vacío
      }
      if (cabeceraStr.startsWith(BOM)) {
        cabeceraStr = cabeceraStr.substring(1);
      }
      final String[] cabecera =
          Arrays.stream(cabeceraStr.split(",")).map(String::trim).toArray(String[]::new);

      List<FilaPadron> tanda = new ArrayList<>(tamanioTanda);
      String linea;
      int numeroDeLinea = 1;
      while ((linea = reader.readLine()) != null) {
        numeroDeLinea++;
        if (!linea.isBlank()) {
          tanda.add(mapear(linea, cabecera, numeroDeLinea));
        }
        if (tanda.size() == tamanioTanda) {
          alLeerTanda.accept(List.copyOf(tanda));
          tanda.clear();
        }
      }
      if (!tanda.isEmpty()) {
        alLeerTanda.accept(List.copyOf(tanda));
      }
    } catch (IOException e) {
      throw new InfrastructureException(ErrorCatalog.CSV_READ_ERROR, e);
    }
  }

  private static BufferedReader abrir(InputStream contenido) {
    return new BufferedReader(new InputStreamReader(contenido, StandardCharsets.UTF_8));
  }

  private FilaPadron mapear(String linea, String[] cabecera, int numeroDeLinea) {
    try {
      return new FilaPadron(numeroDeLinea, donanteParser.procesarLinea(linea, cabecera));
    } catch (IllegalArgumentException e) {
      // Sin el contenido de la línea: trae datos personales (nombre, documento, email)
      log.warn("Línea {} con error de formato: {}", numeroDeLinea, e.getMessage());
      return new FilaPadron(numeroDeLinea, null);
    }
  }
}
