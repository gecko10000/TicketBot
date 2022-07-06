package gecko10000.TicketBot;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Scanner;

public class TicketBot {

    public static void main(String[] args) {
        new TicketBot();
    }

    private String getToken() {
        File file = new File("token.txt");
        Scanner scanner;
        try {
            scanner = new Scanner(file);
        } catch (FileNotFoundException e) {
            System.out.println("File not found: token.txt");
            System.exit(1);
            return null;
        }
        String token = scanner.nextLine();
        scanner.close();
        return token;
    }

    public TicketBot() {
        Mono<Void> bot = DiscordClient.create(getToken())
                .withGateway(gateway -> gateway.on(MessageCreateEvent.class, e -> {
                    if (e.getMessage().getContent().equals("hi")) {
                        return e.getMessage().getChannel().flatMap(channel -> channel.createMessage("hi"));
                    }
                    return Mono.empty();
                }));
        bot.block();
    }

}
