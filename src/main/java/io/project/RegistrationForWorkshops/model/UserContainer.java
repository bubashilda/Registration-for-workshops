package io.project.RegistrationForWorkshops.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class UserContainer {
    private static final List<Long> usersRegistered = new ArrayList<>();
    private static final Map<Long, State> usersState = new HashMap<>();
    private static final Map<Long, UserDAO> userQueue = new HashMap<>();

    private UserContainer(){}

    public static boolean checkRegistered(Long id) {
        return usersRegistered.contains(id);
    }

    public static boolean containsUser(Long id) {
        return usersState.containsKey(id);
    }

    public static void addUser(Long id) {
        usersState.put(id, State.INITIAL);
    }

    public static void markRegistered(Long id) {
        if (!usersRegistered.contains(id)) {
            usersRegistered.add(id);
        }
    }

    public static void addToQueue(Long id, UserDAO user) {
        userQueue.put(id, user);
    }

    public static UserDAO getUserDAO(Long id) {
        return userQueue.get(id);
    }

    public static void setState(Long id, State state) {
        usersState.put(id, state);
    }

    public static State getState(Long id) {
        return usersState.get(id);
    }
}
