"""Generates the JSON resources of milestone 13 (mobs and world): item models (loot bags, primordial pearl, meat nuggets,
triple meat treat, pech wand, turret placers, spawn eggs), loot tables (loot bags with primordial pearls and the gear
roll, crates/urns, entity drops, curiosities in amber ore drops), the triple meat treat recipe, aspects of the new
items and creatures, and the en_us/pt_br lang keys (copied from the old 1.12 keys where they exist).
The world part (biomes, mounds, spawns) is tools/gen_marco13_world.py, run from here at the end.

Run from the repo root after gen_devices.py: python3 tools/gen_marco13.py
"""
import collections
import json
import os
import runpy

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


RARITIES = ["common", "uncommon", "rare"]
CHUNKS = ["beef", "chicken", "pork", "fish", "rabbit", "mutton"]
# (id, old 1.12 lang name, egg?)
MOBS = [("wisp", "Wisp", True), ("firebat", "Firebat", True), ("pech", "Pech", True), ("mind_spider", "MindSpider", True),
        ("brainy_zombie", "BrainyZombie", True), ("giant_brainy_zombie", "GiantBrainyZombie", True), ("thaumic_slime", "ThaumSlime", True),
        ("cultist_knight", "CultistKnight", True), ("cultist_cleric", "CultistCleric", True), ("cultist_leader", "CultistLeader", True),
        ("cultist_portal_lesser", "CultistPortalLesser", True), ("cultist_portal_greater", "CultistPortalGreater", True),
        ("turret_crossbow", "TurretBasic", False), ("turret_crossbow_advanced", "TurretAdvanced", False), ("arcane_bore", "ArcaneBore", False),
        ("golem_orb", "GolemOrb", False), ("golem_dart", "GolemDart", False)]

# ====================================================================== item models
def item_model(name, tex, parent="minecraft:item/generated", **extra):
    m = {"parent": parent, "textures": {"layer0": tex}}
    m.update(extra)
    write(f"{A}/models/item/{name}.json", m)


for r in RARITIES:
    item_model(f"loot_bag_{r}", f"{NS}:items/loot_bag_{r}")
item_model("primordial_pearl_nodule", f"{NS}:items/primordial_pearl_nodule")
item_model("primordial_pearl_mote", f"{NS}:items/primordial_pearl_mote")
item_model("primordial_pearl", f"{NS}:items/primordial_pearl", overrides=[
    {"predicate": {f"{NS}:type": 0.5}, "model": f"{NS}:item/primordial_pearl_nodule"},
    {"predicate": {f"{NS}:type": 1.5}, "model": f"{NS}:item/primordial_pearl_mote"}])
item_model("pech_wand", f"{NS}:items/pech_wand", parent="minecraft:item/handheld")
item_model("triple_meat_treat", f"{NS}:items/tripletreat")
for c in CHUNKS:
    item_model(f"chunk_{c}", f"{NS}:items/chunk_{c}")
for t in ["basic", "advanced", "bore"]:
    item_model(f"turret_{t}", f"{NS}:items/turret_{t}")
