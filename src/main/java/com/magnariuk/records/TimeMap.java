package com.magnariuk.records;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;

import static com.magnariuk.records.TimeEntry.TIME_ENTRY_CODEC;

public record TimeMap(
        String current,
        boolean isPaused,
        List<TimeEntry> entries
) {
    public static final Codec<TimeMap> TIME_MAP_CODEC = RecordCodecBuilder.create(inst ->
            inst.group(
                    Codec.STRING.fieldOf("current").forGetter(TimeMap::current)
                    ,Codec.BOOL.fieldOf("isPaused").forGetter(TimeMap::isPaused),
                    TIME_ENTRY_CODEC.listOf().fieldOf("entries").forGetter(TimeMap::entries)
            ).apply(inst, TimeMap::new));

    public TimeMap update(String _current, TimeEntry entry) {
        List<TimeEntry> newEntries = new ArrayList<>(entries);
        newEntries.add(entry);
        return new TimeMap(_current, this.isPaused ,newEntries);
    }

    public TimeMap setPaused(boolean isPaused) {
        return new TimeMap(this.current, isPaused, this.entries);
    }
}
