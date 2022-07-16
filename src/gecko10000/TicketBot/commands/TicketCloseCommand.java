package gecko10000.TicketBot.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import gecko10000.TicketBot.TicketBot;
import gecko10000.TicketBot.utils.Config;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Optional;

public class TicketCloseCommand extends Command {


    public TicketCloseCommand(TicketBot bot) {
        super(bot);
    }

    @Override
    String getName() {
        return "close";
    }

    @Override
    String getDescription() {
        return "Closes a ticket";
    }

    @Override
    ImmutableApplicationCommandRequest.Builder getCommand() {
        return super.getCommand()
                .addOption(ApplicationCommandOptionData.builder()
                        .name("delay")
                        .description("Optional delay to close ticket")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .build());
    }

    @Override
    Mono<Void> handleCommand(ChatInputInteractionEvent e) {
        Duration delay = Duration.ZERO;
        Optional<String> input = e.getOption("delay")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString);
        if (input.isPresent()) {
            try {
                delay = Duration.parse("PT" + input.get());
            } catch (DateTimeParseException ex) {
                return e.reply("Invalid delay!").withEphemeral(true);
            }
        }
        Duration finalDelay = delay;
        Mono<TextChannel> channelMono = e.getInteraction().getChannel().ofType(TextChannel.class)
                .filter(c -> bot.sql.isTicket(c.getId()));
        if (!delay.isZero()) {
            channelMono = channelMono
                    .flatMap(c -> e.reply(
                    Config.getAndFormat("commands.close.scheduled",
                            finalDelay.toString().substring(2).toLowerCase())).thenReturn(c));
        }
        return channelMono
                .doOnNext(c -> bot.ticketManager.closeTicket(c, finalDelay)).thenReturn("")
                .switchIfEmpty(e.reply(Config.getAndFormat("commands.notTicket")).withEphemeral(true).thenReturn(""))
                .then();
    }
}
