package gecko10000.TicketBot.listeners;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.entity.Guild;
import gecko10000.TicketBot.TicketBot;
import reactor.core.publisher.Mono;

public class ButtonListener {

    private final TicketBot bot;

    public ButtonListener(TicketBot bot) {
        this.bot = bot;
        bot.client.on(ButtonInteractionEvent.class, e -> {
            if (!e.getCustomId().equals("ticket")) return Mono.empty();
            Mono<Guild> guildMono = e.getInteraction().getGuild();
            guildMono.subscribe(guild -> bot.ticketManager.openTicket(guild, e.getInteraction().getUser()));
            return e.deferEdit();
        }).subscribe();
    }

}
