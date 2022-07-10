package gecko10000.TicketBot.listeners;

import discord4j.core.event.domain.channel.TextChannelDeleteEvent;
import gecko10000.TicketBot.TicketBot;
import reactor.core.publisher.Mono;

import java.time.Duration;

// used to sync database upon manual channel deletion

public class DeleteListener {

    private final TicketBot bot;

    public DeleteListener(TicketBot bot) {
        this.bot = bot;
        bot.client.on(TextChannelDeleteEvent.class, e -> {
            // if tickets are synced without a delay, the ticket
            // seems to still exist? this should pretty much cover
            // any request delays, it's not a super important action
            // anyway
            Mono.delay(Duration.ofSeconds(5))
                    .subscribe(t -> bot.sql.syncTickets());
            return Mono.empty();
        }).subscribe();
    }

}
