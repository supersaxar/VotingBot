package voteBot;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.InputStream;
import java.net.URL;

;


public class VoteLogic extends ListenerAdapter {
    private Database database;
    private boolean anon;

    public VoteLogic(Database db) {
        database = db;
    }

    public void onMessageReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        String content = message.getContentRaw();
        if (content.startsWith("!")) {
            MessageBuilder messageBuilder = new MessageBuilder();
            String guildId = message.getGuild().getId();
            try {
                String useChannel = database.getVotingChannel(guildId);
                if (message.getMember() != null && content.startsWith("!SetChannel")) {
                    if (message.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                        String channelName = content.replace("!SetChannel ", "").strip();
                        String channelId = event.getGuild().getTextChannelsByName(channelName, false).get(0).getId();
                        database.setVotingChannel(guildId, channelId);
                        messageBuilder.setContent("Vote or die!\nNew vote channel is " + channelName);
                    } else {
                        messageBuilder.setContent("You are not an admin, so sorry.\nComeback here with more power.");
                    }
                    event.getChannel().sendMessage(messageBuilder.build()).queue();
                } else if (content.startsWith("!Vote")) {
                    if (useChannel != null) {
                        if (event.getChannel().getId().equals(useChannel)) {
                            if (content.startsWith("!VoteAnon")) anon = true;    //secret feature
                            String description = content.replace("!Vote", "").strip();
                            String voteString = "@эвриван \n new bill from: " +
                                    (anon ? "" : message.getAuthor().getAsMention()) + "\n " + description;
                            if (message.getAttachments().size() > 0) {
                                Message.Attachment attachment = message.getAttachments().get(0);
                                InputStream file = new URL(attachment.getUrl()).openStream();
                                event.getChannel().sendFile(file, attachment.getFileName())
                                        .content(voteString)
                                        .queue((botMessage) -> database.insertMessage(botMessage, message.getAuthor().getId()));
                            } else {
                                event.getChannel()
                                        .sendMessage(messageBuilder.setContent(voteString).build())
                                        .queue(botMessage -> database.insertMessage(botMessage, message.getAuthor().getId()));
                            }
                            message.delete().queue();
                        }
                    } else {
                        messageBuilder.setContent("Vote channel doesn't set \nPlease use \"!SetChannel <channelname>\"");
                        event.getChannel().sendMessage(messageBuilder.build()).queue();
                    }
                }
            } catch (Exception e) {
                messageBuilder.setContent("Something went wrong, but no one cares.");
                event.getChannel().sendMessage(messageBuilder.build()).queue();
            }
        }
    }
}