for mob, _, egg in MOBS:
    if egg:
        write(f"{A}/models/item/{mob}_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})

# ====================================================================== loot tables
POTIONS = ["water", "mundane", "thick", "awkward", "night_vision", "long_night_vision", "invisibility", "long_invisibility", "leaping",
           "strong_leaping", "long_leaping", "fire_resistance", "long_fire_resistance", "swiftness", "strong_swiftness", "long_swiftness",
           "slowness", "long_slowness", "water_breathing", "long_water_breathing", "healing", "strong_healing", "harming", "strong_harming",
           "poison", "strong_poison", "long_poison", "regeneration", "strong_regeneration", "long_regeneration", "strength",
           "strong_strength", "long_strength", "weakness", "long_weakness", "luck", "slow_falling", "long_slow_falling"]


def item(name, weight, count=1, functions=None):
    e = {"type": "minecraft:item", "name": name, "weight": weight}
    fs = list(functions or [])
    if count != 1:
        fs.insert(0, {"function": "minecraft:set_count", "count": count})
    if fs:
        e["functions"] = fs
    return e


def pearl(damage, weight):
    """A primordial pearl worn down to `damage` (of 8): set_damage takes the fraction of durability left."""
    fs = [] if damage == 0 else [{"function": "minecraft:set_damage", "damage": (8 - damage) / 8}]
    return item(tc("primordial_pearl"), weight, functions=fs)


def bag(r):
    """ModConfig.postInitLoot: the weighted loot bag lists (one entry is one Utils.generateLoot roll)."""
    e = [item("minecraft:gold_nugget", [2500, 2250, 2000][r], r + 1),
         item(tc("salis_mundus"), [3, 6, 9][r]),
         item("minecraft:chorus_fruit", 5), item("minecraft:compass", 5), item("minecraft:cookie", 5)]
    e += {0: [pearl(7, 1)], 1: [pearl(7, 3), pearl(6, 1)], 2: [pearl(5, 9), pearl(3, 3), pearl(0, 1)]}[r]
    if r == 2:
        e.append(item("minecraft:nether_star", 1))
    e += [item("minecraft:diamond", 10 if r == 0 else 50), item("minecraft:emerald", 15 if r == 0 else 75),
          item("minecraft:gold_ingot", 100), item("minecraft:ender_pearl", 100)]
    if r >= 1:
        e.append(item(tc("amulet_vis_found"), 6))
    baubles = ["amulet_mundane", "ring_mundane", "girdle_mundane", "ring_apprentice", "amulet_fancy", "ring_fancy", "girdle_fancy"]
    for i, (w, rs) in enumerate([(10, [0]), (10, [0]), (10, [0]), (5, [2]), (5, [1]), (5, [1]), (5, [1])]):
        if r in rs:
            e.append(item(tc(baubles[i]), w))
    e += [item("minecraft:experience_bottle", [5, 10, 20][r]),
          item("minecraft:enchanted_golden_apple", [1, 2, 3][r]), item("minecraft:golden_apple", [3, 6, 9][r]),
          item("minecraft:book", 10, functions=[{"function": "minecraft:enchant_with_levels",
                                                  "levels": {"type": "minecraft:uniform", "min": 5, "max": int(5 + r * 0.75 * 17)}}])]
    for p in POTIONS:
        kinds = ["minecraft:potion", "minecraft:splash_potion"] + (["minecraft:lingering_potion"] if r >= 1 else [])
        for k in kinds:
            e.append(item(k, 2, functions=[{"function": "minecraft:set_potion", "id": "minecraft:" + p}]))
    return e


SILK = {"condition": "minecraft:match_tool", "predicate": {
    "predicates": {"minecraft:enchantments": [{"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}
for r, rn in enumerate(RARITIES):
    table = {"type": "minecraft:generic", "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": bag(r)}]}
    if r > 0:
        # Utils.generateLoot: 2.5% per rarity level of a random piece of gear instead (genGear)
        table["functions"] = [{"function": tc("gear_roll"), "rarity": r}]
    write(f"{D}/{NS}/loot_table/loot_bag/{rn}.json", table)
    for kind in ["crate", "urn"]:
        name = f"loot_{kind}_{rn}"
        # BlockLoot.getDrops: 1 + rarity + rand(3) rolls of generateLoot (the block itself with silk touch)
        write(f"{D}/{NS}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "random_sequence": f"{NS}:blocks/{name}", "pools": [
            {"rolls": 1, "bonus_rolls": 0, "conditions": [SILK], "entries": [{"type": "minecraft:item", "name": tc(name)}]},
            {"rolls": {"type": "minecraft:uniform", "min": 1 + r, "max": 3 + r}, "bonus_rolls": 0,
             "conditions": [{"condition": "minecraft:inverted", "term": SILK}],
             "entries": [{"type": "minecraft:loot_table", "value": f"{NS}:loot_bag/{rn}"}]}]})

# BlockOreTC.getDrops: each amber drop has a 6.6% chance to be a preserved curiosity instead
p = f"{D}/{NS}/loot_table/blocks/ore_amber.json"
amber = json.load(open(p, encoding="utf-8"))
children = amber["pools"][0]["entries"][0]["children"]
if not any(c.get("name") == tc("curio_preserved") for c in children):
    children.insert(1, {"type": "minecraft:item", "name": tc("curio_preserved"),
                        "conditions": [{"condition": "minecraft:random_chance", "chance": 0.066}]})
write(p, amber)


def nugget_pool(name, curio):
    return [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": "minecraft:gold_nugget", "functions": [
        {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 0, "max": 2}},
        {"function": "minecraft:enchanted_count_increase", "enchantment": "minecraft:looting",
         "count": {"type": "minecraft:uniform", "min": 0, "max": 1}}]}]},
            {"rolls": 1, "bonus_rolls": 0, "conditions": [{"condition": "minecraft:killed_by_player"},
                                                         {"condition": "minecraft:random_chance_with_enchanted_bonus",
                                                          "enchantment": "minecraft:looting", "unenchanted_chance": 0.0125,
                                                          "enchanted_chance": {"type": "minecraft:linear", "base": 0.0225,
                                                                               "per_level_above_first": 0.01}}],
             "entries": [{"type": "minecraft:item", "name": tc(curio)}]}]


