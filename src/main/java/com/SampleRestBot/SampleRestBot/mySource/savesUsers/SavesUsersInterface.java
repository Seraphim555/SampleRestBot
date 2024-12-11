package com.SampleRestBot.SampleRestBot.mySource.savesUsers;

import java.io.*;

public class SavesUsersInterface {

    public static boolean hasUser(String userName) throws IOException {

        BufferedReader reader = new BufferedReader(new FileReader("src/main/java/com/SampleRestBot/SampleRestBot/mySource/savesUsers/SavesUsers"));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.equals(userName)) {
                reader.close();
                return true;
            }
        }
        reader.close();
        return false;
    }

    public static void saveUser(String userName, String phoneNumber) throws IOException {

        try {
            FileWriter writer = new FileWriter("src/main/java/com/SampleRestBot/SampleRestBot/mySource/savesUsers/SavesUsers");
            writer.write(userName);
            writer.write("\n");
            writer.write(phoneNumber);
            writer.close();
        } catch (IOException e) {
            System.out.println("Ошибка");
        }
    }
}
