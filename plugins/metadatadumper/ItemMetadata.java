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

import java.util.Arrays;
import lombok.Setter;
import net.runelite.api.ItemComposition;

public class ItemMetadata
{
    @Setter
    private int id = -1;
    @Setter
    private String name = null;
    @Setter
    private Boolean members = false;
    @Setter
    private Boolean tradeable_on_ge = false;
    @Setter
    private Boolean stackable = false;
    @Setter
    private Boolean noted = false;
    @Setter
    private Boolean noteable = false;
    @Setter
    private int linked_id = -1;
    @Setter
    private Boolean placeholder = false;
    @Setter
    private Boolean equipable = false;
    @Setter
    private int cost = -1;
    @Setter
    private int low_alch = -1;
    @Setter
    private int high_alch = -1;

    public void populateItemMetadata(ItemComposition itemComposition)
    {
        // Start by fetching basic properties directly from an ItemDefinition
        this.id = itemComposition.getId();
        this.name = itemComposition.getName();
        this.members = itemComposition.isMembers();
        this.tradeable_on_ge = itemComposition.isTradeable();
        this.stackable = itemComposition.isStackable();
        this.linked_id = itemComposition.getLinkedNoteId();
        // Determine cost, then alchemy values
        this.cost = itemComposition.getPrice();
        this.low_alch = (int)Math.floor(itemComposition.getPrice() * 0.4);
        this.high_alch = (int)Math.floor(itemComposition.getPrice() * 0.6);

        // Determine if item is noted... According to RuneLite API:
        // 799 with be returned if the item is noted
        if (itemComposition.getNote() == 799)
        {
            this.noted = true;
        }

        // Determine if item is notable:
        if ((itemComposition.getNote() == 799) || (itemComposition.getLinkedNoteId() != -1))
        {
            // If the item itself is noted or linked ID is noted, it must be notable!
            this.noteable = true;
        }

        // Populate placeholder boolean (is the item ID a placeholder)
        // 14401 if placeholder, -1 otherwise
        if (itemComposition.getPlaceholderTemplateId() == 14401)
        {
            this.placeholder = true;
        }

        // Determine if item is equipable:
        String[] inventoryActions = itemComposition.getInventoryActions();
        // If inventoryActions contains "Wear", "Wield" or "Equip" it is deemed equipable
        if (inventoryActions != null)
        {
            if (Arrays.asList(inventoryActions).contains("Wear") ||
                    Arrays.asList(inventoryActions).contains("Wield") ||
                    Arrays.asList(inventoryActions).contains("Equip"))
            {
                this.equipable = true;
            }
        }
    }
}
