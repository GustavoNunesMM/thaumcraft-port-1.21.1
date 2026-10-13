"""Generates the JSON resources of milestone 7 (essentia: jars, tubes, smelteries, alembic, centrifuge, bellows,
transfusers, phials and labels), the crafting components and arcane stone, plus the vanilla crafting recipes of
ConfigRecipes.initializeNormalRecipes that weren't generated yet.

Run from the repo root: python3 tools/gen_marco7.py  (after gen_marco6; it also runs convert_recipes.py)
"""
import json, os, collections, subprocess, sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
RES = os.path.join(ROOT, "mod/src/main/resources")
A = os.path.join(RES, "assets/thaumcraft")
D = os.path.join(RES, "data")
ORIG = os.path.join(ROOT, "decompiled/assets/thaumcraft")
NS = "thaumcraft"


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
    """1.12 blockstate model names (thaumcraft:tube_side, cube_all) -> 1.21 model ids."""
    if ":" not in m:
        return "minecraft:block/" + m
    ns, path = m.split(":")
    return f"{ns}:block/{path}" if not path.startswith(("block/", "item/")) else m


def copy_model(name, extra=None, kind="block", src=None):
    """Copies a 1.12 element model, giving it a parent (for the display transforms) and fixing vanilla parents."""
    m = json.load(open(f"{ORIG}/models/{kind}/{src or name}.json"))
    m.pop("__createdwith", None)
    if "parent" not in m:
        m = {"parent": "minecraft:block/block", **m}
    elif ":" not in m["parent"]:
        m["parent"] = "minecraft:" + m["parent"]
    for k, v in m.get("textures", {}).items():
        if isinstance(v, str) and v.startswith("blocks/"):
            m["textures"][k] = "minecraft:block/" + v[len("blocks/"):]
    for o in m.get("overrides", []):
        o["predicate"] = {(k if ":" in k else tc(k)): v for k, v in o["predicate"].items()}
        o["model"] = o["model"] if "/" in o["model"].split(":")[1] else model_ref(o["model"])
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


def generated_item(name, *layers):
    model(name, {"parent": "minecraft:item/generated", "textures": {f"layer{i}": l for i, l in enumerate(layers)}}, "item")


def loot_self(name, conditions=True):
    pool = {"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": tc(name)}]}
    if conditions:
        pool["conditions"] = [{"condition": "minecraft:survives_explosion"}]
    write(f"{D}/{NS}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [pool], "random_sequence": f"{NS}:blocks/{name}"})


def add_tag(kind, ns, path, values):
    p = f"{D}/{ns}/tags/{kind}/{path}.json"
    d = json.load(open(p)) if os.path.exists(p) else {"replace": False, "values": []}
    for v in values:
        if v not in d["values"]:
            d["values"].append(v)
    write(p, d)


def multipart(name, side_model, core):
    """BlockTube multipart: an arm per connected side and the core."""
    rot = {"north": {"y": 180, "x": 90}, "south": {"x": 90}, "east": {"y": 270, "x": 90}, "west": {"y": 90, "x": 90},
           "up": {"x": 180}, "down": {}}
    parts = [{"when": {d: "true"}, "apply": dict({"model": tc("block/" + side_model)}, **r)} for d, r in rot.items()]
    if core == "tube_core":
        parts.append({"when": {"OR": [{d: "false"} for d in ["north", "east", "south", "west", "up", "down"]]},
                      "apply": {"model": tc("block/tube_core")}})
    else:
        parts.append({"apply": {"model": tc("block/" + core)}})
    blockstate(name, {"multipart": parts})


HFACING = {"north": 0, "east": 90, "south": 180, "west": 270}

# ====================================================================== jars
for jar in ["jar_normal", "jar_void"]:
    for suffix in ["", "_25", "_50", "_75", "_100"]:
        copy_model(jar + suffix, {"render_type": "minecraft:translucent"})
    simple_state(jar)
    copy_model(jar, kind="item")
    # The jar keeps its essentia and label (JarBlockEntity.collectImplicitComponents).
    write(f"{D}/{NS}/loot_table/blocks/{jar}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
          "entries": [{"type": "minecraft:item", "name": tc(jar), "functions": [{"function": "minecraft:copy_components",
                       "source": "block_entity", "include": ["thaumcraft:essentia", "thaumcraft:aspect_filter"]}]}],
          "conditions": [{"condition": "minecraft:survives_explosion"}]}], "random_sequence": f"{NS}:blocks/{jar}"})

