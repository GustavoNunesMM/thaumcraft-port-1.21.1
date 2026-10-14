"""Generates the resources of milestone 14 (the eldritch): the eldritch creature model layers (via
convert_eldritch_models.py), item textures for the eldritch eye and runed tablet, blockstates/models of porous stone, the
dimensional tear and the Outer Lands blocks (altar, obelisk, capstone, lock, portal, crab vent), item models and spawn
eggs, loot tables (guardian, reliquary chest), tags, the Outer Lands dimension (dimension type, level stem, eldritch
biome), the overworld obelisk feature, lang keys (en_us + pt_br) and aspects.

Shared files (lang, tags, the block atlas, the eerie biome of gen_marco13_world.py) are merge-updated, so run this after
the other gen scripts: ... gen_marco12.py, gen_golems.py, gen_marco13*.py, gen_marco14.py.

Run from the repo root: python3 tools/gen_marco14.py
"""
import collections
import json
import os
import subprocess
import sys

from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
RES = os.path.join(ROOT, "mod/src/main/resources")
A = os.path.join(RES, "assets/thaumcraft")
D = os.path.join(RES, "data")
NS = "thaumcraft"

subprocess.run([sys.executable, os.path.join(ROOT, "tools/convert_eldritch_models.py")], check=True)


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def tc(n):
    return n if ":" in n else f"{NS}:{n}"


def model(name, obj, kind="block"):
    write(f"{A}/models/{kind}/{name}.json", obj)


def blockstate(name, obj):
    write(f"{A}/blockstates/{name}.json", obj)


def generated(name, tex=None):
    model(name, {"parent": "minecraft:item/generated", "textures": {"layer0": tex or f"{NS}:items/{name}"}}, "item")


def block_item(name, parent=None):
    model(name, {"parent": tc("block/" + (parent or name))}, "item")


def add_tag(kind, ns, path, values):
    p = f"{D}/{ns}/tags/{kind}/{path}.json"
    d = json.load(open(p)) if os.path.exists(p) else {"replace": False, "values": []}
    for v in values:
        if v not in d["values"]:
            d["values"].append(v)
    write(p, d)


# ====================================================================== item textures (none in the 1.12 assets)
os.makedirs(f"{A}/textures/items", exist_ok=True)
eye = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
d = ImageDraw.Draw(eye)
d.ellipse((1, 3, 14, 12), fill=(38, 12, 52, 255), outline=(12, 4, 18, 255))
d.ellipse((4, 4, 11, 11), fill=(120, 40, 160, 255))
d.ellipse((6, 5, 9, 10), fill=(10, 0, 14, 255))
d.point((5, 5), fill=(230, 200, 255, 255))
d.point((6, 5), fill=(200, 160, 240, 255))
eye.save(f"{A}/textures/items/eldritch_eye.png")
tab = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
d = ImageDraw.Draw(tab)
d.rectangle((3, 1, 12, 14), fill=(70, 66, 76, 255), outline=(36, 34, 40, 255))
d.rectangle((4, 2, 11, 13), outline=(96, 92, 104, 255))
for (x, y) in [(5, 4), (6, 4), (8, 4), (10, 4), (5, 6), (7, 6), (9, 6), (6, 8), (8, 8), (10, 8), (5, 10), (7, 10), (8, 10), (10, 11), (6, 12)]:
    d.point((x, y), fill=(150, 70, 200, 255))
tab.save(f"{A}/textures/items/runed_tablet.png")

# the obelisk/lock/vent textures live in textures/models: add them to the block atlas
atlas_p = f"{RES}/assets/minecraft/atlases/blocks.json"
atlas = json.load(open(atlas_p))
for t in ["obelisk_side", "obelisk_side_2", "obelisk_cap", "obelisk_cap_2", "obelisk_cap_altar", "crabvent", "eldritch_lock"]:
    src = {"type": "single", "resource": f"{NS}:models/{t}"}
    if src not in atlas["sources"]:
        atlas["sources"].append(src)
write(atlas_p, atlas)

