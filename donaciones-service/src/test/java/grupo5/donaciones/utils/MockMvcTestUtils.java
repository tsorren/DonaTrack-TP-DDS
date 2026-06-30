package grupo5.donaciones.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;

public class MockMvcTestUtils {

  public static StandaloneMockMvcBuilder standaloneSetup(Object... controllers) {
    StandaloneMockMvcBuilder builder = MockMvcBuilders.standaloneSetup(controllers);
    Properties properties = new Properties();
    try (InputStream is =
        MockMvcTestUtils.class.getClassLoader().getResourceAsStream("common-routes.properties")) {
      if (is != null) {
        properties.load(is);
        for (String name : properties.stringPropertyNames()) {
          builder.addPlaceholderValue(name, properties.getProperty(name));
        }
      }
    } catch (IOException e) {
      // Ignore or log
    }
    return builder;
  }
}
