package dev.btedach.dachutility.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;


public final class Constants {

    public static final Component prefixComponent = Component.text("ᾠ ");

    public static final Component reportPrefix = Component.text("[", NamedTextColor.DARK_GRAY)
            .append(Component.text("Report System", NamedTextColor.YELLOW))
            .append(Component.text("]", NamedTextColor.DARK_GRAY));

    public static final Component ping = Component.text("[", NamedTextColor.DARK_GRAY)
            .append(Component.text("Ping", NamedTextColor.YELLOW))
            .append(Component.text("] ", NamedTextColor.DARK_GRAY));

}
