package grupo5.common.responses;

public record ApiResponse<Recurso>(boolean success, String message, Recurso data) {
  public static <Recurso> ApiResponse<Recurso> success(String message, Recurso data) {
    return new ApiResponse<>(true, message, data);
  }

  public static ApiResponse<Void> success(String message) {
    return new ApiResponse<>(true, message, null);
  }
}