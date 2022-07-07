package gecko10000.TicketBot;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import reactor.core.publisher.Mono;

public class TicketBot {

    public static void main(String[] args) {
        new TicketBot();
    }

    public TicketBot() {
        new Config();
        GatewayDiscordClient client = DiscordClient.create(Config.getProperty("botToken")).login().block();
        new TicketButtonManager(client);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            client.logout().block();
            System.out.println("Logged out.");
        }));
        Mono.never().block(); // keep running the bot for now
    }

}
