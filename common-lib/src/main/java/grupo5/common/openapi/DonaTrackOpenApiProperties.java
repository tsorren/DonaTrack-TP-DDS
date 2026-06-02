package grupo5.common.openapi;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "donatrack.openapi")
public class DonaTrackOpenApiProperties {
  private String title;
  private String version = "v1";
  private String description = "";
  private String serverUrl;
  private String serverDescription = "Entorno local";

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getServerUrl() {
    return serverUrl;
  }

  public void setServerUrl(String serverUrl) {
    this.serverUrl = serverUrl;
  }

  public String getServerDescription() {
    return serverDescription;
  }

  public void setServerDescription(String serverDescription) {
    this.serverDescription = serverDescription;
  }
}