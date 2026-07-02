package grupo5.common;

import grupo5.common.handlers.GlobalExceptionHandler;
import grupo5.common.logging.LoggingAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import({GlobalExceptionHandler.class, LoggingAutoConfiguration.class})
public class CommonLibAutoConfiguration {}
