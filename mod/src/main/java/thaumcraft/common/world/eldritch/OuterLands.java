package thaumcraft.common.world.eldritch;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import thaumcraft.Thaumcraft;

/**
 * The Outer Lands dimension (ModConfig dimensionOuterId in 1.12): an endless labyrinth of ancient stone floating in
 * the void, biome {@code thaumcraft:eldritch}. Flying is disrupted there (PlayerEvents.handleMisc), rift foci fizzle
 * and eldritch guardians are at full strength. Travel goes through eldritch portals; the way back leads to the portal
 * the player came through.
 */
public final class OuterLands {
   public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "outer_lands"));
   public static final ResourceKey<Biome> BIOME = ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "eldritch"));
   private static final String RETURN = "thaumcraft:outer_lands_return";

   private OuterLands() {
   }

   public static boolean isOuterLands(Level level) {
      return level != null && level.dimension() == LEVEL;
   }

   /** Steps through an eldritch portal at {@code portal}: into the maze, or back home from it. */
   public static void travel(ServerPlayer player, BlockPos portal) {
      if (isOuterLands(player.level())) {
         leave(player);
      } else {
         enter(player, portal);
      }
   }

   public static boolean enter(ServerPlayer player, BlockPos portal) {
      ServerLevel target = player.server.getLevel(LEVEL);
      if (target == null) return false;
      CompoundTag ret = new CompoundTag();
      ret.putString("dim", player.level().dimension().location().toString());
      ret.putDouble("x", portal.getX() + 0.5);
      ret.putDouble("y", portal.getY() - 1);
      ret.putDouble("z", portal.getZ() + 2.5);
      player.getPersistentData().put(RETURN, ret);
      BlockPos arrive = MazeGenerator.arrival(MazeGenerator.region(portal.getX() >> 4), MazeGenerator.region(portal.getZ() >> 4));
      return go(player, target, Vec3.atBottomCenterOf(arrive));
   }

   public static boolean leave(ServerPlayer player) {
      CompoundTag ret = player.getPersistentData().getCompound(RETURN);
      ServerLevel target = null;
      Vec3 pos = null;
      if (ret.contains("dim")) {
         target = player.server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(ret.getString("dim"))));
         pos = new Vec3(ret.getDouble("x"), ret.getDouble("y"), ret.getDouble("z"));
      }
      if (target == null) {
         target = player.server.overworld();
         pos = Vec3.atBottomCenterOf(target.getSharedSpawnPos());
      }
      return go(player, target, pos);
   }

   private static boolean go(ServerPlayer player, ServerLevel target, Vec3 pos) {
      player.setPortalCooldown(100);
      player.changeDimension(new DimensionTransition(target, pos, Vec3.ZERO, player.getYRot(), player.getXRot(),
         DimensionTransition.PLAY_PORTAL_SOUND.then(DimensionTransition.PLACE_PORTAL_TICKET)));
      player.setPortalCooldown(100);
      target.playSound(null, BlockPos.containing(pos), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.6F);
      return true;
   }
}