# ====================================================================== tubes
for m in ["tube_side", "tube_side_restrict", "tube_core", "tube_core_valve", "tube_buffer_core", "tube_filter_core"]:
    copy_model(m)
TUBES = {"tube": ("tube_side", "tube_core", "tube_normal"), "tube_valve": ("tube_side", "tube_core_valve", "tube_valve"),
         "tube_restrict": ("tube_side_restrict", "tube_core", "tube_restrict"), "tube_oneway": ("tube_side", "tube_core", "tube_oneway"),
         "tube_filter": ("tube_side", "tube_filter_core", "tube_filter"), "tube_buffer": ("tube_side", "tube_buffer_core", "tube_buffer")}
for name, (side, core, item_tex) in TUBES.items():
    multipart(name, side, core)
    copy_model(name, kind="item")
    loot_self(name)

# ====================================================================== smelteries
for kind in ["basic", "thaumium", "void"]:
    name = f"smelter_{kind}"
    for state in ["on", "off"]:
        copy_model(f"{name}_{state}")
    blockstate(name, {"variants": {f"enabled={str(lit).lower()},facing={f}": dict({"model": tc(f"block/{name}_{'on' if lit else 'off'}")},
                                   **({"y": y} if y else {})) for f, y in HFACING.items() for lit in (False, True)}})
    block_item(name, f"{name}_off")
    loot_self(name)
for name in ["smelter_aux", "smelter_vent"]:
    copy_model(name)
    blockstate(name, {"variants": {f"facing={f}": dict({"model": tc("block/" + name)}, **({"y": y} if y else {})) for f, y in HFACING.items()}})
    block_item(name)
    loot_self(name)

# ====================================================================== alembic, centrifuge, bellows, transfusers
copy_model("alembic_normal")
simple_state("alembic", "alembic_normal")
block_item("alembic", "alembic_normal")
loot_self("alembic")
# Centrifuge and bellows are drawn by their block entity renderers; the block model only gives the break particles.
for name in ["centrifuge", "bellows"]:
    copy_model(name)
    model(name + "_particle", {"textures": {"particle": tex(name)}})
    loot_self(name)
    block_item(name)
simple_state("centrifuge", "centrifuge_particle")
blockstate("bellows", {"variants": {f"enabled={e},facing={f}": {"model": tc("block/bellows_particle")}
                                    for f in ["down", "up", "north", "south", "west", "east"] for e in ["false", "true"]}})
ALL_FACING = {"down": {"x": 180}, "up": {}, "east": {"y": 90, "x": 90}, "west": {"y": 270, "x": 90}, "north": {"x": 90},
              "south": {"y": 180, "x": 90}}
for name in ["essentia_input", "essentia_output"]:
    copy_model(name)
    blockstate(name, {"variants": {f"facing={f}": dict({"model": tc("block/" + name)}, **r) for f, r in ALL_FACING.items()}})
    block_item(name)
    loot_self(name)

# ====================================================================== stone
CUBE_MODELS = {"stone_arcane": "arcane_stone", "stone_ancient": "ancient_stone", "stone_eldritch_tile": "eldritch_stone"}
for name, m in CUBE_MODELS.items():
    copy_model(m)
    rots = [{}, {"x": 90}, {"y": 90}, {"x": 90, "y": 90}]
    blockstate(name, {"variants": {"": [dict({"model": tc("block/" + m)}, **r) for r in rots]}})
    block_item(name, m)
CUBE_ALL = {"stone_arcane_brick": "arcane_brick_stone", "stone_ancient_tile": "ancient_tile", "stone_ancient_glyphed": "ancient_glyph",
            "stone_ancient_doorway": "eldritch_door"}
for name, t in CUBE_ALL.items():
    model(name, {"parent": "minecraft:block/cube_all", "textures": {"all": tex(t)}})
    simple_state(name)
    block_item(name)
for i in (1, 2, 3):
    model(f"stone_ancient_rock_{i}", {"parent": "minecraft:block/cube_all", "textures": {"all": tex(f"ancient_rock_stone_{i}")}})
