package gecko10000.TicketBot;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import gecko10000.TicketBot.commands.CommandRegistry;
import gecko10000.TicketBot.commands.TicketCreateCommand;
import gecko10000.TicketBot.listeners.DeleteListener;
import gecko10000.TicketBot.listeners.LeaveListener;
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
        client = DiscordClient.create(Config.get("botToken")).gateway()
                .setEnabledIntents(IntentSet.of(Intent.GUILD_MEMBERS, Intent.GUILD_MESSAGES).or(IntentSet.nonPrivileged()))
                .login().block();
        if (client == null) System.exit(1); // "waaah client might be null" -IntelliJ

        ticketManager = new TicketManager(this);
        sql = new SQLManager(this);
        new TicketButtonManager(this);
        new DeleteListener(this);
        new LeaveListener(this);
        new CommandRegistry(this);

        sql.syncTickets().subscribe();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            sql.save();
            client.logout().block();
            System.out.println("Logged out.");
        }));
        Mono.never().block(); // keep running the bot for now
    }

}
