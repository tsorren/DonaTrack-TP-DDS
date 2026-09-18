package grupo5.common;

import grupo5.common.handlers.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import({GlobalExceptionHandler.class})
public class CommonLibAutoConfiguration {}