blockstate("stone_ancient_rock", {"variants": {"": [{"model": tc(f"block/stone_ancient_rock_{i}")} for i in (1, 2, 3)]}})
block_item("stone_ancient_rock", "stone_ancient_rock_2")
for name in ["stone_arcane", "stone_arcane_brick", "stone_ancient", "stone_ancient_tile", "stone_ancient_glyphed", "stone_eldritch_tile"]:
    loot_self(name)
# Ancient rock and the doorway can't be broken; no loot.

with open(os.path.join(ROOT, "tools/vanilla/oak_stairs.json")) as f:
    OAK_STAIRS = f.read()
STAIRS = {"stairs_arcane": ("arcane_stone_1", "arcane_stone_2", "arcane_stone_3"),
          "stairs_arcane_brick": ("arcane_brick_stone",) * 3, "stairs_ancient": ("ancient_tile",) * 3}
for name, (bottom, top, side) in STAIRS.items():
    tx = {"bottom": tex(bottom), "top": tex(top), "side": tex(side)}
    model(name, {"parent": "minecraft:block/stairs", "textures": tx})
    model(name + "_inner", {"parent": "minecraft:block/inner_stairs", "textures": tx})
    model(name + "_outer", {"parent": "minecraft:block/outer_stairs", "textures": tx})
    blockstate(name, {"variants": json.loads(OAK_STAIRS.replace("minecraft:block/oak_stairs", tc("block/" + name)))["variants"]})
    block_item(name)
    loot_self(name)
SLABS = {"slab_arcane_stone": (("arcane_stone_1", "arcane_stone_2", "arcane_stone_3"), "block/arcane_stone"),
         "slab_arcane_brick": (("arcane_brick_stone",) * 3, "block/stone_arcane_brick"),
         "slab_ancient": (("ancient_stone_1", "ancient_stone_2", "ancient_stone_3"), "block/ancient_stone"),
         "slab_eldritch": (("eldritch_stone_1", "eldritch_stone_2", "eldritch_stone_3"), "block/eldritch_stone")}
for name, ((bottom, top, side), full) in SLABS.items():
    tx = {"bottom": tex(bottom), "top": tex(top), "side": tex(side)}
    model(name, {"parent": "minecraft:block/slab", "textures": tx})
    model(name + "_top", {"parent": "minecraft:block/slab_top", "textures": tx})
    blockstate(name, {"variants": {"type=bottom": {"model": tc("block/" + name)}, "type=top": {"model": tc("block/" + name + "_top")},
                                   "type=double": {"model": tc(full)}}})
    block_item(name)
    write(f"{D}/{NS}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [
        {"type": "minecraft:item", "name": tc(name), "functions": [
            {"function": "minecraft:set_count", "count": 2, "add": False, "conditions": [
                {"condition": "minecraft:block_state_property", "block": tc(name), "properties": {"type": "double"}}]},
            {"function": "minecraft:explosion_decay"}]}]}], "random_sequence": f"{NS}:blocks/{name}"})

STONES = list(CUBE_MODELS) + list(CUBE_ALL) + ["stone_ancient_rock"] + list(STAIRS) + list(SLABS)
add_tag("block", "minecraft", "mineable/pickaxe", [tc(n) for n in STONES] + [tc(n) for n in [
    "smelter_basic", "smelter_thaumium", "smelter_void", "smelter_aux", "smelter_vent", "essentia_input", "essentia_output",
    "tube", "tube_valve", "tube_restrict", "tube_oneway", "tube_filter", "tube_buffer"]])
add_tag("block", "minecraft", "mineable/axe", [tc(n) for n in ["alembic", "centrifuge", "bellows"]])
add_tag("block", "minecraft", "stairs", [tc(n) for n in STAIRS])
add_tag("item", "minecraft", "stairs", [tc(n) for n in STAIRS])
add_tag("block", "minecraft", "slabs", [tc(n) for n in SLABS])
add_tag("item", "minecraft", "slabs", [tc(n) for n in SLABS])
add_tag("block", "minecraft", "beacon_base_blocks", [tc(n) for n in ["stone_arcane", "stone_arcane_brick", "stone_ancient", "stone_ancient_tile",
                                                                      "stone_ancient_glyphed", "stone_eldritch_tile"]])
