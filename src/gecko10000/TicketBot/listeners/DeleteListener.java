package gecko10000.TicketBot.listeners;

import discord4j.core.event.domain.channel.TextChannelDeleteEvent;
import gecko10000.TicketBot.TicketBot;
import gecko10000.TicketBot.utils.Utils;
import reactor.core.publisher.Mono;

import java.time.Duration;

// used to sync database upon manual channel deletion

public class DeleteListener {

    private final TicketBot bot;

    public DeleteListener(TicketBot bot) {
        this.bot = bot;
        bot.client.on(TextChannelDeleteEvent.class, e -> {
            bot.sql.syncTickets();
            return Mono.empty();
        }).subscribe();
    }

}
