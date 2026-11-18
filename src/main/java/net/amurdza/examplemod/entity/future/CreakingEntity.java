package net.amurdza.examplemod.entity.future;

import com.blackgear.vanillabackport.common.level.entities.mob.monster.creaking.Creaking;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** Saved AOE creakings now share the native backport's gaze and heart behavior. */
public class CreakingEntity extends Creaking {
    public CreakingEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }
}