add_tag("block", "minecraft", "wither_immune", [tc("stone_ancient_rock"), tc("stone_ancient_doorway")])
add_tag("block", "minecraft", "dragon_immune", [tc("stone_ancient_rock"), tc("stone_ancient_doorway")])

# ====================================================================== items
generated_item("phial", f"{NS}:items/phial")
generated_item("phial_filled", f"{NS}:items/phial", f"{NS}:items/phial_overlay")
generated_item("label", f"{NS}:items/label")
generated_item("label_filled", f"{NS}:items/label", f"{NS}:items/label_overlay")
SIMPLE = ["jar_brace", "fabric", "vis_resonator", "mechanism_simple", "mechanism_complex", "plate_brass", "plate_iron", "plate_thaumium",
          "plate_void", "filter", "morphic_resonator", "mirrored_glass", "void_seed", "mind_clockwork", "mind_biothaumic", "module_vision",
          "module_aggression"]
for name in SIMPLE:
    generated_item(name, f"{NS}:items/{name}")
for name in ["caster_gauntlet", "caster_gauntlet_focus", "caster_basic_model", "caster_basic_focus_model"]:
    copy_model(name, kind="item")
copy_model("caster_basic", kind="item")
# The gauntlet models use the armour skin, outside the block atlas folders: add it to the atlas.
p = f"{RES}/assets/minecraft/atlases/blocks.json"
atlas = json.load(open(p)) if os.path.exists(p) else {"sources": []}
for t in [f"{NS}:entity/armor/caster_basic"]:
    if {"type": "single", "resource": t} not in atlas["sources"]:
        atlas["sources"].append({"type": "single", "resource": t})
write(p, atlas)

add_tag("item", "c", "plates", ["#c:plates/brass", "#c:plates/iron", "#c:plates/thaumium", "#c:plates/void"])
for metal in ["brass", "iron", "thaumium", "void"]:
    add_tag("item", "c", f"plates/{metal}", [tc(f"plate_{metal}")])

# ====================================================================== vanilla crafting recipes (initializeNormalRecipes)
R = f"{D}/{NS}/recipe"


def cond(*items):
    return [{"type": "neoforge:item_exists", "item": tc(i)} for i in items]


def shaped(name, result, count, pattern, key, needs=()):
    write(f"{R}/{name}.json", {"neoforge:conditions": cond(*needs), "type": "minecraft:crafting_shaped", "category": "misc",
                               "pattern": pattern, "key": {k: ({"tag": v[1:]} if v.startswith("#") else {"item": tc(v) if ":" in v or v in needs else v})
                                                          for k, v in key.items()},
                               "result": {"id": tc(result) if ":" not in result else result, "count": count}})


def shapeless(name, result, count, ingredients, needs=(), components=None):
    res = {"id": result if ":" in result else tc(result), "count": count}
    if components:
        res["components"] = components
    write(f"{R}/{name}.json", {"neoforge:conditions": cond(*needs), "type": "minecraft:crafting_shapeless", "category": "misc",
                               "ingredients": [{"tag": i[1:]} if i.startswith("#") else {"item": i if ":" in i else tc(i)} for i in ingredients],
                               "result": res})


shaped("ironplate", "plate_iron", 3, ["BBB"], {"B": "#c:ingots/iron"}, ["plate_iron"])
shaped("brassplate", "plate_brass", 3, ["BBB"], {"B": "#c:ingots/brass"}, ["plate_brass"])
shaped("thaumiumplate", "plate_thaumium", 3, ["BBB"], {"B": "#c:ingots/thaumium"}, ["plate_thaumium"])
shaped("voidplate", "plate_void", 3, ["BBB"], {"B": "#c:ingots/void"}, ["plate_void"])
shaped("phial", "phial", 8, [" C ", "G G", " G "], {"G": "#c:glass_blocks", "C": "minecraft:clay_ball"}, ["phial"])
shapeless("jarlabel", "label", 4, ["#c:dyes/black", "#c:slime_balls", "minecraft:paper", "minecraft:paper", "minecraft:paper", "minecraft:paper"],
          ["label"])
