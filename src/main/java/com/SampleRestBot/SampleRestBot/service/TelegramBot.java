package com.SampleRestBot.SampleRestBot.service;

import com.SampleRestBot.SampleRestBot.config.BotConfig;
import com.SampleRestBot.SampleRestBot.model.User;
import com.SampleRestBot.SampleRestBot.model.UserRepository;
import com.SampleRestBot.SampleRestBot.mySource.StageOfChat;
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
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

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

            switch (messageText){

                case "/start":
                    stageOfChat = StageOfChat.START;
                    registerUser(update.getMessage());
                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                    break;

                case "Вызов официанта":
                    //stageOfChat = StageOfChat.CALLING_THE_WAITER;
                    stageOfChat = StageOfChat.START;
                    sendMessage(chatId, "Григорий сейчас подойдет к вам)");
                    break;

                case "Info": // в кейс инфо можно попасть находясь на любом стейдже диалога
                    stageOfChat = StageOfChat.INFO;
                    sendMessage(chatId, "Какой вопрос вас интересует?");
                    break;

                case "Забронировать столик":
                    stageOfChat = StageOfChat.RESERVE_OF_TABLE;
                    sendMessage(chatId, "На какое число вы бы хотели назначить бронь?");
                    break;

                case "Назад":
                    switch (stageOfChat){
                        case INFO, CALLING_THE_WAITER, RESERVE_OF_TABLE -> {
                            stageOfChat = StageOfChat.START;
                            sendMessage(chatId, "Чем могу вам помочь?");
                        }
                    }
                    break;

                default: sendMessage(chatId, "Сорян, пока не знаю такой команды...");

            }
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
        //log.info("Replied to user {}", name);

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

        }

        try {
            execute(message);
        }
        catch (TelegramApiException e){
            log.error("Произошла ошибка: {}", e.getMessage());
            //log.error("Error occurred: {}", e.getMessage());

        }
    }

    private static ReplyKeyboardMarkup startReplyKeyboardMarkup() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        //keyboardMarkup.setResizeKeyboard(true);
        //делает кнопки меньше

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();

        row.add("Info");
        row.add("Вызов официанта");
        row.add("Забронировать столик");

        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }

    private static ReplyKeyboardMarkup infoReplyKeyboardMarkup() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        //keyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();

        row.add("Меню");
        row.add("Локация");
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
        //keyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();

        row.add("03.11");
        row.add("04.11");
        row.add("05.11");

        keyboardRows.add(row);

        row = new KeyboardRow();

        row.add("Назад");

        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }

    private static ReplyKeyboardMarkup callingReplyKeyboardMarkup() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        //keyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();

        row.add("000");
        row.add("0");
        row.add("00000");

        keyboardRows.add(row);

        row = new KeyboardRow();

        row.add("Назад");

        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }

}
