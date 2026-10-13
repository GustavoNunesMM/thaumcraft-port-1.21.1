"""Generates the JSON resources of milestone 2 (ores, crystals, trees, plants, materials, worldgen).

Run from the repo root: python3 tools/gen_marco2.py
Everything it writes is plain data; edit this script rather than the generated files.
"""
import json, os, shutil

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
RES = os.path.join(ROOT, "mod/src/main/resources")
A = os.path.join(RES, "assets/thaumcraft")
D = os.path.join(RES, "data")
NS = "thaumcraft"


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def tc(name):
    return f"{NS}:{name}"


def tex(name):
    return f"{NS}:blocks/{name}"


def blockstate(name, obj):
    write(f"{A}/blockstates/{name}.json", obj)


def model(name, obj, kind="block"):
    write(f"{A}/models/{kind}/{name}.json", obj)


def simple_state(name, model_name=None):
    blockstate(name, {"variants": {"": {"model": tc("block/" + (model_name or name))}}})


def block_item(name, parent=None):
    model(name, {"parent": tc("block/" + (parent or name))}, "item")


def generated_item(name, texture):
    model(name, {"parent": "minecraft:item/generated", "textures": {"layer0": texture}}, "item")


def loot_self(name):
    write(f"{D}/{NS}/loot_table/blocks/{name}.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "bonus_rolls": 0,
                   "entries": [{"type": "minecraft:item", "name": tc(name)}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}],
        "random_sequence": f"{NS}:blocks/{name}"})


SILK = {"condition": "minecraft:match_tool", "predicate": {"predicates": {"minecraft:enchantments": [
    {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}
SHEARS_OR_SILK = {"condition": "minecraft:any_of", "terms": [
    {"condition": "neoforge:can_item_perform_ability", "ability": "shears_dig"}, SILK]}


def loot_ore(name, drop, count=None):
    funcs = []
    if count:
        funcs.append({"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": count[0], "max": count[1]}, "add": False})
    funcs += [{"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
              {"function": "minecraft:explosion_decay"}]
    write(f"{D}/{NS}/loot_table/blocks/{name}.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": tc(name), "conditions": [SILK]},
            {"type": "minecraft:item", "name": drop, "functions": funcs}]}]}],
        "random_sequence": f"{NS}:blocks/{name}"})


# ====================================================================== blocks

ORES = {  # name: (texture, loot)
    "ore_amber": ("ore_amber", ("thaumcraft:amber", (1, 2))),
    "ore_cinnabar": ("ore_cinnabar", None),
    "ore_quartz": ("ore_quartz", ("minecraft:quartz", None)),
    "deepslate_ore_cinnabar": ("deepslate_ore_cinnabar", None),
    "deepslate_ore_quartz": ("deepslate_ore_quartz", ("minecraft:quartz", None)),
}
for name, (t, loot) in ORES.items():
    simple_state(name)
    model(name, {"parent": "minecraft:block/cube_all", "textures": {"all": tex(t)}})
    block_item(name)
    if loot:
        loot_ore(name, loot[0], loot[1])
    else:
        loot_self(name)

for name in ["metal_thaumium", "metal_void", "metal_brass", "plank_greatwood", "plank_silverwood"]:
    simple_state(name)
    model(name, {"parent": "minecraft:block/cube_all", "textures": {"all": tex(name)}})
    block_item(name)
    loot_self(name)

# Amber: translucent like the original BlockTranslucent.
simple_state("amber_block")
model("amber_block", {"parent": "minecraft:block/cube_bottom_top", "render_type": "minecraft:translucent",
                      "textures": {"bottom": tex("amber_top"), "top": tex("amber_top"), "side": tex("amber_side")}})
simple_state("amber_brick")
model("amber_brick", {"parent": "minecraft:block/cube_all", "render_type": "minecraft:translucent", "textures": {"all": tex("amber_brick")}})
for n in ["amber_block", "amber_brick"]:
    block_item(n)
    loot_self(n)

