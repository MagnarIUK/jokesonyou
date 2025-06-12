package com.magnariuk.records;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record TimeEntry(String player, long time) {

    public static final Codec<TimeEntry> TIME_ENTRY_CODEC = RecordCodecBuilder.create(inst ->
            inst.group(
                    Codec.STRING.fieldOf("player").forGetter(TimeEntry::player),
                    Codec.LONG.fieldOf("time").forGetter(TimeEntry::time)
            ).apply(inst, TimeEntry::new));
}

