package gecko10000.TicketBot;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.MessageCreateSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;

public class TicketButtonManager {

    private final GatewayDiscordClient client;

    public TicketButtonManager(GatewayDiscordClient client) {
        this.client = client;
        sendNewMessage().subscribe(this::deletePreviousMessages);
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
        Mono<GuildMessageChannel> channelMono = client.getChannelById(Snowflake.of(Config.getProperty("supportChannel"))).cast(GuildMessageChannel.class);
        Mono<User> selfMono = client.getSelf();
        channelMono.zipWith(selfMono).subscribe(chanUserTuple -> {
            Mono<Long> countMono = getMessagesToDelete(chanUserTuple.getT1(), chanUserTuple.getT2().getId(), newEmbed).count();
            Flux<Snowflake> deleteFlux = getMessagesToDelete(chanUserTuple.getT1(), chanUserTuple.getT2().getId(), newEmbed);
            countMono.subscribe(count -> {
                if (count == 1) {
                    deleteFlux
                            .subscribe(s -> client.getMessageById(chanUserTuple.getT1().getId(), s)
                                    .subscribe(m -> m.delete().subscribe()));
                } else {
                    chanUserTuple.getT1().bulkDelete(deleteFlux).subscribe();
                }
                System.out.printf("Deleted %d old messages.\n", count);
            });
        });
    }

    private MessageCreateSpec composeMessage() {
        return MessageCreateSpec.builder()
                .content("Make a ticket NOW!")
                .addComponent(ActionRow.of(Button.danger("1", "Click me!")))
                .build();
    }

    private Mono<Snowflake> sendNewMessage() {
        Snowflake channelId = Snowflake.of(Config.getProperty("supportChannel"));
        return client.getChannelById(channelId)
                .cast(GuildMessageChannel.class)
                .flatMap(channel -> channel.createMessage(composeMessage()))
                .map(Message::getId);
    }

}