# Logs
for wood in ["greatwood", "silverwood"]:
    name = f"log_{wood}"
    model(name, {"parent": "minecraft:block/cube_column", "textures": {"end": tex(f"log_{wood}_top"), "side": tex(f"log_{wood}")}})
    model(name + "_horizontal", {"parent": "minecraft:block/cube_column_horizontal",
                                 "textures": {"end": tex(f"log_{wood}_top"), "side": tex(f"log_{wood}")}})
    blockstate(name, {"variants": {
        "axis=x": {"model": tc(f"block/{name}_horizontal"), "x": 90, "y": 90},
        "axis=y": {"model": tc(f"block/{name}")},
        "axis=z": {"model": tc(f"block/{name}_horizontal"), "x": 90}}})
    block_item(name)
    loot_self(name)

    # Leaves: greatwood takes the biome foliage colour (tinted vanilla leaves model); silverwood's texture is final.
    leaves = f"leaves_{wood}"
    simple_state(leaves)
    model(leaves, {"parent": "minecraft:block/leaves" if wood == "greatwood" else "minecraft:block/cube_all",
                   "render_type": "minecraft:cutout_mipped", "textures": {"all": tex(leaves)}})
    block_item(leaves)

    sap = f"sapling_{wood}"
    blockstate(sap, {"variants": {"stage=0": {"model": tc("block/" + sap)}, "stage=1": {"model": tc("block/" + sap)}}})
    model(sap, {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout", "textures": {"cross": tex(sap)}})
    generated_item(sap, tex(sap))
    loot_self(sap)

    # Stairs and slabs from the planks
    plank = f"plank_{wood}"
    stairs, slab = f"stairs_{wood}", f"slab_{wood}"
    tx = {"bottom": tex(plank), "top": tex(plank), "side": tex(plank)}
    model(stairs, {"parent": "minecraft:block/stairs", "textures": tx})
    model(stairs + "_inner", {"parent": "minecraft:block/inner_stairs", "textures": tx})
    model(stairs + "_outer", {"parent": "minecraft:block/outer_stairs", "textures": tx})
    # Same shapes/rotations as vanilla oak stairs.
    with open(os.path.join(ROOT, "tools/vanilla/oak_stairs.json")) as f:
        vanilla = f.read().replace("minecraft:block/oak_stairs", tc("block/" + stairs))
    variants = json.loads(vanilla)["variants"]
    blockstate(stairs, {"variants": variants})
    block_item(stairs)
    loot_self(stairs)

    model(slab, {"parent": "minecraft:block/slab", "textures": tx})
    model(slab + "_top", {"parent": "minecraft:block/slab_top", "textures": tx})
    blockstate(slab, {"variants": {
        "type=bottom": {"model": tc("block/" + slab)},
        "type=top": {"model": tc("block/" + slab + "_top")},
        "type=double": {"model": tc("block/" + plank)}}})
    block_item(slab)
    write(f"{D}/{NS}/loot_table/blocks/{slab}.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": tc(slab), "functions": [
            {"function": "minecraft:set_count", "count": 2, "add": False, "conditions": [
                {"condition": "minecraft:block_state_property", "block": tc(slab), "properties": {"type": "double"}}]},
            {"function": "minecraft:explosion_decay"}]}]}],
        "random_sequence": f"{NS}:blocks/{slab}"})


def chance_list(denominators):
    return [round(1.0 / d, 6) for d in denominators]


