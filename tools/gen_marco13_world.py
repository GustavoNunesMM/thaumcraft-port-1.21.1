"""Milestone 13 (mobs and world) - world generation resources: the magical forest and eerie biomes, their features,
the mound / lesser crimson portal surface features, spawn biome modifiers, the ambient grass block, and the tags.
Shared tag files are merge-updated (load, add, write). Also (re)builds the mound structure template.

Run from the repo root: python3 tools/gen_marco13_world.py   (called by tools/gen_marco13.py)
"""
import colorsys
import json
import os
import subprocess
import sys

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


def tc(n):
    return n if ":" in n else f"{NS}:{n}"


def add_tag(kind, ns, path, values):
    p = f"{D}/{ns}/tags/{kind}/{path}.json"
    d = json.load(open(p, encoding="utf-8")) if os.path.exists(p) else {"replace": False, "values": []}
    for v in values:
        if v not in d["values"]:
            d["values"].append(v)
    write(p, d)


# ====================================================================== ambient grass
write(f"{A}/blockstates/grass_ambient.json", {"variants": {
    "snowy=false": [{"model": tc("block/grass_ambient"), "y": r} if r else {"model": tc("block/grass_ambient")} for r in (0, 90, 180, 270)],
    "snowy=true": {"model": tc("block/grass_ambient_snow")}}})
