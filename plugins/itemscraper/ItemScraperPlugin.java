/*
 * Copyright (c) 2019, PH01L <phoil@osrsbox.com>
 * All rights reserved.
 * 
 * NOTE: This plugin requires the runelite-client/pom.xml file to be modified:
 *	<dependencies>
 *      <dependency>
 *			<groupId>com.googlecode.json-simple</groupId>
 *			<artifactId>json-simple</artifactId>
 *			<version>1.1.1</version>
 *		</dependency>
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
package net.runelite.client.plugins.itemscraper;

import lombok.extern.slf4j.Slf4j;
import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.events.CommandExecuted;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.game.ItemManager;
import java.awt.image.BufferedImage;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;
import javax.imageio.ImageIO;
import java.util.Arrays;
import org.json.simple.JSONObject;

@Slf4j
@PluginDescriptor(
        name = "Item Scraper",
        description = "Scrape every item from the OSRS Cache",
        tags = {"scraper", "item"},
        enabledByDefault = false
)

public class ItemScraperPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private ItemScraperConfig config;

    @Inject
    private ItemManager itemManager;

    @Provides
    ItemScraperConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(ItemScraperConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        log.debug(">>> Starting up ItemScraperPlugin...");
    }

    @Override
    protected void shutDown() throws Exception
    {
        log.debug(">>> Shutting down ItemScraperPlugin...");
    }

    @Subscribe
    public void onCommandExecuted(CommandExecuted commandExecuted)
    {
        switch (commandExecuted.getCommand())
        {
            case "dump":
            {
                dumpData();
                break;
            }
        }
    }

    private void dumpData()
    {
        log.debug(">>> Lets dump some data...");

        // items_summary.json
        JSONObject summaryJSON = new JSONObject();
        // items_scraper.json
        JSONObject scraperJSON = new JSONObject();

        int start = config.startConfig();
        int end = config.endConfig();

        for(int itemID=start; itemID<end; itemID++)
        {
            // For each potential item:
            // 1) Save ItemDefinition as a JSON object
            // 2) Save Item image as a PNG file
            log.debug(">>> Current itemID: " + itemID);

            // Fetch the item composition
            ItemComposition itemComposition = client.getItemDefinition(itemID);
            if (itemComposition != null)
            {
                if (itemComposition.getName() == "null" || itemComposition.getName() == "Null" || itemComposition.getName() == null)
                {
                    // Skip any items where name in null
                    continue;
                }

                //////////////////////////////////////////////////
                // Define first JSON Objects structure: items_summary.json
                // {
                //     "0":{
                //          "name":"Dwarf remains",
                //          "id":0
                //     },
                //     "1":{
                //          "name":"Toolkit",
                //          "id":1
                //     },
                //     ...
                // }
                JSONObject itemSummaryJSON = new JSONObject();

                // Append current item in JSON format to summary.json array
                // This only includes item ID and item name
                // For example: "0":{"name":"Dwarf remains","id":0}
                itemSummaryJSON.put("id", itemComposition.getId());
                itemSummaryJSON.put("name", itemComposition.getName());
                summaryJSON.put(itemID, itemSummaryJSON);

                //////////////////////////////////////////////////
                // Define second JSON Objects structure: items_scraper.json
                //  {
                //        "id":0
                //        "name":"Dwarf remains",
                //        "members":true,
                //        "tradeable":false,
                //        "stackable":false,
                //        "noted":false,
                //        "noteable":false,
                //        "linked_id": null,
                //        "equipable":false,
                //        "cost":1,
                //        "lowalch":0,
                //        "highalch":0,
                //  }
                JSONObject itemJSON = new JSONObject();

                // Populate JSON with everything useful

                // Start by fetching basic properties directly from cache
                itemJSON.put("id", itemComposition.getId());
                itemJSON.put("name", itemComposition.getName());
                itemJSON.put("members", itemComposition.isMembers());
                itemJSON.put("tradeable_on_ge", itemComposition.isTradeable());
                itemJSON.put("stackable", itemComposition.isStackable());

                // Determine if item is noted:
                // According to RuneLite API...
                // -1 will be returned if the item is not noted
                // 799 with be returned if the item is noted
                if (itemComposition.getNote() == 799)
                {
                    itemJSON.put("noted", true);
                }
                else if (itemComposition.getNote() == -1)
                {
                    itemJSON.put("noted", false);
                }
                else
                {
                    itemJSON.put("noted", false);
                }

                // Determine if item is notable:
                if (itemComposition.getNote() == 799)
                {
                    // If the item itself is noted, it must be notable!
                    itemJSON.put("noteable", true);
                }
                else if (itemComposition.getLinkedNoteId() != -1)
                {
                    // If the item has a linked note ID, it must be notable!
                    itemJSON.put("noteable", true);
                }
                else
                {
                    itemJSON.put("noteable", false);
                }

                // Populate linked item ID using getLinkedNoteId. Calling the method:
                // 1. On a noted item -> the ID of the item in unnoted form
                // 2. On an unnoted item -> the ID of the item in noted form
                if (itemComposition.getLinkedNoteId() == -1)
                {
                    itemJSON.put("linked_id", null);
                }
                else
                {
                    itemJSON.put("linked_id", itemComposition.getLinkedNoteId());
                }

                // Populate placeholder boolean (is the item ID a placeholder)
                // 14401 if placeholder, -1 otherwise
                if (itemComposition.getPlaceholderTemplateId() == 14401)
                {
                    itemJSON.put("placeholder", true);
                }
                else
                {
                    itemJSON.put("placeholder", false);
                }

                // Determine if item is equipable:
                String[] inventoryActions = itemComposition.getInventoryActions();
                // If inventoryActions contains "Wear" or "Wield" it is deemed equipable
                if (inventoryActions != null)
                {
                    if (Arrays.asList(inventoryActions).contains("Wear"))
                    {
                        itemJSON.put("equipable", true);
                    }
                    else if (Arrays.asList(inventoryActions).contains("Wield"))
                    {
                        itemJSON.put("equipable", true);
                    }
                    else if (Arrays.asList(inventoryActions).contains("Equip"))
                    {
                        itemJSON.put("equipable", true);
                    }
                    else
                    {
                        itemJSON.put("equipable", false);
                    }
                }
                else
                {
                    itemJSON.put("equipable", false);
                }

                // Determine cost, then lowalch and highalch from the cost
                itemJSON.put("cost", itemComposition.getPrice());
                int lowAlch = (int)Math.floor(itemComposition.getPrice() * 0.4);
                itemJSON.put("lowalch", lowAlch);
                int highAlch = (int)Math.floor(itemComposition.getPrice() * 0.6);
                itemJSON.put("highalch", highAlch);

                // Append item to itemscraper object
                scraperJSON.put(itemID, itemJSON);

                // If user wants to dump item icon image
                if (config.dumpItemIcons())
                {
                    // Try to save the item icon
                    try
                    {
                        String directory = "items-icons";
                        File dir = new File(directory);
                        if (!dir.exists()) dir.mkdirs();
                        String outName = "items-icons/"  + itemID + ".png";
                        File outputFile = new File(outName);
                        BufferedImage iconImage = itemManager.getImage(itemID);
                        ImageIO.write(iconImage, "png", outputFile);
                    } catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }
        // Save items_summary.json file will all items
        String summaryFileOut = "items-summary.json";
        try (FileWriter file = new FileWriter(summaryFileOut))
        {
            file.write(summaryJSON.toJSONString());
            file.flush();

        } catch (IOException e)
        {
            e.printStackTrace();
        }
        // Save items_scraper.json file will all items
        String itemscaperFileOut = "items-scraper.json";
        try (FileWriter file = new FileWriter(itemscaperFileOut))
        {
            file.write(scraperJSON.toJSONString());
            file.flush();

        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