# Leaf drops follow 1.12 BlockLeaves with getSaplingDropChance() = 75: 1/75, fortune lowers it by 2<<fortune.
for wood in ["greatwood", "silverwood"]:
    leaves = f"leaves_{wood}"
    pools = [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:alternatives", "children": [
        {"type": "minecraft:item", "name": tc(leaves), "conditions": [SHEARS_OR_SILK]},
        {"type": "minecraft:item", "name": tc(f"sapling_{wood}"), "conditions": [
            {"condition": "minecraft:survives_explosion"},
            {"condition": "minecraft:table_bonus", "enchantment": "minecraft:fortune", "chances": chance_list([75, 71, 67, 59])}]}]}]}]
    if wood == "silverwood":
        # dropApple: a quicksilver drop, 1 in (200 - (10 << fortune)) * 0.75
        pools.append({"rolls": 1, "bonus_rolls": 0,
                      "conditions": [{"condition": "minecraft:inverted", "term": SHEARS_OR_SILK}],
                      "entries": [{"type": "minecraft:item", "name": tc("nugget_quicksilver"), "conditions": [
                          {"condition": "minecraft:survives_explosion"},
                          {"condition": "minecraft:table_bonus", "enchantment": "minecraft:fortune", "chances": chance_list([150, 135, 120, 90])}]}]})
    write(f"{D}/{NS}/loot_table/blocks/{leaves}.json", {"type": "minecraft:block", "pools": pools, "random_sequence": f"{NS}:blocks/{leaves}"})

# Plants
for name in ["shimmerleaf", "cinderpearl", "vishroom"]:
    simple_state(name)
    model(name, {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout", "textures": {"cross": tex(name)}})
    generated_item(name, tex(name))
    loot_self(name)

# ====================================================================== vis crystals
# The crystal OBJ holds 8 shards growing up from the floor. Each size shows size+1 of them, in three random
# arrangements (the original shuffled them per position); multipart rotates the model onto every rock face.
shutil.copy(os.path.join(ROOT, "decompiled/assets/thaumcraft/models/obj/crystal.obj"), f"{A}/models/block/crystal.obj")
with open(f"{A}/models/block/crystal.mtl", "w") as f:
    f.write("newmtl Material__25\nKd 1.0 1.0 1.0\nmap_Kd #crystal\nneoforge_TintIndex 0\n")
SHARDS = ["s014", "s015", "s016", "s017", "s018", "s019", "s024", "s026"]
ARRANGEMENTS = [[0, 1, 2, 3, 4, 5, 6, 7], [5, 2, 7, 0, 3, 6, 1, 4], [3, 6, 0, 4, 7, 1, 5, 2]]
for size in range(4):
    for v, order in enumerate(ARRANGEMENTS):
        shown = {SHARDS[i] for i in order[:size + 1]}
        model(f"crystal/size{size}_{v}", {
            "loader": "neoforge:obj", "model": tc("models/block/crystal.obj"), "flip_v": True,
            "render_type": "minecraft:cutout", "emissive_ambient": False,
            "textures": {"crystal": tex("crystal"), "particle": tex("crystal")},
            "visibility": {s: (s in shown) for s in SHARDS}})
# face property -> rotation that turns an upward crystal away from that face (same as end rod facings)
FACE_ROT = {"down": {}, "up": {"x": 180}, "north": {"x": 90, "y": 180}, "south": {"x": 90},
            "west": {"x": 90, "y": 90}, "east": {"x": 90, "y": 270}}
CRYSTALS = {"crystal_aer": ("aer", 1), "crystal_ignis": ("ignis", 1), "crystal_aqua": ("aqua", 1), "crystal_terra": ("terra", 1),
            "crystal_ordo": ("ordo", 1), "crystal_perditio": ("perditio", 1), "crystal_vitium": ("vitium", 1)}
crystal_multipart = []
for face, rot in FACE_ROT.items():
    for size in range(4):
        crystal_multipart.append({"when": {face: "true", "size": str(size)},
                                  "apply": [dict({"model": tc(f"block/crystal/size{size}_{v}")}, **rot) for v in range(3)]})
for name, (aspect, _) in CRYSTALS.items():
    blockstate(name, {"multipart": crystal_multipart})
    generated_item(name, f"{NS}:items/crystal_planter")
    # Drops size+1 vis crystals of its aspect, never itself (no silk touch in the original either).
    funcs = [{"function": "minecraft:set_components", "components": {"thaumcraft:essentia": {aspect: 1}}}]
    for size in range(1, 4):
        funcs.append({"function": "minecraft:set_count", "count": size + 1, "add": False, "conditions": [
            {"condition": "minecraft:block_state_property", "block": tc(name), "properties": {"size": str(size)}}]})
    write(f"{D}/{NS}/loot_table/blocks/{name}.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": tc("crystal_essence"), "functions": funcs}]}],
        "random_sequence": f"{NS}:blocks/{name}"})

