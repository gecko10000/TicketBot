package gecko10000.TicketBot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import gecko10000.TicketBot.TicketBot;
import gecko10000.TicketBot.utils.Config;
import gecko10000.TicketBot.utils.Utils;
import reactor.core.publisher.Mono;

public class RememberUsernameCommand extends Command {

    public RememberUsernameCommand(TicketBot bot) {
        super(bot);
    }

    @Override
    String getName() {
        return "remember";
    }

    @Override
    String getDescription() {
        return "Saves the given username as the ticket creator's panel username.";
    }

    @Override
    ImmutableApplicationCommandRequest.Builder getCommand() {
        return super.getCommand()
                .addOption(ApplicationCommandOptionData.builder()
                        .name("username")
                        .description("The new username to set for the ticket's opener")
                        .required(true)
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .build());
    }

    @Override
    Mono<Void> handleCommand(ChatInputInteractionEvent e) {
        String newUsername = e.getOption("username")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .get().asString();
        Snowflake channelId = e.getInteraction().getChannelId();
        if (!bot.sql.isTicket(channelId)) {
            return e.reply(Config.getAndFormat("commands.notTicket")).withEphemeral(true);
        }
        Snowflake ticketOpener = bot.sql.getTicketOpener(channelId);
        bot.sql.setUsername(ticketOpener, newUsername);
        return e.reply(Config.getAndFormat("commands.remember.success", Utils.userMention(ticketOpener), newUsername)).withEphemeral(true);
    }
}
