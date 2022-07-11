package gecko10000.TicketBot;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ComponentInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.component.ActionComponent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.*;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import gecko10000.TicketBot.utils.Config;
import gecko10000.TicketBot.utils.Utils;
import org.simpleyaml.configuration.ConfigurationSection;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TicketManager {

    private final TicketBot bot;

    public TicketManager(TicketBot bot) {
        this.bot = bot;
    }

    private TextChannelCreateSpec buildChannel(int ticketNum, Member member, Snowflake everyone, Snowflake ticketSupport, Snowflake ticketManage) {
        return TextChannelCreateSpec.builder()
                .name(member.getUsername() + "-" + ticketNum)
                .addPermissionOverwrite(PermissionOverwrite.forRole(everyone, PermissionSet.none(), PermissionSet.of(Permission.VIEW_CHANNEL)))
                .addPermissionOverwrite(PermissionOverwrite.forMember(bot.client.getSelfId(), PermissionSet.of(Permission.VIEW_CHANNEL), PermissionSet.none()))
                .addPermissionOverwrite(PermissionOverwrite.forRole(ticketSupport, PermissionSet.of(Permission.VIEW_CHANNEL), PermissionSet.none()))
                .addPermissionOverwrite(PermissionOverwrite.forRole(ticketManage, PermissionSet.of(Permission.VIEW_CHANNEL), PermissionSet.none()))
                .addPermissionOverwrite(PermissionOverwrite.forMember(member.getId(), PermissionSet.of(Permission.VIEW_CHANNEL), PermissionSet.none()))
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
                .flatMap(g -> g.getEveryoneRole()
                        .map(r -> Tuples.of(g, r)))
                .map(t -> Tuples.of(
                        t.getT1(/*guild*/),
                        t.getT2(/*everyone role*/),
                        buildChannel(ticketNum, member, t.getT2().getId(), supportRole, manageRole)))
                .flatMap(t -> t.getT1(/*guild*/).createTextChannel(t.getT3())
                        .map(c -> Tuples.of(t.getT1(), t.getT2(), c)))
                .doOnNext(t -> bot.sql.insertTicket(t.getT3(/*channel*/).getId(), member.getId(), ticketNum))
                .flatMap(t -> t.getT1().getRoleById(supportRole)
                        .zipWith(t.getT1().getRoleById(manageRole))
                        .map(roles -> Tuples.of(t.getT3(), t.getT2(), roles.getT1(), roles.getT2())))
                .doOnNext(t -> System.out.println("Created ticket " + ticketNum + " for " + Utils.userString(member) + "."))
                .doOnNext(t -> bot.sql.syncTickets())
                .flatMap(chanAndRoles -> ticketIntroSequence(chanAndRoles.getT1(), member, /*roles*/ chanAndRoles.getT2(), chanAndRoles.getT3(), chanAndRoles.getT4()))
                .subscribe();
    }

    private Mono<Void> ticketIntroSequence(TextChannel channel, Member member, Entity... toPing) {
        return ghostPing(channel, Stream.concat(Stream.of(member), Stream.of(toPing)).toArray(Entity[]::new))
                .then(sendFirstMessage(channel, member))
                // need the message to edit later
                .flatMap(m -> askPanelUsername(channel, member)
                        .map(s -> Tuples.of(m, s)))
                .flatMap(t -> t.getT1()
                        .edit(MessageEditSpec.create().withEmbeds(addUsername(member, t.getT2()).build()))
                        .then(askTicketTopic(channel, member))
                        .map(topic -> Tuples.of(t.getT1(), t.getT2(), topic)))
                .flatMap(t -> t.getT1().edit(MessageEditSpec.create()
                        .withEmbeds(addTicketType(member, t.getT2(), t.getT3()).build()))
                        .thenReturn(t.getT3()))
                .flatMap(topic -> channel.edit().withName(topic + "-" + bot.sql.getTicketNumber(channel.getId())))
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

    private EmbedCreateSpec.Builder addUsername(Member member, String username) {
        return initialEmbed(member).addField("Panel Username", username.equals("") ? Config.get("messages.username.default") : username, true);
    }

    private EmbedCreateSpec.Builder addTicketType(Member member, String username, String type) {
        return addUsername(member, username).addField("Ticket Type", Utils.titleCase(type) + " Support", true);
    }

    private Mono<Message> sendFirstMessage(TextChannel channel, Member member) {
        return channel.createMessage(MessageCreateSpec.builder()
                .addEmbed(initialEmbed(member)
                        .build())
                .build());
    }

    private static final String NO_ACCOUNT = "ticket-no-user";

    private MessageCreateSpec buildPanelMessage() {
        return MessageCreateSpec.builder()
                .addEmbed(EmbedCreateSpec.builder()
                        .title(Config.<String>get("messages.username.title"))
                        .description(Config.<String>get("messages.username.description"))
                        .build())
                .addComponent(ActionRow.of(Button.primary(NO_ACCOUNT, ReactionEmoji.unicode(Config.get("messages.username.buttonEmoji")), "No panel account")))
                .build();
    }

    private Mono<String> askPanelUsername(TextChannel channel, Member member) {
        Mono<Message> messageMono = channel.createMessage(buildPanelMessage());
        Mono<String> tempListener = bot.client.on(MessageCreateEvent.class)
                .filter(e -> e.getMember().isPresent() && e.getMember().get().getId().equals(member.getId())) // correct member
                .flatMap(e -> Mono.zip(e.getMessage().getChannel(), Mono.just(e)))
                .filter(channelAndEvent -> channel.getId().equals(channelAndEvent.getT1().getId())) // correct member
                .map(Tuple2::getT2)
                .map(e -> Tuples.of(e, e.getMessage().getContent()))
                .flatMap(t -> t.getT1().getMessage().delete().thenReturn(t.getT2()))
                .timeout(Duration.ofHours(12))
                .onErrorResume(TimeoutException.class, e -> Mono.just(""))
                .next();
        Mono<String> buttonPressListener = bot.client.on(ButtonInteractionEvent.class)
                .filter(e -> e.getCustomId().equals(NO_ACCOUNT))
                .filter(e -> {
                    Optional<Member> m = e.getInteraction().getMember();
                    return m.isPresent() && m.get().getId().equals(member.getId());
                })
                .next()
                .timeout(Duration.ofHours(12))
                .onErrorResume(TimeoutException.class, e -> Mono.empty())
                .flatMap(ComponentInteractionEvent::deferEdit)
                .then(Mono.just(""));
        return messageMono.zipWith(tempListener.or(buttonPressListener))
                .flatMap(t -> t.getT1().delete().thenReturn(t.getT2())); // delete message once response is sent
    }

    private EmbedCreateFields.Field ticketTypeToField(String type) {
        return EmbedCreateFields.Field.of(Utils.titleCase(type), Config.get("ticketTypes." + type + ".description"), true);
    }

    private Button typeToButton(String s) {
        return Button.primary("ticket-type-" + s, ReactionEmoji.unicode(Config.get("ticketTypes." + s + ".emoji")), Utils.titleCase(s));
    }

    private String idToTicketType(String id) {
        final String prefix = "ticket-type-";
        return id.startsWith(prefix) ? id.substring(prefix.length()) : null;
    }

    private MessageCreateSpec buildTicketMessage() {
        EmbedCreateSpec.Builder embedSpec = EmbedCreateSpec.builder()
                .title(Config.<String>get("messages.topic.title"))
                .description(Config.<String>get("messages.topic.description"));
        Set<String> types = Config.getConfig().getConfigurationSection("ticketTypes").getKeys(false);
        for (String key : types) {
            embedSpec.addField(ticketTypeToField(key));
        }
        MessageCreateSpec.Builder msgSpec = MessageCreateSpec.builder()
                .addEmbed(embedSpec.build());
        List<ActionComponent> buttons = new ArrayList<>(types.size());
        for (String key : types) {
            buttons.add(typeToButton(key));
        }
        return msgSpec.addComponent(ActionRow.of(buttons)).build();
    }

    private Mono<String> askTicketTopic(TextChannel channel, Member member) {
        Mono<Message> messageMono = channel.createMessage(buildTicketMessage());
        Mono<String> getTicketTopic = bot.client.on(ButtonInteractionEvent.class)
                .filter(e -> {
                    Optional<Member> m = e.getInteraction().getMember();
                    return m.isPresent() && m.get().getId().equals(member.getId());
                })
                .map(ButtonInteractionEvent::getCustomId)
                .map(this::idToTicketType)
                .timeout(Duration.ofHours(12))
                .onErrorResume(TimeoutException.class, e -> Mono.just(Config.ticketTypes().get(0)))
                .next();
        return messageMono.zipWith(getTicketTopic)
                .flatMap(t -> t.getT1().delete().thenReturn(t.getT2()));
    }

}
