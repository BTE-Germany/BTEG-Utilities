package dev.btedach.dachutility.utils;

import dev.btedach.dachutility.DACHUtility;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.File;

public final class Constants {

    public static final String prefix = "ᾠ";//+ NamedTextColor.GRAY;//NamedTextColor.DARK_GRAY+"["+NamedTextColor.YELLOW+"MB"+NamedTextColor.DARK_GRAY+"] "+NamedTextColor.GRAY;

    public static final String staffPrefix = NamedTextColor.DARK_GRAY+"["+NamedTextColor.YELLOW+"SC"+NamedTextColor.DARK_GRAY+"] "+NamedTextColor.GRAY;

    public static final String builderPrefix = NamedTextColor.DARK_GRAY+"["+NamedTextColor.YELLOW+"BC"+NamedTextColor.DARK_GRAY+"] "+NamedTextColor.GRAY;

    public static final String mutedChatPrefix = NamedTextColor.DARK_GRAY+"["+NamedTextColor.YELLOW+"TC"+NamedTextColor.DARK_GRAY+"] "+NamedTextColor.GRAY;
    
    public static final String reportPrefix = NamedTextColor.DARK_GRAY+"["+NamedTextColor.YELLOW+"Report System"+NamedTextColor.DARK_GRAY+"] "+NamedTextColor.GRAY;

    public static final String JAR_PATH = new File(DACHUtility.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile().getAbsolutePath()+"\\";

    public static final String DELIMITER = ":";

    public static final String ping = NamedTextColor.DARK_GRAY+"["+NamedTextColor.YELLOW+"Ping"+NamedTextColor.DARK_GRAY+"] "+NamedTextColor.GRAY;

}
