"""Generates the JSON resources of milestone 8 (infusion: matrix, pedestals, pillars, stabilizers, redstone inlay,
candles, matrix stones) and the infusion recipes (convert_recipes.py infusion + the infusion enchantments).

Run from the repo root: python3 tools/gen_marco8.py  (after gen_marco7)
"""
import json, os, collections, shutil, subprocess, sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
RES = os.path.join(ROOT, "mod/src/main/resources")
A = os.path.join(RES, "assets/thaumcraft")
D = os.path.join(RES, "data")
ORIG = os.path.join(ROOT, "decompiled/assets/thaumcraft")
NS = "thaumcraft"
COLORS = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue",
          "brown", "green", "red", "black"]
OLD_COLOR = {"light_blue": "lightblue", "light_gray": "silver"}


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def tc(n):
    return n if ":" in n else f"{NS}:{n}"


def tex(n):
    return f"{NS}:blocks/{n}"


def model_ref(m):
    if ":" not in m:
        return "minecraft:block/" + m
    ns, path = m.split(":")
    return m if path.startswith(("block/", "item/")) else f"{ns}:block/{path}"


def copy_model(name, extra=None, kind="block", src=None):
    m = json.load(open(f"{ORIG}/models/{kind}/{src or name}.json"))
    m.pop("__createdwith", None)
    if "parent" not in m and "elements" in m:
        m = {"parent": "minecraft:block/block", **m}
    elif "parent" in m:
        m["parent"] = model_ref(m["parent"]) if kind == "block" or m["parent"].startswith(NS) else (
            m["parent"] if ":" in m["parent"] else "minecraft:" + m["parent"])
    for k, v in m.get("textures", {}).items():
        if isinstance(v, str) and v.startswith("blocks/"):
            m["textures"][k] = "minecraft:block/" + v[len("blocks/"):]
    if extra:
        m.update(extra)
    write(f"{A}/models/{kind}/{name}.json", m)


def model(name, obj, kind="block"):
    write(f"{A}/models/{kind}/{name}.json", obj)


def blockstate(name, obj):
    write(f"{A}/blockstates/{name}.json", obj)


def simple_state(name, model_name=None):
    blockstate(name, {"variants": {"": {"model": tc("block/" + (model_name or name))}}})


def block_item(name, parent=None):
    model(name, {"parent": tc("block/" + (parent or name))}, "item")


def loot_self(name):
    write(f"{D}/{NS}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
          "entries": [{"type": "minecraft:item", "name": tc(name)}], "conditions": [{"condition": "minecraft:survives_explosion"}]}],
          "random_sequence": f"{NS}:blocks/{name}"})


def add_tag(kind, ns, path, values):
    p = f"{D}/{ns}/tags/{kind}/{path}.json"
    d = json.load(open(p)) if os.path.exists(p) else {"replace": False, "values": []}
    for v in values:
        if v not in d["values"]:
            d["values"].append(v)
    write(p, d)


# ====================================================================== matrix, pedestals, pillars
copy_model("infusion_matrix")
model("infusion_matrix_particle", {"textures": {"particle": tex("infuser_normal")}})
simple_state("infusion_matrix", "infusion_matrix_particle")
block_item("infusion_matrix")
loot_self("infusion_matrix")

PEDESTALS = {"pedestal_arcane": "pedestal_normal", "pedestal_ancient": "pedestal_ancient", "pedestal_eldritch": "pedestal_eldritch"}
for name, m in PEDESTALS.items():
    copy_model(m)
    blockstate(name, {"variants": {f"charge={c}": {"model": tc("block/" + m)} for c in range(16)}})
    block_item(name, m)
    loot_self(name)

shutil.copy(f"{ORIG}/models/block/pillar.obj", f"{A}/models/block/pillar.obj")
with open(f"{A}/models/block/pillar.mtl", "w") as f:
    f.write("newmtl texture\nKd 1.0 1.0 1.0\nmap_Kd #texture\n")
HFACING = {"north": 0, "east": 90, "south": 180, "west": 270}
STONE_OF = {"pillar_arcane": ("pillar_normal", "stone_arcane"), "pillar_ancient": ("pillar_ancient", "stone_ancient"),
            "pillar_eldritch": ("pillar_eldritch", "stone_eldritch_tile")}
