package gecko10000.TicketBot;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import gecko10000.TicketBot.listeners.DeleteListener;
import gecko10000.TicketBot.utils.Config;
import gecko10000.TicketBot.utils.SQLManager;
import reactor.core.publisher.Mono;

public class TicketBot {

    public static void main(String[] args) {
        new TicketBot();
    }

    public final GatewayDiscordClient client;
    public final TicketManager ticketManager;
    public final SQLManager sql;

    public TicketBot() {
        Config.loadConfig();
        client = DiscordClient.create(Config.get("botToken")).login().block();
        if (client == null) System.exit(1); // "waaah client might be null" -IntelliJ

        new TicketButtonManager(this);
        new DeleteListener(this);
        ticketManager = new TicketManager(this);
        sql = new SQLManager(this);

        sql.syncTickets();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            sql.save();
            client.logout().block();
            System.out.println("Logged out.");
        }));
        Mono.never().block(); // keep running the bot for now
    }

}
