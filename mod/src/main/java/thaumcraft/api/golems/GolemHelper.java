package thaumcraft.api.golems;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.golems.seals.ISeal;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.seals.SealPos;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.golems.seals.SealHandler;
import thaumcraft.common.golems.tasks.TaskHandler;

public class GolemHelper {
   /** Open provisioning requests per dimension (server side). */
   public static final Map<ResourceKey<Level>, ArrayList<ProvisionRequest>> provisionRequests = new HashMap<>();
   static final int LISTLIMIT = 1000;

   public static void registerSeal(ISeal seal) {
      SealHandler.registerSeal(seal);
   }

   public static ISeal getSeal(String key) {
      return SealHandler.getSeal(key);
   }

   /** The seal item placing this seal type: "thaumcraft:pickup" is placed by the item thaumcraft:seal_pickup. */
   public static ItemStack getSealStack(String key) {
      String[] sp = key.split(":");
      String ns = sp.length > 1 ? sp[0] : "thaumcraft";
      String path = sp.length > 1 ? sp[1] : sp[0];
      Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(ns, "seal_" + path));
      return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
   }

   @Nullable
   public static ISealEntity getSealEntity(Level level, @Nullable SealPos pos) {
      return SealHandler.getSealEntity(level, pos);
   }

   public static void addGolemTask(Level level, Task task) {
      TaskHandler.addTask(level, task);
   }

   private static void add(Level level, ProvisionRequest pr) {
      List<ProvisionRequest> list = provisionRequests.computeIfAbsent(level.dimension(), k -> new ArrayList<>());
      if (!list.contains(pr)) list.add(pr);
      if (list.size() > LISTLIMIT) list.remove(0);
   }

   public static void requestProvisioning(Level level, ISealEntity seal, ItemStack stack) {
      add(level, new ProvisionRequest(seal, stack.copy()));
   }

   public static void requestProvisioning(Level level, BlockPos pos, Direction side, ItemStack stack) {
      add(level, new ProvisionRequest(pos, side, stack.copy()));
   }

   public static void requestProvisioning(Level level, Entity entity, ItemStack stack) {
      add(level, new ProvisionRequest(entity, stack.copy()));
   }

   public static void requestProvisioning(Level level, BlockPos pos, Direction side, ItemStack stack, int ui) {
      ProvisionRequest pr = new ProvisionRequest(pos, side, stack.copy());
      pr.setUI(ui);
      add(level, pr);
   }

   public static void requestProvisioning(Level level, Entity entity, ItemStack stack, int ui) {
      ProvisionRequest pr = new ProvisionRequest(entity, stack.copy());
      pr.setUI(ui);
      add(level, pr);
   }

   /** The count-th block of the seal's area, walking it in order. */
   public static BlockPos getPosInArea(ISealEntity seal, int count) {
      Direction f = seal.getSealPos().face;
      BlockPos a = seal.getArea();
      int xx = 1 + (a.getX() - 1) * (f.getStepX() == 0 ? 2 : 1);
      int yy = 1 + (a.getY() - 1) * (f.getStepY() == 0 ? 2 : 1);
      int zz = 1 + (a.getZ() - 1) * (f.getStepZ() == 0 ? 2 : 1);
      int qx = f.getStepX() != 0 ? f.getStepX() : 1;
      int qy = f.getStepY() != 0 ? f.getStepY() : 1;
      int qz = f.getStepZ() != 0 ? f.getStepZ() : 1;
      int y = qy * (count / zz / xx) % yy + f.getStepY();
      int x = qx * (count / zz) % xx + f.getStepX();
      int z = qz * count % zz + f.getStepZ();
      return seal.getSealPos().pos.offset(x - (f.getStepX() == 0 ? xx / 2 : 0), y - (f.getStepY() == 0 ? yy / 2 : 0),
         z - (f.getStepZ() == 0 ? zz / 2 : 0));
   }

   public static AABB getBoundsForArea(ISealEntity seal) {
      Direction f = seal.getSealPos().face;
      BlockPos p = seal.getSealPos().pos;
      BlockPos a = seal.getArea();
      return new AABB(p.getX(), p.getY(), p.getZ(), p.getX() + 1, p.getY() + 1, p.getZ() + 1)
         .move(f.getStepX(), f.getStepY(), f.getStepZ())
         .expandTowards(f.getStepX() != 0 ? (a.getX() - 1) * f.getStepX() : 0.0, f.getStepY() != 0 ? (a.getY() - 1) * f.getStepY() : 0.0,
            f.getStepZ() != 0 ? (a.getZ() - 1) * f.getStepZ() : 0.0)
         .inflate(f.getStepX() == 0 ? a.getX() - 1 : 0.0, f.getStepY() == 0 ? a.getY() - 1 : 0.0, f.getStepZ() == 0 ? a.getZ() - 1 : 0.0);
   }
}