# ====================================================================== blocks
STONE = f"{NS}:blocks/ancient_stone_0"
TILE = f"{NS}:blocks/ancient_tile"
model("stone_porous", {"parent": "minecraft:block/cube_all", "textures": {"all": f"{NS}:blocks/porous_stone"}})
blockstate("stone_porous", {"variants": {"": {"model": tc("block/stone_porous")}}})
block_item("stone_porous")

model("hole", {"textures": {"particle": f"{NS}:blocks/empty"}})
blockstate("hole", {"variants": {"": {"model": tc("block/hole")}}})
model("eldritch_portal", {"textures": {"particle": f"{NS}:blocks/empty"}})
blockstate("eldritch_portal", {"variants": {"": {"model": tc("block/eldritch_portal")}}})

model("eldritch_altar", {"parent": "minecraft:block/cube_bottom_top", "textures": {
    "top": f"{NS}:models/obelisk_cap_altar", "bottom": TILE, "side": STONE}})
blockstate("eldritch", {"variants": {f"eyes={i}": {"model": tc("block/eldritch_altar")} for i in range(5)}})
block_item("eldritch", "eldritch_altar")

V = {"top": (0, 5.333), "middle": (5.333, 10.667), "bottom": (10.667, 16)}
for part, (v0, v1) in V.items():
    for lit in (False, True):
        side = f"{NS}:models/obelisk_side" + ("_2" if lit else "")
        name = f"eldritch_obelisk_{part}" + ("_lit" if lit else "")
        model(name, {"parent": "minecraft:block/block", "textures": {"side": side, "end": TILE, "particle": side},
                     "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": {
                         **{f: {"texture": "#side", "uv": [0, v0, 16, v1]} for f in ("north", "south", "east", "west")},
                         "up": {"texture": "#end", "cullface": "up"}, "down": {"texture": "#end", "cullface": "down"}}}]})
blockstate("eldritch_obelisk", {"variants": {f"lit={str(lit).lower()},part={part}": {"model": tc(f"block/eldritch_obelisk_{part}" + ("_lit" if lit else ""))}
                                             for part in V for lit in (False, True)}})
block_item("eldritch_obelisk", "eldritch_obelisk_middle")
for lit in (False, True):
    cap = f"{NS}:models/obelisk_cap" + ("_2" if lit else "")
    model("eldritch_cap" + ("_lit" if lit else ""), {"parent": "minecraft:block/cube_bottom_top", "textures": {"top": cap, "bottom": TILE, "side": cap}})
blockstate("eldritch_cap", {"variants": {f"lit={str(lit).lower()}": {"model": tc("block/eldritch_cap" + ("_lit" if lit else ""))} for lit in (False, True)}})
block_item("eldritch_cap")

model("eldritch_lock", {"parent": "minecraft:block/orientable", "textures": {
    "front": f"{NS}:models/eldritch_lock", "side": STONE, "top": STONE}})
# facing points into the sealed chamber: the lock face looks the other way, at whoever comes with the tablet
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}
OPP = {"north": "south", "south": "north", "east": "west", "west": "east"}
blockstate("eldritch_lock", {"variants": {f"facing={f},opening={o}": {"model": tc("block/eldritch_lock"), "y": ROT[OPP[f]]}
                                          for f in ROT for o in ("false", "true")}})
block_item("eldritch_lock")

model("eldritch_crab_spawner", {"parent": "minecraft:block/cube_bottom_top", "textures": {
    "top": f"{NS}:models/crabvent", "bottom": STONE, "side": STONE}})
blockstate("eldritch_crab_spawner", {"variants": {"": {"model": tc("block/eldritch_crab_spawner")}}})
block_item("eldritch_crab_spawner")

generated("eldritch_eye")
generated("runed_tablet")
generated("creative_placer_obelisk", f"{NS}:items/creative_obelisk")
EGGS = ["eldritch_guardian", "eldritch_crab", "inhabited_zombie", "eldritch_warden", "eldritch_golem", "taintacle_giant"]
for e in EGGS:
    model(f"{e}_spawn_egg", {"parent": "minecraft:item/template_spawn_egg"}, "item")

