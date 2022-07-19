package gecko10000.TicketBot.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import discord4j.discordjson.json.PermissionsEditRequest;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import gecko10000.TicketBot.TicketBot;
import gecko10000.TicketBot.utils.Config;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

public class TicketAddUserCommand extends Command {

    public TicketAddUserCommand(TicketBot bot) {
        super(bot);
    }

    @Override
    String getName() {
        return "add";
    }

    @Override
    String getDescription() {
        return "Adds a user to the ticket.";
    }

    @Override
    ImmutableApplicationCommandRequest.Builder getCommand() {
        return super.getCommand()
                .addOption(ApplicationCommandOptionData.builder()
                        .name("user")
                        .description("The user to add to the ticket")
                        .required(true)
                        .type(ApplicationCommandOption.Type.USER.getValue())
                        .build());
    }

    @Override
    Mono<Void> handleCommand(ChatInputInteractionEvent e) {
        return e.getOption("user")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .get().asUser()
                .flatMap(u -> e.getInteraction().getChannel()
                        .ofType(TextChannel.class)
                        .map(c -> Tuples.of(u, c)))
                .filter(t -> bot.sql.isTicket(t.getT2().getId()))
                .flatMap(t -> t.getT2().addMemberOverwrite(t.getT1().getId(),
                        PermissionOverwrite.forMember(t.getT1().getId(), PermissionSet.of(Permission.VIEW_CHANNEL), PermissionSet.none()))
                        .thenReturn(t.getT1()))
                .flatMap(u -> e.reply(Config.getAndFormat("commands.add.success", u.getMention())).thenReturn(""))
                .switchIfEmpty(e.reply(Config.getAndFormat("commands.notTicket")).withEphemeral(true).thenReturn(""))
                .then();
    }
}
