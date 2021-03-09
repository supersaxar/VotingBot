package voteBot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

public class Scheduler {

    public Scheduler(Database database, JDA jda) {
        Timer timer = new Timer();
        timer.schedule(new EndingVotesTask(database, jda),0, 3600000);
    }

    private static class VoteResultsTask extends TimerTask {
        private final Message message;

        public VoteResultsTask(Message message) {
            this.message = message;
        }

        @Override
        public void run() {
            message.getChannel()
                    .retrieveMessageById(message.getId()).queue((message2) -> message2.reply(message2.getReactions().stream()
                    .map(reaction -> "<:" + reaction.getReactionEmote().getAsReactionCode() + ">: " + reaction.getCount())
                    .collect(Collectors.joining("\n", "Vote "+ message.getJumpUrl()+ " ends:\n", "")))
                    .queue());
        }
    }

    private static class EndingVotesTask extends TimerTask {
        private final Database database;
        private final JDA jda;

        public EndingVotesTask(Database database, JDA jda) {
            this.database = database;
            this.jda = jda;
        }

        @Override
        public void run() {
            ArrayList<Document> documents = database.getVoteForNextHour();
            Timer timer = new Timer();
            for (Document document : documents) {
                //because we save it as sec not as ms
                Date endTime = new Date(document.get("endTime", Long.class) * 1000L);
                jda.getGuildById(document.get("guildId", String.class))
                        .getTextChannelById(document.get("channelId", String.class))
                        .retrieveMessageById(document.get("messageId", String.class))
                        .queue(message -> timer.schedule(new VoteResultsTask(message), endTime));
            }
        }
    }
}


