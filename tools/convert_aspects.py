"""Converts Thaumcraft 1.12 ConfigAspects.java (deobfuscated) into 1.21 datapack JSON.

Output: data/thaumcraft/thaumcraft/aspects/{items,entities}/vanilla.json
Entries referencing Thaumcraft's own items are skipped until those items are ported.
"""
import json, os, re, sys

SRC = sys.argv[1]
OUT = sys.argv[2]

COLORS = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
          "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"]

# (name, meta) -> list of targets. Target "#ns:tag" = tag, "block:ns:id" = block-only, else item id.
# meta None = no meta given, "*" = wildcard (32767).
META = {
    ("DIRT", 2): ["podzol"],
    **{("DOUBLE_PLANT", i): [n] for i, n in enumerate(["sunflower", "lilac", "tall_grass", "large_fern", "rose_bush", "peony"])},
    ("SANDSTONE", None): ["sandstone"], ("SANDSTONE", 1): ["chiseled_sandstone"], ("SANDSTONE", 2): ["cut_sandstone"],
    ("STONEBRICK", None): ["stone_bricks"], ("STONEBRICK", 1): ["mossy_stone_bricks"],
    ("STONEBRICK", 2): ["cracked_stone_bricks"], ("STONEBRICK", 3): ["chiseled_stone_bricks"],
    ("SPONGE", 0): ["sponge"], ("SPONGE", 1): ["wet_sponge"],
    ("DYE", 0): ["ink_sac"], ("DYE", 15): ["bone_meal"], ("DYE", 2): ["green_dye"],
    ("DYE", 3): ["cocoa_beans"], ("DYE", 4): ["lapis_lazuli"],
    ("GOLDEN_APPLE", 0): ["golden_apple"], ("GOLDEN_APPLE", 1): ["enchanted_golden_apple"],
    **{("SKULL", i): [n] for i, n in enumerate(["skeleton_skull", "wither_skeleton_skull", "zombie_head",
                                                "player_head", "creeper_head", "dragon_head"])},
}
RENAME = {
    "GRASS": ["grass_block"], "GRASS_PATH": ["dirt_path"], "HARDENED_CLAY": ["terracotta"],
    "MAGMA": ["magma_block"], "MELON_BLOCK": ["melon"], "MELON": ["melon_slice"],
    "SPECKLED_MELON": ["glistering_melon_slice"], "MOB_SPAWNER": ["spawner"], "NOTEBLOCK": ["note_block"],
    "WATERLILY": ["lily_pad"], "WEB": ["cobweb"], "DEADBUSH": ["dead_bush"], "BOAT": ["oak_boat"],
    "CHORUS_FRUIT_POPPED": ["popped_chorus_fruit"], "POTIONITEM": ["potion"],
    "TALLGRASS": ["short_grass", "fern"], "YELLOW_FLOWER": ["dandelion"], "RED_FLOWER": ["#minecraft:small_flowers"],
    "WOOL": ["#minecraft:wool"], "CONCRETE": [f"{c}_concrete" for c in COLORS],
    "CONCRETE_POWDER": [f"{c}_concrete_powder" for c in COLORS],
    "STAINED_HARDENED_CLAY": [f"{c}_terracotta" for c in COLORS],
    "WOODEN_BUTTON": ["#minecraft:wooden_buttons"], "WOODEN_PRESSURE_PLATE": ["#minecraft:wooden_pressure_plates"],
    "TRAPDOOR": ["#minecraft:wooden_trapdoors"], "FISH": ["cod", "salmon", "tropical_fish", "pufferfish"],
    "COOKED_FISH": ["cooked_cod", "cooked_salmon"], "LIT_REDSTONE_ORE": [],
    "PORTAL": ["block:minecraft:nether_portal"], "END_PORTAL": ["block:minecraft:end_portal"],
    "FIRE": ["block:minecraft:fire", "block:minecraft:soul_fire"], "TRIPWIRE": ["block:minecraft:tripwire"],
    "SILVER_SHULKER_BOX": ["light_gray_shulker_box"], "GOLDEN_RAIL": ["powered_rail"], "SILVER_GLAZED_TERRACOTTA": ["light_gray_glazed_terracotta"],
}

