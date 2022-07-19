package gecko10000.TicketBot.utils;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.User;
import gecko10000.TicketBot.TicketBot;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.sql.Connection;
import java.util.stream.Stream;

public class SQLManager {

    private final TicketBot bot;
    private final SQLHelper sql;


    public SQLManager(TicketBot bot) {
        this.bot = bot;
        Connection connection = SQLHelper.openSQLite(new File("database.db").toPath());
        this.sql = new SQLHelper(connection);
        createTables();
    }

    private void createTables() {
        sql.execute("CREATE TABLE IF NOT EXISTS tickets (" +
                "channel TEXT PRIMARY KEY," +
                "user TEXT," +
                "number INTEGER);");
        sql.execute("CREATE TABLE IF NOT EXISTS usernames (" +
                "snowflake TEXT PRIMARY KEY," +
                "username TEXT);");
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

    public Stream<Snowflake> getUserTickets(Snowflake user) {
        return sql.<String>queryResultList("SELECT channel FROM tickets WHERE user=?;", user.asString())
                .stream().map(Snowflake::of);
    }

    public Snowflake getTicketOpener(Snowflake ticketId) {
        return Snowflake.of(sql.<String>querySingleResult("SELECT user FROM tickets WHERE channel=?;", ticketId.asString()));
    }

    public boolean isTicket(Snowflake channel) {
        return getTicketNumber(channel) != null;
    }

    public String getUsername(Snowflake id) {
        return sql.querySingleResult("SELECT username FROM usernames WHERE snowflake=?;", id.asString());
    }

    public void setUsername(Snowflake id, String username) {
        sql.execute("INSERT OR IGNORE INTO usernames VALUES (?, ?);", id.asString(), username);
    }

    public void removeUsername(Snowflake id) {
        sql.execute("DELETE FROM usernames WHERE snowflake=?;", id.asString());
    }

    public void save() {
        sql.close();
    }

}
