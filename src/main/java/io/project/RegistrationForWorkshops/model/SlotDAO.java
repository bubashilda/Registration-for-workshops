package io.project.RegistrationForWorkshops.model;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class SlotDAO {
    private final int id;
    private int initialCapacity = 5;
    private int reserveCapacity = 5;
    private int initialRecords = 0;
    private int reserveRecords = 0;
    private String professor;
    private List<Subject> subjectsList;
    private LocalDate time;
    private String sheet;
    private String range;
    private SlotLocation location;

    public SlotDAO(int id) {
        this.id = id;
        location = new SlotLocation();
    }

    @Override
    public String toString() {
        return String.format(
                """
                ID: %d
                Преподаватель: %s
                Предметы: %s
                Время: %s
                Свободных мест: %d
                Резерв: %d
                """,
                id, professor, subjectsList.toString(), time.toString(),
                initialCapacity - initialRecords, reserveCapacity - reserveRecords);

    }
}
