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
package net.runelite.client.plugins.bagspace;

import lombok.Getter;
import java.awt.Color;
import javax.inject.Inject;
import net.runelite.api.Item;
import net.runelite.api.ItemID;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.ImageUtil;

@PluginDescriptor(
        name = "Bag Space",
        description = "Display the current bag (inventory) space",
        tags = {"osrsbox", "bag", "inventory"}
)
public class BagSpacePlugin extends Plugin
{
    private BagSpaceCounter counter;

    @Inject
    private Client client;
    
	@Inject
	private ItemManager itemManager;

	@Inject
	private InfoBoxManager infoBoxManager;

    @Getter
    private int bagSpaceCount;

    @Override
    protected void startUp() throws Exception
    {
        addCounter();
        determineBagSpace();
    }

    @Override
    protected void shutDown() throws Exception
    {
        bagSpaceCount = 0;
        removeCounter();
    }

	Color getBagStateColor()
	{
		if (bagSpaceCount < 20)
		{
			return Color.green;
		}
		else if (bagSpaceCount < 28)
		{
			return Color.orange;
		}

		return Color.red;
	}

    @Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
        ItemContainer container = event.getItemContainer();

        if (container == client.getItemContainer(InventoryID.INVENTORY))
		{
            determineBagSpace();
        }
    }
    
    private void determineBagSpace()
    {
        bagSpaceCount = 0;
        ItemContainer container = client.getItemContainer(InventoryID.INVENTORY);

        if (container == null)
        {
            return;
        }

        Item[] items = container.getItems();

        for (int i = 0; i < items.length; i++)
        {
            if (items[i].getId() != -1)
            {
                bagSpaceCount++;
            }
        }
    }

    private void addCounter()
    {
        if (counter != null)
        {
            return;
        }

        counter = new BagSpaceCounter(itemManager.getImage(ItemID.SANDBAG), this);
        counter.setTooltip("Bag space");
        infoBoxManager.addInfoBox(counter);
    }

    private void removeCounter()
    {
        if (counter == null)
        {
            return;
        }

        infoBoxManager.removeInfoBox(counter);
        counter = null;
    }
}
