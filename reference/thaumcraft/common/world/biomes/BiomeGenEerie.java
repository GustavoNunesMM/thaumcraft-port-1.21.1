package thaumcraft.common.world.biomes;

import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityWitch;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biome.BiomeProperties;
import net.minecraft.world.biome.Biome.SpawnListEntry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.entities.monster.EntityBrainyZombie;
import thaumcraft.common.entities.monster.EntityEldritchGuardian;
import thaumcraft.common.entities.monster.EntityGiantBrainyZombie;
import thaumcraft.common.entities.monster.EntityWisp;

public class BiomeGenEerie extends Biome {
   public BiomeGenEerie(BiomeProperties par1) {
      super(par1);
      this.setRegistryName("thaumcraft", "eerie");
      this.spawnableCreatureList.clear();
      this.spawnableCreatureList.add(new SpawnListEntry(EntityBat.class, 3, 1, 1));
      this.spawnableMonsterList.add(new SpawnListEntry(EntityWitch.class, 8, 1, 1));
      this.spawnableMonsterList.add(new SpawnListEntry(EntityEnderman.class, 4, 1, 1));
      if (ModConfig.CONFIG_WORLD.allowSpawnAngryZombie) {
         this.spawnableMonsterList.add(new SpawnListEntry(EntityBrainyZombie.class, 32, 1, 1));
         this.spawnableMonsterList.add(new SpawnListEntry(EntityGiantBrainyZombie.class, 8, 1, 1));
      }

      if (ModConfig.CONFIG_WORLD.allowSpawnWisp) {
         this.spawnableMonsterList.add(new SpawnListEntry(EntityWisp.class, 3, 1, 1));
      }

      if (ModConfig.CONFIG_WORLD.allowSpawnElder) {
         this.spawnableMonsterList.add(new SpawnListEntry(EntityEldritchGuardian.class, 1, 1, 1));
      }

      this.decorator.treesPerChunk = 2;
      this.decorator.flowersPerChunk = 1;
      this.decorator.grassPerChunk = 2;
   }

   @SideOnly(Side.CLIENT)
   public int getGrassColorAtPos(BlockPos pos) {
      return 4212800;
   }

   @SideOnly(Side.CLIENT)
   public int getFoliageColorAtPos(BlockPos pos) {
      return 4212800;
   }

   public int getSkyColorByTemp(float par1) {
      return 2237081;
   }

   public int getWaterColorMultiplier() {
      return 3035999;
   }
}
