package io.project.RegistrationForWorkshops.model;

import io.project.RegistrationForWorkshops.sheets.SheetsApi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SlotContainer {
    private static final Map<Integer, SlotDAO> slots = new HashMap<>();

    private SlotContainer() {}

    public static void updateSlots(SheetsApi sheetApi) throws IOException {
        slots.clear();
        for (SlotDAO lesson : sheetApi.readAllSheets()) {
            slots.put(lesson.getId(), lesson);
        }
    }

    public static SlotDAO find(int id) {
        return slots.get(id);
    }

    public static List<SlotDAO> getSlots() {
        return new ArrayList<>(slots.values());
    }
}
