package net.amurdza.examplemod.worldgen.feature;

//import com.crypticmushroom.minecraft.midnight.common.world.gen.feature.config.TemplateTreeConfig;
//import com.mojang.serialization.Codec;
//import net.minecraft.core.BlockPos;
//import net.minecraft.core.Vec3i;
//import net.minecraft.core.registries.Registries;
//import net.minecraft.util.RandomSource;
//import net.minecraft.world.level.ChunkPos;
//import net.minecraft.world.level.WorldGenLevel;
//import net.minecraft.world.level.block.Mirror;
//import net.minecraft.world.level.block.Rotation;
//import net.minecraft.world.level.levelgen.feature.Feature;
//import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
//import net.minecraft.world.level.levelgen.structure.BoundingBox;
//import net.minecraft.world.level.levelgen.structure.templatesystem.*;
//
//import java.util.List;
//import java.util.Objects;
//import java.util.Optional;
//
//public class GiantMoonshroom extends Feature<TemplateTreeConfig> {
//    public GiantMoonshroom(Codec<TemplateTreeConfig> pCodec) {
//        super(pCodec);
//    }
//
//    @Override
//    public boolean place(FeaturePlaceContext<TemplateTreeConfig> pContext) {
//        WorldGenLevel level=pContext.level();
//        TemplateTreeConfig config= pContext.config();
//        RandomSource rand=pContext.random();
//        BlockPos origin=pContext.origin();
//
//        StructureTemplateManager templateManager = level.getLevel().getServer().getStructureManager();
//        StructureTemplate template = templateManager.getOrCreate(config.templates.get(rand.nextInt(config.templates.size())));
//        StructurePlaceSettings settings = (new StructurePlaceSettings()).setRandom(rand);
//        ChunkPos chunkPos = new ChunkPos(origin);
//        Rotation rot = Rotation.getRandom(rand);
//        settings.setRotation(rot).setBoundingBox(new BoundingBox(chunkPos.getMinBlockX() - 16, level.getMinBuildHeight(), chunkPos.getMinBlockZ() - 16, chunkPos.getMaxBlockX() + 16, level.getMaxBuildHeight(), chunkPos.getMaxBlockZ() + 16));
//        if (config.ignoreAir) {
//            settings.addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR);
//        }
//
//            Optional<StructureProcessorList> processors = level.getLevel().getServer().registryAccess().registryOrThrow(Registries.PROCESSOR_LIST).getOptional(config.processorList);
//            processors.ifPresent((list) -> {
//                List<StructureProcessor> var10000 = list.list();
//                Objects.requireNonNull(settings);
//                var10000.forEach(settings::addProcessor);
//            });
//        Vec3i size = template.getSize(rot);
//        BlockPos offset = origin.offset(new BlockPos(-size.getX() / 2, 0, -size.getZ() / 2));
//        BlockPos templateZero = template.getZeroPositionWithTransform(offset, Mirror.NONE, rot);
//        templateZero = this.adjustPosition(level, template, templateZero, offset, origin, size, rot, config);
//        return templateZero != null && template.placeInWorld(level, templateZero, templateZero, settings, rand, 3);
//    }
//
//    protected BlockPos adjustPosition(WorldGenLevel level, StructureTemplate template, BlockPos templateZero, BlockPos offset, BlockPos origin, Vec3i size, Rotation rot, TemplateTreeConfig config) {
//        return templateZero;
//    }
//}
