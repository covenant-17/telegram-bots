package dev.telegrambots.managerbot;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            ManagerBot bot = new ManagerBot();
            botsApi.registerBot(bot);
            bot.notifyStartup();
            System.out.println("Manager Bot started.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
