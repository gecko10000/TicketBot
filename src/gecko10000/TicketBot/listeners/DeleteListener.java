package gecko10000.TicketBot.listeners;

import discord4j.core.event.domain.channel.TextChannelDeleteEvent;
import gecko10000.TicketBot.TicketBot;

import java.time.Duration;

// used to sync database upon manual channel deletion

public class DeleteListener {

    public DeleteListener(TicketBot bot) {
        bot.client.on(TextChannelDeleteEvent.class)
                .delaySequence(Duration.ofSeconds(1))
                .flatMap(e -> bot.sql.syncTickets())
                .subscribe();
    }

}
