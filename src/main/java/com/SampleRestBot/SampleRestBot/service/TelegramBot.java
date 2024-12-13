package com.SampleRestBot.SampleRestBot.service;

import com.SampleRestBot.SampleRestBot.config.BotConfig;
import com.SampleRestBot.SampleRestBot.model.Reservation;
import com.SampleRestBot.SampleRestBot.model.ReservationRepository;
import com.SampleRestBot.SampleRestBot.model.User;
import com.SampleRestBot.SampleRestBot.model.UserRepository;
import com.SampleRestBot.SampleRestBot.mySource.GetNearThreeDays;
import com.SampleRestBot.SampleRestBot.mySource.StageOfChat;
import com.SampleRestBot.SampleRestBot.mySource.generalConstants.GeneralConstants;
import jakarta.transaction.Transactional;
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
import jakarta.annotation.PostConstruct;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

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

    @Autowired
    private ReservationRepository reservationRepository;

    @PostConstruct
    public void initializeBot() {
        try {
            List<BotCommand> listOfCommands = List.of(
                    new BotCommand("/start", "Перезапуск")
            );
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
            initializeReservations();
        } catch (TelegramApiException e) {
            log.error("Ошибка инициализации команд: {}", e.getMessage());
        }
    }


    //можно будет потом удалить
    @Autowired
    public void setReservationRepository(ReservationRepository repository) {
        log.info("ReservationRepository успешно внедрен: {}", repository != null);
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

                switch (stageOfChat) {

                    case START -> {
                        switch (messageText) {

                            case "Вызов официанта":
                                //stageOfChat = StageOfChat.CALLING_THE_WAITER;
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
                            }
                            else if (messageText.equals("Назад")) {
                                stageOfChat = StageOfChat.RESERVE_OF_TABLE;
                                sendMessage(chatId, "На какое число вы бы хотели назначить бронь?");
                            } else sendMessage(chatId, "Введите число от 1 до 6");

                        } catch (NumberFormatException e) {

                            if (messageText.equals("Назад")) {
                                stageOfChat = StageOfChat.RESERVE_OF_TABLE;
                                sendMessage(chatId, "На какое число вы бы хотели назначить бронь?");
                            } else sendMessage(chatId, "Неверный формат ввода.");

                        }
                    }

                    case RESERVE_OF_TABLE_TIME -> {
                        String selectedTime = messageText;

                        if (selectedTime.equals("11:00") || selectedTime.equals("15:00") || selectedTime.equals("19:00")) {
                            List<Reservation> availableTables = reservationRepository.findByDateAndTime(GetNearThreeDays.getToday(), selectedTime);

                            if (!availableTables.isEmpty()) {
                                Reservation table = availableTables.stream().filter(r -> !r.isReserved()).findFirst().orElse(null);

                                if (table != null) {
                                    table.setReserved(true);
                                    reservationRepository.save(table);
                                    sendMessage(chatId, "Столик успешно забронирован на " + selectedTime);
                                    stageOfChat = StageOfChat.START;
                                } else {
                                    sendMessage(chatId, "На выбранное время все столики заняты.");
                                }
                            } else {
                                sendMessage(chatId, "На указанное время нет свободных столиков.");
                            }
                        } else if (messageText.equals("Назад")) {
                            stageOfChat = StageOfChat.RESERVE_OF_TABLE;
                            sendMessage(chatId, "На какое число вы бы хотели назначить бронь?");
                        } else {
                            sendMessage(chatId, "Пожалуйста, выберите время из предложенных вариантов.");
                        }
                    }

                }
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

    @Transactional
    private void initializeReservations() {
        if (reservationRepository == null) {
            log.error("ReservationRepository is null!");
            return;
        }

        try {
            log.info("Очистка старых данных из таблицы бронирований...");
            reservationRepository.deleteAll();
            reservationRepository.flush();

            Random random = new Random();
            LocalTime startTime = LocalTime.of(12, 0); // Время начала (12:00)
            LocalTime endTime = LocalTime.of(22, 0);  // Время окончания (22:00)

            LocalDate today = LocalDate.now(); // Текущая дата
            LocalTime currentTime = LocalTime.now(); // Текущее время
            List<String> waiterNames = List.of("Анна", "Иван", "Мария", "Петр");

            int tableNumberCounter = 1; // Счетчик номеров столиков
            int waiterIndex = 0;        // Индекс официанта

            for (String dateString : List.of(
                    GetNearThreeDays.getToday(),
                    GetNearThreeDays.getTomorrow(),
                    GetNearThreeDays.getNextTomorrow()
            )) {
                LocalDate date = LocalDate.parse(dateString, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                LocalTime timeIterator = startTime;

                while (!timeIterator.isAfter(endTime)) {
                    if (date.isEqual(today) && timeIterator.isBefore(currentTime)) {
                        timeIterator = timeIterator.plusHours(1);
                        continue;
                    }

                    String time = timeIterator.toString();
                    for (int capacity : new int[]{2, 4, 6}) {
                        Reservation newReservation = new Reservation();
                        newReservation.setDate(dateString);
                        newReservation.setTime(time);
                        newReservation.setTableNumber(tableNumberCounter++);
                        newReservation.setReserved(random.nextBoolean());
                        newReservation.setCapacity(capacity);
                        newReservation.setWaiterName(waiterNames.get(waiterIndex));

                        if (tableNumberCounter % 4 == 1) { // Меняем официанта каждые 4 стола
                            waiterIndex = (waiterIndex + 1) % waiterNames.size();
                        }

                        if (!reservationRepository.existsByDateAndTimeAndTableNumber(dateString, time, tableNumberCounter)) {
                            reservationRepository.save(newReservation);
                        } else {
                            log.warn("Запись уже существует: date={}, time={}, tableNumber={}", dateString, time, tableNumberCounter);
                        }
                        // Добавляем паузу после сохранения каждого объекта
                        try {
                            Thread.sleep(50); // Пауза 50 мс
                        } catch (InterruptedException e) {
                            log.error("Ошибка в потоке при ожидании: {}", e.getMessage());
                            Thread.currentThread().interrupt();
                        }
                    }

                    timeIterator = timeIterator.plusHours(1);
                    if (tableNumberCounter % 10 == 0) {
                        reservationRepository.flush();
                    }
                }
            }

            reservationRepository.flush();
            log.info("Бронирования инициализированы.");
        } catch (Exception e) {
            log.error("Ошибка при инициализации бронирований: {}", e.getMessage());
        }
    }

    //Генерация случайной вместимости столика (2, 4 или 6).
    /*private int randomCapacity(Random random) {
        int[] capacities = {2, 4, 6};
        return capacities[random.nextInt(capacities.length)];
    }*/

    //для того, чтобы выводился точный формат даты.
    public class GetNearThreeDays {

        private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        public static String getToday() {
            return LocalDate.now().format(formatter);
        }

        public static String getTomorrow() {
            return LocalDate.now().plusDays(1).format(formatter);
        }

        public static String getNextTomorrow() {
            return LocalDate.now().plusDays(2).format(formatter);
        }
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
        row.add("Выбрать другое удобное мне время");
        keyboardRows.add(row);

        row = new KeyboardRow();
        row.add("Назад");
        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }

}
