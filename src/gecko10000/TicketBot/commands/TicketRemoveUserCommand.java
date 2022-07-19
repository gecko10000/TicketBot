package gecko10000.TicketBot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import gecko10000.TicketBot.TicketBot;
import gecko10000.TicketBot.utils.Config;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

public class TicketRemoveUserCommand extends Command {

    public TicketRemoveUserCommand(TicketBot bot) {
        super(bot);
    }

    @Override
    String getName() {
        return "remove";
    }

    @Override
    String getDescription() {
        return "Removes a user from the ticket.";
    }

    @Override
    ImmutableApplicationCommandRequest.Builder getCommand() {
        return super.getCommand()
                .addOption(ApplicationCommandOptionData.builder()
                        .name("name")
                        .description("The user to remove from the ticket")
                        .required(true)
                        .type(ApplicationCommandOption.Type.USER.getValue())
                        .build());
    }

    @Override
    Mono<Void> handleCommand(ChatInputInteractionEvent e) {
        return e.getOption("name")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .get().asUser()
                .filter(u -> !u.getId().equals(bot.client.getSelfId()))
                .flatMap(u -> e.getInteraction().getChannel().ofType(TextChannel.class).map(c -> Tuples.of(u, c)))
                .flatMap(t -> {
                    Snowflake channel = t.getT2().getId(), user = t.getT1().getId();
                    if (!bot.sql.isTicket(channel))
                        return e.reply(Config.getAndFormat("commands.notTicket"))
                                .withEphemeral(true);
                    return t.getT2().getEffectivePermissions(user)
                            .filter(p -> p.contains(Permission.VIEW_CHANNEL))
                            .flatMap(ign -> t.getT2().addMemberOverwrite(user,
                                    PermissionOverwrite.forMember(user, PermissionSet.none(), PermissionSet.of(Permission.VIEW_CHANNEL))).thenReturn(""))
                            .flatMap(ign -> e.reply(Config.getAndFormat("commands.remove.success", t.getT1().getMention()))
                                    .thenReturn(""))
                            .switchIfEmpty(e.reply(Config.getAndFormat("commands.remove.already", t.getT1().getMention()))
                                    .withEphemeral(true)
                                    .thenReturn(""));
                })
                .switchIfEmpty(e.reply(Config.getAndFormat("commands.remove.self")) // tried to remove the bot
                        .withEphemeral(true)
                        .thenReturn(""))
                .then();
    }
}
