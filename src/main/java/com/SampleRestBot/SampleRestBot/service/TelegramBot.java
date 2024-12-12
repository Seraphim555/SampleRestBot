package com.SampleRestBot.SampleRestBot.service;

import com.SampleRestBot.SampleRestBot.config.BotConfig;
import com.SampleRestBot.SampleRestBot.model.User;
import com.SampleRestBot.SampleRestBot.model.UserRepository;
import com.SampleRestBot.SampleRestBot.mySource.GetNearThreeDays;
import com.SampleRestBot.SampleRestBot.mySource.StageOfChat;
import com.SampleRestBot.SampleRestBot.mySource.generalConstants.GeneralConstants;
import com.SampleRestBot.SampleRestBot.mySource.savesUsers.SavesUsersInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private UserRepository userRepository;

    final BotConfig config;

    public TelegramBot(BotConfig config) {
        this.config = config;
        List<BotCommand> listofCommands = new ArrayList<>();
        listofCommands.add(new BotCommand("/start", "Перезапуск"));

        try {
            this.execute(new SetMyCommands(listofCommands, new BotCommandScopeDefault(), null));
        }
        catch (TelegramApiException e) {
            log.error("Ошибка передачи боту списка команд: {}", e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    StageOfChat stageOfChat = StageOfChat.START;

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();

            long chatId = update.getMessage().getChatId();

            if (messageText.equals("/start")) {
                registerUser(update.getMessage());
                stageOfChat = StageOfChat.START;
                startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
            }
            else {

                switch (stageOfChat) { // Прописать все текстовые ответы в виде отдельных констант

                    case START -> {
                        switch (messageText) {

                            case "Info":
                                stageOfChat = StageOfChat.INFO;
                                sendMessage(chatId, "Какой вопрос вас интересует?");
                                break;

                            case "Вызов официанта":
                                try {
                                    if (SavesUsersInterface.hasUser(update.getMessage().getChat().getUserName())) {
                                        sendMessage(chatId, "Григорий сейчас подойдет к вам)");
                                    }
                                    else {
                                        stageOfChat = StageOfChat.USER_REGISTRATION;
                                        sendMessage(chatId, "Чтобы воспользоваться этой функцией, сначала нужно зарегистрироваться  :)");
                                    }
                                } catch (IOException e) { throw new RuntimeException(e); } // Здесь прописать нормальные логи
                                break;

                            case "Забронировать столик":
                                try {
                                    if (SavesUsersInterface.hasUser(update.getMessage().getChat().getUserName())) {
                                        stageOfChat = StageOfChat.RESERVE_OF_TABLE;
                                        sendMessage(chatId, "На какое число вы бы хотели назначить бронь?");
                                    }
                                    else {
                                        stageOfChat = StageOfChat.USER_REGISTRATION;
                                        sendMessage(chatId, "Чтобы воспользоваться этой функцией, сначала нужно зарегистрироваться  :)");
                                    }
                                } catch (IOException e) { throw new RuntimeException(e); }
                                break;

                            default:
                                sendMessage(chatId, "Неверный формат ввода.");

                        }
                    }

                    // Какой вопрос вас интересует?
                    case INFO -> {
                        switch (messageText) {

                            case "Меню":
                                sendMessage(chatId, "Пока умеем только варить пельмени");
                                break;

                            case "Локация":
                                sendMessage(chatId, "Наш филиал располагается по адресу: \nг. Екатеринбург, ул. Тургенева, д. 4");
                                break;

                            case "Новинки":
                                sendMessage(chatId, "Пока что ничего нового(");
                                break;

                            case "Назад":
                                stageOfChat = StageOfChat.START;
                                sendMessage(chatId, "Чем могу вам помочь?");
                                break;

                            default:
                                sendMessage(chatId, "Неверный формат ввода.");

                        }
                    }

                    // На какое число вы бы хотели назначить бронь?
                    case RESERVE_OF_TABLE -> {
                        if (messageText.equals(GetNearThreeDays.getToday()) || messageText.equals(GetNearThreeDays.getTomorrow()) || messageText.equals(GetNearThreeDays.getNextTomorrow())) {
                            stageOfChat = StageOfChat.RESERVE_OF_TABLE_PERSON;
                            sendMessage(chatId, "На какое количество человек нужен столик? (до 6 человек)");
                        } else if (messageText.equals("Назад")) {
                            stageOfChat = StageOfChat.START;
                            sendMessage(chatId, "Чем могу вам помочь?");
                        } else sendMessage(chatId, "Неверный формат ввода.");

                    }

                    // На какое количество человек нужен столик? (до 6 человек)
                    case RESERVE_OF_TABLE_PERSON -> {
                        try {

                            int count = Integer.parseInt(messageText);

                            if (1 <= count && count <= GeneralConstants.getMaxTableCapacity()) {
                                stageOfChat = StageOfChat.RESERVE_OF_TABLE_TIME;
                                if (count == 1)
                                    sendMessage(chatId, "Для вас есть персональное место!\nВыберите удобное время:");
                                else
                                    sendMessage(chatId, "Есть свободные места для " + messageText + "-x человек." + "\nВыберите удобное время:");
                            } else {
                                sendMessage(chatId, "Введите число от 1 до 6");
                            }
                        }
                        catch (NumberFormatException e) {

                            if (messageText.equals("Назад")) {
                                stageOfChat = StageOfChat.RESERVE_OF_TABLE;
                                sendMessage(chatId, "На какое число вы бы хотели назначить бронь?");
                            } else sendMessage(chatId, "Неверный формат ввода.");

                        }
                    }

                    // Выберите удобное время
                    case RESERVE_OF_TABLE_TIME -> {
                        stageOfChat = StageOfChat.START;
                        sendMessage(chatId, "Запись подтверждена, ждем вас с нетерпением!");
                    }

                    // Чтобы воспользоваться этой функцией, сначала нужно зарегистрироваться  :)
                    case USER_REGISTRATION -> {
                        if (messageText.equals("Назад")) {
                            stageOfChat = StageOfChat.START;
                            sendMessage(chatId, "Чем могу вам помочь?");
                        } else sendMessage(chatId, "Неверный формат ввода.");
                    }

                }
            }
        }

        else if (update.hasMessage() && update.getMessage().hasContact()) {

            String userName = update.getMessage().getChat().getUserName();
            String phoneNumber = update.getMessage().getContact().getPhoneNumber();

            try {
                SavesUsersInterface.saveUser(userName, phoneNumber);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }

            stageOfChat = StageOfChat.START;
            sendMessage(update.getMessage().getChatId(), "Теперь мы с вами знакомы, спасибо за доверие!");
        }

        else if (update.hasMessage()) {
            sendMessage(update.getMessage().getChatId(), "Неверный формат ввода.");
        }

    }

    private void registerUser(Message msg){

        if(userRepository.findById(msg.getChatId()).isEmpty()){

            Long chatId = msg.getChatId();
            Chat chat = msg.getChat();

            if (chat != null) {
                User user = new User();
                user.setChatId(chatId);
                user.setFirstName(chat.getFirstName());
                user.setLastName(chat.getLastName());
                user.setUserName(chat.getUserName());
                user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

                try {
                    userRepository.save(user);
                    log.info("Добавлен новый пользователь: {}", user.getUserName());
                }
                catch (Exception e) {
                    log.error("Произошла ошибка при добавлении нового пользователя: {}", e.getMessage());
                }
            }
            else {
                log.warn("Чат пустой: {}", msg.getChatId());
            }
        }
    }

    private void startCommandReceived(long chatId, String name){

        String answer = "Доброго времени суток, " + name + ", чем могу вам помочь?";

        log.info("Ответил пользователю {}", name);

        sendMessage(chatId, answer);

    }

    private void sendMessage(long chatId, String textToSend){

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        switch (stageOfChat){

            case START -> {
                ReplyKeyboardMarkup keyboardMarkup = startReplyKeyboardMarkup();
                message.setReplyMarkup(keyboardMarkup);
            }

            case INFO -> {
                ReplyKeyboardMarkup keyboardMarkup = infoReplyKeyboardMarkup();
                message.setReplyMarkup(keyboardMarkup);
            }

            case RESERVE_OF_TABLE -> {
                ReplyKeyboardMarkup keyboardMarkup = reserveReplyKeyboardMarkup();
                message.setReplyMarkup(keyboardMarkup);
            }

            case CALLING_THE_WAITER -> {
                ReplyKeyboardMarkup keyboardMarkup = callingReplyKeyboardMarkup();
                message.setReplyMarkup(keyboardMarkup);
            }

            case RESERVE_OF_TABLE_PERSON -> {
                ReplyKeyboardMarkup keyboardMarkup = reservePersonReplyKeyboardMarkup();
                message.setReplyMarkup(keyboardMarkup);
            }

            case RESERVE_OF_TABLE_TIME -> {
                ReplyKeyboardMarkup keyboardMarkup = reserveTimeReplyKeyboardMarkup();
                message.setReplyMarkup(keyboardMarkup);
            }

            case USER_REGISTRATION -> {
                ReplyKeyboardMarkup keyboardMarkup = registrationReplyKeyboardMarkup();
                message.setReplyMarkup(keyboardMarkup);
            }

        }

        try {
            execute(message);
        }
        catch (TelegramApiException e){
            log.error("Произошла ошибка: {}", e.getMessage());
        }
    }

    private static ReplyKeyboardMarkup startReplyKeyboardMarkup() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        //делает кнопки меньше

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add("Info");
        keyboardRows.add(row);

        row = new KeyboardRow();
        row.add("Вызов официанта");
        keyboardRows.add(row);

        row = new KeyboardRow();
        row.add("Забронировать столик");
        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }

    private static ReplyKeyboardMarkup infoReplyKeyboardMarkup() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add("Меню");
        keyboardRows.add(row);

        row = new KeyboardRow();
        row.add("Локация");
        keyboardRows.add(row);

        row = new KeyboardRow();
        row.add("Новинки");
        keyboardRows.add(row);

        row = new KeyboardRow();
        row.add("Назад");
        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }

    private static ReplyKeyboardMarkup reserveReplyKeyboardMarkup() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        String today = GetNearThreeDays.getToday();
        String tomorrow = GetNearThreeDays.getTomorrow();
        String nextTomorrow = GetNearThreeDays.getNextTomorrow();

        KeyboardRow row = new KeyboardRow();
        row.add(today);
        keyboardRows.add(row);

        row = new KeyboardRow();
        row.add(tomorrow);
        keyboardRows.add(row);

        row = new KeyboardRow();
        row.add(nextTomorrow);
        keyboardRows.add(row);

        row = new KeyboardRow();
        row.add("Назад");
        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }

    private static ReplyKeyboardMarkup callingReplyKeyboardMarkup() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add("Назад");
        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }

    private static ReplyKeyboardMarkup reservePersonReplyKeyboardMarkup() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add("Назад");
        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }

    private static ReplyKeyboardMarkup reserveTimeReplyKeyboardMarkup() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add("11:00");
        keyboardRows.add(row);

        row = new KeyboardRow();
        row.add("15:00");
        keyboardRows.add(row);

        row = new KeyboardRow();
        row.add("19:00");
        keyboardRows.add(row);

        row = new KeyboardRow();
        row.add("Назад");
        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }

    private static ReplyKeyboardMarkup registrationReplyKeyboardMarkup() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        // keyboardMarkup.setOneTimeKeyboard(true); // Убираем клавиатуру после отправки контакта

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardButton contactButton = new KeyboardButton();
        contactButton.setText("Отправить контакт");
        contactButton.setRequestContact(true); // Включаем запрос контакта

        KeyboardRow row = new KeyboardRow();
        row.add(contactButton);
        keyboardRows.add(row);

        row = new KeyboardRow();
        row.add("Назад");
        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }

}
