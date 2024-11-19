package com.SampleRestBot.SampleRestBot.service;

import com.SampleRestBot.SampleRestBot.config.BotConfig;
import com.SampleRestBot.SampleRestBot.model.User;
import com.SampleRestBot.SampleRestBot.model.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;

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
                    //default: sendMessage(chatId, "Это не то, чего я ждал...");
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
                user.setRegistredAt(new Timestamp(System.currentTimeMillis()));

                // Логи нужно прописать, но пока так
                try {
                    userRepository.save(user);
                    //log.info("user saved: " + user);
                } catch (Exception e) {
                    // log.error("Error saving user", e);
                }
            } else {
                // log.warn("Chat is null for message: " + msg);
            }
        }
    }

    private void startCommandReceived(long chatId, String name){

        String answer = "Здорова, " + name + ", не суетись!";
        //String answer = "Привет, " + name + ", напши свое имя  -_-";

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

        }
    }

}
