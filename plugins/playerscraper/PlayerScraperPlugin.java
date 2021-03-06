/*
 * Copyright (c) 2019, PH01L <phoil@osrsbox.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.playerscraper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Provides;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ObjectArrays;
import java.util.*;
import java.util.Objects;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javax.annotation.Nullable;
import java.util.regex.Pattern;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.inject.Provider;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.ConfigChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.PlayerMenuOptionClicked;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.util.Text;
import org.apache.commons.lang3.ArrayUtils;
import net.runelite.api.*;
import net.runelite.api.kit.KitType;

@Slf4j
@PluginDescriptor(
        name = "Player Scraper",
        description = "A simple plugin to scrape player(s) and save equipment to JSON files",
        tags = {"scrape", "player"},
        enabledByDefault = false,
        loadWhenOutdated = true
)
public class PlayerScraperPlugin extends Plugin
{
    private static final String LOOKUP = "Scrape";
    private static final String KICK_OPTION = "Kick";
    private static final ImmutableList<String> AFTER_OPTIONS = ImmutableList.of("Message", "Add ignore", "Remove friend", KICK_OPTION);
    private static final Pattern BOUNTY_PATTERN = Pattern.compile("<col=ff0000>You've been assigned a target: (.*)</col>");

    @Inject
    @Nullable
    private Client client;

    @Inject
    private Provider<MenuManager> menuManager;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private ScheduledExecutorService executor;

    @Inject
    private PlayerScraperConfig config;

    @Provides
    PlayerScraperConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(PlayerScraperConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        if (config.menuOption() && client != null)
        {
            menuManager.get().addPlayerMenuItem(LOOKUP);
        }
    }

    @Override
    protected void shutDown() throws Exception
    {
        if (client != null)
        {
            menuManager.get().removePlayerMenuItem(LOOKUP);
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (event.getGroup().equals("playerscraper"))
        {
            if (client != null)
            {
                menuManager.get().removePlayerMenuItem(LOOKUP);

                if (config.menuOption())
                {
                    menuManager.get().addPlayerMenuItem(LOOKUP);
                }
            }
        }
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event)
    {
        if (!config.menuOption())
        {
            return;
        }

        int groupId = WidgetInfo.TO_GROUP(event.getActionParam1());
        String option = event.getOption();

        if (groupId == WidgetInfo.FRIENDS_LIST.getGroupId() || groupId == WidgetInfo.CLAN_CHAT.getGroupId() ||
                groupId == WidgetInfo.CHATBOX.getGroupId() && !KICK_OPTION.equals(option) || //prevent from adding for Kick option (interferes with the raiding party one)
                groupId == WidgetInfo.RAIDING_PARTY.getGroupId() || groupId == WidgetInfo.PRIVATE_CHAT_MESSAGE.getGroupId())
        {
            boolean after;

            if (!AFTER_OPTIONS.contains(option))
            {
                return;
            }

            final MenuEntry lookup = new MenuEntry();
            lookup.setOption(LOOKUP);
            lookup.setTarget(event.getTarget());
            lookup.setType(MenuAction.RUNELITE.getId());
            lookup.setParam0(event.getActionParam0());
            lookup.setParam1(event.getActionParam1());
            lookup.setIdentifier(event.getIdentifier());

            insertMenuEntry(lookup, client.getMenuEntries());
        }
    }

    private void insertMenuEntry(MenuEntry newEntry, MenuEntry[] entries)
    {
        MenuEntry[] newMenu = ObjectArrays.concat(entries, newEntry);
        int menuEntryCount = newMenu.length;
        ArrayUtils.swap(newMenu, menuEntryCount - 1, menuEntryCount - 2);
        client.setMenuEntries(newMenu);
    }

    @Subscribe
    public void onPlayerMenuOptionClicked(PlayerMenuOptionClicked event)
    {
        if (event.getMenuOption().equals(LOOKUP))
        {
            playerScraper(Text.removeTags(event.getMenuTarget()));
        }
    }

    private void playerScraper(String targetPlayerName)
    {
        executor.execute(() ->
        {
            // Start the actual plugin code
            log.debug(">>> Starting playerScraper...");

            // Get a list of all players in the client cache
            List<Player> players = client.getPlayers();

            if (config.allPlayers())
            {
                log.debug(">>> Mode: Get all players in client cache ..");

                for (Player player : players)
                {
                    String finalPlayerName = player.getName();
                    log.debug(">>> Player name: {}", finalPlayerName);

                    Optional<Player> targetPlayer = players.stream()
                            .filter(Objects::nonNull)
                            .filter(p -> p.getName().equals(finalPlayerName)).findFirst();

                    if (targetPlayer.isPresent())
                    {
                        // Get the target player and process
                        Player p = targetPlayer.get();
                        processPlayer(p);
                    }
                }
            }
            else
            {
                log.debug(">>> Mode: Get specific player...");
                log.debug(">>> Player name: {}", targetPlayerName);

                String tempPlayerName = Text.removeTags(targetPlayerName);

                // The player menu uses a non-breaking space in the player name, we need to replace this to compare
                // against the playerName in the player cache.
                String finalPlayerName = tempPlayerName.replace('\u00A0', ' ');

                // Get the target player
                Optional<Player> targetPlayer = players.stream()
                        .filter(Objects::nonNull)
                        .filter(p -> p.getName().equals(finalPlayerName)).findFirst();

                // First check if the target player is in the cache
                if (targetPlayer.isPresent())
                {
                    // Get the target player and process
                    Player p = targetPlayer.get();
                    processPlayer(p);
                }
            }
        });
    }

    private void processPlayer(Player p)
    {
        TargetPlayer targetPlayer = new TargetPlayer();
        targetPlayer.populateTargetPlayer(p, client);

        // Create output directory if it doesn't exist
        String directory = "playerscraper";
        File dir = new File(directory);
        if (!dir.exists()) dir.mkdirs();

        // Initialize Gson builder, then generate JSON
        Gson gson = new GsonBuilder().create();
        String json = gson.toJson(targetPlayer);

        // Save items_metadata.json file will all item metadata
        String summaryFileOut = targetPlayer.name + ".json";
        try (FileWriter fw = new FileWriter(summaryFileOut))
        {
            fw.write(json);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
