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
package net.runelite.client.plugins.npclocations;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Provides;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.game.ItemManager;

@Slf4j
@PluginDescriptor(
        name = "NPC Locations",
        description = "Scrape NPC locations",
        tags = {"osrsbox", "scraper", "npcs"},
        enabledByDefault = false
)

public class NpcLocationsPlugin extends Plugin
{
    private final Map<Integer, NpcLocation> npcs = new HashMap<>();

    @Inject
    private Client client;

    @Inject
    private NpcLocationsConfig config;

    @Inject
    private ItemManager itemManager;

    @Provides
    NpcLocationsConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(NpcLocationsConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        log.debug(">>> Starting up NpcLocationsPlugin...");
    }

    @Override
    protected void shutDown() throws Exception
    {
        log.debug(">>> Shutting down NpcLocationsPlugin...");
        npcs.clear();
    }

    @Subscribe
    public void onCommandExecuted(CommandExecuted commandExecuted)
    {
        switch (commandExecuted.getCommand())
        {
            case "dumpnpcs":
            {
                dumpsNpcs();
                break;
            }
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (config.scrapeNpcLocation())
        {
            for (NPC npc : client.getNpcs())
            {
                // Return null NPCs
                if (npc == null || npc.getName() == null)
                {
                    return;
                }

                // Get the NPC index
                int npcIndex = npc.getIndex();

                // If the NPC is known, append location
                // Else, create NPC
                if (npcs.containsKey(npcIndex))
                {
                    NpcLocation npcLocation = npcs.get(npcIndex);
                    List<Integer> location = determineCoordinates(npc);

                    boolean saved = false;
                    for (List meow : npcLocation.npcWorldLocations)
                    {
                        if (location.equals(meow))
                        {
                            saved = true;
                            break;
                        }
                    }
                    if (!saved)
                    {
                        log.debug(">>> Adding NPC: " + npc.getName());
                        npcLocation.npcWorldLocations.add(location);
                    }
                }
                else
                {
                    log.debug(">>> Adding NPC: " + npc.getName());
                    NpcLocation npcLocation = new NpcLocation(npc);
                    npcs.putIfAbsent(npcIndex, npcLocation);
                    List<Integer> location = determineCoordinates(npc);
                    npcLocation.npcWorldLocations.add(location);
                }
            }
        }
    }

    private List<Integer> determineCoordinates(NPC npc)
    {
        WorldPoint wp = npc.getWorldLocation();
        int x = wp.getX();
        int y = wp.getY();
        int p = wp.getPlane();
        List<Integer> location = new ArrayList<>();
        location.add(x);
        location.add(y);
        location.add(p);
        return location;
    }

    private void dumpsNpcs()
    {
        log.debug(">>> dumpsNpcs...");

        // Initialize Gson builder, then generate JSON
        Gson gson = new GsonBuilder().create();
        String json = gson.toJson(npcs);

        // Save items_metadata.json file will all item metadata
        String summaryFileOut = "npcs-locations.json";
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
