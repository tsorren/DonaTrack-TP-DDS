package grupo5.common;

import grupo5.common.handlers.GlobalExceptionHandler;
import grupo5.common.logging.LoggingAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@AutoConfiguration
@Import({GlobalExceptionHandler.class, LoggingAutoConfiguration.class})
@PropertySource("classpath:common-routes.properties")
public class CommonLibAutoConfiguration {}
