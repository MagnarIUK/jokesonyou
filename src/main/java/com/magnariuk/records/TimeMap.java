package com.magnariuk.records;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;

import static com.magnariuk.records.TimeEntry.TIME_ENTRY_CODEC;
import static com.magnariuk.records.PenaltyEntry.PENALTY_ENTRY_CODEC;

public record TimeMap(
        String current,
        boolean isPaused,
        List<TimeEntry> entries,
        List<PenaltyEntry> penalties
) {
    public static final Codec<TimeMap> TIME_MAP_CODEC = RecordCodecBuilder.create(inst ->
            inst.group(
                    Codec.STRING.fieldOf("current").forGetter(TimeMap::current)
                    ,Codec.BOOL.fieldOf("isPaused").forGetter(TimeMap::isPaused),
                    TIME_ENTRY_CODEC.listOf().fieldOf("entries").forGetter(TimeMap::entries),
                    PENALTY_ENTRY_CODEC.listOf().fieldOf("penalties").forGetter(TimeMap::penalties)
            ).apply(inst, TimeMap::new));

    public TimeMap update(String _current, TimeEntry entry) {
        List<TimeEntry> newEntries = new ArrayList<>(entries);
        newEntries.add(entry);
        return new TimeMap(_current, this.isPaused, newEntries, this.penalties);
    }

    public TimeMap updatePenalty(String targetPlayer, long penaltyAmount) {
        List<PenaltyEntry> newPenalties = new ArrayList<>();
        boolean found = false;

        for (PenaltyEntry entry : this.penalties) {
            if (entry.player().equals(targetPlayer)) {
                newPenalties.add(new PenaltyEntry(targetPlayer, entry.time() + penaltyAmount));
                found = true;
            } else {
                newPenalties.add(entry);
            }
        }

        if (!found) {
            newPenalties.add(new PenaltyEntry(targetPlayer, penaltyAmount));
        }

        return new TimeMap(this.current, this.isPaused, this.entries, newPenalties);
    }

    public TimeMap setPaused(boolean isPaused) {
        return new TimeMap(this.current, isPaused, this.entries, this.penalties);
    }
}