ORE = {
    "stone": ["stone"], "stoneGranite": ["granite"], "stoneDiorite": ["diorite"], "stoneAndesite": ["andesite"],
    "cobblestone": ["#c:cobblestones"], "dirt": ["dirt"], "grass": ["grass_block"], "glowstone": ["glowstone"],
    "logWood": ["#minecraft:logs"], "treeLeaves": ["#minecraft:leaves"], "treeSapling": ["#minecraft:saplings"],
    "vine": ["vine"], "sugarcane": ["sugar_cane"], "blockCactus": ["cactus"], "blockGlass": ["#c:glass_blocks"],
    "bone": ["#c:bones"], "egg": ["#c:eggs"], "enderpearl": ["#c:ender_pearls"], "endstone": ["#c:end_stones"],
    "feather": ["#c:feathers"], "gunpowder": ["#c:gunpowders"], "leather": ["#c:leathers"],
    "netherrack": ["#c:netherracks"], "netherStar": ["#c:nether_stars"], "obsidian": ["#c:obsidians"],
    "sand": ["#c:sands"], "slimeball": ["#c:slime_balls"], "string": ["#c:strings"], "gravel": ["#c:gravels"],
    "cropCarrot": ["#c:crops/carrot"], "cropNetherWart": ["#c:crops/nether_wart"],
    "cropPotato": ["#c:crops/potato"], "cropWheat": ["#c:crops/wheat"], "ingotBrickNether": ["nether_brick"],
    "itemDropUranium": [], "itemRubber": ["#c:rubbers"], "gear*": ["#c:gears"],
}
PREFIX = {"ore": "ores", "ingot": "ingots", "dust": "dusts", "gem": "gems"}
TC_ONLY = {"Amber", "Cinnabar"}  # Thaumcraft materials, not ported yet

ENTITY = {
    "Ozelot": "ocelot", "PigZombie": "zombified_piglin", "LavaSlime": "magma_cube", "SnowMan": "snow_golem",
    "VillagerGolem": "iron_golem", "XPOrb": "experience_orb", "EnderCrystal": "end_crystal",
    "EvocationIllager": "evoker", "IllusionIllager": "illusioner", "VindicationIllager": "vindicator",
    "WitherBoss": "wither", "MushroomCow": "mooshroom",
}

def snake(s):
    return re.sub(r"(?<=[a-z0-9])([A-Z])", r"_\1", s).lower()

def resolve_stack(src, name, meta):
    if name.startswith("RECORD_"): return ["music_disc_" + name[7:].lower()]
    if name == "COAL" and meta == "*": return ["coal", "charcoal"]
    if (name, meta) in META: return META[(name, meta)]
    if meta not in (None, "*", 0) and (name, meta) not in META:
        raise KeyError(f"{src}.{name}:{meta}")
    if name in RENAME: return RENAME[name]
    if (name, None) in META: return META[(name, None)]
    return [name.lower()]

def resolve_ore(key):
    if key in ORE: return ORE[key]
    m = re.match(r"(ore|ingot|dust|gem|cluster)([A-Z]\w*)", key)
    if not m: raise KeyError(key)
    kind, mat = m.groups()
    if kind == "cluster" or mat in TC_ONLY or key == "quicksilver": return None
    return [f"#c:{PREFIX[kind]}/{snake(mat)}"]

def split_args(s):
    out, depth, cur = [], 0, ""
    for ch in s:
        if ch in "([": depth += 1
        if ch in ")]": depth -= 1
        if ch == "," and depth == 0: out.append(cur.strip()); cur = ""
        else: cur += ch
    out.append(cur.strip())
    return out

