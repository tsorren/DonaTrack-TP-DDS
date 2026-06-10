package grupo5.common.responses;

public record ApiResponse<Contenido>(boolean success, String message, Contenido data) {
  public static <Contenido> ApiResponse<Contenido> success(String message, Contenido data) {
    return new ApiResponse<>(true, message, data);
  }

  public static ApiResponse<Void> success(String message) {
    return new ApiResponse<>(true, message, null);
  }
}
