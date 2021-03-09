package voteBot;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import net.dv8tion.jda.api.entities.Message;
import org.bson.Document;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static com.mongodb.client.model.Filters.*;
import static java.time.temporal.ChronoUnit.*;

public class Database {

    private MongoDatabase database;

    public Database(String url) {
        MongoClient mongoClient = new MongoClient(new MongoClientURI(url));
        database = mongoClient.getDatabase("golosowanieBot");
        database.getCollection("Voting").dropIndexes();
        database.getCollection("Voting").createIndex(Indexes.ascending("date"),
                new IndexOptions().expireAfter(7L, TimeUnit.of(DAYS)));
    }

    public void insertMessage(Message message, String userId) {
        MongoCollection<Document> collection = database.getCollection("Voting");
        Document doc = new Document()
                .append("guildId", message.getGuild().getId())
                .append("channelId", message.getChannel().getId())
                .append("messageId", message.getId())
                .append("userId", userId)
                .append("endTime", message.getTimeCreated().plus(1, DAYS).toInstant().getEpochSecond());
        collection.insertOne(doc);
    }

    public String getVotingChannel(String guildId) {
        Document guildDoc = database.getCollection("Guilds").find(eq("guildId", guildId)).first();
        return guildDoc != null ? guildDoc.get("channelId", String.class) : null;
    }

    public void setVotingChannel(String guidId, String useChannelId) {
        database.getCollection("Guilds")
                .insertOne(new Document().append("guildId", guidId).append("channelId", useChannelId));
    }

    public ArrayList<Document> getVoteForNextHour() {
        MongoCollection<Document> collection = database.getCollection("Voting");
        ArrayList<Document> documents = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now();
        try (MongoCursor<Document> cursor = collection
                .find(and(lte("endTime", now.plus(1, HOURS).toEpochSecond()),
                        gte("endTime", now.toEpochSecond()))).iterator()) {
            while (cursor.hasNext()) {
                documents.add(cursor.next());
            }
        }
        return documents;
    }
}
