package grupo5.common.responses;

public record ApiResponse<T>(boolean success, String message, T data) {
  public static <T> ApiResponse<T> ok(String message, T data) {
    return new ApiResponse<>(true, message, data);
  }

  public static ApiResponse<Void> ok(String message) {
    return new ApiResponse<>(true, message, null);
  }
}
