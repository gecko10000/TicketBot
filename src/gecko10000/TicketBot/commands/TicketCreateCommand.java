package gecko10000.TicketBot.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Member;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import gecko10000.TicketBot.TicketBot;
import gecko10000.TicketBot.utils.Config;
import reactor.core.publisher.Mono;

import java.util.Optional;

public class TicketCreateCommand extends Command {

    public TicketCreateCommand(TicketBot bot) {
        super(bot);
    }

    @Override
    String getName() {
        return "ticket";
    }

    @Override
    String getDescription() {
        return "Creates a ticket";
    }

    @Override
    ImmutableApplicationCommandRequest.Builder getCommand() {
        return super.getCommand().defaultPermission(true);
    }

    @Override
    Mono<Void> handleCommand(ChatInputInteractionEvent e) {
        Optional<Member> m = e.getInteraction().getMember();
        if (m.isEmpty()) return Mono.empty();
        return bot.ticketManager.openTicket(m.get())
                .flatMap(c -> e.reply(Config.getAndFormat("commands.create.success", c.getMention())).thenReturn(""))
                .switchIfEmpty(e.reply(Config.getAndFormat("commands.create.failure")).withEphemeral(true).thenReturn(""))
                .then();
    }
}