def find_calls(text):
    for m in re.finditer(r"ThaumcraftApi\.(registerObjectTag|registerComplexObjectTag|registerEntityTag)\(", text):
        i, depth = m.end(), 1
        while depth:
            depth += {"(": 1, ")": -1}.get(text[i], 0); i += 1
        yield m.group(1), split_args(text[m.end():i - 1])

STACK = re.compile(r"new ItemStack\((Items|Blocks|ItemsTC|BlocksTC)\.([A-Za-z_0-9]+)(?:, ?\d+, ?(\d+))?\)$")

def parse_target(arg):
    """Returns list of targets, or None to skip (Thaumcraft content / unsupported)."""
    if arg.startswith('"'): return resolve_ore(arg.strip('"'))
    m = STACK.match(arg)
    if not m: return None
    src, name, meta = m.groups()
    if src.endswith("TC"): return None
    meta = None if meta is None else ("*" if meta == "32767" else int(meta))
    return resolve_stack(src, name, meta)

def parse_aspects(arg):
    out, res = {}, {}
    m = re.match(r"new AspectList\((new ItemStack\([^()]*\))\)", arg)
    if m:
        t = parse_target(m.group(1))
        if not t: return None
        res["copy"] = t[0]
    elif not arg.startswith("new AspectList()"): return None
    for a, n in re.findall(r"\.(?:add|merge)\(Aspect\.(\w+), (\d+)\)", arg):
        out[a] = out.get(a, 0) + int(n)
    if out: res["aspects"] = out
    return res

def target_entry(t):
    if t.startswith("#"): return {"tag": t[1:]}
    if t.startswith("block:"): return {"block": t[6:]}
    return {"item": t if ":" in t else f"minecraft:{t}"}

def main():
    text = re.sub(r"\s+", " ", open(SRC).read())
    # Aspect constant name -> tag
    aspect_src = open(os.path.join(os.path.dirname(SRC), "../../api/aspects/Aspect.java")).read()
    const = dict(re.findall(r"Aspect (\w+) = new Aspect\(\"(\w+)\"", aspect_src))
    items, entities, skipped = [], [], []
    for fn, args in find_calls(text):
        if fn == "registerEntityTag":
            name = args[0].strip('"')
            if name.startswith("Thaumcraft.") or len(args) > 2:  # TC mobs / NBT variants: later
                skipped.append(f"entity {name}"); continue
            asp = parse_aspects(args[1])
            if not asp or "copy" in asp or "aspects" not in asp: skipped.append(f"entity {name}"); continue
            entities.append({"entity": "minecraft:" + ENTITY.get(name, snake(name)),
                             "aspects": {const[k]: v for k, v in asp["aspects"].items()}})
            continue
        try:
            targets = parse_target(args[0])
        except KeyError as e:
            skipped.append(f"unmapped {e}"); continue
        asp = parse_aspects(args[1]) if len(args) > 1 else None
        if targets is None or asp is None:
            skipped.append(f"{fn}({args[0][:60]})"); continue
        if "aspects" in asp: asp["aspects"] = {const[k]: v for k, v in asp["aspects"].items()}
        if "copy" in asp:
            c = target_entry(asp["copy"])
            if "item" not in c: skipped.append(f"copy-from-tag {args[0][:50]}"); continue
            asp["copy"] = c["item"]
        for t in targets:
            e = target_entry(t) | asp
            if fn == "registerComplexObjectTag": e["complex"] = True
            items.append(e)
    os.makedirs(f"{OUT}/items", exist_ok=True); os.makedirs(f"{OUT}/entities", exist_ok=True)
    json.dump({"entries": items}, open(f"{OUT}/items/vanilla.json", "w"), indent=1)
    json.dump({"entries": entities}, open(f"{OUT}/entities/vanilla.json", "w"), indent=1)
    print(f"items: {len(items)}  entities: {len(entities)}  skipped: {len(skipped)}")
    for s in skipped:
        if "TC" not in s and "Thaumcraft" not in s: print("  skip:", s)

main()
