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

public class TicketRenameCommand extends Command {

    public TicketRenameCommand(TicketBot bot) {
        super(bot);
    }

    @Override
    String getName() {
        return "rename";
    }

    @Override
    String getDescription() {
        return "Renames a ticket.";
    }

    @Override
    ImmutableApplicationCommandRequest.Builder getCommand() {
        return super.getCommand()
                .addOption(ApplicationCommandOptionData.builder()
                        .name("name")
                        .description("The ticket's new name")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(true)
                        .build());
    }

    @Override
    Mono<Void> handleCommand(ChatInputInteractionEvent e) {
        String newName = e.getOption("name")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .get();
        return e.getInteraction().getChannel()
                .filter(c -> bot.sql.isTicket(c.getId()))
                .ofType(TextChannel.class)
                .flatMap(c -> bot.ticketManager.renameTicket(c, newName, bot.sql.getTicketNumber(c.getId())))
                .flatMap(c -> e.reply(Config.getAndFormat("commands.rename.success", c.getName().substring(1/*remove "#"*/))).thenReturn(""))
                .switchIfEmpty(e.reply(Config.getAndFormat("commands.notTicket")).withEphemeral(true).thenReturn(""))
                .then();
    }
}
