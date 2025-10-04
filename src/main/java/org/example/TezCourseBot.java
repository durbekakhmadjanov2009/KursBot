package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TezCourseBot extends TelegramLongPollingBot {
    private static final Long GROUP_CHAT_ID = -4813304380L; // Your group chat ID
    private final DatabaseHandler dbHandler;
    private Integer currentCourseId;

    public TezCourseBot() {
        this.dbHandler = new DatabaseHandler();
    }

    @Override
    public String getBotUsername() {
        return "BizningOquvMarkaz_bot";
    }

    @Override
    public String getBotToken() {
        return "8170546121:AAEej7nQaJ2LTxHHTCaDWTWRGydhIc6UIUo"; // Replace with actual Stanford School bot token
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                handleMessage(update);
            } else if (update.hasCallbackQuery()) {
                handleCallbackQuery(update);
            } else if (update.getMessage().hasContact()) {
                handleContact(update);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sendText(getChatId(update), "⚠️ <b>Xatolik yuz berdi!</b> Iltimos, qayta urinib ko'ring. 😊");
        } catch (Exception e) {
            e.printStackTrace();
            sendText(getChatId(update), "😔 <b>Kutilmagan xatolik!</b> Keyinroq qayta urinib ko'ring.");
        }
    }

    private void handleMessage(Update update) throws SQLException {
        String messageText = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();
        User user = dbHandler.getUser(chatId);

        if (user == null) {
            user = new User();
            user.setChatId(chatId);
        }

        if (messageText.equals("/start")) {
            sendMainMenu(chatId);
            user.setState("MAIN_MENU");
            dbHandler.saveUser(user);
            return;
        }

        switch (user.getState()) {
            case "WAITING_FOR_NAME":
                user.setFullName(messageText);
                user.setState("WAITING_FOR_PHONE");
                dbHandler.saveUser(user);
                askForPhone(chatId);
                break;
            case "WAITING_FOR_PHONE":
                if (messageText.matches("\\+998\\d{9}")) {
                    user.setPhone(messageText);
                    user.setState("WAITING_FOR_ADDRESS");
                    dbHandler.saveUser(user);
                    askForAddress(chatId);
                } else {
                    sendText(chatId, "❌ <b>Noto'g'ri format!</b> Telefon raqamingizni <b>+998XXXXXXXXX</b> shaklida kiriting. 😊");
                }
                break;
            case "WAITING_FOR_ADDRESS":
                user.setAddress(messageText);
                user.setState("WAITING_FOR_TIME");
                dbHandler.saveUser(user);
                askForCourseTime(chatId);
                break;
            default:
                handleMainMenuCommands(chatId, messageText);
        }
    }

    private void handleCallbackQuery(Update update) throws SQLException {
        String callbackData = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        User user = dbHandler.getUser(chatId);

        if (user == null) {
            user = new User();
            user.setChatId(chatId);
        }

        try {
            if (callbackData.startsWith("course_")) {
                String courseIdStr = callbackData.split("_")[1];
                if (courseIdStr.matches("\\d+")) {
                    int courseId = Integer.parseInt(courseIdStr);
                    Course course = dbHandler.getCourseById(courseId);
                    if (course != null) {
                        sendCourseDetails(chatId, course);
                    } else {
                        sendText(chatId, "😔 <b>Kurs topilmadi!</b> Boshqa kursni tanlang.");
                    }
                }
            } else if (callbackData.startsWith("reg")) {
                String courseIdStr = callbackData.substring(3);
                if (courseIdStr.matches("\\d+")) {
                    currentCourseId = Integer.parseInt(courseIdStr);
                    Course course = dbHandler.getCourseById(currentCourseId);
                    if (course != null) {
                        user.setCourse(course.getName());
                        user.setState("WAITING_FOR_NAME");
                        dbHandler.saveUser(user);
                        askForName(chatId);
                    } else {
                        sendText(chatId, "😔 <b>Kurs topilmadi!</b> Boshqa kursni tanlang.");
                    }
                }
            } else if (callbackData.startsWith("time_")) {
                String time = callbackData.split("_")[1];
                user.setCourseTime(time);
                user.setState("CONFIRMATION");
                dbHandler.saveUser(user);
                showConfirmation(chatId);
            } else if (callbackData.startsWith("branch_")) {
                String branchName = callbackData.substring(7);
                user.setAddress(branchName);
                user.setState("WAITING_FOR_TIME");
                dbHandler.saveUser(user);
                askForCourseTime(chatId);
            } else if (callbackData.equals("confirm_yes")) {
                dbHandler.saveRegistration(user, currentCourseId);
                completeRegistration(chatId);
                sendMainMenu(chatId);
            } else if (callbackData.equals("confirm_no") || callbackData.equals("cancel_registration")) {
                sendText(chatId, "❌ <b>Ro'yxatdan o'tish bekor qilindi.</b> Boshqa amalni tanlang! 😊");
                sendMainMenu(chatId);
                user.setState("MAIN_MENU");
                dbHandler.saveUser(user);
            } else if (callbackData.equals("back_to_courses")) {
                showCourses(chatId);
                user.setState("SELECTING_COURSE");
                dbHandler.saveUser(user);
            } else if (callbackData.equals("back_to_main")) {
                sendMainMenu(chatId);
                user.setState("MAIN_MENU");
                dbHandler.saveUser(user);
            }
        } catch (Exception e) {
            sendText(chatId, "😔 <b>Kutilmagan xatolik!</b> Keyinroq qayta urinib ko'ring.");
            e.printStackTrace();
        }
    }

    private void sendCourseDetails(Long chatId, Course course) {
        String details = String.format("""
            🌟 <b>%s</b> 🌟
            
            🕒 <b>Davomiylik:</b> %s
            💸 <b>Narxi:</b> %s
            📖 <b>Tavsif:</b> %s
            🔗 <b>Batafsil:</b> %s
            
            🚀 Bizning oquv markaz bilan bilim oling! Ro'yxatdan o'tish uchun pastdagi tugmani bosing!
            """,
                course.getName(),
                course.getDuration(),
                course.getPrice(),
                course.getDescription(),
                course.getDetailsUrl());

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> registerRow = new ArrayList<>();
        InlineKeyboardButton registerBtn = InlineKeyboardButton.builder()
                .text("🚀 Ro'yxatdan o'tish")
                .callbackData("reg" + course.getId())
                .build();
        registerRow.add(registerBtn);
        rows.add(registerRow);

        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backBtn = InlineKeyboardButton.builder()
                .text("⬅️ Boshqa kurslar")
                .callbackData("back_to_courses")
                .build();
        backRow.add(backBtn);
        rows.add(backRow);

        markup.setKeyboard(rows);

        SendMessage message = SendMessage.builder()
                .chatId(chatId.toString())
                .text(details)
                .parseMode("HTML")
                .replyMarkup(markup)
                .build();

        sendMessage(message);
    }

    private void handleContact(Update update) throws SQLException {
        Long chatId = update.getMessage().getChatId();
        String phoneNumber = update.getMessage().getContact().getPhoneNumber();

        if (!phoneNumber.startsWith("+")) {
            phoneNumber = "+" + phoneNumber;
        }

        User user = dbHandler.getUser(chatId);
        if (user == null) {
            user = new User();
            user.setChatId(chatId);
        }

        user.setPhone(phoneNumber);
        user.setState("WAITING_FOR_ADDRESS");
        dbHandler.saveUser(user);

        askForAddress(chatId);
    }

    private void handleMainMenuCommands(Long chatId, String command) throws SQLException {
        switch (command) {
            case "🚀 Ro'yxatdan o'tish":
                startRegistration(chatId);
                break;
            case "📚 Kurslar":
                showCourses(chatId);
                break;
            case "📞 Aloqa":
                sendContactInfo(chatId);
                break;
            case "ℹ️ Ma'lumotlar":
                sendInfo(chatId);
                break;
            default:
                sendText(chatId, "🤔 <b>Noma'lum buyruq!</b> Iltimos, quyidagi menyudan tanlang. 😊");
        }
    }

    private void sendMainMenu(Long chatId) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("🚀 Ro'yxatdan o'tish");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("📚 Kurslar");
        row2.add("📞 Aloqa");

        KeyboardRow row3 = new KeyboardRow();
        row3.add("ℹ️ Ma'lumotlar");

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);

        SendMessage message = SendMessage.builder()
                .chatId(chatId.toString())
                .text("""
                    🎉 <b>Bizning oquv markaz rasmiy botiga xush kelibsiz!</b> 🎉
                    
                    📚 Chortoq shahridagi eng zamonaviy ta'lim markazi bilan bilim oling!
                    Quyidagi menyudan kerakli bo'limni tanlang: 👇""")
                .parseMode("HTML")
                .replyMarkup(keyboardMarkup)
                .build();

        sendMessage(message);
    }

    private void startRegistration(Long chatId) throws SQLException {
        User user = dbHandler.getUser(chatId);
        if (user == null) {
            user = new User();
            user.setChatId(chatId);
        }
        user.setState("SELECTING_COURSE");
        dbHandler.saveUser(user);
        showCourses(chatId);
    }

    private void showCourses(Long chatId) throws SQLException {
        List<Course> courses = dbHandler.getAllCourses();

        if (courses.isEmpty()) {
            sendText(chatId, "😔 <b>Hozircha mavjud kurslar yo'q.</b> Keyinroq qayta urinib ko'ring!");
            return;
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Course course : courses) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton courseBtn = InlineKeyboardButton.builder()
                    .text(course.getName() + " (" + course.getDuration() + ")")
                    .callbackData("course_" + course.getId())
                    .build();
            row.add(courseBtn);
            rows.add(row);
        }

        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backBtn = InlineKeyboardButton.builder()
                .text("⬅️ Asosiy menyuga qaytish")
                .callbackData("back_to_main")
                .build();
        backRow.add(backBtn);
        rows.add(backRow);

        markup.setKeyboard(rows);

        SendMessage message = SendMessage.builder()
                .chatId(chatId.toString())
                .text("""
                    📚 <b>Stanford School kurslari:</b>
                    
                    Quyidagi kurslardan birini tanlang va ro'yxatdan o'tishni boshlang! 👇""")
                .parseMode("HTML")
                .replyMarkup(markup)
                .build();

        sendMessage(message);
    }

    private void askForName(Long chatId) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId.toString())
                .text("👤 <b>Ism va familiyangizni kiriting:</b>\nMasalan: Aliyev Valijon")
                .parseMode("HTML")
                .build();

        sendMessage(message);
    }

    private void askForPhone(Long chatId) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();

        KeyboardButton contactBtn = new KeyboardButton("📱 Raqamni ulashish");
        contactBtn.setRequestContact(true);
        row.add(contactBtn);

        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);

        SendMessage message = SendMessage.builder()
                .chatId(chatId.toString())
                .text("""
                    📞 <b>Telefon raqamingizni kiriting:</b>
                    Format: <b>+998XXXXXXXXX</b>
                    Yoki pastdagi tugma orqali raqamingizni ulashing! 👇""")
                .parseMode("HTML")
                .replyMarkup(keyboardMarkup)
                .build();

        sendMessage(message);
    }

    private void askForAddress(Long chatId) throws SQLException {
        List<String> branches = dbHandler.getAllBranches();
        if (branches.isEmpty()) {
            sendText(chatId, "😔 <b>Hozircha mavjud filiallar yo'q.</b> Keyinroq qayta urinib ko'ring!");
            return;
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (String branch : branches) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton branchBtn = InlineKeyboardButton.builder()
                    .text(branch)
                    .callbackData("branch_" + branch)
                    .build();
            row.add(branchBtn);
            rows.add(row);
        }

        List<InlineKeyboardButton> cancelRow = new ArrayList<>();
        InlineKeyboardButton cancelBtn = InlineKeyboardButton.builder()
                .text("❌ Bekor qilish")
                .callbackData("cancel_registration")
                .build();
        cancelRow.add(cancelBtn);
        rows.add(cancelRow);

        markup.setKeyboard(rows);

        SendMessage message = SendMessage.builder()
                .chatId(chatId.toString())
                .text("""
                    📍 <b>Oquv markaz filialini tanlang:</b>
                    Chortoq yoki boshqa filiallardan eng qulayini tanlang! 👇""")
                .parseMode("HTML")
                .replyMarkup(markup)
                .build();

        sendMessage(message);
    }

    private void askForCourseTime(Long chatId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> morningRow = new ArrayList<>();
        InlineKeyboardButton morningBtn = InlineKeyboardButton.builder()
                .text("🌞 Ertalab: 8-12")
                .callbackData("time_Ertalab: 8-12")
                .build();
        morningRow.add(morningBtn);
        rows.add(morningRow);

        List<InlineKeyboardButton> afternoonRow = new ArrayList<>();
        InlineKeyboardButton afternoonBtn = InlineKeyboardButton.builder()
                .text("🌆 Tushdan keyin: 12-20")
                .callbackData("time_Tushdan keyin: 12-20")
                .build();
        afternoonRow.add(afternoonBtn);
        rows.add(afternoonRow);

        List<InlineKeyboardButton> cancelRow = new ArrayList<>();
        InlineKeyboardButton cancelBtn = InlineKeyboardButton.builder()
                .text("❌ Bekor qilish")
                .callbackData("cancel_registration")
                .build();
        cancelRow.add(cancelBtn);
        rows.add(cancelRow);

        markup.setKeyboard(rows);

        SendMessage message = SendMessage.builder()
                .chatId(chatId.toString())
                .text("""
                    ⏰ <b>Kurs vaqtini tanlang:</b>
                    Oquv markaz darslari uchun qulay vaqtni tanlang! 👇""")
                .parseMode("HTML")
                .replyMarkup(markup)
                .build();

        sendMessage(message);
    }

    private void showConfirmation(Long chatId) throws SQLException {
        User user = dbHandler.getUser(chatId);

        String text = String.format("""
            📋 <b>Ma'lumotlaringizni tasdiqlang:</b>
            
            📚 <b>Kurs:</b> %s
            👤 <b>Ism, familiya:</b> %s
            📞 <b>Telefon:</b> %s
            📍 <b>Filial:</b> %s
            ⏰ <b>Kurs vaqti:</b> %s
            
            ✅ Ma'lumotlar to'g'rimi? Tasdiqlang yoki bekor qiling!""",
                user.getCourse(), user.getFullName(), user.getPhone(),
                user.getAddress(), user.getCourseTime());

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton yesBtn = InlineKeyboardButton.builder()
                .text("✅ Tasdiqlash")
                .callbackData("confirm_yes")
                .build();

        InlineKeyboardButton noBtn = InlineKeyboardButton.builder()
                .text("❌ Bekor qilish")
                .callbackData("confirm_no")
                .build();

        row.add(yesBtn);
        row.add(noBtn);
        rows.add(row);
        markup.setKeyboard(rows);

        SendMessage message = SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .parseMode("HTML")
                .replyMarkup(markup)
                .build();

        sendMessage(message);
    }

    private void completeRegistration(Long chatId) throws SQLException {
        sendText(chatId, """
            🎉 <b>Tabriklaymiz!</b> Siz Oquv markaziga kurslariga muvaffaqiyatli ro'yxatdan o'tdingiz! 🥳
            📬 Tez orada siz bilan bog'lanamiz.""");

        User user = dbHandler.getUser(chatId);
        String registrationDetails = String.format("""
            📋 <b>Yangi ro'yxatdan o'tgan o'quvchi:</b>
            
            👤 <b>Ism, familiya:</b> %s
            📞 <b>Telefon:</b> %s
            📍 <b>Filial:</b> %s
            📚 <b>Kurs:</b> %s
            ⏰ <b>Kurs vaqti:</b> %s
            🆔 <b>Chat ID:</b> %d
            """,
                user.getFullName(),
                user.getPhone(),
                user.getAddress(),
                user.getCourse(),
                user.getCourseTime(),
                chatId);

        sendText(GROUP_CHAT_ID, registrationDetails);
    }

    private void sendContactInfo(Long chatId) {
        sendText(chatId, """
            📞 <b>Bog'lanish uchun:</b>
            
            ☎️ <b>Telefon:</b> +998901234567
            📧 <b>Email:</b> info@stanfordschool.uz
            📍 <b>Manzil:</b> Chortoq shahar, Namangan viloyati
            
            🕒 <b>Ish vaqti:</b> Dushanba-Shanba, 08:00 - 20:00""");
    }

    private void sendInfo(Long chatId) {
        sendText(chatId, """
            🏫 <b>Oquv markaz haqida:</b>
            
            Chortoq shahridagi eng nufuzli ta'lim markazi sifatida biz yuqori sifatli ta'lim va zamonaviy o'qitish usullarini taklif qilamiz. 
            Tajribali o'qituvchilarimiz va qulay muhitimiz bilan o'quvchilarimizning bilimlarini oshirishga yordam beramiz.
            
            🌟 <b>Bizning maqsadimiz:</b> Sizning muvaffaqiyatingiz uchun eng yaxshi ta'limni ta'minlash!""");
    }

    private Long getChatId(Update update) {
        if (update.hasMessage()) {
            return update.getMessage().getChatId();
        } else if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getMessage().getChatId();
        }
        return null;
    }

    @Override
    public void onClosing() {
        dbHandler.close();
        super.onClosing();
    }

    private void sendText(Long chatId, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .parseMode("HTML")
                .build();
        sendMessage(message);
    }

    private void sendMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
