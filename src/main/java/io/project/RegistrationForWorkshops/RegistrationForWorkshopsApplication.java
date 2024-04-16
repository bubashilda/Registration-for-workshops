package io.project.RegistrationForWorkshops;

import io.project.RegistrationForWorkshops.model.SlotContainer;
import io.project.RegistrationForWorkshops.sheets.SheetsApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;

@SpringBootApplication
@EnableScheduling
public class RegistrationForWorkshopsApplication {
	@Autowired
	SheetsApi sheetsApi;

	@Scheduled(fixedRate = 3_600_000)
	public void updateSlots() {
		try {
			SlotContainer.updateSlots(sheetsApi);
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}

	public static void main(String[] args) {
		SpringApplication.run(RegistrationForWorkshopsApplication.class, args);
	}
}