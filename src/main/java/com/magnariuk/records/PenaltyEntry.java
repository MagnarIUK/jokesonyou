package com.magnariuk.records;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record PenaltyEntry(String player, long time) {
    public static final Codec<PenaltyEntry> PENALTY_ENTRY_CODEC = RecordCodecBuilder.create(inst ->
            inst.group(
                    Codec.STRING.fieldOf("player").forGetter(PenaltyEntry::player),
                    Codec.LONG.fieldOf("time").forGetter(PenaltyEntry::time)
            ).apply(inst, PenaltyEntry::new));
}
