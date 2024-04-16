package io.project.RegistrationForWorkshops.sheets;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;
import io.project.RegistrationForWorkshops.model.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@Component
@PropertySource("application.properties")
public class SheetsApi {
    private final Sheets SheetsService;
    private final SheetsConfig config;

    public SheetsApi(SheetsConfig config) throws IOException {
        this.config = config;
        GoogleCredential credential = GoogleCredential.fromStream(new FileInputStream(config.getJsonKeyFilePath()))
                .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));

        SheetsService = new Sheets.Builder(
                credential.getTransport(),
                credential.getJsonFactory(),
                credential)
                .setApplicationName("MY APP")
                .build();
    }

    private List<String> getListNames() throws IOException {
        List<String> sheetTitles = new ArrayList<>();
        Spreadsheet res2 = SheetsService.spreadsheets().get(config.getSpreadsheet()).execute();
        for(Sheet sheet: res2.getSheets()){
            sheetTitles.add(sheet.getProperties().getTitle());
        }
        return sheetTitles;
    }

    private int getSheetSize(String tableName) throws IOException {
        return SheetsService.spreadsheets().values().get(config.getSpreadsheet(),
                tableName+"!A:BN").execute().getValues().get(2).size();
    }

    private ValueRange getTable(String tableName) throws IOException {
        return SheetsService.spreadsheets().values().get(config.getSpreadsheet(),
                tableName+"!A:BN").execute();
    }

    public List<SlotDAO> readAllSheets() throws IOException {
        List<String> sheetTitles = getListNames();
        List<SlotDAO> slots = new ArrayList<>();
        for (String sheetTitle: sheetTitles) {
            slots.addAll(readData(getTable(sheetTitle), getSheetSize(sheetTitle), sheetTitle));
        }
        return slots;
    }

    LocalDate parseDate(String representation) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm dd.MM.yyyy");
        LocalDateTime adaptedDate;
        try {
            adaptedDate = LocalDateTime.parse(representation.split(", ")[1].split("-")[0]
                    + " " + representation.split(", ")[0] + ".2024", formatter);
        } catch (IndexOutOfBoundsException e) {
            adaptedDate = LocalDateTime.now();
        }

        return adaptedDate.toLocalDate();
    }

    List<Subject> parseSubjectList(String representation) {
        return Arrays.stream(Subject.values())
                .filter(elem -> representation.contains(elem.toString()))
                .collect(Collectors.toList());
    }

    List<SlotDAO> getSlotDescription(List<List<Object>> rows, int width, String sheetTitle) {
        List<SlotDAO> slots = new ArrayList<>();
        NextItem nextItem = NextItem.NAME;
        for (int col = 0; col < width; col += 11) {
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                List<Object> row = rows.get(rowIndex);
                if (row.isEmpty() || rowIndex < 4) continue;

                if (rowIndex == 4 && !row.get(col).equals("")){
                    slots.add(new SlotDAO(SlotIdCounter.generateID()));
                    slots.get(slots.size() - 1).setSheet(config.getSpreadsheet());
                    slots.get(slots.size() - 1).setRange(sheetTitle);
                    slots.get(slots.size() - 1).getLocation().setColumnIndex(col);
                    slots.get(slots.size() - 1).setProfessor(row.get(col).toString());
                    slots.get(slots.size() - 1).getLocation().setStartIndex(rowIndex);
                    nextItem = NextItem.SUBJECT;
                    continue;
                }

                if (nextItem == NextItem.NAME && row.size() >= col + 1 && !row.get(col).equals("")) {
                    slots.add(new SlotDAO(SlotIdCounter.generateID()));
                    slots.get(slots.size() - 1).setSheet(config.getSpreadsheet());
                    slots.get(slots.size() - 1).setRange(sheetTitle);
                    slots.get(slots.size() - 1).getLocation().setColumnIndex(col);
                    slots.get(slots.size() - 1).getLocation().setStartIndex(rowIndex);
                    slots.get(slots.size() - 1).setProfessor(row.get(col).toString());
                    slots.get(slots.size() - 2).getLocation().setEndIndex(rowIndex);
                    nextItem = NextItem.SUBJECT;
                    continue;
                }

                if (nextItem == NextItem.SUBJECT) {
                    slots.get(slots.size() - 1).setSubjectsList(parseSubjectList(row.get(col).toString()));
                    nextItem = NextItem.TIME;
                    continue;
                }

                if (nextItem == NextItem.TIME) {
                    slots.get(slots.size() - 1).setTime(parseDate(row.get(col).toString()));
                    nextItem = NextItem.NAME;
                }
            }
        }
        return slots;
    }

    List<SlotDAO> updateSlotCapacity(List<SlotDAO> slots, List<List<Object>> rows) {
        for (SlotDAO slot: slots) {
            int slotColumn = slot.getLocation().getColumnIndex();

            if (slot.getLocation().getEndIndex() < 0) {
                for (int rowSlot = slot.getLocation().getStartIndex(); rowSlot < rows.size() ; rowSlot++) {
                    List<Object> row = rows.get(rowSlot);
                    if (row.isEmpty() || rowSlot < 3) continue;

                    if (row.size() >= slotColumn + 5 && !row.get(slotColumn + 1).equals("")) {
                        slot.setInitialRecords(slot.getInitialRecords() + 1);
                    }

                    if (row.size() >= slotColumn + 9 && !row.get(slotColumn + 5).equals("")) {
                        slot.setReserveRecords(slot.getReserveRecords() + 1);
                    }
                }
                slot.setInitialCapacity(Math.max(slot.getInitialRecords(), slot.getInitialCapacity()));
                slot.setReserveCapacity(Math.max(slot.getReserveRecords(), slot.getReserveCapacity()));
                continue;
            }
            slot.setInitialCapacity(slot.getLocation().getEndIndex() - slot.getLocation().getStartIndex());
            for (int rowSlot = slot.getLocation().getStartIndex(); rowSlot < slot.getLocation().getEndIndex(); rowSlot++) {
                List<Object> row = rows.get(rowSlot);
                if (row.isEmpty() || rowSlot < 3) continue;

                if (row.size() >= slotColumn + 5 && !row.get(slotColumn + 1).equals("")
                        || row.size() >= slotColumn + 9 && !row.get(slotColumn + 5).equals("")) {
                    slot.setInitialRecords(slot.getInitialRecords() + 1);
                }
            }
        }
        return slots;
    }

    private List<SlotDAO> readData(ValueRange table, int width, String sheetTitle) {
        List<List<Object>> rows = table.getValues();
        if (rows == null || rows.isEmpty()) {
            return new ArrayList<>();
        }

        List<SlotDAO> slots = getSlotDescription(rows, width, sheetTitle);
        slots = updateSlotCapacity(slots, rows);

        return slots;
    }

    public void updateCells(String spreadsheetId, String range, List<List<Object>> values) throws IOException {
        ValueRange body = new ValueRange().setValues(values);
        SheetsService.spreadsheets().values()
                .update(spreadsheetId, range, body)
                .setValueInputOption("RAW")
                .execute();
    }
}