# ====================================================================== items
ITEMS = ["amber", "quicksilver", "ingot_thaumium", "ingot_void", "ingot_brass", "nugget_thaumium", "nugget_void", "nugget_brass",
         "nugget_quicksilver", "nugget_quartz", "nugget_copper", "cluster_iron", "cluster_gold", "cluster_copper",
         "cluster_cinnabar", "cluster_quartz", "crystal_essence"]
for name in ITEMS:
    generated_item(name, f"{NS}:items/{name}")

# ====================================================================== recipes
R = f"{D}/{NS}/recipe"


def item(i):
    return {"tag": i[1:]} if i.startswith("#") else {"item": i if ":" in i else tc(i)}


def rid(i):
    return i if ":" in i else tc(i)


def shaped(name, result, count, pattern, key, category="misc"):
    write(f"{R}/{name}.json", {"type": "minecraft:crafting_shaped", "category": category, "pattern": pattern,
                               "key": {k: item(v) for k, v in key.items()}, "result": {"id": rid(result), "count": count}})


def cook(name, ingredient, result, xp, count=1):
    for kind, time in [("smelting", 200), ("blasting", 100)]:
        write(f"{R}/{name}_{kind}.json", {"type": f"minecraft:{kind}", "category": "misc", "ingredient": item(ingredient),
                                          "result": {"id": rid(result), "count": count}, "experience": xp, "cookingtime": time})


for metal in ["thaumium", "void", "brass"]:
    shaped(f"{metal}tonuggets", f"nugget_{metal}", 9, ["#"], {"#": f"#c:ingots/{metal}"})
    shaped(f"nuggetsto{metal}", f"ingot_{metal}", 1, ["###", "###", "###"], {"#": f"#c:nuggets/{metal}"})
    shaped(f"{metal}ingotstoblock", f"metal_{metal}", 1, ["###", "###", "###"], {"#": f"ingot_{metal}"}, "building")
    shaped(f"{metal}blocktoingots", f"ingot_{metal}", 9, ["#"], {"#": f"metal_{metal}"})
shaped("quartztonuggets", "nugget_quartz", 9, ["#"], {"#": "minecraft:quartz"})
shaped("quicksilvertonuggets", "nugget_quicksilver", 9, ["#"], {"#": "quicksilver"})
shaped("nuggetstoquicksilver", "quicksilver", 1, ["###", "###", "###"], {"#": "#c:nuggets/quicksilver"})
shaped("ambertoblock", "amber_block", 1, ["##", "##"], {"#": "#c:gems/amber"}, "building")
shaped("amberblocktobrick", "amber_brick", 4, ["##", "##"], {"#": "amber_block"}, "building")
shaped("amberbricktoblock", "amber_block", 4, ["##", "##"], {"#": "amber_brick"}, "building")
shaped("amberblocktoamber", "amber", 4, ["#"], {"#": "amber_block"})
shaped("shimmerleaftoquicksilver", "quicksilver", 1, ["#"], {"#": "shimmerleaf"})
shaped("cinderpearltoblazepowder", "minecraft:blaze_powder", 1, ["#"], {"#": "cinderpearl"})
for wood in ["greatwood", "silverwood"]:
    shaped(f"plank{wood}", f"plank_{wood}", 4, ["W"], {"W": f"log_{wood}"}, "building")
    shaped(f"stairs{wood}", f"stairs_{wood}", 4, ["K  ", "KK ", "KKK"], {"K": f"plank_{wood}"}, "building")
    shaped(f"slab{wood}", f"slab_{wood}", 6, ["KKK"], {"K": f"plank_{wood}"}, "building")
    write(f"{R}/charcoal_from_log_{wood}.json", {"type": "minecraft:smelting", "category": "misc", "ingredient": item(f"log_{wood}"),
                                               "result": {"id": "minecraft:charcoal"}, "experience": 0.5, "cookingtime": 200})
