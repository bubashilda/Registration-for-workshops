package io.project.RegistrationForWorkshops.service;

import io.project.RegistrationForWorkshops.sheets.SheetsApi;
import io.project.RegistrationForWorkshops.config.BotConfig;
import io.project.RegistrationForWorkshops.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class TelegramBot extends TelegramLongPollingBot {
    final BotConfig config;
    @Autowired
    private SheetsApi sheetsApi;
    private final Logger log = LoggerFactory.getLogger(TelegramBot.class);
    private final String regexpGroup = "(Б|М|А)(0|1)[0-9]-[0-9][0-9][0-9]";
    private final String regexpID = "[0-9]*";
    private final String regexpMail = "[a-zA-Z.]*@phystech.edu";

    public TelegramBot(BotConfig config) {
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "info how to use bot"));
        listOfCommands.add(new BotCommand("/register", "sign up"));
        listOfCommands.add(new BotCommand("/settings", "change log in data"));
        listOfCommands.add(new BotCommand("/slots", "get most relevant slot"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getBotToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            long userId = update.getMessage().getFrom().getId();
            long chatId = update.getMessage().getChatId();
            if (!UserContainer.containsUser(userId)) {
                UserContainer.addUser(userId);
            }
            State userState = UserContainer.getState(userId);
            String messageText = update.getMessage().getText();
            switch (messageText) {
                case "/help":
                case "/start":
                    sendMessage(userId, Messages.START_MESSAGE.toString());
                    setState(userId, State.INITIAL);
                    break;
                case "/cancel":
                    setState(userId, State.INITIAL);
                    break;
                case "/register":
                    registerUser(userId, update.getMessage().getChatId());
                    break;
                case "/slots":
                    showAvailableSlots(userId, chatId);
                    break;
                case "/settings":
                    break;
            }
            switch (userState) {
                case EXPECT_NAME:
                    setNameAndAskGroup(userId, chatId, messageText);
                    break;
                case EXPECT_GROUP:
                    setGroupAndAskEmail(userId, chatId, messageText);
                    break;
                case EXPECT_EMAIL:
                    setEmailAndFinishRegistration(userId, chatId, messageText);
                    break;
                case EXPECT_ID:
                    makeRecordInSheet(userId, chatId, messageText);
                    break;
            }
        }
        if (update.hasCallbackQuery()) {
            String callback = update.getCallbackQuery().getMessage().getText();
            String callbackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            if (callbackData.equals("Sign Up")) {
                String[] queryText = callback.split("\n");
                String text = String.join("\n", Arrays.copyOfRange(queryText, 0, queryText.length - 2))
                        + System.lineSeparator() + Messages.CHOOSE_ID;
                setState(chatId, State.EXPECT_ID);
                executeEditMessageText(text, chatId, messageId);
            } else if (Arrays.stream(Subject.values()).map(Subject::getCallback).toList().contains(callbackData)) {
                filterBySubject(callbackData, chatId, messageId);
            }
        }
    }

    public static String getColumnName(int columnNumber) {
        StringBuilder columnName = new StringBuilder();
        while (columnNumber > 0) {
            int modulo = (columnNumber - 1) % 26;
            columnName.insert(0, (char) ('A' + modulo));
            columnNumber = (columnNumber - modulo) / 26;
        }
        return columnName.toString();
    }

    private void makeRecordInSheet(long userId, long chatId, String message) {
        setState(userId, State.INITIAL);
        String[] tokens = message.split(" ");
        if (!tokens[0].matches(regexpID)) {
            return;
        }
        if (SlotContainer.find(Integer.parseInt(tokens[0])) == null) {
            sendMessage(chatId, "Неправильно указан ID, попробуй еще раз");
        }
        sendMessage(chatId, "Запись успешно произведена");
        try {
            SlotDAO slot = SlotContainer.find(Integer.parseInt(tokens[0]));
            String colStart;
            String colEnd;
            String row;
            if (slot.getInitialRecords() == slot.getReserveCapacity()) {
                colStart = getColumnName(slot.getLocation().getColumnIndex() + 7);
                colEnd = getColumnName(slot.getLocation().getColumnIndex() + 11);
                row = String.valueOf(slot.getLocation().getStartIndex() + slot.getReserveRecords() + 1);
            } else {
                colStart = getColumnName(slot.getLocation().getColumnIndex() + 2);
                colEnd = getColumnName(slot.getLocation().getColumnIndex() + 6);
                row = String.valueOf(slot.getLocation().getStartIndex() + slot.getInitialRecords() + 1);
            }
            String sheetId = slot.getSheet();
            String range = slot.getRange() + "!" + colStart + row + ":" + colEnd + row;
            UserDAO user = UserContainer.getUserDAO(userId);
            List<List<Object>> request = List.of(List.of(
                    user.getLastName() + " " + user.getFirstName() + " " + user.getPatronymic(),
                    user.getGroup(),
                    user.getEmail(),
                    tokens[1])
            );
            sheetsApi.updateCells(sheetId, range, request);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<SlotDAO> getAvailableSlots() {
        return SlotContainer.getSlots().stream()
                .filter(slot -> ChronoUnit.DAYS.between(slot.getTime(), LocalDate.now()) <= 1)
                .filter(slot -> slot.getInitialRecords() + slot.getReserveRecords()
                        < slot.getInitialCapacity() + slot.getReserveCapacity())
                .collect(Collectors.toList());
    }

    private void filterBySubject(String subject, long chatId, long messageId) {
        List<SlotDAO> reply = getAvailableSlots()
                .stream()
                .filter(slot -> slot.getSubjectsList().stream().anyMatch(elem -> elem.getCallback().equals(subject)))
                .toList();
        String answer = reply.stream()
                .map(Object::toString)
                .collect(Collectors.joining("\n"));
        executeEditMessageText(answer, chatId, messageId);
    }

    private void showAvailableSlots(long userId, long chatId) {
        List<SlotDAO> relevantSlots = getAvailableSlots();
        relevantSlots = relevantSlots.subList(0, Math.min(relevantSlots.size(), 10));
        StringBuilder ans = new StringBuilder();
        for (SlotDAO elem : relevantSlots) {
            ans.append(elem.toString()).append("\n");
        }
        ans.append(Messages.CHOOSE_SUBJECT);
        SendMessage reply = new SendMessage();
        reply.setChatId(String.valueOf(chatId));
        reply.setText(ans.toString());

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        for (Subject subject : Subject.values()) {
            var button = new InlineKeyboardButton();
            button.setText(subject.toString());
            button.setCallbackData(subject.getCallback());
            rowInLine.add(button);
        }

        rowsInLine.add(rowInLine);

        if (UserContainer.checkRegistered(userId)) {
            var button = new InlineKeyboardButton();
            button.setText("Записаться");
            button.setCallbackData("Sign Up");
            rowsInLine.add(List.of(button));
        }

        markupInLine.setKeyboard(rowsInLine);
        reply.setReplyMarkup(markupInLine);
        sendMessage(reply);
    }

    private void registerUser(long userId, long chatId) {
        if (UserContainer.checkRegistered(userId)) {
            sendMessage(chatId, Messages.ALREADY_REGISTERED.toString());
            return;
        }
        UserContainer.addToQueue(userId, new UserDAO(userId));
        setState(userId, State.EXPECT_NAME);
        sendMessage(chatId, Messages.NAME_REQUEST_MESSAGE.toString());
    }

    private void setNameAndAskGroup(long userId, long chatId, String message) {
        String[] name = message.split(" ");
        if (name.length != 3) {
            sendMessage(chatId, Messages.INCORRECT_FORMAT.toString());
            return;
        }
        UserContainer.getUserDAO(userId).setLastName(name[0]);
        UserContainer.getUserDAO(userId).setFirstName(name[1]);
        UserContainer.getUserDAO(userId).setPatronymic(name[2]);
        setState(userId, State.EXPECT_GROUP);
        sendMessage(chatId, Messages.GROUP_REQUEST_MESSAGE.toString());
    }

    private void setGroupAndAskEmail(long userId, long chatId, String message) {
        if (!message.matches(regexpGroup)) {
            sendMessage(chatId, Messages.INCORRECT_FORMAT.toString());
            return;
        }
        UserContainer.getUserDAO(userId).setGroup(message);
        setState(userId, State.EXPECT_EMAIL);
        sendMessage(chatId, Messages.EMAIL_REQUEST_MESSAGE.toString());
    }

    private void setEmailAndFinishRegistration(long userId, long chatId, String message) {
        if (!message.matches(regexpMail)) {
            sendMessage(chatId, Messages.INCORRECT_FORMAT.toString());
            return;
        }
        UserContainer.getUserDAO(userId).setEmail(message);
        setState(userId, State.INITIAL);
        UserContainer.markRegistered(userId);
        sendMessage(chatId, Messages.REGISTRATION_FINISHED.toString());
    }

    private void sendMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        sendMessage(message);
    }

    private void executeEditMessageText(String text, long chatId, long messageId) {
        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setMessageId((int) messageId);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    private void setState(long userId, State state) {
        UserContainer.setState(userId, state);
    }
}
