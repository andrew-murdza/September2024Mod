package net.amurdza.examplemod.worldgen.feature.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public class RegionalGridChoiceMinimumSection {

    public static final Codec<RegionalGridChoiceMinimumSection> CODEC =
            RecordCodecBuilder.create(inst -> inst.group(
                    Codec.intRange(1, 512).fieldOf("section_width")
                            .forGetter(c -> c.sectionWidth),

                    Codec.intRange(1, 512).fieldOf("section_depth")
                            .forGetter(c -> c.sectionDepth),

                    GridChoiceEntry.CODEC.listOf().fieldOf("features")
                            .forGetter(c -> c.features)
            ).apply(inst, RegionalGridChoiceMinimumSection::new));

    public final int sectionWidth;
    public final int sectionDepth;
    public final List<GridChoiceEntry> features;

    public RegionalGridChoiceMinimumSection(
            int sectionWidth,
            int sectionDepth,
            List<GridChoiceEntry> features
    ) {
        this.sectionWidth = sectionWidth;
        this.sectionDepth = sectionDepth;
        this.features = features;
    }
}
