package gecko10000.TicketBot.utils;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.User;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Utils {

    public static final String ZWSP = "\u200B";

    public static String smartS(long count) {
        return count == 1 ? "" : "s";
    }

    public static String userString(User user) {
        return user.getUsername() + "#" + user.getDiscriminator() + " (" + user.getId().asString() + ")";
    }

    public static String titleCase(String string) {
        return Arrays.stream(string.split(" "))
                .map(s -> Character.toUpperCase(s.charAt(0)) + s.substring(1))
                .collect(Collectors.joining(" "));
    }

    public static String userMention(Snowflake id) {
        return "<@" + id.asString() + ">";
    }

}
