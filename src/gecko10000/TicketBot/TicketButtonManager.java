package gecko10000.TicketBot;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.util.Color;
import gecko10000.TicketBot.utils.Config;
import org.simpleyaml.configuration.ConfigurationSection;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;

public class TicketButtonManager {

    private static final String MAKE_TICKET = "ticket-create";

    private final TicketBot bot;

    public TicketButtonManager(TicketBot bot) {
        this.bot = bot;
        sendNewMessage()
                .doOnNext(this::deletePreviousMessages)
                .subscribe();
        bot.client.on(ButtonInteractionEvent.class, e -> {
            if (!e.getCustomId().equals(MAKE_TICKET)) return Mono.empty();
            e.getInteraction().getMember().ifPresent(bot.ticketManager::openTicket);
            return e.deferEdit();
        }).subscribe();
    }

    private Flux<Snowflake> getMessagesToDelete(MessageChannel channel, Snowflake id, Snowflake newEmbed) {
        return channel.getMessagesBefore(newEmbed)
                .filter(d -> {
                    Optional<User> author = d.getAuthor();
                    return author.isPresent() && author.get().getId().equals(id);
                })
                .map(Message::getId);
    }

    private void deletePreviousMessages(Snowflake newEmbed) {
        Mono<GuildMessageChannel> channelMono = bot.client.getChannelById(Config.getSF("supportChannel")).cast(GuildMessageChannel.class);
        Mono<User> selfMono = bot.client.getSelf();
        channelMono.zipWith(selfMono).flatMap(chanUserTuple -> {
            Mono<Long> countMono = getMessagesToDelete(chanUserTuple.getT1(), chanUserTuple.getT2().getId(), newEmbed).count();
            Flux<Snowflake> deleteFlux = getMessagesToDelete(chanUserTuple.getT1(), chanUserTuple.getT2().getId(), newEmbed);
            return countMono.flatMap(count -> {
                if (count == 1) {
                    return deleteFlux
                            .flatMap(s -> bot.client.getMessageById(chanUserTuple.getT1().getId(), s))
                            .flatMap(Message::delete)
                            .next();
                } else {
                    return chanUserTuple.getT1().bulkDelete(deleteFlux).next();
                }
            });
        }).subscribe();
    }

    private MessageCreateSpec composeMessage() {
        ConfigurationSection embedCfg = Config.getConfig().getConfigurationSection("embed");
        return MessageCreateSpec.builder()
                .addEmbed(EmbedCreateSpec.builder()
                        .color(Color.of(Integer.parseInt(embedCfg.getString("color"), 16)))
                        .title(embedCfg.getString("title"))
                        .thumbnail(embedCfg.getString("image"))
                        .description(embedCfg.getString("description"))
                        .build())
                .addComponent(ActionRow.of(Button.primary(
                        MAKE_TICKET, ReactionEmoji.unicode(embedCfg.getString("button.emoji")), embedCfg.getString("button.text"))))
                .build();
    }

    private Mono<Snowflake> sendNewMessage() {
        Snowflake channelId = Config.getSF("supportChannel");
        return bot.client.getChannelById(channelId)
                .cast(GuildMessageChannel.class)
                .flatMap(channel -> channel.createMessage(composeMessage()))
                .map(Message::getId);
    }

}
