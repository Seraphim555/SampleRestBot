package com.SampleRestBot.SampleRestBot.service;

import com.SampleRestBot.SampleRestBot.config.BotConfig;
import com.SampleRestBot.SampleRestBot.model.User;
import com.SampleRestBot.SampleRestBot.model.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private UserRepository userRepository;

    final BotConfig config;

    public TelegramBot(BotConfig config) {
        this.config = config;
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();

            long chatId = update.getMessage().getChatId();

            switch (messageText){

                case "/start":

                    registerUser(update.getMessage());
                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
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
                } catch (Exception e) {
                    log.error("Произошла ошибка при добавлении нового пользователя: {}", e.getMessage());
                }
            }
            else {
                log.warn("Чат пустой: {}", msg.getChatId());
            }
        }
    }

    private void startCommandReceived(long chatId, String name){

        String answer = "Здорова, " + name + ", не суетись!";

        log.info("Ответил пользователю {}", name);
        //log.info("Replied to user {}", name);

        sendMessage(chatId, answer);

    }

    private void sendMessage(long chatId, String textToSend){

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        try{
            execute(message);
        }
        catch (TelegramApiException e){
            log.error("Произошла ошибка: {}", e.getMessage());
            //log.error("Error occurred: {}", e.getMessage());

        }
    }
}
