package gecko10000.TicketBot.listeners;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.guild.MemberLeaveEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import gecko10000.TicketBot.TicketBot;
import gecko10000.TicketBot.utils.Config;

public class LeaveListener {

    public LeaveListener(TicketBot bot) {
        bot.client.on(MemberLeaveEvent.class)
                .map(MemberLeaveEvent::getUser)
                .map(User::getId)
                .doOnNext(s -> bot.sql.getUserTickets(s)
                            .map(bot.client::getChannelById)
                            .forEach(m -> m.ofType(MessageChannel.class)
                                    .flatMap(c -> c.createMessage(createLeaveMessage(s)))
                                    .subscribe()))
                .subscribe();
    }

    private MessageCreateSpec createLeaveMessage(Snowflake user) {
        return MessageCreateSpec.builder()
                .addEmbed(EmbedCreateSpec.builder()
                        .description(Config.getAndFormat("messages.userLeft", "<@" + user.asString() + ">"))
                        .build())
                .build();
    }

}