for name, (t, stone) in STONE_OF.items():
    model(name, {"parent": "minecraft:block/block", "loader": "neoforge:obj", "model": tc("models/block/pillar.obj"), "flip_v": True,
                 "render_type": "minecraft:cutout", "textures": {"texture": tex(t), "particle": tex(t)}})
    blockstate(name, {"variants": {f"facing={f}": dict({"model": tc("block/" + name)}, **({"y": y} if y else {})) for f, y in HFACING.items()}})
    block_item(name)
    # BlockPillar.breakBlock: the two stones it was made of.
    write(f"{D}/{NS}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
          "entries": [{"type": "minecraft:item", "name": tc(stone), "functions": [{"function": "minecraft:set_count", "count": 2}]}]}],
          "random_sequence": f"{NS}:blocks/{name}"})

for name in ["matrix_speed", "matrix_cost"]:
    model(name, {"parent": "minecraft:block/cube_all", "textures": {"all": tex(name)}})
    simple_state(name)
    block_item(name)
    loot_self(name)

copy_model("stabilizer", {"render_type": "minecraft:cutout"})
simple_state("stabilizer")
block_item("stabilizer")
loot_self("stabilizer")

# ====================================================================== redstone inlay
for m in os.listdir(f"{ORIG}/models/block"):
    if m.startswith("inlay"):
        copy_model(m[:-5], {"render_type": "minecraft:cutout"} if m in ("inlay_dot.json", "inlay_side.json", "inlay_side_alt.json",
                                                                         "inlay_extend.json") else None)
bs = json.load(open(f"{ORIG}/blockstates/inlay.json"))


def fix_when(w):
    if "OR" in w:
        return {"OR": [fix_when(x) for x in w["OR"]]}
    return {k: (str(v).lower() if isinstance(v, bool) else v) for k, v in w.items()}


def fix_apply(a):
    if isinstance(a, list):
        return [fix_apply(x) for x in a]
    return dict(a, model=model_ref(a["model"]))


blockstate("inlay", {"multipart": [dict({"apply": fix_apply(p["apply"])}, **({"when": fix_when(p["when"])} if "when" in p else {}))
                                   for p in bs["multipart"]]})
