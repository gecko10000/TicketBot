package gecko10000.TicketBot.commands;

import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import gecko10000.TicketBot.TicketBot;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CommandRegistry {

    private final Map<String, Command> commands = new ConcurrentHashMap<>();
    private final TicketBot bot;

    public CommandRegistry(TicketBot bot) {
        this.bot = bot;
        initCommands();
        bot.client.on(ChatInputInteractionEvent.class)
                .flatMap(e -> commands.get(e.getCommandName())
                        .handleCommand(e))
                .subscribe();
    }

    private void initCommands() {
        List.of(
                new TicketCreateCommand(bot),
                new TicketRenameCommand(bot),
                new TicketCloseCommand(bot),
                new TicketAddUserCommand(bot),
                new TicketRemoveUserCommand(bot),
                new ForgetUsernameCommand(bot)
        ).forEach(c -> commands.put(c.getName(), c));
    }

}
