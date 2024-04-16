package io.project.RegistrationForWorkshops.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDAO extends Student {
    private final Long telegramID;

    public UserDAO(Long id) {
        this.telegramID = id;
    }
}
