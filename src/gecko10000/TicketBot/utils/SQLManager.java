package gecko10000.TicketBot.utils;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.User;
import gecko10000.TicketBot.TicketBot;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.sql.Connection;

public class SQLManager {

    private final TicketBot bot;
    private final SQLHelper sql;


    public SQLManager(TicketBot bot) {
        this.bot = bot;
        Connection connection = SQLHelper.openSQLite(new File("database.db").toPath());
        this.sql = new SQLHelper(connection);
        createTable();
    }

    private void createTable() {
        sql.execute("CREATE TABLE IF NOT EXISTS tickets (" +
                "channel TEXT PRIMARY KEY," +
                "user TEXT," +
                "number INTEGER);");
    }

    public void insertTicket(Snowflake channel, Snowflake user, int ticketNumber) {
        sql.execute("INSERT INTO tickets VALUES (?, ?, ?);", channel.asString(), user.asString(), ticketNumber);
    }

    public void deleteTicket(Snowflake channel) {
        sql.execute("DELETE FROM tickets WHERE channel=?;", channel.asString());
    }

    // Removes tickets from the database
    // if the corresponding channels no longer exist.
    public Mono<Void> syncTickets() {
        Snowflake support = Config.getSF("supportChannel");
        return Flux.fromIterable(sql.<String>queryResultList("SELECT channel FROM tickets;"))
                .map(Snowflake::of)
                .flatMap(s -> bot.client.getChannelById(s)
                        .onErrorResume(t -> {
                            deleteTicket(s);
                            return bot.client.getChannelById(support);
                        }))
                .filter(c -> c.getId().equals(support))
                .count()
                .filter(c -> c != 0)
                .map(c -> c + " ticket" + Utils.smartS(c) + " deleted from database.")
                .doOnNext(System.out::println)
                .then();
    }

    public int countTickets(User user) {
        return sql.querySingleResult("SELECT COUNT(*) FROM tickets WHERE user=?;", user.getId().asString());
    }

    public Integer getTicketNumber(Snowflake channel) {
        return sql.querySingleResult("SELECT number FROM tickets WHERE channel=?;", channel.asString());
    }

    public boolean isTicket(Snowflake channel) {
        return getTicketNumber(channel) != null;
    }

    public void save() {
        sql.close();
    }

}
