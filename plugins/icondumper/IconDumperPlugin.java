/*
 * Copyright (c) 2021, PH01L <phoil@osrsbox.com>
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
package net.runelite.client.plugins.icondumper;

import com.google.inject.Provides;
import java.io.IOException;
import java.io.File;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.game.ItemManager;

@Slf4j
@PluginDescriptor(
        name = "Icon Dumper",
        description = "Extract icons for items",
        tags = {"osrsbox", "items"},
        enabledByDefault = false
)

public class IconDumperPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private IconDumperConfig config;

    @Inject
    private ItemManager itemManager;

    @Provides
    IconDumperConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(IconDumperConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        log.debug(">>> Starting up IconDumperPlugin...");
    }

    @Override
    protected void shutDown() throws Exception
    {
        log.debug(">>> Shutting down IconDumperPlugin...");
    }

    @Subscribe
    public void onCommandExecuted(CommandExecuted commandExecuted)
    {
        switch (commandExecuted.getCommand())
        {
            case "icons":
            {
                dumpIcons();
                break;
            }
        }
    }

    private void dumpIcons()
    {
        log.debug(">>> Starting item icon dumper...");

        int start = config.startAtId();
        int end = config.endAtId();

        for(int itemId=start; itemId<end; itemId++)
        {
            log.debug("  > Current item ID: " + itemId);
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
}
