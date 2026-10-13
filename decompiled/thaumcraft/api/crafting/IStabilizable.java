package thaumcraft.api.crafting;

@Deprecated
public interface IStabilizable {
   @Deprecated
   void addStability();

   @Deprecated
   IStabilizable.EnumStability getStability();

   @Deprecated
   public static enum EnumStability {
      VERY_STABLE,
      STABLE,
      UNSTABLE,
      VERY_UNSTABLE;
   }
}
