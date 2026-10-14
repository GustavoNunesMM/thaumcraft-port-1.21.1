package thaumcraft.common.registers;

import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import thaumcraft.common.entities.projectile.GrappleEntity;
import thaumcraft.common.items.misc.CreativeFluxSpongeItem;
import thaumcraft.common.items.tools.GrappleGunItem;

/**
 * Late additions found missing by the audit: the arcane grappler (gun, head, spool and its grapple entity), the arcane
 * activator rail (BlockRailPowered named activator_rail: powered constructs stop on it), the creative flux sponge and the
 * tin/silver/lead nuggets and clusters.
 */
public final class ModExtras {
   /** ItemGrappleGun's "loaded" tag: a grapple is out (the model without its head). */
   public static final Supplier<DataComponentType<Boolean>> GRAPPLE_LOADED = ModDataComponents.COMPONENTS.registerComponentType(
      "grapple_loaded", b -> b.persistent(com.mojang.serialization.Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

   public static final DeferredItem<GrappleGunItem> GRAPPLE_GUN = ModItems.ITEMS.registerItem("grapple_gun", GrappleGunItem::new);
   public static final DeferredItem<Item> GRAPPLE_GUN_TIP = ModItems.ITEMS.registerSimpleItem("grapple_gun_tip");
   public static final DeferredItem<Item> GRAPPLE_GUN_SPOOL = ModItems.ITEMS.registerSimpleItem("grapple_gun_spool");
   public static final DeferredItem<CreativeFluxSpongeItem> CREATIVE_FLUX_SPONGE = ModItems.ITEMS.registerItem("creative_flux_sponge",
      CreativeFluxSpongeItem::new);

   public static final Supplier<EntityType<GrappleEntity>> GRAPPLE = ModEntities.ENTITIES.register("grapple",
      () -> EntityType.Builder.<GrappleEntity>of(GrappleEntity::new, MobCategory.MISC).sized(0.1F, 0.1F)
         .clientTrackingRange(4).updateInterval(1).build("grapple"));

   /** BlockRailPowered (no activator flag in 1.12, so it behaves as a powered rail). */
   public static final DeferredBlock<PoweredRailBlock> ACTIVATOR_RAIL = ModBlocks.BLOCKS.registerBlock("activator_rail",
      p -> new PoweredRailBlock(p, true), BlockBehaviour.Properties.of().noCollission().strength(0.7F).sound(SoundType.METAL));
   public static final DeferredItem<BlockItem> ACTIVATOR_RAIL_ITEM = ModItems.ITEMS.registerSimpleBlockItem(ACTIVATOR_RAIL);

   /**
    * The ItemsTC.nuggets / clusters variants for tin, silver and lead (always registered in 1.12): the infernal furnace's
    * bonus nuggets and the purification of those ores when some mod adds them.
    */
   public static final DeferredItem<Item> NUGGET_TIN = ModItems.ITEMS.registerSimpleItem("nugget_tin");
   public static final DeferredItem<Item> NUGGET_SILVER = ModItems.ITEMS.registerSimpleItem("nugget_silver");
   public static final DeferredItem<Item> NUGGET_LEAD = ModItems.ITEMS.registerSimpleItem("nugget_lead");
   public static final DeferredItem<Item> CLUSTER_TIN = ModItems.ITEMS.registerSimpleItem("cluster_tin");
   public static final DeferredItem<Item> CLUSTER_SILVER = ModItems.ITEMS.registerSimpleItem("cluster_silver");
   public static final DeferredItem<Item> CLUSTER_LEAD = ModItems.ITEMS.registerSimpleItem("cluster_lead");

   private ModExtras() {
   }

   public static void init() {
   }
}
