package dev.telegrambots.converterbot;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new ConverterBot());
            System.out.println("Converter Bot started.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}