package gecko10000.TicketBot.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.RestClient;
import gecko10000.TicketBot.TicketBot;
import gecko10000.TicketBot.utils.Config;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Command {

    final TicketBot bot;

    public Command(TicketBot bot) {
        this.bot = bot;
        RestClient restClient = bot.client.getRestClient();
        Mono<Long> idMono = restClient.getApplicationId();
        if (Config.get("devMode")) {
            idMono.zipWith(bot.client.getGuilds().next())
                    .flatMap(t -> restClient.getApplicationService()
                            .createGuildApplicationCommand(t.getT1(), t.getT2().getId().asLong(), getCommand()))
                    .subscribe();
        } else {
            idMono
                    .flatMap(id -> restClient.getApplicationService()
                            .createGlobalApplicationCommand(id, getCommand()))
                    .subscribe();
        }
    }

    ApplicationCommandRequest getCommand() {
        return ApplicationCommandRequest.builder()
                .name(getName())
                .description(getDescription())
                .build();
    }

    abstract String getName();
    abstract String getDescription();

    abstract Mono<Void> handleCommand(ChatInputInteractionEvent e);

}
