package gecko10000.TicketBot;

import discord4j.common.util.Snowflake;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.TextChannelCreateSpec;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import gecko10000.TicketBot.utils.Config;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.stream.Collectors;

public class TicketManager {

    private final TicketBot bot;

    public TicketManager(TicketBot bot) {
        this.bot = bot;
    }

    private TextChannelCreateSpec buildChannel(int ticketNum, Snowflake user, Snowflake everyone, Snowflake ticketSupport, Snowflake ticketManage) {
        return TextChannelCreateSpec.builder()
                .name("ticket-" + ticketNum)
                .addPermissionOverwrite(PermissionOverwrite.forRole(everyone, PermissionSet.none(), PermissionSet.of(Permission.VIEW_CHANNEL)))
                .addPermissionOverwrite(PermissionOverwrite.forMember(bot.client.getSelfId(), PermissionSet.of(Permission.VIEW_CHANNEL), PermissionSet.none()))
                .addPermissionOverwrite(PermissionOverwrite.forRole(ticketSupport, PermissionSet.of(Permission.VIEW_CHANNEL), PermissionSet.none()))
                .addPermissionOverwrite(PermissionOverwrite.forRole(ticketManage, PermissionSet.of(Permission.VIEW_CHANNEL), PermissionSet.none()))
                .addPermissionOverwrite(PermissionOverwrite.forMember(user, PermissionSet.of(Permission.VIEW_CHANNEL), PermissionSet.none()))
                .parentId(Config.getSF("ticketCategory"))
                .build();
    }

    private boolean canOpenTicket(User user) {
        return bot.sql.countTickets(user) < Config.<Integer>get("maxConcurrentTickets");
    }

    public void openTicket(Guild guild, User user) {
        // do not open more tickets
        if (!canOpenTicket(user)) {
            return;
        }
        int ticketNum = Config.incrementTicketCount();
        guild.getEveryoneRole()
                .map(Role::getId)
                .map(everyone -> buildChannel(ticketNum, user.getId(), everyone, Config.getSF("ticketSupportRole"), Config.getSF("ticketManageRole")))
                .flatMap(guild::createTextChannel)
                .map(channel -> {
                    bot.sql.insertTicket(channel.getId(), user.getId(), ticketNum);
                    return channel;
                })
                .flatMap(channel -> channel.createMessage("uh"))
                .subscribe();
        System.out.println("Created ticket " + ticketNum + " for " + user.getUsername() + "#" + user.getDiscriminator() + ".");
        bot.sql.syncTickets();
    }

    public Mono<Void> ghostPing(TextChannel channel, Entity... usersAndRoles) {
        return channel
                .createMessage(Arrays.stream(usersAndRoles)
                    .map(e -> e instanceof User user ? user.getMention() : ((Role) e).getMention())
                    .collect(Collectors.joining(" ")))
                .flatMap(Message::delete);
    }
}
