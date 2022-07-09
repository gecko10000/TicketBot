package gecko10000.TicketBot;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.TextChannelCreateSpec;
import gecko10000.TicketBot.utils.Config;
import reactor.core.publisher.Flux;

public class ButtonListener {

    private final TicketBot bot;

    public ButtonListener(TicketBot bot) {
        this.bot = bot;
        Flux<ButtonInteractionEvent> eventFlux = bot.client.on(ButtonInteractionEvent.class)
                .filter(e -> e.getCustomId().equals("ticket"));
        eventFlux.flatMap(e -> e.getInteraction().getGuild())
                .zipWith(eventFlux.map(e -> e.getInteraction().getUser()))
                .subscribe(guildUserTuple -> bot.ticketManager.openTicket(guildUserTuple.getT1(), guildUserTuple.getT2()));
    }

}
