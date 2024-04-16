package io.project.RegistrationForWorkshops.sheets;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Getter
@Configuration
@PropertySource("application.properties")
public class SheetsConfig {
    @Value("${sheet}")
    private String spreadsheet;
    @Value("${key.path}")
    private String jsonKeyFilePath;
}
