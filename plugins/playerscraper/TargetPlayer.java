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

import lombok.Setter;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.kit.KitType;
import java.util.HashMap;
import java.util.Map;

public class TargetPlayer
{
    @Setter
    @Getter
    public String name = null;
    @Setter
    private int world = -1;
    @Setter
    private int combat_level = -1;

    private final Map<String, Map> items = new HashMap<>();


    public void populateTargetPlayer(Player player, Client client)
    {
        this.name = player.getName();
        this.world = client.getWorld();
        this.combat_level = player.getCombatLevel();

        PlayerComposition playerComposition = player.getPlayerComposition();

        // Loop through the kit types (each equipment slot)
        for (KitType kitType : KitType.values())
        {
            // Get the item ID number from the player for the specific slot
            int itemId = playerComposition.getEquipmentId(kitType);

            if (itemId != -1)
            {
                // Get the Item composition for the slot
                ItemComposition itemComposition = client.getItemDefinition(itemId);

                // Create a map for the specific item in a specific slot, it maps:
                // 1. Descriptor of item (e.g., name, price, id) to
                // 2. The actual value of the item
                Map<String, Object> currentSlot = new HashMap<String, Object>();

                // Populate the currentSlot map with properties
                currentSlot.put("name", itemComposition.getName());
                currentSlot.put("id", itemComposition.getId());

                // Append current slot map to player JSON object
                items.put(kitType.toString().toLowerCase(), currentSlot);
            }
        }

    }
}
