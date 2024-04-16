package io.project.RegistrationForWorkshops.model;

import lombok.Data;

@Data
public class SlotLocation {
    private int startIndex = -1;
    private int endIndex = -1;
    private int columnIndex = -1;
}
