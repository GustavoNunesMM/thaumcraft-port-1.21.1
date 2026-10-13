"""Converts the 1.12 ModelBiped armor models of client/renderers/models/gear (fortress, robe, knight, leader) into
1.21 LayerDefinitions: client/renderers/models/gear/GearModelLayers.java (generated, don't edit by hand).

Every `new ModelRenderer(this, u, v)` + addBox/mirror/setRotationPoint/setRotation becomes a CubeListBuilder and a
PartPose; `bipedX.addChild(part)` becomes an addOrReplaceChild under the matching humanoid part, keeping the
`if (f < 1.0F)` branches; `bipedX.cubeList.clear()` replaces the humanoid part by an empty one.

Run from the repo root: python3 tools/convert_gear_models.py
"""
import os
import re

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
SRC = os.path.join(ROOT, "reference/thaumcraft/client/renderers/models/gear")
OUT = os.path.join(ROOT, "mod/src/main/java/thaumcraft/client/renderers/models/gear/GearModelLayers.java")
MODELS = {"Fortress": "ModelFortressArmor", "Robe": "ModelRobe", "Knight": "ModelKnightArmor", "Leader": "ModelLeaderArmor"}
BIPED = {"bipedHead": "head", "bipedHeadwear": "hat", "bipedBody": "body", "bipedRightArm": "right_arm", "bipedLeftArm": "left_arm",
         "bipedRightLeg": "right_leg", "bipedLeftLeg": "left_leg"}
BIPED_POSE = {"head": "PartPose.ZERO", "hat": "PartPose.ZERO", "body": "PartPose.ZERO", "right_arm": "PartPose.offset(-5.0F, 2.0F, 0.0F)",
              "left_arm": "PartPose.offset(5.0F, 2.0F, 0.0F)", "right_leg": "PartPose.offset(-1.9F, 12.0F, 0.0F)",
              "left_leg": "PartPose.offset(1.9F, 12.0F, 0.0F)"}


def fl(s):
    s = s.strip().replace("(float)", "").replace("Math.PI", "3.141592653589793")
    s = re.sub(r"(\d)[Ff]\b", r"\1", s)
    v = float(eval(s))
    return f"{v:.7g}F" if "e" not in f"{v:.7g}" else f"{v}F"


def constructor_body(text, cls):
    start = text.index(f"public {cls}(float f)")
    i = text.index("{", start) + 1
    depth = 1
    j = i
    while depth:
        if text[j] == "{":
            depth += 1
        elif text[j] == "}":
            depth -= 1
        j += 1
    return text[i:j - 1]


def expand_mask_loop(body):
    m = re.search(r"for \(int a = 0; a < 3; a\+\+\) \{(.*?)\n      \}", body, re.S)
    if not m:
        return body
    out = []
    for a in range(3):
        b = m.group(1).replace("this.Mask[a]", f"this.Mask{a}").replace("52 + a * 24", str(52 + a * 24))
        out.append(b)
    body = body[:m.start()] + "\n".join(out) + body[m.end():]
    body = re.sub(r"this\.Mask = new ModelRenderer\[3\];", "", body)
    return re.sub(r"this\.Mask\[(\d)\]", r"this.Mask\1", body)


