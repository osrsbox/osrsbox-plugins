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
package net.runelite.client.plugins.metadatadumper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Provides;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.NPCComposition;
import net.runelite.api.events.CommandExecuted;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.game.ItemManager;

@Slf4j
@PluginDescriptor(
        name = "Metadata Dumper",
        description = "Extract metadata for items/npcs from the OSRS Cache",
        tags = {"osrsbox", "metadata", "scraper", "items", "npcs"},
        enabledByDefault = false
)

public class MetadataDumperPlugin extends Plugin
{
    private final Map<Integer, ItemMetadata> items = new HashMap<>();
    private final Map<Integer, NpcMetadata> npcs = new HashMap<>();

    @Inject
    private Client client;

    @Inject
    private MetadataDumperConfig config;

    @Inject
    private ItemManager itemManager;

    @Provides
    MetadataDumperConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(MetadataDumperConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        log.debug(">>> Starting up MetadataDumperPlugin...");
    }

    @Override
    protected void shutDown() throws Exception
    {
        log.debug(">>> Shutting down MetadataDumperPlugin...");
    }

    @Subscribe
    public void onCommandExecuted(CommandExecuted commandExecuted)
    {
        switch (commandExecuted.getCommand())
        {
            case "items":
            {
                dumpItemMetadata();
                break;
            }
            case "npcs":
            {
                dumpNpcMetadata();
                break;
            }
        }
    }

    private void dumpItemMetadata()
    {
        log.debug(">>> Starting item metadata dump...");

        int start = config.startConfig();
        int end = config.endConfig();

        for(int itemId=start; itemId<end; itemId++)
        {
            log.debug("  > Current item ID: " + itemId);

            // Fetch the item composition
            ItemComposition itemComposition = client.getItemDefinition(itemId);
            if (itemComposition != null)
            {
                if (itemComposition.getName().equalsIgnoreCase("NULL"))
                {
                    // Skip items with a null name
                    continue;
                }
                // Parse the ItemDefinition to an ItemMetadata object
                ItemMetadata itemMetadata = new ItemMetadata();
                itemMetadata.populateItemMetadata(itemComposition);
                items.put(itemId, itemMetadata);
            }
            // If user wants to dump item icon image
            if (config.dumpItemIcons())
            {
                // Try to save the item icon
                try
                {
                    String directory = "items-icons";
                    File dir = new File(directory);
                    if (!dir.exists()) dir.mkdirs();
                    String outName = "items-icons/"  + itemId + ".png";
                    File outputFile = new File(outName);
                    BufferedImage iconImage = itemManager.getImage(itemId);
                    ImageIO.write(iconImage, "png", outputFile);
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }

        // Initialize Gson builder, then generate JSON
        Gson gson = new GsonBuilder().create();
        String json = gson.toJson(items);

        // Save items_metadata.json file will all item metadata
        String summaryFileOut = "items-metadata.json";
        try (FileWriter fw = new FileWriter(summaryFileOut))
        {
            fw.write(json);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void dumpNpcMetadata()
    {
        log.debug(">>> Starting npc metadata dump...");

        int start = config.startConfig();
        int end = config.endConfig();

        for(int npcId=start; npcId<end; npcId++)
        {
            log.debug("  > Current NPC ID: " + npcId);

            // Fetch the item composition
            NPCComposition npcComposition = client.getNpcDefinition(npcId);
            if (npcComposition != null)
            {
                if (npcComposition.getName().equalsIgnoreCase("NULL"))
                {
                    // Skip npcs with a null name
                    continue;
                }
                // Parse the NPCComposition to an NpcMetadata object
                NpcMetadata npcMetadata = new NpcMetadata();
                npcMetadata.populateNpcMetadata(npcComposition);
                npcs.put(npcId, npcMetadata);
            }
        }

        // Initialize Gson builder, then generate JSON
        Gson gson = new GsonBuilder().create();
        String json = gson.toJson(npcs);

        // Save items_metadata.json file will all item metadata
        String summaryFileOut = "npcs-metadata.json";
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
