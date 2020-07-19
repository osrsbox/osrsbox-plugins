/*
 * Copyright (c) 2020, PH01L <phoil@osrsbox.com>
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
package net.runelite.client.plugins.chatdumper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Provides;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Calendar;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import lombok.extern.slf4j.Slf4j;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.api.events.CommandExecuted;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.MessageNode;

@Slf4j
@PluginDescriptor(
        name = "Chat Dumper",
        description = "Extract chat messages and save to an external JSON file",
        tags = {"osrsbox", "chat", "scraper"},
        enabledByDefault = false
)

public class ChatDumperPlugin extends Plugin
{
    private final ArrayList<ChatMessageData> messages = new ArrayList<ChatMessageData>();

    @Inject
    private Client client;

    @Inject
    private ChatDumperConfig config;

    @Provides
    ChatDumperConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(ChatDumperConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        log.debug(">>> Starting up ChatDumper...");
    }

    @Override
    protected void shutDown() throws Exception
    {
        log.debug(">>> Shutting down ChatDumper...");
        messages.clear();
    }

    @Subscribe
    public void onCommandExecuted(CommandExecuted commandExecuted)
    {
        switch (commandExecuted.getCommand())
        {
            case "csave":
            {
                // When command is found, dump data to JSON and clear array
                saveChatMessageData();
                messages.clear();
                break;
            }
        }
    }

	@Subscribe(priority = -2) // run after ChatMessageManager
	public void onChatMessage(ChatMessage chatMessage)
	{
        final MessageNode messageNode = chatMessage.getMessageNode();
        
        // If the config states only public, skip other chat types
        // This is only really included as an example to filter chat types
        if (config.saveOnlyPublicChat())
        {
            if (messageNode.getType().name() != "PUBLICCHAT")
            {
                return;
            }
        }
        
        // Create new chat message object, populate and add to array
        ChatMessageData chatMessageData = new ChatMessageData();
        chatMessageData.populateChatMessageData(messageNode, client.getWorld());
        messages.add(chatMessageData);
	}

    private void saveChatMessageData()
    {
        // Get timestamp for a unqiue file name
        String pattern = "yyyy-MM-dd-HH:mm:ss";
        DateFormat df = new SimpleDateFormat(pattern);
        Date today = Calendar.getInstance().getTime();
        String fileOut = df.format(today);
        fileOut = fileOut + ".json";

        // Create JSON export
        Gson gson = new GsonBuilder().create();
        String json = gson.toJson(messages);

        // Save the JSON file
        try (FileWriter fw = new FileWriter(fileOut))
        {
            fw.write(json);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
