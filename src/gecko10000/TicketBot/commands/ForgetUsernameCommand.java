package gecko10000.TicketBot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import gecko10000.TicketBot.TicketBot;
import gecko10000.TicketBot.utils.Config;
import gecko10000.TicketBot.utils.Utils;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

public class ForgetUsernameCommand extends Command {

    public ForgetUsernameCommand(TicketBot bot) {
        super(bot);
    }

    @Override
    String getName() {
        return "forget";
    }

    @Override
    String getDescription() {
        return "Removes the user's associated panel username in case it was mistyped or has changed.";
    }

    @Override
    Mono<Void> handleCommand(ChatInputInteractionEvent e) {
        Snowflake channelId = e.getInteraction().getChannelId();
        if (!bot.sql.isTicket(channelId))
            return e.reply(Config.getAndFormat("commands.notTicket")).withEphemeral(true);
        Snowflake ticketOpener = bot.sql.getTicketOpener(channelId);
        String oldUsername = bot.sql.getUsername(ticketOpener);
        if (oldUsername == null)
            return e.reply(Config.getAndFormat("commands.forget.noUser", Utils.userMention(ticketOpener))).withEphemeral(true);
        bot.sql.removeUsername(ticketOpener);
        return e.reply(Config.getAndFormat("commands.forget.success", oldUsername, Utils.userMention(ticketOpener))).withEphemeral(true);
    }
}
