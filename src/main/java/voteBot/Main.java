package voteBot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;


public class Main {

    private static String databaseUrl = System.getProperty("database.url");
    private static String botToken = System.getProperty("bot.token");

    public static void main(String[] args) throws Exception {
        JDA jda = JDABuilder.createDefault(botToken).build();
        Database database = new Database(databaseUrl);
        VoteLogic bot = new VoteLogic(database);
        new Scheduler(database, jda);
        jda.addEventListener(bot);
    }
}