def convert(name, cls):
    text = open(os.path.join(SRC, cls + ".java")).read()
    body = expand_mask_loop(constructor_body(text, cls))
    parts = {}
    order = []
    lines = [l.strip() for l in body.split("\n") if l.strip()]
    out = []
    indent = "      "
    for l in lines:
        m = re.match(r"this\.(\w+) = new ModelRenderer\(this, (\d+), (\d+)\);", l)
        if m:
            parts[m.group(1)] = {"cubes": [f"texOffs({m.group(2)}, {m.group(3)})"], "point": None, "rot": None}
            order.append(m.group(1))
            continue
        m = re.match(r"this\.(\w+)\.mirror = true;", l)
        if m:
            parts[m.group(1)]["cubes"].append("mirror()")
            continue
        m = re.match(r"this\.(\w+)\.addBox\((.*)\);", l)
        if m:
            args = [a.strip() for a in m.group(2).split(",")]
            box = ", ".join(fl(a) for a in args[:6])
            if len(args) == 7:
                box += f", new CubeDeformation({fl(args[6])})"
            parts[m.group(1)]["cubes"].append(f"addBox({box})")
            continue
        m = re.match(r"this\.(\w+)\.setRotationPoint\((.*)\);", l)
        if m:
            parts[m.group(1)]["point"] = [fl(a) for a in m.group(2).split(",")]
            continue
        m = re.match(r"this\.setRotation\(this\.(\w+), (.*)\);", l)
        if m:
            parts[m.group(1)]["rot"] = [fl(a) for a in m.group(2).split(",")]
            continue
        if re.match(r"this\.\w+\.setTextureSize|super\(|this\.texture(Width|Height)", l):
            continue
        m = re.match(r"this\.(biped\w+)\.cubeList\.clear\(\);", l)
        if m:
            p = BIPED[m.group(1)]
            out.append(f"{indent}{p} = root.addOrReplaceChild(\"{p}\", CubeListBuilder.create(), {BIPED_POSE[p]});")
            continue
        m = re.match(r"this\.(biped\w+)\.addChild\(this\.(\w+)\);", l)
        if m:
            p = BIPED[m.group(1)]
            child = m.group(2)
            out.append(f"{indent}{p}.addOrReplaceChild(\"{child}\", {child}, {child}Pose);")
            PARENTS.setdefault(name, []).append((p, child))
            continue
        if l in ("if (f < 1.0F) {", "if (!(f < 1.0F)) {"):
            out.append(f"{indent}{l}")
            indent += "   "
            continue
        if l == "} else {":
            out.append(f"{indent[:-3]}}} else {{")
            continue
        if l == "}":
            indent = indent[:-3]
            out.append(f"{indent}}}")
            continue
        raise SystemExit(f"{cls}: can't convert: {l}")
    decl = []
    for p in order:
        d = parts[p]
        decl.append(f"      CubeListBuilder {p} = CubeListBuilder.create().{'.'.join(d['cubes'])};")
        pt = d["point"] or ["0.0F", "0.0F", "0.0F"]
        rt = d["rot"] or ["0.0F", "0.0F", "0.0F"]
        decl.append(f"      PartPose {p}Pose = PartPose.offsetAndRotation({', '.join(pt)}, {', '.join(rt)});")
    head = [f"   /** {cls}(f): f is 1.0 for the chest and boots model, 0.5 for the helmet and leggings one. */",
            f"   public static LayerDefinition create{name}(float f) {{",
            "      MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(f), 0.0F);",
            "      PartDefinition root = mesh.getRoot();"]
    head += [f"      PartDefinition {p} = root.getChild(\"{p}\");" for p in BIPED.values()]
    return "\n".join(head + decl + out + ["      return LayerDefinition.create(mesh, 128, 64);", "   }"])


PARENTS = {}
methods = [convert(n, c) for n, c in MODELS.items()]
tables = []
for n in MODELS:
    seen = []
    for p, c in PARENTS[n]:
        if (p, c) not in seen:
            seen.append((p, c))
    tables.append(f"   public static final String[][] {n.upper()}_PARTS = {{\n" +
                  ",\n".join(f"      {{\"{p}\", \"{c}\"}}" for p, c in seen) + "\n   };")
java = f"""package thaumcraft.client.renderers.models.gear;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The 1.12 armor models (ModelFortressArmor, ModelRobe, ModelKnightArmor, ModelLeaderArmor) as layer definitions.
 * Generated by tools/convert_gear_models.py from the reference sources; don't edit by hand.
 */
@SuppressWarnings("unused")
public final class GearModelLayers {{
   private GearModelLayers() {{
   }}

{chr(10).join(tables)}

{(chr(10) + chr(10)).join(methods)}
}}
"""
os.makedirs(os.path.dirname(OUT), exist_ok=True)
open(OUT, "w").write(java)
print("wrote", OUT)