model("inlay", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:items/inlay"}}, "item")
if not os.path.exists(f"{ORIG}/../thaumcraft/textures/items/inlay.png"):
    copy_model("inlay", kind="item")
loot_self("inlay")

# ====================================================================== candles
copy_model("candle", {"render_type": "minecraft:cutout"})
for c in COLORS:
    name = f"candle_{c}"
    blockstate(name, {"variants": {"": [dict({"model": tc("block/candle")}, **({"y": y} if y else {})) for y in (0, 90, 180, 270)]}})
    block_item(name, "candle")
    loot_self(name)

model("effect_glimmer", {"textures": {"particle": "minecraft:block/glowstone"}})
simple_state("effect_glimmer")

model("rare_earth", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:items/rare_earth"}}, "item")

add_tag("block", "minecraft", "mineable/pickaxe", [tc(n) for n in ["infusion_matrix", "matrix_speed", "matrix_cost", "stabilizer"]
                                                   + list(PEDESTALS) + list(STONE_OF)])

# ====================================================================== recipes
R = f"{D}/{NS}/recipe"
CANDLE_DYES = {c: f"#c:dyes/{c}" for c in COLORS}
write(f"{R}/tallowcandle.json", {"neoforge:conditions": [{"type": "neoforge:item_exists", "item": tc("candle_white")}],
                                 "type": "minecraft:crafting_shaped", "category": "building", "pattern": [" S ", " T ", " T "],
                                 "key": {"S": {"tag": "c:strings"}, "T": {"item": tc("tallow")}},
                                 "result": {"id": tc("candle_white"), "count": 3}})
for c in COLORS:
    write(f"{R}/tallowcandle{c}.json", {"neoforge:conditions": [{"type": "neoforge:item_exists", "item": tc(f"candle_{c}")}],
                                        "type": "minecraft:crafting_shapeless", "category": "building",
                                        "ingredients": [{"tag": f"c:dyes/{c}"}, [{"item": tc(f"candle_{o}")} for o in COLORS if o != c]],
                                        "result": {"id": tc(f"candle_{c}")}})

# The infusion enchantments (InfusionEnchantmentRecipe in ConfigRecipes): any enchanted book plus one item.
ASPECT_NAMES = {"SENSES": "sensus", "EARTH": "terra", "DESIRE": "desiderium", "WATER": "aqua", "AVERSION": "aversio", "ENTROPY": "perditio",
                "ORDER": "ordo", "EXCHANGE": "permutatio", "FIRE": "ignis", "ENERGY": "potentia", "AIR": "aer", "BEAST": "bestia",
                "FLUX": "vitium", "LIGHT": "lux"}
SECOND = {"Items.RABBIT_FOOT": {"item": "minecraft:rabbit_foot"}, "Items.LEAD": {"item": "minecraft:lead"}, "Blocks.TNT": {"item": "minecraft:tnt"},
          "ItemsTC.salisMundus": {"item": tc("salis_mundus")}, "Items.MAP": {"item": "minecraft:map"},
          "Blocks.REDSTONE_BLOCK": {"item": "minecraft:redstone_block"}, "ItemsTC.crystalEssence": {"item": tc("crystal_essence")},
          '"nitor"': {"tag": "thaumcraft:nitor"}}
src = open(os.path.join(ROOT, "reference/thaumcraft/common/config/ConfigRecipes.java")).read()
import re
for m in re.finditer(r'InfusionEnchantmentRecipe IE\w+ = new InfusionEnchantmentRecipe\(\s*EnumInfusionEnchantment\.(\w+),\s*new AspectList\(\)(.*?),\s*'
                     r'new IngredientNBTTC\(new ItemStack\(Items\.ENCHANTED_BOOK\)\),\s*(?:new ItemStack\()?([\w."]+)\)?\s*\);', src, re.S):
    ench = m.group(1).lower()
    aspects = {ASPECT_NAMES[a]: int(n) for a, n in re.findall(r'\.add\(Aspect\.(\w+), (\d+)\)', m.group(2))}
    write(f"{R}/ie{ench}.json", {"neoforge:conditions": [{"type": "neoforge:item_exists", "item": tc("infusion_matrix")}],
                                "type": tc("infusion_enchantment"), "enchantment": ench, "aspects": aspects,
                                "components": [{"item": "minecraft:enchanted_book"}, SECOND[m.group(3)]]})
    print("IE", ench, aspects, m.group(3))

# ====================================================================== lang
L = f"{A}/lang/en_us.json"
en = json.load(open(L, encoding="utf-8"), object_pairs_hook=collections.OrderedDict)
L2 = f"{A}/lang/pt_br.json"
pt = json.load(open(L2, encoding="utf-8"), object_pairs_hook=collections.OrderedDict)
for d in (en, pt):
    for c in COLORS:
        old = d.get(f"tile.candle_{OLD_COLOR.get(c, c)}.name") or d.get(f"block.thaumcraft.candle_{OLD_COLOR.get(c, c)}")
        if old and f"block.thaumcraft.candle_{c}" not in d:
            d[f"block.thaumcraft.candle_{c}"] = old
    for k, old in {"item.thaumcraft.rare_earth": "item.nugget.rareearth.name", "block.thaumcraft.inlay": "tile.inlay.name",
                   "block.thaumcraft.stabilizer": "tile.stabilizer.name", "block.thaumcraft.matrix_speed": "tile.matrix_speed.name",
                   "block.thaumcraft.matrix_cost": "tile.matrix_cost.name", "block.thaumcraft.infusion_matrix": "tile.infusion_matrix.name",
                   "block.thaumcraft.pedestal_arcane": "tile.pedestal_arcane.name", "block.thaumcraft.pedestal_ancient": "tile.pedestal_ancient.name",
                   "block.thaumcraft.pedestal_eldritch": "tile.pedestal_eldritch.name"}.items():
        if old in d and k not in d:
            d[k] = d[old]
en.setdefault("block.thaumcraft.effect_glimmer", "Glimmer")
pt.setdefault("block.thaumcraft.effect_glimmer", "Cintilação")
for ench in ["collector", "destructive", "burrowing", "sounding", "refining", "arcing", "essence", "lamplight", "visbattery", "vischarge",
             "swift", "agile", "infested"]:
    for d in (en, pt):
        old = d.get(f"enchantment.infusion.{ench.upper()}")
        if old:
            d.setdefault(f"enchantment.infusion.{ench}", old)
json.dump(en, open(L, "w", encoding="utf-8"), indent=1, ensure_ascii=False)
json.dump(pt, open(L2, "w", encoding="utf-8"), indent=1, ensure_ascii=False)

subprocess.run([sys.executable, os.path.join(ROOT, "tools/convert_recipes.py"), "arcane", "crucible", "infusion"], check=True)
print("ok")
