package gecko10000.TicketBot.utils;

import discord4j.core.object.entity.User;

public class Utils {

    public static final String ZWSP = "\u200B";

    public static String smartS(long count) {
        return count == 1 ? "" : "s";
    }

    public static String userString(User user) {
        return user.getUsername() + "#" + user.getDiscriminator() + " (" + user.getId().asString() + ")";
    }

}
