package gecko10000.TicketBot;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.MessageEditSpec;
import discord4j.core.spec.TextChannelCreateSpec;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import gecko10000.TicketBot.utils.Config;
import gecko10000.TicketBot.utils.Utils;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public void openTicket(Member member) {
        // do not open more tickets if at max
        if (!canOpenTicket(member)) {
            return;
        }
        int ticketNum = Config.incrementTicketCount();
        Snowflake supportRole = Config.getSF("ticketSupportRole"), manageRole = Config.getSF("ticketManageRole");
        member.getGuild()
                // use tuple/Mono zip to carry role to ghostPing and guild to createTextChannel
                .flatMap(g -> Mono.zip(
                        g.getEveryoneRole(),
                        Mono.just(g)))
                .map(t -> Tuples.of(
                        buildChannel(ticketNum, member.getId(), t.getT1().getId(), supportRole, manageRole),
                        t.getT1(/*everyone role*/),
                        t.getT2(/*guild*/)))
                .flatMap(t -> Mono.zip(
                        t.getT3(/*guild*/).createTextChannel(t.getT1(/*spec*/)),
                        Mono.just(t.getT2(/*everyone role*/)),
                        Mono.just(t.getT3(/*guild*/))))
                .flatMap(t -> {
                    bot.sql.insertTicket(t.getT1(/*channel*/).getId(), member.getId(), ticketNum);
                    return Mono.zip(
                            Mono.just(t.getT1(/*channel*/)),
                            Mono.just(t.getT2(/*everyone role*/)),
                            t.getT3(/*guild*/).getRoleById(supportRole),
                            t.getT3(/*guild*/).getRoleById(manageRole));
                })
                .flatMap(chanAndRoles -> {
                    System.out.println("Created ticket " + ticketNum + " for " + Utils.userString(member) + ".");
                    bot.sql.syncTickets();
                    return ticketIntroSequence(chanAndRoles.getT1(), member, /*roles*/ chanAndRoles.getT2(), chanAndRoles.getT3(), chanAndRoles.getT4());
                })
                .subscribe();
    }

    private Mono<Void> ticketIntroSequence(TextChannel channel, Member member, Entity... toPing) {
        return ghostPing(channel, Stream.concat(Stream.of(member), Stream.of(toPing)).toArray(Entity[]::new))
                .then(sendFirstMessage(channel, member))
                .flatMap(m -> Mono.zip(Mono.just(m), askPanelUsername(channel, member)))
                .flatMap(t -> t.getT1().edit(MessageEditSpec.create()
                        .withEmbeds(addUsername(initialEmbed(member), t.getT2()).build())))
                .then();
    }

    private Mono<Void> ghostPing(TextChannel channel, Entity... usersAndRoles) {
        return channel
                .createMessage(Arrays.stream(usersAndRoles)
                    .map(e -> e instanceof User user ? user.getMention() : ((Role) e).getMention())
                    .collect(Collectors.joining(" ")))
                .flatMap(Message::delete);
    }

    private EmbedCreateSpec.Builder initialEmbed(Member member) {
        return EmbedCreateSpec.builder()
                .description(String.format(Config.<String>get("messages.welcome"), member.getMention()));
    }

    private EmbedCreateSpec.Builder addUsername(EmbedCreateSpec.Builder b, String username) {
        return b.addField("Panel Username", username.equals("") ? "No account" : username, true);
    }

    private Mono<Message> sendFirstMessage(TextChannel channel, Member member) {
        return channel.createMessage(MessageCreateSpec.builder()
                .addEmbed(initialEmbed(member)
                        .build())
                .build());
    }

    private MessageCreateSpec buildPanelMessage() {
        return MessageCreateSpec.builder()
                .addEmbed(EmbedCreateSpec.builder()
                        .title(Config.<String>get("messages.username.title"))
                        .description(Config.<String>get("messages.username.description"))
                        .build())
                .addComponent(ActionRow.of(Button.primary("noUser", ReactionEmoji.unicode(Config.get("messages.username.buttonEmoji")), "No panel account")))
                .build();
    }

    private Mono<String> askPanelUsername(TextChannel channel, Member member) {
        Mono<Message> messageMono = channel.createMessage(buildPanelMessage());
        Mono<String> tempListener = bot.client.on(MessageCreateEvent.class)
                .filter(e -> e.getMember().isPresent() && e.getMember().get().getId().equals(member.getId())) // correct member
                .flatMap(e -> Mono.zip(e.getMessage().getChannel(), Mono.just(e)))
                .filter(channelAndEvent -> channel.getId().equals(channelAndEvent.getT1().getId())) // correct member
                .map(Tuple2::getT2)
                .flatMap(e -> {
                    String content = e.getMessage().getContent();
                    return e.getMessage().delete().thenReturn(content);
                })
                .timeout(Duration.ofHours(12))
                .onErrorResume(TimeoutException.class, e -> Mono.empty())
                .next();
        Mono<String> buttonPressListener = bot.client.on(ButtonInteractionEvent.class)
                .filter(e -> e.getCustomId().equals("noUser"))
                .map(e -> "")
                .timeout(Duration.ofHours(12))
                .onErrorResume(TimeoutException.class, e -> Mono.empty())
                .next();
        return messageMono.zipWith(tempListener.or(buttonPressListener))
                .flatMap(t -> t.getT1().delete().thenReturn(t.getT2())); // delete message once response is sent
    }

}
