package de.btegermany.utilities.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class PlotsCommand extends Command {

    public PlotsCommand() {
        super("plotsystem");
    }

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        if (!(commandSender instanceof ProxiedPlayer player)) {
            commandSender.sendMessage(new ComponentBuilder("ᾠ §cDiesen §cCommand §ckönnen §cnur §cSpieler §cnutzen.").create());
            return;
        }

        ServerInfo serverInfo = ProxyServer.getInstance().getServerInfo("Plot-1");

        if (serverInfo == null) {
            player.sendMessage(new ComponentBuilder("ᾠ §cDer §cPlotserver §cist §cleider §cnicht §cverfügbar.").create());
            return;
        }

        player.connect(serverInfo);
    }

}
