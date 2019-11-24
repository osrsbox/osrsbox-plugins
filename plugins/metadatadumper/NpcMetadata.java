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

import lombok.Setter;
import net.runelite.api.NPCComposition;

public class NpcMetadata
{
    @Setter
    private int id = -1;
    @Setter
    private String name = null;
    @Setter
    private int combat_level = -1;
    @Setter
    private int[] model_ids = null;
    @Setter
    private int size = -1;
    @Setter
    private Boolean clickable = false;
    @Setter
    private String[] actions = null;

    public void populateNpcMetadata(NPCComposition npcComposition)
    {
        // Start by fetching basic properties directly from an NPCComposition
        this.id = npcComposition.getId();
        this.name = npcComposition.getName();
        this.combat_level = npcComposition.getCombatLevel();
        this.model_ids = npcComposition.getModels();
        this.size = npcComposition.getSize();
        this.clickable = npcComposition.isClickable();
        this.actions = npcComposition.getActions();
    }
}