write(f"{R}/jarlabelaspect.json", {"neoforge:conditions": cond("label_filled"), "type": tc("jar_label"), "category": "misc"})
shapeless("jarlabelnull", "label", 1, ["label_filled"], ["label", "label_filled"])
shaped("brassbrace", "jar_brace", 2, ["NSN", "S S", "NSN"], {"N": "#c:nuggets/brass", "S": "#c:rods/wooden"}, ["jar_brace"])
shapeless("scribingtoolscraft1", "scribing_tools", 1, ["phial", "minecraft:feather", "#c:dyes/black"], ["phial", "scribing_tools"])
shaped("stonearcane", "stone_arcane", 9, ["KKK", "KCK", "KKK"], {"K": "#c:stones", "C": "crystal_essence"}, ["stone_arcane", "crystal_essence"])
shaped("brickarcane", "stone_arcane_brick", 4, ["KK", "KK"], {"K": "stone_arcane"}, ["stone_arcane", "stone_arcane_brick"])
for name, block, stairs, slab in [("arcane", "stone_arcane", "stairs_arcane", "slab_arcane_stone"),
                                  ("arcanebrick", "stone_arcane_brick", "stairs_arcane_brick", "slab_arcane_brick"),
                                  ("ancient", "stone_ancient", "stairs_ancient", "slab_ancient"),
                                  ("eldritch", "stone_eldritch_tile", None, "slab_eldritch")]:
    if stairs:
        shaped(f"stairs{name}", stairs, 4, ["K  ", "KK ", "KKK"], {"K": block}, [block, stairs])
    shaped(f"slab{name}", slab, 6, ["KKK"], {"K": block}, [block, slab])

# ShapedArcaneVoidJar: a warded jar plus vis and an entropy crystal (the original also kept the jar's contents).
write(f"{R}/jarvoid.json", {"neoforge:conditions": cond("jar_normal", "jar_void"), "type": tc("arcane_shaped"), "research": "WARDEDJARS",
                            "vis": 50, "crystals": {"perditio": 1}, "pattern": ["J"], "key": {"J": {"item": tc("jar_normal")}},
                            "result": {"id": tc("jar_void")}})

# ====================================================================== lang
L = f"{A}/lang/en_us.json"
en = json.load(open(L, encoding="utf-8"), object_pairs_hook=collections.OrderedDict)
L2 = f"{A}/lang/pt_br.json"
pt = json.load(open(L2, encoding="utf-8"), object_pairs_hook=collections.OrderedDict)
OLD = {"item.thaumcraft.plate_brass": "item.plate.brass.name", "item.thaumcraft.plate_iron": "item.plate.iron.name",
       "item.thaumcraft.plate_thaumium": "item.plate.thaumium.name", "item.thaumcraft.plate_void": "item.plate.void.name",
       "item.thaumcraft.mind_clockwork": "item.mind.clockwork.name", "item.thaumcraft.mind_biothaumic": "item.mind.biothaumic.name",
       "item.thaumcraft.module_vision": "item.module.vision.name", "item.thaumcraft.module_aggression": "item.module.aggression.name",
       "item.thaumcraft.phial": "item.phial.empty.name", "item.thaumcraft.phial_filled": "item.phial.filled.name",
       "item.thaumcraft.label": "item.label.blank.name", "item.thaumcraft.jar_brace": "item.jar_brace.name"}
for new, old in OLD.items():
    for d in (en, pt):
        if old in d and new not in d:
            d[new] = d[old]
en["item.thaumcraft.label_filled"] = "Marked Label (%s)"
pt["item.thaumcraft.label_filled"] = "Rótulo Marcado (%s)"
pt["item.thaumcraft.label"] = "Rótulo em Branco"
pt["item.thaumcraft.phial_filled"] = "Frasco de Essentia de %s"
for k in ["item.label.blank.name"]:
    pt[k] = "Rótulo em Branco"
pt["item.label.filled.name"] = "Rótulo Marcado"
json.dump(en, open(L, "w", encoding="utf-8"), indent=1, ensure_ascii=False)
json.dump(pt, open(L2, "w", encoding="utf-8"), indent=1, ensure_ascii=False)

subprocess.run([sys.executable, os.path.join(ROOT, "tools/convert_recipes.py"), "arcane", "crucible"], check=True)
print("ok")