def entity_loot(name, pools):
    write(f"{D}/{NS}/loot_table/entities/{name}.json", {"type": "minecraft:entity", "random_sequence": f"{NS}:entities/{name}", "pools": pools})


# loot_tables/cultist.json and pech.json of 1.12
entity_loot("cultist_knight", nugget_pool("cultist", "curio_rites"))
entity_loot("cultist_cleric", nugget_pool("cultist", "curio_rites"))
entity_loot("pech", nugget_pool("pech", "curio_knowledge"))
# EntityFireBat.getDropItem: gunpowder (dropFewItems: rand(3) + rand(looting + 1))
entity_loot("firebat", [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": "minecraft:gunpowder", "functions": [
    {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 0, "max": 2}},
    {"function": "minecraft:enchanted_count_increase", "enchantment": "minecraft:looting", "count": {"type": "minecraft:uniform", "min": 0, "max": 1}}]}]}])
# the brainy zombies use the zombie's table (their brain is dropped in code)
for z in ["brainy_zombie", "giant_brainy_zombie"]:
    entity_loot(z, [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:loot_table", "value": "minecraft:entities/zombie"}]}])

# ====================================================================== recipes
write(f"{D}/{NS}/recipe/triple_meat_treat.json", {"type": tc("triple_meat_treat"), "category": "misc"})

# ====================================================================== tags
def add_tag(kind, ns, path, values):
    p = f"{D}/{ns}/tags/{kind}/{path}.json"
    d = json.load(open(p)) if os.path.exists(p) else {"replace": False, "values": []}
    for v in values:
        if v not in d["values"]:
            d["values"].append(v)
    write(p, d)


add_tag("item", NS, "meat_nuggets", [tc(f"chunk_{c}") for c in CHUNKS])
add_tag("item", "c", "foods/cooked_meat", [tc(f"chunk_{c}") for c in CHUNKS])
add_tag("item", "c", "foods", [tc(f"chunk_{c}") for c in CHUNKS] + [tc("triple_meat_treat")])
add_tag("entity_type", "minecraft", "undead", [tc("brainy_zombie"), tc("giant_brainy_zombie")])
add_tag("entity_type", "minecraft", "zombies", [tc("brainy_zombie"), tc("giant_brainy_zombie")])
add_tag("entity_type", "minecraft", "arthropod", [tc("mind_spider")])
add_tag("entity_type", "minecraft", "can_breathe_under_water", [tc(n) for n in ["cultist_leader", "turret_crossbow",
                                                                         "turret_crossbow_advanced", "arcane_bore"]])
add_tag("entity_type", "c", "bosses", [tc("cultist_leader"), tc("cultist_portal_greater")])

# ====================================================================== aspects
write(f"{D}/{NS}/thaumcraft/aspects/items/mobs.json", {"priority": 1, "entries": [
    {"item": tc("triple_meat_treat"), "aspects": {"victus": 10, "desiderium": 10}},
    {"item": tc("loot_bag_common"), "aspects": {"desiderium": 10}},
    {"item": tc("loot_bag_uncommon"), "aspects": {"desiderium": 20}},
    {"item": tc("loot_bag_rare"), "aspects": {"desiderium": 30}},
    {"item": tc("primordial_pearl"), "aspects": {"aer": 10, "ignis": 10, "aqua": 10, "terra": 10, "ordo": 10, "perditio": 10}},
    {"item": tc("pech_wand"), "aspects": {"praecantatio": 10, "permutatio": 10, "instrumentum": 5}},
] + [{"item": tc(f"chunk_{c}"), "aspects": {"victus": 5, "perditio": 1}} for c in CHUNKS]})
write(f"{D}/{NS}/thaumcraft/aspects/entities/mobs.json", {"entries": [
    {"entity": tc("firebat"), "aspects": {"bestia": 5, "volatus": 5, "ignis": 10}},
    # STUB: the pech's and wisp's aspects depended on their NBT type; the entity aspect data is keyed by type only
    {"entity": tc("pech"), "aspects": {"humanus": 10, "auram": 5, "permutatio": 10, "desiderium": 5}},
    {"entity": tc("wisp"), "aspects": {"auram": 5, "volatus": 5}},
    {"entity": tc("thaumic_slime"), "aspects": {"victus": 5, "aqua": 5, "vitium": 5, "alkimia": 5}},
    {"entity": tc("brainy_zombie"), "aspects": {"exanimis": 20, "humanus": 10, "cognitio": 5, "aversio": 5}},
    {"entity": tc("giant_brainy_zombie"), "aspects": {"exanimis": 25, "humanus": 15, "cognitio": 5, "aversio": 10}},
    {"entity": tc("mind_spider"), "aspects": {"vitium": 5, "ignis": 5}},
    {"entity": tc("cultist_knight"), "aspects": {"alienis": 5, "humanus": 15, "aversio": 5}},
    {"entity": tc("cultist_cleric"), "aspects": {"alienis": 5, "humanus": 15, "aversio": 5}},
    {"entity": tc("cultist_leader"), "aspects": {"alienis": 40, "aversio": 40, "humanus": 40}},
]})

# ====================================================================== lang
L = f"{A}/lang/en_us.json"
en = json.load(open(L, encoding="utf-8"), object_pairs_hook=collections.OrderedDict)
L2 = f"{A}/lang/pt_br.json"
pt = json.load(open(L2, encoding="utf-8"), object_pairs_hook=collections.OrderedDict)
for d in (en, pt):
    keys = {f"item.{NS}.loot_bag_{r}": f"item.loot_bag.{r}.name" for r in RARITIES}
    keys.update({f"item.{NS}.primordial_pearl.{v}": f"item.primordial_pearl.{v}.name" for v in ["pearl", "nodule", "mote"]})
    keys[f"item.{NS}.primordial_pearl"] = "item.primordial_pearl.pearl.name"
    keys.update({f"item.{NS}.chunk_{c}": f"item.chunk.{c}.name" for c in CHUNKS})
    keys.update({f"item.{NS}.turret_{t}": f"item.turret.{t}.name" for t in ["basic", "advanced", "bore"]})
    keys.update({f"entity.{NS}.{mob}": f"entity.{old}.name" for mob, old, _ in MOBS})
    keys[f"entity.{NS}.pech.mage"] = "entity.Pech.1.name"
    keys[f"entity.{NS}.pech.stalker"] = "entity.Pech.2.name"
    for k, old in keys.items():
        if old not in d:
            raise SystemExit(f"no lang for {k} ({old})")
        d[k] = d[old].strip()
en[f"entity.{NS}.cultist_leader.custom"] = "Praetor %s"
pt[f"entity.{NS}.cultist_leader.custom"] = "Pretor %s"
for mob, _, egg in MOBS:
    if egg:
        en[f"item.{NS}.{mob}_spawn_egg"] = en[f"entity.{NS}.{mob}"] + " Spawn Egg"
        pt[f"item.{NS}.{mob}_spawn_egg"] = "Ovo Gerador de " + pt[f"entity.{NS}.{mob}"]

# keys for the GUIs of the constructs and the world (filled in by the other parts of the milestone)
EXTRA = {
    "biome.thaumcraft.magical_forest": ("Magical Forest", "Floresta Mágica"),
    "biome.thaumcraft.eerie": ("Eerie", "Sinistro"),
}
for k, (e, p_) in EXTRA.items():
    en[k] = e
    pt[k] = p_
json.dump(en, open(L, "w", encoding="utf-8"), indent=1, ensure_ascii=False)
json.dump(pt, open(L2, "w", encoding="utf-8"), indent=1, ensure_ascii=False)

# ====================================================================== world
WORLD = os.path.join(os.path.dirname(os.path.abspath(__file__)), "gen_marco13_world.py")
if os.path.exists(WORLD):
    runpy.run_path(WORLD, run_name="__main__")
print("ok")
