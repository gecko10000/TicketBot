package gecko10000.TicketBot.listeners;

import discord4j.core.event.domain.channel.TextChannelDeleteEvent;
import gecko10000.TicketBot.TicketBot;
import reactor.core.publisher.Mono;

import java.time.Duration;

// used to sync database upon manual channel deletion

public class DeleteListener {

    public DeleteListener(TicketBot bot) {
        bot.client.on(TextChannelDeleteEvent.class)
                .flatMap(e -> bot.sql.syncTickets())
                .subscribe();
    }

}
