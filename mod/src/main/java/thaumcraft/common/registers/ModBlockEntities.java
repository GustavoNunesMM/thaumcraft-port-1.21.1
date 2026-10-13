package thaumcraft.common.registers;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.crafting.ArcaneWorkbenchBlockEntity;
import thaumcraft.common.blocks.crafting.CrucibleBlockEntity;
import thaumcraft.common.blocks.misc.NitorBlockEntity;
import thaumcraft.common.blocks.crafting.ResearchTableBlockEntity;

public final class ModBlockEntities {
   public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Thaumcraft.MODID);

   public static final Supplier<BlockEntityType<ResearchTableBlockEntity>> RESEARCH_TABLE = BLOCK_ENTITIES.register("research_table",
      () -> BlockEntityType.Builder.of(ResearchTableBlockEntity::new, ModBlocks.RESEARCH_TABLE.get()).build(null));

   public static final Supplier<BlockEntityType<ArcaneWorkbenchBlockEntity>> ARCANE_WORKBENCH = BLOCK_ENTITIES.register("arcane_workbench",
      () -> BlockEntityType.Builder.of(ArcaneWorkbenchBlockEntity::new, ModBlocks.ARCANE_WORKBENCH.get()).build(null));

   public static final Supplier<BlockEntityType<CrucibleBlockEntity>> CRUCIBLE = BLOCK_ENTITIES.register("crucible",
      () -> BlockEntityType.Builder.of(CrucibleBlockEntity::new, ModBlocks.CRUCIBLE.get()).build(null));
   public static final Supplier<BlockEntityType<NitorBlockEntity>> NITOR = BLOCK_ENTITIES.register("nitor",
      () -> BlockEntityType.Builder.of(NitorBlockEntity::new, ModBlocks.NITOR.values().stream().map(b -> b.get())
         .toArray(net.minecraft.world.level.block.Block[]::new)).build(null));

   public static final Supplier<BlockEntityType<thaumcraft.common.blocks.essentia.JarBlockEntity>> JAR = BLOCK_ENTITIES.register("jar",
      () -> BlockEntityType.Builder.of(thaumcraft.common.blocks.essentia.JarBlockEntity::new, ModBlocks.JAR_NORMAL.get(), ModBlocks.JAR_VOID.get()).build(null));
   public static final Supplier<BlockEntityType<thaumcraft.common.blocks.essentia.TubeBlockEntity>> TUBE = BLOCK_ENTITIES.register("tube",
      () -> BlockEntityType.Builder.of(thaumcraft.common.blocks.essentia.TubeBlockEntity::new, ModBlocks.TUBE.get()).build(null));
   public static final Supplier<BlockEntityType<thaumcraft.common.blocks.essentia.TubeValveBlockEntity>> TUBE_VALVE = BLOCK_ENTITIES.register("tube_valve",
      () -> BlockEntityType.Builder.of(thaumcraft.common.blocks.essentia.TubeValveBlockEntity::new, ModBlocks.TUBE_VALVE.get()).build(null));
   public static final Supplier<BlockEntityType<thaumcraft.common.blocks.essentia.TubeRestrictBlockEntity>> TUBE_RESTRICT = BLOCK_ENTITIES.register("tube_restrict",
      () -> BlockEntityType.Builder.of(thaumcraft.common.blocks.essentia.TubeRestrictBlockEntity::new, ModBlocks.TUBE_RESTRICT.get()).build(null));
   public static final Supplier<BlockEntityType<thaumcraft.common.blocks.essentia.TubeOnewayBlockEntity>> TUBE_ONEWAY = BLOCK_ENTITIES.register("tube_oneway",
      () -> BlockEntityType.Builder.of(thaumcraft.common.blocks.essentia.TubeOnewayBlockEntity::new, ModBlocks.TUBE_ONEWAY.get()).build(null));
   public static final Supplier<BlockEntityType<thaumcraft.common.blocks.essentia.TubeFilterBlockEntity>> TUBE_FILTER = BLOCK_ENTITIES.register("tube_filter",
      () -> BlockEntityType.Builder.of(thaumcraft.common.blocks.essentia.TubeFilterBlockEntity::new, ModBlocks.TUBE_FILTER.get()).build(null));
   public static final Supplier<BlockEntityType<thaumcraft.common.blocks.essentia.TubeBufferBlockEntity>> TUBE_BUFFER = BLOCK_ENTITIES.register("tube_buffer",
      () -> BlockEntityType.Builder.of(thaumcraft.common.blocks.essentia.TubeBufferBlockEntity::new, ModBlocks.TUBE_BUFFER.get()).build(null));
   public static final Supplier<BlockEntityType<thaumcraft.common.blocks.essentia.SmelterBlockEntity>> SMELTER = BLOCK_ENTITIES.register("smelter",
      () -> BlockEntityType.Builder.of(thaumcraft.common.blocks.essentia.SmelterBlockEntity::new, ModBlocks.SMELTER_BASIC.get(),
         ModBlocks.SMELTER_THAUMIUM.get(), ModBlocks.SMELTER_VOID.get()).build(null));
   public static final Supplier<BlockEntityType<thaumcraft.common.blocks.essentia.AlembicBlockEntity>> ALEMBIC = BLOCK_ENTITIES.register("alembic",
      () -> BlockEntityType.Builder.of(thaumcraft.common.blocks.essentia.AlembicBlockEntity::new, ModBlocks.ALEMBIC.get()).build(null));
   public static final Supplier<BlockEntityType<thaumcraft.common.blocks.essentia.CentrifugeBlockEntity>> CENTRIFUGE = BLOCK_ENTITIES.register("centrifuge",
      () -> BlockEntityType.Builder.of(thaumcraft.common.blocks.essentia.CentrifugeBlockEntity::new, ModBlocks.CENTRIFUGE.get()).build(null));
   public static final Supplier<BlockEntityType<thaumcraft.common.blocks.devices.BellowsBlockEntity>> BELLOWS = BLOCK_ENTITIES.register("bellows",
      () -> BlockEntityType.Builder.of(thaumcraft.common.blocks.devices.BellowsBlockEntity::new, ModBlocks.BELLOWS.get()).build(null));
   public static final Supplier<BlockEntityType<thaumcraft.common.blocks.essentia.EssentiaTransportBlockEntity>> ESSENTIA_TRANSPORT = BLOCK_ENTITIES.register("essentia_transport",
      () -> BlockEntityType.Builder.of(thaumcraft.common.blocks.essentia.EssentiaTransportBlockEntity::new, ModBlocks.ESSENTIA_INPUT.get(),
         ModBlocks.ESSENTIA_OUTPUT.get()).build(null));

   public static final Supplier<BlockEntityType<thaumcraft.common.blocks.infusion.PedestalBlockEntity>> PEDESTAL = BLOCK_ENTITIES.register("pedestal",
      () -> BlockEntityType.Builder.of(thaumcraft.common.blocks.infusion.PedestalBlockEntity::new, ModBlocks.PEDESTAL_ARCANE.get(),
         ModBlocks.PEDESTAL_ANCIENT.get(), ModBlocks.PEDESTAL_ELDRITCH.get()).build(null));
   public static final Supplier<BlockEntityType<thaumcraft.common.blocks.infusion.InfusionMatrixBlockEntity>> INFUSION_MATRIX = BLOCK_ENTITIES.register(
      "infusion_matrix", () -> BlockEntityType.Builder.of(thaumcraft.common.blocks.infusion.InfusionMatrixBlockEntity::new,
         ModBlocks.INFUSION_MATRIX.get()).build(null));
   public static final Supplier<BlockEntityType<thaumcraft.common.blocks.devices.StabilizerBlockEntity>> STABILIZER = BLOCK_ENTITIES.register("stabilizer",
      () -> BlockEntityType.Builder.of(thaumcraft.common.blocks.devices.StabilizerBlockEntity::new, ModBlocks.STABILIZER.get()).build(null));

   private ModBlockEntities() {
   }
}
