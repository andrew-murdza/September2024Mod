package net.amurdza.examplemod.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;
import org.joml.SimplexNoise;

import java.util.concurrent.ConcurrentHashMap;


public final class AOEDensityFunctions {
    public static final DeferredRegister<Codec<? extends DensityFunction>> DENSITY_FUNCTION_TYPES = DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, AOEMod.MOD_ID);

    public static final RegistryObject<Codec<? extends DensityFunction>> SHIFT_DENSITY_FUNCTION_TYPE = DENSITY_FUNCTION_TYPES.register("shift", Shift.CODEC::codec);

    public static void register(IEventBus eventBus) {
        DENSITY_FUNCTION_TYPES.register(eventBus);
    }

    public static final Long2DoubleOpenHashMap heightmap = new Long2DoubleOpenHashMap();
    public static DensityFunction temperature;
    public static boolean isFloatingIslands = false;

    protected record Shift(DensityFunction input, DensityFunction shiftX, DensityFunction shiftY, DensityFunction shiftZ) implements DensityFunction {
        private static final MapCodec<Shift> DATA_CODEC = RecordCodecBuilder.mapCodec((data) -> {
            return data.group(DensityFunction.HOLDER_HELPER_CODEC.fieldOf("input").forGetter(Shift::input), DensityFunction.HOLDER_HELPER_CODEC.fieldOf("shift_x").forGetter(Shift::shiftX), DensityFunction.HOLDER_HELPER_CODEC.fieldOf("shift_y").forGetter(Shift::shiftY), DensityFunction.HOLDER_HELPER_CODEC.fieldOf("shift_z").forGetter(Shift::shiftZ)).apply(data, Shift::new);
        });
        public static final KeyDispatchDataCodec<Shift> CODEC = AOEDensityFunctions.makeCodec(DATA_CODEC);

        @Override
        public double compute(@NotNull FunctionContext context) {
            return input.compute(new DensityFunction.SinglePointContext((int) (context.blockX()+shiftX().compute(context)), (int) (context.blockY()+shiftY().compute(context)), (int) (context.blockZ()+shiftZ().compute(context))));
        }

        @Override
        public void fillArray(double @NotNull [] densities, ContextProvider context) {
            context.fillAllDirectly(densities, this);
        }

        @Override
        public @NotNull DensityFunction mapAll(Visitor visitor) {
            return visitor.apply(new Shift(input().mapAll(visitor), shiftX().mapAll(visitor), shiftY().mapAll(visitor), shiftZ().mapAll(visitor)));
        }

        @Override
        public double minValue() {
            return -1875000d;
        }

        @Override
        public double maxValue() {
            return 1875000d;
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }

    }

    static <O> KeyDispatchDataCodec<O> makeCodec(MapCodec<O> codec) {
        return KeyDispatchDataCodec.of(codec);
    }
}