write(f"{A}/models/block/grass_ambient.json", {"parent": "minecraft:block/grass_block", "render_type": "minecraft:cutout_mipped"})
write(f"{A}/models/block/grass_ambient_snow.json", {"parent": "minecraft:block/grass_block_snow"})
write(f"{A}/models/item/grass_ambient.json", {"parent": tc("block/grass_ambient")})
write(f"{D}/{NS}/loot_table/blocks/grass_ambient.json", {"type": "minecraft:block", "random_sequence": f"{NS}:blocks/grass_ambient", "pools": [
    {"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:alternatives", "children": [
        {"type": "minecraft:item", "name": tc("grass_ambient"), "conditions": [{"condition": "minecraft:match_tool", "predicate": {
            "predicates": {"minecraft:enchantments": [{"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]},
        {"type": "minecraft:item", "name": "minecraft:dirt", "conditions": [{"condition": "minecraft:survives_explosion"}]}]}]}]})
add_tag("block", "minecraft", "mineable/shovel", [tc("grass_ambient")])
add_tag("block", "minecraft", "dirt", [tc("grass_ambient")])
add_tag("block", "minecraft", "animals_spawnable_on", [tc("grass_ambient")])
add_tag("block", "minecraft", "valid_spawn", [tc("grass_ambient")])
add_tag("item", "minecraft", "dirt", [tc("grass_ambient")])


# ====================================================================== biomes
def sky_color(temp):
    """OverworldBiomes.calculateSkyColor (= the 1.12 Biome.getSkyColorByTemp)."""
    f = max(-1.0, min(1.0, temp / 3.0))
    r, g, b = colorsys.hsv_to_rgb((0.62222224 - f * 0.05) % 1.0, 0.5 + f * 0.1, 1.0)
    return (min(255, int(r * 255)) << 16) | (min(255, int(g * 255)) << 8) | min(255, int(b * 255))


def spawn(t, w, lo, hi):
    return {"type": t, "weight": w, "minCount": lo, "maxCount": hi}


# 1.12 Biome default spawn lists
DEFAULT_CREATURES = [spawn("minecraft:sheep", 12, 4, 4), spawn("minecraft:pig", 10, 4, 4), spawn("minecraft:chicken", 10, 4, 4),
                     spawn("minecraft:cow", 8, 4, 4)]
DEFAULT_MONSTERS = [spawn("minecraft:spider", 100, 4, 4), spawn("minecraft:zombie", 95, 4, 4), spawn("minecraft:zombie_villager", 5, 1, 1),
                    spawn("minecraft:skeleton", 100, 4, 4), spawn("minecraft:creeper", 100, 4, 4), spawn("minecraft:slime", 100, 4, 4),
                    spawn("minecraft:enderman", 10, 1, 4), spawn("minecraft:witch", 5, 1, 1)]
DEFAULT_AMBIENT = [spawn("minecraft:bat", 10, 8, 8)]

# the vanilla forest's feature lists, steps 0-10 (vegetal decoration = 9 is replaced per biome)
FOREST_STEPS = [
    [], ["minecraft:lake_lava_underground", "minecraft:lake_lava_surface"], ["minecraft:amethyst_geode"],
    ["minecraft:monster_room", "minecraft:monster_room_deep"], [], [],
    ["minecraft:ore_dirt", "minecraft:ore_gravel", "minecraft:ore_granite_upper", "minecraft:ore_granite_lower", "minecraft:ore_diorite_upper",
     "minecraft:ore_diorite_lower", "minecraft:ore_andesite_upper", "minecraft:ore_andesite_lower", "minecraft:ore_tuff",
     "minecraft:ore_coal_upper", "minecraft:ore_coal_lower", "minecraft:ore_iron_upper", "minecraft:ore_iron_middle",
     "minecraft:ore_iron_small", "minecraft:ore_gold", "minecraft:ore_gold_lower", "minecraft:ore_redstone", "minecraft:ore_redstone_lower",
     "minecraft:ore_diamond", "minecraft:ore_diamond_medium", "minecraft:ore_diamond_large", "minecraft:ore_diamond_buried",
     "minecraft:ore_lapis", "minecraft:ore_lapis_buried", "minecraft:ore_copper", "minecraft:underwater_magma", "minecraft:disk_sand",
     "minecraft:disk_clay", "minecraft:disk_gravel"],
    [], ["minecraft:spring_water", "minecraft:spring_lava"], None, ["minecraft:freeze_top_layer"]]


def biome(temp, downfall, precipitation, grass, foliage, water, sky, vegetal, spawners, music=None):
    steps = [list(s) if s is not None else vegetal for s in FOREST_STEPS]
    effects = {"fog_color": 12638463, "sky_color": sky, "water_color": water, "water_fog_color": 329011, "grass_color": grass,
               "foliage_color": foliage,
               "mood_sound": {"block_search_extent": 8, "offset": 2.0, "sound": "minecraft:ambient.cave", "tick_delay": 6000}}
    if music:
        effects["music"] = {"max_delay": 24000, "min_delay": 12000, "replace_current_music": False, "sound": music}
    sp = {k: [] for k in ["ambient", "axolotls", "creature", "misc", "monster", "underground_water_creature", "water_ambient", "water_creature"]}
    sp["underground_water_creature"] = [spawn("minecraft:glow_squid", 10, 4, 6)]
    sp.update(spawners)
    return {"has_precipitation": precipitation, "temperature": temp, "downfall": downfall, "effects": effects, "spawn_costs": {},
            "spawners": sp, "carvers": {"air": ["minecraft:cave", "minecraft:cave_extra_underground", "minecraft:canyon"]}, "features": steps}


# BiomeGenMagicalForest: temperature 0.8, rainfall 0.4; grass 5635969, foliage 6750149, water 30702
write(f"{D}/{NS}/worldgen/biome/magical_forest.json", biome(
    0.8, 0.4, True, 5635969, 6750149, 30702, sky_color(0.8),
    ["minecraft:glow_lichen", "minecraft:forest_flowers", tc("magical_forest_trees"), "minecraft:flower_default",
     "minecraft:patch_grass_forest", "minecraft:brown_mushroom_normal", "minecraft:red_mushroom_normal", tc("magical_forest_decoration"),
     "minecraft:patch_sugar_cane", "minecraft:patch_pumpkin"],
    {"creature": DEFAULT_CREATURES + [spawn("minecraft:wolf", 2, 1, 3), spawn("minecraft:horse", 2, 1, 3)],
     "monster": DEFAULT_MONSTERS + [spawn("minecraft:witch", 3, 1, 1), spawn("minecraft:enderman", 3, 1, 1), spawn("minecraft:vex", 1, 1, 1),
                                    spawn(tc("pech"), 20, 1, 2), spawn(tc("wisp"), 20, 1, 2)],
     "ambient": DEFAULT_AMBIENT},
    music="minecraft:music.overworld.forest"))

# BiomeGenEerie: temperature 0.8, no rain; grass/foliage 4212800, sky 2237081, water 3035999. Its creature list was
# cleared and given bats (ambient here). STUB: the eldritch guardian (weight 1) belongs to the eldritch milestone.
write(f"{D}/{NS}/worldgen/biome/eerie.json", biome(
    0.8, 0.5, False, 4212800, 4212800, 3035999, 2237081,
    ["minecraft:glow_lichen", tc("eerie_trees"), "minecraft:flower_default", "minecraft:patch_grass_forest", "minecraft:brown_mushroom_normal",
     "minecraft:red_mushroom_normal", "minecraft:patch_sugar_cane", "minecraft:patch_pumpkin"],
    {"creature": [],
     "monster": DEFAULT_MONSTERS + [spawn("minecraft:witch", 8, 1, 1), spawn("minecraft:enderman", 4, 1, 1), spawn(tc("brainy_zombie"), 32, 1, 1),
                                    spawn(tc("giant_brainy_zombie"), 8, 1, 1), spawn(tc("wisp"), 3, 1, 1)],
     "ambient": DEFAULT_AMBIENT + [spawn("minecraft:bat", 3, 1, 1)]}))

add_tag("worldgen/biome", "c", "is_magical", [tc("magical_forest"), tc("eerie")])
add_tag("worldgen/biome", "c", "is_spooky", [tc("eerie")])
add_tag("worldgen/biome", "c", "is_forest", [tc("magical_forest")])
add_tag("worldgen/biome", "c", "is_overworld", [tc("magical_forest")])
add_tag("worldgen/biome", "minecraft", "is_forest", [tc("magical_forest")])
add_tag("worldgen/biome", "minecraft", "is_overworld", [tc("magical_forest")])

# ====================================================================== features
TREE_PLACEMENT = [{"type": "minecraft:count", "count": {"type": "minecraft:weighted_list", "distribution": [
    {"weight": 9, "data": 2}, {"weight": 1, "data": 3}]}},
                  {"type": "minecraft:in_square"},
                  {"type": "minecraft:surface_water_depth_filter", "max_water_depth": 0},
                  {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR"},
                  {"type": "minecraft:biome"}]


def feature(name, ftype, config, placement):
    write(f"{D}/{NS}/worldgen/configured_feature/{name}.json", {"type": ftype, "config": config})
    if placement is not None:
        write(f"{D}/{NS}/worldgen/placed_feature/{name}.json", {"feature": tc(name), "placement": placement})


feature("big_magic_tree", tc("big_magic_tree"), {}, None)
feature("magical_forest_trees", tc("magical_forest_trees"), {}, TREE_PLACEMENT)
feature("magical_forest_decoration", tc("magical_forest_decoration"), {},
        [{"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING"}, {"type": "minecraft:biome"}])
# the default 1.12 tree: 1 in 10 a big oak
feature("eerie_trees", "minecraft:random_selector", {"features": [{"chance": 0.1, "feature": "minecraft:fancy_oak_checked"}],
                                                     "default": "minecraft:oak_checked"}, TREE_PLACEMENT)
# ThaumcraftWorldGenerator.generateSurface: 1 chunk in 100 a mound, else 1 in 500 a lesser crimson portal
feature("mound", tc("mound"), {}, [{"type": "minecraft:rarity_filter", "chance": 100}])
feature("cultist_portal", tc("cultist_portal"), {}, [{"type": "minecraft:rarity_filter", "chance": 500}])

BM = f"{D}/{NS}/neoforge/biome_modifier"
write(f"{BM}/surface_structures.json", {"type": "neoforge:add_features", "biomes": "#minecraft:is_overworld",
                                        "features": [tc("mound"), tc("cultist_portal")], "step": "surface_structures"})
# ConfigEntities.postInitEntitySpawns
write(f"{BM}/spawn_brainy_zombie.json", {"type": "neoforge:add_spawns",
                                         "biomes": {"type": "neoforge:and", "values": [
                                             "#minecraft:is_overworld", {"type": "neoforge:not", "value": "#c:is_mushroom"}]},
                                         "spawners": [spawn(tc("brainy_zombie"), 10, 1, 1)]})
write(f"{BM}/spawn_pech.json", {"type": "neoforge:add_spawns", "biomes": "#c:is_magical", "spawners": [spawn(tc("pech"), 10, 1, 1)]})
write(f"{BM}/spawn_firebat.json", {"type": "neoforge:add_spawns", "biomes": "#minecraft:is_nether", "spawners": [spawn(tc("firebat"), 10, 1, 2)]})
write(f"{BM}/spawn_wisp.json", {"type": "neoforge:add_spawns", "biomes": "#minecraft:is_nether", "spawners": [spawn(tc("wisp"), 5, 1, 1)]})

# ====================================================================== mound template
subprocess.run([sys.executable, os.path.join(ROOT, "tools/mound_extract.py")], check=True)
print("gen_marco13_world: done")
