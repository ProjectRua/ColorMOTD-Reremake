/*
 * Copyright (C) 2019 Bukkit Commons
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.andylizi.colormotdreremake.bungee;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.UUID;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.*;
import net.andylizi.colormotdreremake.common.Config;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.Favicon;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

@RequiredArgsConstructor
public class MotdListener implements Listener {
    private final BungeeMain plugin;

    private final LoadingCache<BufferedImage, Favicon> faviconCache = CacheBuilder.newBuilder()
            .weakKeys()
            .build(CacheLoader.from(Favicon::create));

    @EventHandler(priority = EventPriority.NORMAL)
    public void onServerListPing(ProxyPingEvent event) {
        String ip = event.getConnection().getAddress().getHostString();

        if(!plugin.getFirewall().canFlushMotd(ip)) //刷MOTD检查
            return;

        ServerPing res = event.getResponse();
        Config config = plugin.config();
        String motd = config.isMaintenanceMode() ? plugin.getBungeePlaceHolder().applyPlaceHolder(config.getMaintenanceModeMotd(),ip) : plugin.getBungeePlaceHolder().applyPlaceHolder(config.randomMotd(),ip);
        BufferedImage favicon = plugin.favicons().chooseFavicon(config.isMaintenanceMode());

        BaseComponent[] components = TextComponent.fromLegacyText(plugin.getBungeePlaceHolder().applyPlaceHolder(motd,ip));
        res.setDescriptionComponent(components.length == 1 ? components[0] : new TextComponent(components));
        res.setFavicon(favicon == null ? null : faviconCache.getUnchecked(favicon));

        if (!config.isShowPing()) {
            String onlineMsg = plugin.getBungeePlaceHolder().applyPlaceHolder(config.randomOnlineMsg(),ip);
            if (onlineMsg != null)
                res.setVersion(new ServerPing.Protocol(plugin.getBungeePlaceHolder().applyPlaceHolder( onlineMsg,ip), -1));
        }

        List<String> players = config.getPlayers();
        if (players != null && !players.isEmpty()) {
            res.setPlayers(new ServerPing.Players(0, ProxyServer.getInstance().getOnlineCount(), config.getPlayers().stream()
                    .map(line -> plugin.getBungeePlaceHolder().applyPlaceHolder(line,ip))
                    .map(line -> new ServerPing.PlayerInfo(line, UUID.randomUUID())).toArray(ServerPing.PlayerInfo[]::new)));
        }
    }
}
