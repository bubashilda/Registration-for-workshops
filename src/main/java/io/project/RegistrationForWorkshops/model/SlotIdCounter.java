package io.project.RegistrationForWorkshops.model;

public final class SlotIdCounter {
    private static int id= 239;
    private SlotIdCounter() {}

    public static int generateID() {
        return id++;
    }
}
