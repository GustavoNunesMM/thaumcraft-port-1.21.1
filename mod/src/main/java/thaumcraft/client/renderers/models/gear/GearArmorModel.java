package thaumcraft.client.renderers.models.gear;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * ModelCustomArmor and its subclasses: a humanoid armor model whose extra pieces hang off the humanoid parts (so the
 * vanilla pose copied from the wearer moves them), with the per-piece hiding and cloth swinging of their render().
 */
public class GearArmorModel extends HumanoidModel<LivingEntity> {
   private final Map<String, ModelPart> parts = new HashMap<>();

   public GearArmorModel(ModelPart root, String[][] layout) {
      super(root);
      for (String[] pc : layout) {
         ModelPart parent = root.getChild(pc[0]);
         if (parent.hasChild(pc[1])) parts.put(pc[1], parent.getChild(pc[1]));
      }
   }

   public void show(String name, boolean visible) {
      ModelPart p = parts.get(name);
      if (p != null) p.visible = visible;
   }

   public void xRot(float x, String... names) {
      for (String n : names) {
         ModelPart p = parts.get(n);
         if (p != null) p.xRot = x;
      }
   }

   /** The shared start of the render() methods: a and b follow the legs, c the one further back. */
   static float[] swing(float limbSwing, float limbSwingAmount) {
      float a = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
      float b = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
      return new float[]{a, b, Math.min(a, b)};
   }

   /** ModelRobe.render. */
   public void animateRobe(float limbSwing, float limbSwingAmount) {
      float c = swing(limbSwing, limbSwingAmount)[2];
      xRot(c - 0.1047198F, "FrontclothR1", "FrontclothL1");
      xRot(c - 0.3316126F, "FrontclothR2", "FrontclothL2");
      xRot(-c + 0.1047198F, "ClothBackR1", "ClothBackL1");
      xRot(-c + 0.2268928F, "ClothBackR2", "ClothBackL2", "ClothBackR3", "ClothBackL3");
   }

   /** ModelKnightArmor.render. */
   public void animateKnight(float limbSwing, float limbSwingAmount) {
      float c = swing(limbSwing, limbSwingAmount)[2];
      xRot(c - 0.1047198F, "Frontcloth1");
      xRot(c - 0.3316126F, "Frontcloth2");
      xRot(-c / 2.0F + 0.1396263F, "Cloak1");
      xRot(-c / 2.0F + 0.3069452F, "Cloak2");
      xRot(-c / 2.0F + 0.4465716F, "Cloak3");
   }

   /** ModelLeaderArmor.render. */
   public void animateLeader(float limbSwing, float limbSwingAmount) {
      float[] s = swing(limbSwing, limbSwingAmount);
      xRot(s[0] - 0.1047198F, "LegClothR");
      xRot(s[1] - 0.1047198F, "LegClothL");
      xRot(-s[2] / 2.0F + 0.1396263F, "Cloak1");
      xRot(-s[2] / 2.0F + 0.3069452F, "Cloak2");
      xRot(-s[2] / 2.0F + 0.4465716F, "Cloak3");
   }

   /** ModelFortressArmor.render: the set (pieces worn) shows more ornaments; the helm shows its mask and goggles. */
   public void setupFortress(int set, int mask, boolean goggles) {
      show("Goggles", goggles);
      for (int a = 0; a < 3; a++) show("Mask" + a, mask == a);
      for (String n : new String[]{"Scroll", "OrnamentL", "OrnamentL2", "OrnamentR", "OrnamentR2", "Gemornament", "Gem", "ShoulderplateL2",
         "ShoulderplateL3", "ShoulderplateR2", "ShoulderplateR3", "SidepanelR3", "SidepanelL3"}) {
         show(n, set >= 3);
      }
      for (String n : new String[]{"Book", "flapL", "flapR", "ShoulderplateLtop", "ShoulderplateL1", "ShoulderplateRtop", "ShoulderplateR1",
         "SidepanelR2", "SidepanelL2"}) {
         show(n, set >= 2);
      }
   }
}