add_tag("block", "minecraft", "mineable/pickaxe", [tc("stone_porous")])
add_tag("block", NS, "mineable/primal_crusher", [tc("stone_porous")])

# ====================================================================== loot tables
# Port addition (no eldritch eye drop existed in 1.12, where the item had no use): guardians killed by a player rarely drop
# an eldritch eye, the key to the altars.
write(f"{D}/{NS}/loot_table/entities/eldritch_guardian.json", {"type": "minecraft:entity", "random_sequence": f"{NS}:entities/eldritch_guardian",
    "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": tc("eldritch_eye")}],
               "conditions": [{"condition": "minecraft:killed_by_player"},
                              {"condition": "minecraft:random_chance_with_enchanted_bonus", "enchantment": "minecraft:looting",
                               "unenchanted_chance": 0.1, "enchanted_chance": {"type": "minecraft:linear", "base": 0.15, "per_level_above_first": 0.05}}]}]})
write(f"{D}/{NS}/loot_table/chests/outer_lands_reliquary.json", {"type": "minecraft:chest", "random_sequence": f"{NS}:chests/outer_lands_reliquary",
    "pools": [
        {"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": tc("runed_tablet")}]},
        {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 5}, "bonus_rolls": 0, "entries": [
            {"type": "minecraft:item", "name": tc("eldritch_eye"), "weight": 3},
            {"type": "minecraft:item", "name": tc("void_seed"), "weight": 4,
             "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 3}}]},
            {"type": "minecraft:item", "name": tc("amber"), "weight": 6,
             "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 4}}]},
            {"type": "minecraft:item", "name": tc("ingot_thaumium"), "weight": 6,
             "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 4}}]},
            {"type": "minecraft:item", "name": "minecraft:ender_pearl", "weight": 6,
             "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 3}}]},
            {"type": "minecraft:item", "name": "minecraft:diamond", "weight": 2},
            {"type": "minecraft:item", "name": tc("primordial_pearl"), "weight": 1}]}]})
for e in ["eldritch_crab", "inhabited_zombie", "eldritch_warden", "eldritch_golem", "taintacle_giant"]:
    write(f"{D}/{NS}/loot_table/entities/{e}.json", {"type": "minecraft:entity", "random_sequence": f"{NS}:entities/{e}", "pools": []})

# ====================================================================== entity tags
add_tag("entity_type", "minecraft", "undead", [tc("eldritch_guardian"), tc("eldritch_warden"), tc("inhabited_zombie")])
add_tag("entity_type", "minecraft", "sensitive_to_smite", [tc("eldritch_guardian"), tc("eldritch_warden"), tc("inhabited_zombie")])
add_tag("entity_type", "minecraft", "zombies", [tc("inhabited_zombie")])
add_tag("entity_type", "minecraft", "arthropod", [tc("eldritch_crab")])
add_tag("entity_type", "minecraft", "sensitive_to_bane_of_arthropods", [tc("eldritch_crab")])
add_tag("entity_type", "minecraft", "can_breathe_under_water", [tc("eldritch_warden"), tc("eldritch_golem"), tc("taintacle_giant")])
add_tag("entity_type", "minecraft", "fall_damage_immune", [tc("eldritch_golem")])
add_tag("entity_type", "c", "bosses", [tc("eldritch_warden"), tc("eldritch_golem"), tc("taintacle_giant")])
# creatures that count as IEldritchMob without implementing it
add_tag("entity_type", NS, "eldritch", [{"id": tc("mind_spider"), "required": False}])

# ====================================================================== the Outer Lands
write(f"{D}/{NS}/dimension_type/outer_lands.json", {
    "ultrawarm": False, "natural": False, "piglin_safe": False, "respawn_anchor_works": False, "bed_works": False, "has_raids": False,
    "has_skylight": False, "has_ceiling": False, "coordinate_scale": 1.0, "ambient_light": 0.0, "fixed_time": 18000,
    "logical_height": 256, "height": 256, "min_y": 0, "infiniburn": "#minecraft:infiniburn_end", "effects": "minecraft:the_end",
    "monster_spawn_light_level": {"type": "minecraft:uniform", "min_inclusive": 0, "max_inclusive": 7}, "monster_spawn_block_light_limit": 0})
write(f"{D}/{NS}/dimension/outer_lands.json", {"type": tc("outer_lands"), "generator": {
    "type": tc("outer_lands"), "biome_source": {"type": "minecraft:fixed", "biome": tc("eldritch")}}})


def spawn(t, w, lo, hi):
    return {"type": t, "weight": w, "minCount": lo, "maxCount": hi}


# BiomeGenEldritch ("Outer Lands"): temperature 0.8, rainfall 0.2, black sky; only inhabited zombies and guardians.
write(f"{D}/{NS}/worldgen/biome/eldritch.json", {
    "has_precipitation": False, "temperature": 0.8, "downfall": 0.2,
    "effects": {"fog_color": 0, "sky_color": 0, "water_color": 4159204, "water_fog_color": 329011, "grass_color": 4212800, "foliage_color": 4212800,
                "mood_sound": {"block_search_extent": 8, "offset": 2.0, "sound": "minecraft:ambient.cave", "tick_delay": 6000}},
    "spawn_costs": {},
    "spawners": {k: [] for k in ["ambient", "axolotls", "creature", "misc", "underground_water_creature", "water_ambient", "water_creature"]}
    | {"monster": [spawn(tc("inhabited_zombie"), 1, 1, 1), spawn(tc("eldritch_guardian"), 1, 1, 1)]},
    "carvers": {}, "features": []})

# BiomeGenEerie: the eldritch guardian (weight 1) left out by the mobs milestone.
eerie_p = f"{D}/{NS}/worldgen/biome/eerie.json"
if os.path.exists(eerie_p):
    eerie = json.load(open(eerie_p))
    mons = eerie["spawners"].setdefault("monster", [])
    if not any(m["type"] == tc("eldritch_guardian") for m in mons):
        mons.append(spawn(tc("eldritch_guardian"), 1, 1, 1))
    write(eerie_p, eerie)

# The eldritch altar with its obelisks, rare on the overworld surface.
write(f"{D}/{NS}/worldgen/configured_feature/eldritch_obelisk.json", {"type": tc("eldritch_obelisk"), "config": {}})
write(f"{D}/{NS}/worldgen/placed_feature/eldritch_obelisk.json", {"feature": tc("eldritch_obelisk"), "placement": [
    {"type": "minecraft:rarity_filter", "chance": 400}, {"type": "minecraft:in_square"},
    {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]})
write(f"{D}/{NS}/neoforge/biome_modifier/eldritch_obelisk.json", {"type": "neoforge:add_features", "biomes": "#minecraft:is_overworld",
                                                                   "features": [tc("eldritch_obelisk")], "step": "surface_structures"})

# ====================================================================== aspects (ConfigAspects)
write(f"{D}/{NS}/{NS}/aspects/items/eldritch.json", {"priority": 1, "entries": [
    {"item": tc("stone_porous"), "aspects": {"terra": 5, "vacuos": 5}},
    {"item": tc("eldritch"), "aspects": {"vacuos": 10, "alienis": 10}},
    {"item": tc("eldritch_obelisk"), "aspects": {"vacuos": 10, "alienis": 10}},
    {"item": tc("eldritch_cap"), "aspects": {"vacuos": 10, "alienis": 10}},
    {"item": tc("eldritch_lock"), "aspects": {"vacuos": 10, "alienis": 10, "machina": 10}},
    {"item": tc("eldritch_crab_spawner"), "aspects": {"vacuos": 10, "alienis": 10, "bestia": 15}},
    {"item": tc("eldritch_eye"), "aspects": {"alienis": 10, "sensus": 10, "vacuos": 5}},
    {"item": tc("runed_tablet"), "aspects": {"alienis": 10, "terra": 5, "cognitio": 5}},
]})
write(f"{D}/{NS}/{NS}/aspects/entities/eldritch.json", {"entries": [
    {"entity": tc("eldritch_guardian"), "aspects": {"alienis": 20, "mortuus": 20, "exanimis": 20}},
    {"entity": tc("eldritch_crab"), "aspects": {"alienis": 10, "bestia": 10, "vinculum": 10}},
    {"entity": tc("inhabited_zombie"), "aspects": {"alienis": 10, "exanimis": 10, "humanus": 5}},
    {"entity": tc("eldritch_warden"), "aspects": {"alienis": 40, "mortuus": 40, "exanimis": 40}},
    {"entity": tc("eldritch_golem"), "aspects": {"alienis": 40, "potentia": 40, "machina": 40}},
    {"entity": tc("taintacle_giant"), "aspects": {"alienis": 40, "bestia": 40, "vitium": 40}},
]})

# ====================================================================== lang
L = f"{A}/lang/en_us.json"
L2 = f"{A}/lang/pt_br.json"
en = json.load(open(L, encoding="utf-8"), object_pairs_hook=collections.OrderedDict)
pt = json.load(open(L2, encoding="utf-8"), object_pairs_hook=collections.OrderedDict)
COPY = {
    "block.thaumcraft.stone_porous": "tile.stone_porous.name",
    "block.thaumcraft.hole": "tile.hole.name",
    "block.thaumcraft.eldritch": "tile.eldritch.altar.name",
    "block.thaumcraft.eldritch_obelisk": "tile.eldritch.obelisk_base.name",
    "block.thaumcraft.eldritch_cap": "tile.eldritch.cap.name",
    "block.thaumcraft.eldritch_lock": "tile.eldritch.lock.name",
    "block.thaumcraft.eldritch_portal": "tile.eldritch.teleporter.name",
    "block.thaumcraft.eldritch_crab_spawner": "tile.eldritch.crabspawner.name",
    "item.thaumcraft.eldritch_eye": "item.eldritch_eye.name",
    "item.thaumcraft.runed_tablet": "item.runed_tablet.name",
    "item.thaumcraft.creative_placer_obelisk": "item.creative_placer.obelisk.name",
}
ENT = {"eldritch_guardian": "EldritchGuardian", "eldritch_crab": "EldritchCrab", "inhabited_zombie": "InhabitedZombie",
       "eldritch_warden": "EldritchWarden", "eldritch_golem": "EldritchGolem", "taintacle_giant": "TaintacleGiant", "eldritch_orb": "EldritchOrb"}
for k, v in ENT.items():
    COPY[f"entity.thaumcraft.{k}"] = f"entity.{v}.name"
for dct in (en, pt):
    for k, old in COPY.items():
        if k not in dct:
            if old not in dct:
                raise SystemExit(f"no lang for {k} ({old})")
            dct[k] = dct[old]
for e in EGGS:
    en.setdefault(f"item.thaumcraft.{e}_spawn_egg", en[f"entity.thaumcraft.{e}"] + " Spawn Egg")
    pt.setdefault(f"item.thaumcraft.{e}_spawn_egg", "Ovo Gerador de " + pt[f"entity.thaumcraft.{e}"])
NEW = {
    "item.eldritch_eye.text": ("It stares back. Somewhere an altar waits for it.", "Ele te encara de volta. Em algum lugar, um altar o aguarda."),
    "tc.eldritch.portal_open": ("The obelisks blaze and a portal tears open above the altar.",
                                "Os obeliscos ardem e um portal se rasga acima do altar."),
    "biome.thaumcraft.eldritch": ("Outer Lands", "Terras Exteriores"),
    "dimension.thaumcraft.outer_lands": ("Outer Lands", "Terras Exteriores"),
}
for k, (e_, p_) in NEW.items():
    en.setdefault(k, e_)
    pt.setdefault(k, p_)
json.dump(en, open(L, "w", encoding="utf-8"), indent=1, ensure_ascii=False)
json.dump(pt, open(L2, "w", encoding="utf-8"), indent=1, ensure_ascii=False)
print("ok")
