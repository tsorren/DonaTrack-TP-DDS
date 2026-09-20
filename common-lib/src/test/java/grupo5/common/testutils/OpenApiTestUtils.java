package grupo5.common.testutils;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import java.nio.file.Files;
import java.nio.file.Path;

public final class OpenApiTestUtils {

  private OpenApiTestUtils() {}

  public static String resolveContractPath(String yamlFilename) {
    Path pathInSubmodule = Path.of("../docs/arquitectura/contratos/", yamlFilename);
    if (Files.exists(pathInSubmodule)) {
      return pathInSubmodule.toAbsolutePath().toString();
    }
    Path pathInRoot = Path.of("docs/arquitectura/contratos/", yamlFilename);
    if (Files.exists(pathInRoot)) {
      return pathInRoot.toAbsolutePath().toString();
    }
    throw new IllegalStateException("No se encontro la especificacion OpenAPI: " + yamlFilename);
  }

  public static void assertResponseConformsToOpenApi(
      String specPath,
      String path,
      Request.Method method,
      int statusCode,
      String responseBodyJson) {
    OpenApiInteractionValidator validator = OpenApiInteractionValidator.createFor(specPath).build();
    SimpleResponse.Builder responseBuilder = SimpleResponse.Builder.status(statusCode);
    if (responseBodyJson != null && !responseBodyJson.isBlank()) {
      responseBuilder.withContentType("application/json").withBody(responseBodyJson);
    }
    ValidationReport report = validator.validateResponse(path, method, responseBuilder.build());
    if (report.hasErrors()) {
      throw new AssertionError(
          "Discrepancia detectada contra OpenAPI ("
              + specPath
              + " en "
              + method
              + " "
              + path
              + "): "
              + report.getMessages());
    }
  }
}