cook("quicksilver_from_ore", "#c:ores/cinnabar", "quicksilver", 1.0)
cook("amber_from_ore", "ore_amber", "amber", 1.0)
cook("quartz_from_ore", "ore_quartz", "minecraft:quartz", 1.0)
cook("quartz_from_deepslate_ore", "deepslate_ore_quartz", "minecraft:quartz", 1.0)
for cluster, result in [("iron", "minecraft:iron_ingot"), ("gold", "minecraft:gold_ingot"), ("copper", "minecraft:copper_ingot"),
                        ("cinnabar", "quicksilver"), ("quartz", "minecraft:quartz")]:
    cook(f"{cluster}_from_cluster", f"cluster_{cluster}", result, 1.0, 2)

# ====================================================================== tags


def tag(registry, ns, path, values):
    write(f"{D}/{ns}/tags/{registry}/{path}.json", {"replace": False, "values": values})


def both(ns, path, values):
    tag("block", ns, path, values)
    tag("item", ns, path, values)


ore_blocks = [tc(n) for n in ORES]
tag("block", "minecraft", "mineable/pickaxe", ore_blocks + [tc(n) for n in ["metal_thaumium", "metal_void", "metal_brass", "amber_block", "amber_brick"]]
    + [tc(n) for n in CRYSTALS])
tag("block", "minecraft", "mineable/axe", [tc(f"{k}_{w}") for w in ["greatwood", "silverwood"] for k in ["log", "plank", "stairs", "slab"]])
tag("block", "minecraft", "mineable/hoe", [tc("leaves_greatwood"), tc("leaves_silverwood")])
tag("block", "minecraft", "needs_stone_tool", [tc("ore_amber")])
tag("block", "minecraft", "needs_iron_tool", [tc("ore_cinnabar"), tc("deepslate_ore_cinnabar")])
tag("block", "minecraft", "beacon_base_blocks", [tc(n) for n in ["metal_thaumium", "metal_void", "metal_brass", "amber_block", "amber_brick"]])
both(NS, "greatwood_logs", [tc("log_greatwood")])
both(NS, "silverwood_logs", [tc("log_silverwood")])
both("minecraft", "logs_that_burn", ["#thaumcraft:greatwood_logs", "#thaumcraft:silverwood_logs"])
both("minecraft", "planks", [tc("plank_greatwood"), tc("plank_silverwood")])
both("minecraft", "wooden_stairs", [tc("stairs_greatwood"), tc("stairs_silverwood")])
both("minecraft", "wooden_slabs", [tc("slab_greatwood"), tc("slab_silverwood")])
both("minecraft", "leaves", [tc("leaves_greatwood"), tc("leaves_silverwood")])
both("minecraft", "saplings", [tc("sapling_greatwood"), tc("sapling_silverwood")])
both("c", "ores", ["#c:ores/cinnabar", "#c:ores/amber"])
both("c", "ores/cinnabar", [tc("ore_cinnabar"), tc("deepslate_ore_cinnabar")])
both("c", "ores/amber", [tc("ore_amber")])
both("c", "ores/quartz", [tc("ore_quartz"), tc("deepslate_ore_quartz")])
both("c", "ores_in_ground/stone", [tc("ore_cinnabar"), tc("ore_amber"), tc("ore_quartz")])
both("c", "ores_in_ground/deepslate", [tc("deepslate_ore_cinnabar"), tc("deepslate_ore_quartz")])
both("c", "storage_blocks", ["#c:storage_blocks/thaumium", "#c:storage_blocks/void", "#c:storage_blocks/brass", "#c:storage_blocks/amber"])
for m in ["thaumium", "void", "brass"]:
    both("c", f"storage_blocks/{m}", [tc(f"metal_{m}")])
    tag("item", "c", f"ingots/{m}", [tc(f"ingot_{m}")])
    tag("item", "c", f"nuggets/{m}", [tc(f"nugget_{m}")])
