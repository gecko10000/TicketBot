package gecko10000.TicketBot.commands;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import gecko10000.TicketBot.TicketBot;
import gecko10000.TicketBot.utils.Config;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

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
        Duration delay = Duration.ofHours(24); // default
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
        return e.getInteraction().getChannel().ofType(TextChannel.class)
                .filter(c -> bot.sql.isTicket(c.getId())) // early return for non-tickets
                .flatMap(c -> finalDelay.isZero() ? Mono.just(c) : sendScheduleMessage(e, finalDelay).thenReturn(c))
                .flatMap(c -> c.getGuild()
                        .flatMap(Guild::getEveryoneRole)
                        .map(Role::getId)
                        .flatMap(s -> c.addRoleOverwrite(s, PermissionOverwrite.forRole(s,
                                PermissionSet.none(),
                                PermissionSet.of(Permission.SEND_MESSAGES, Permission.VIEW_CHANNEL))))
                        .thenReturn(c))
                .flatMap(c -> closeTicket(e, c, finalDelay).thenReturn(""))
                .switchIfEmpty(e.reply(Config.getAndFormat("commands.notTicket")).withEphemeral(true).thenReturn(""))
                .then();
    }

    private static final String REOPEN = "ticket-reopen";

    private Mono<Void> closeTicket(ChatInputInteractionEvent event, TextChannel channel, Duration delay) {
        return bot.client.on(ButtonInteractionEvent.class)
                .filter(e -> e.getCustomId().equals(REOPEN)) // reopen id
                .filterWhen(e -> e.getMessage().get().getChannel() // correct channel
                            .ofType(MessageChannel.class)
                            .map(c -> c.getId().equals(channel.getId())))
                .flatMap(e -> Mono.zip(
                            e.getMessage().map(m -> m.delete().thenReturn("")).orElse(Mono.just("")),
                            channel.getGuild()
                                    .flatMap(Guild::getEveryoneRole)
                                    .map(Role::getId)
                                    .flatMap(s -> channel.addRoleOverwrite(s, PermissionOverwrite.forRole(s,
                                            PermissionSet.of(Permission.SEND_MESSAGES),
                                            PermissionSet.of(Permission.VIEW_CHANNEL))))
                                    .thenReturn(e)).map(Tuple2::getT2))
                .flatMap(e -> channel.createMessage(reopenMessage(e))
                        .thenReturn(""))
                .next().thenReturn("")
                .timeout(delay)
                .onErrorResume(TimeoutException.class, e -> bot.ticketManager.closeTicket(channel)
                        .onErrorResume(t -> Mono.empty()).thenReturn(""))
                .then();
    }

    private Mono<Void> sendScheduleMessage(ChatInputInteractionEvent e, Duration delay) {
        return e.reply(Config.getAndFormat("commands.close.scheduled",
                        delay.toString().substring(2).toLowerCase()))
                .withComponents(ActionRow.of(Button.primary(REOPEN, "Re-open ticket")));
    }

    private MessageCreateSpec reopenMessage(ButtonInteractionEvent e) {
        return MessageCreateSpec.builder()
                .addEmbed(EmbedCreateSpec.builder()
                        .description(Config.getAndFormat("commands.close.reopened", e.getInteraction().getUser().getMention()))
                        .build())
                .build();
    }
}
