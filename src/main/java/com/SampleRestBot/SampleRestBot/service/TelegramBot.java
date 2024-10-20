package com.SampleRestBot.SampleRestBot.service;

import com.SampleRestBot.SampleRestBot.config.BotConfig;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class TelegramBot extends TelegramLongPollingBot {

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
                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                    break;

                case "Лиза":
                    sendMessage(chatId, "Оу, привет Лиза :*");
                    break;

                case "Ярослав":
                    sendMessage(chatId, "Ярик гей))))))");
                    break;

                default: sendMessage(chatId, "Сорян, пока не знаю такой команды...");
                //default: sendMessage(chatId, "Это не то, чего я ждал...");
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