both("c", "storage_blocks/amber", [tc("amber_block")])
tag("item", "c", "ingots", ["#c:ingots/thaumium", "#c:ingots/void", "#c:ingots/brass"])
tag("item", "c", "nuggets", ["#c:nuggets/thaumium", "#c:nuggets/void", "#c:nuggets/brass", "#c:nuggets/quicksilver",
                             "#c:nuggets/quartz", "#c:nuggets/copper"])
tag("item", "c", "nuggets/quicksilver", [tc("nugget_quicksilver")])
tag("item", "c", "nuggets/quartz", [tc("nugget_quartz")])
tag("item", "c", "nuggets/copper", [tc("nugget_copper")])
tag("item", "c", "gems", ["#c:gems/amber"])
tag("item", "c", "gems/amber", [tc("amber")])

# ====================================================================== worldgen
W = f"{D}/{NS}/worldgen"
STONE = {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables"}
DEEP = {"predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables"}


def configured(name, typ, config):
    write(f"{W}/configured_feature/{name}.json", {"type": typ, "config": config})


def placed(name, feature, placement):
    write(f"{W}/placed_feature/{name}.json", {"feature": tc(feature), "placement": placement})


def uniform_height(lo, hi):
    return {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": lo}, "max_inclusive": {"absolute": hi}}}


BIOME = {"type": "minecraft:biome"}
# 1.12 placed single ore blocks at random spots: 18 cinnabar below y=51, 18 quartz below y=64, 20 amber up to 24 under
# the surface. 1.21 adds 64 layers below y=0: the same 18 tries are spread over the taller range (stone and deepslate),
# so a chunk holds as much cinnabar/quartz as in the original instead of extra deepslate ore on top of it.
configured("ore_cinnabar", "minecraft:replace_single_block", {"targets": [
    {"target": STONE, "state": {"Name": tc("ore_cinnabar")}}, {"target": DEEP, "state": {"Name": tc("deepslate_ore_cinnabar")}}]})
placed("ore_cinnabar", "ore_cinnabar", [{"type": "minecraft:count", "count": 18}, {"type": "minecraft:in_square"}, uniform_height(-64, 50), BIOME])
configured("ore_quartz", "minecraft:replace_single_block", {"targets": [
    {"target": STONE, "state": {"Name": tc("ore_quartz")}}, {"target": DEEP, "state": {"Name": tc("deepslate_ore_quartz")}}]})
placed("ore_quartz", "ore_quartz", [{"type": "minecraft:count", "count": 18}, {"type": "minecraft:in_square"}, uniform_height(-64, 63), BIOME])
configured("ore_amber", "minecraft:replace_single_block", {"targets": [{"target": STONE, "state": {"Name": tc("ore_amber")}}]})
placed("ore_amber", "ore_amber", [
    {"type": "minecraft:count", "count": 20}, {"type": "minecraft:in_square"},
    {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR_WG"},
    # 1 to 25 blocks down; random_offset only reaches 16, so: -13, then -12..+12.
    {"type": "minecraft:random_offset", "xz_spread": 0, "y_spread": -13},
    {"type": "minecraft:random_offset", "xz_spread": 0, "y_spread": {"type": "minecraft:uniform", "min_inclusive": -12, "max_inclusive": 12}},
    BIOME])
configured("vis_crystals", tc("vis_crystals"), {})
placed("vis_crystals", "vis_crystals", [BIOME])
# Trees: one try per 25 (greatwood) / 80 (silverwood) chunks, near the chunk centre like the original (8 +- 4),
# which also keeps these large trees inside the area world generation may write to.
CENTRE = {"type": "minecraft:random_offset", "xz_spread": {"type": "minecraft:uniform", "min_inclusive": 4, "max_inclusive": 12}, "y_spread": 0}
configured("greatwood_tree", tc("greatwood_tree"), {})
configured("silverwood_tree", tc("silverwood_tree"), {})
placed("greatwood", "greatwood_tree", [{"type": "minecraft:rarity_filter", "chance": 25}, CENTRE,
                                       {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING_NO_LEAVES"}, BIOME])
placed("silverwood", "silverwood_tree", [{"type": "minecraft:rarity_filter", "chance": 80}, CENTRE,
                                         {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING_NO_LEAVES"}, BIOME])
configured("cinderpearl_patch", tc("plant_patch"), {"to_place": {"type": "minecraft:simple_state_provider", "state": {"Name": tc("cinderpearl")}}})
placed("cinderpearl", "cinderpearl_patch", [
    {"type": "minecraft:rarity_filter", "chance": 30}, {"type": "minecraft:random_offset", "xz_spread": 8, "y_spread": 0},
    {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING"},
    {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:matching_block_tag", "offset": [0, -1, 0], "tag": "minecraft:sand"}},
    BIOME])
# Cinderpearl: hot (temperature > 1) biomes topped with sand.
tag("worldgen/biome", NS, "has_cinderpearl", ["#c:is_desert", "#c:is_badlands", "#c:is_savanna"])

BM = f"{D}/{NS}/neoforge/biome_modifier"


def add_features(name, biomes, features, step):
    write(f"{BM}/{name}.json", {"type": "neoforge:add_features", "biomes": biomes, "features": [tc(f) for f in features], "step": step})


add_features("ores", "#minecraft:is_overworld", ["ore_cinnabar", "ore_quartz", "ore_amber"], "underground_ores")
add_features("vis_crystals", "#minecraft:is_overworld", ["vis_crystals"], "underground_decoration")
add_features("magic_trees", "#minecraft:is_overworld", ["greatwood", "silverwood"], "vegetal_decoration")
add_features("cinderpearl", "#thaumcraft:has_cinderpearl", ["cinderpearl"], "vegetal_decoration")

# ====================================================================== aspects (ConfigAspects)
# priority 1: applied after the vanilla tables, so these beat broad tags like #minecraft:logs (as in ConfigAspects).
write(f"{D}/{NS}/{NS}/aspects/items/thaumcraft.json", {"priority": 1, "entries": [
    {"tag": "c:ores/cinnabar", "aspects": {"terra": 5, "metallum": 10, "alkimia": 5, "mortuus": 5}},
    {"tag": "c:ores/amber", "aspects": {"terra": 5, "vinculum": 10, "vitreus": 10}},
    {"tag": "c:gems/amber", "aspects": {"vinculum": 10, "vitreus": 10}},
    {"item": tc("quicksilver"), "aspects": {"metallum": 10, "mortuus": 5, "alkimia": 5}},
    {"item": tc("cluster_iron"), "aspects": {"ordo": 5, "metallum": 15, "terra": 5}},
    {"item": tc("cluster_gold"), "aspects": {"ordo": 5, "metallum": 15, "terra": 5, "desiderium": 10}},
    {"item": tc("cluster_cinnabar"), "aspects": {"ordo": 5, "metallum": 15, "terra": 5, "alkimia": 5, "mortuus": 5}},
    {"item": tc("cluster_quartz"), "aspects": {"ordo": 5, "vitreus": 10}},
    *[{"item": tc(n), "aspects": {a: 15, "vitreus": 10}} for n, (a, _) in CRYSTALS.items()],
    {"item": tc("log_greatwood"), "aspects": {"herba": 20, "victus": 5}},
    {"item": tc("log_silverwood"), "aspects": {"herba": 20, "auram": 5}},
    {"item": tc("leaves_greatwood"), "aspects": {"herba": 5}},
    {"item": tc("leaves_silverwood"), "aspects": {"herba": 5}},
    {"item": tc("sapling_greatwood"), "aspects": {"herba": 15, "victus": 5}},
    {"item": tc("sapling_silverwood"), "aspects": {"herba": 15, "auram": 5}},
    {"item": tc("shimmerleaf"), "aspects": {"herba": 5, "auram": 10, "potentia": 5}},
    {"item": tc("cinderpearl"), "aspects": {"herba": 5, "auram": 5, "ignis": 10}},
    {"item": tc("vishroom"), "aspects": {"herba": 2, "mortuus": 1, "praecantatio": 1, "perditio": 1}},
]})

print("ok")
