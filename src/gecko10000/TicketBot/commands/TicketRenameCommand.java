package gecko10000.TicketBot.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import gecko10000.TicketBot.TicketBot;
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
        return "Renames a ticket";
    }

    @Override
    Mono<Void> handleCommand(ChatInputInteractionEvent e) {
        return e.reply("don't use this yet");
    }
}
