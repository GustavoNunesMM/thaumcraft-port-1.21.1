"""Converts Thaumcraft 1.12 research JSON (assets/thaumcraft/research/*.json) into the port's datapack format.

Run from the repo root: python3 tools/convert_research.py
Output: mod/src/main/resources/data/thaumcraft/thaumcraft/research/<category>.json

Changes made to each entry (everything else is copied as is):
- item strings with 1.12 metadata/NBT become 1.21 strings: "id", "id;count" or "id;count;{components json}",
  and "oredict:x" becomes a tag "#c:..."
- recipe names are lower-cased (1.21 resource locations cannot contain upper case)
"""
import json, os, re, glob

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
SRC = os.path.join(ROOT, "decompiled/assets/thaumcraft/research")
OUT = os.path.join(ROOT, "mod/src/main/resources/data/thaumcraft/thaumcraft/research")

META = {
    "thaumcraft:ingot": ["ingot_thaumium", "ingot_void", "ingot_brass"],
    "thaumcraft:plate": ["plate_brass", "plate_iron", "plate_thaumium", "plate_void"],
    "thaumcraft:metal": ["metal_brass", "metal_brass", "metal_thaumium", "metal_void"],
    "thaumcraft:nugget": ["minecraft:iron_nugget", "nugget_copper", "nugget_tin", "nugget_silver", "nugget_lead", "nugget_quicksilver",
                          "nugget_thaumium", "nugget_void", "nugget_brass", "nugget_quartz", "rare_earth"],
    "thaumcraft:cluster": ["cluster_iron", "cluster_gold", "cluster_copper", "cluster_tin", "cluster_silver", "cluster_lead",
                           "cluster_cinnabar", "cluster_quartz"],
    "thaumcraft:curio": ["curio_arcane", "curio_preserved", "curio_ancient", "curio_eldritch", "curio_knowledge", "curio_twisted",
                         "curio_rites"],
    "thaumcraft:mind": ["mind_clockwork", "mind_biothaumic"],
    "thaumcraft:module": ["module_vision", "module_aggression"],
    "thaumcraft:turret": ["turret_crossbow", "turret_crossbow_advanced", "arcane_bore"],
    "thaumcraft:amulet_vis": ["amulet_vis_found", "amulet_vis"],
    "thaumcraft:nitor": [f"nitor_{c}" for c in ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
                                                "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"]],
    "minecraft:dye": ["minecraft:ink_sac"] + [None] * 14 + ["minecraft:bone_meal"],
}
RENAME = {
    "minecraft:hardened_clay": "minecraft:terracotta", "minecraft:noteblock": "minecraft:note_block", "minecraft:web": "minecraft:cobweb",
    "thaumcraft:leather": "minecraft:leather", "thaumcraft:seal": "thaumcraft:seal_blank",
}
ORE = {"chest": "#c:chests"}
ENCH = {0: "minecraft:protection", 16: "minecraft:sharpness", 33: "minecraft:silk_touch", 35: "minecraft:fortune"}


def convert_item(s):
    if s.startswith("oredict:"):
        return ORE[s[len("oredict:"):]]
    parts = s.split(";")
    name = parts[0].replace("minecraft:thaumcraft:", "thaumcraft:")
    nums = [int(p) for p in parts[1:] if re.fullmatch(r"-?\d+", p)]
    nbt = next((p for p in parts[1:] if p.startswith("{")), None)
    count = nums[0] if nums else 1
    meta = nums[1] if len(nums) > 1 else 0
    components = None
    if name in META:
        name = META[name][meta]
        if ":" not in name:
            name = "thaumcraft:" + name
    name = RENAME.get(name, name)
    if nbt and "Aspects" in nbt:
        aspects = {k: int(a) for a, k in re.findall(r"amount:(\d+),key:'(\w+)'", nbt)}
        components = {"thaumcraft:essentia": aspects}
    elif nbt and "ench" in nbt:
        levels = {ENCH[int(i)]: int(l) for i, l in re.findall(r"id:(\d+)s,lvl:(\d+)s", nbt)}
        name = "minecraft:enchanted_book"
        components = {"minecraft:stored_enchantments": {"levels": levels}}
    out = name
    if count != 1 or components:
        out += f";{count}"
    if components:
        out += ";" + json.dumps(components, separators=(",", ":"))
    return out


def convert_icon(s):
    return s if "textures/" in s or s.startswith("focus:") else convert_item(s)


def convert_stage(st):
    st = dict(st)
    for k in ("required_item", "required_craft"):
        if k in st:
            st[k] = [convert_item(i) for i in st[k]]
    if "recipes" in st:
        st["recipes"] = [r.lower() for r in st["recipes"]]
    return st


os.makedirs(OUT, exist_ok=True)
total = 0
for f in sorted(glob.glob(os.path.join(SRC, "*.json"))):
    data = json.load(open(f))
    entries = []
    for e in data["entries"]:
        e = dict(e)
        if "icons" in e:
            e["icons"] = [convert_icon(i) for i in e["icons"]]
        if "reward_item" in e:
            e["reward_item"] = [convert_item(i) for i in e["reward_item"]]
        e["stages"] = [convert_stage(s) for s in e.get("stages", [])]
        if "addenda" in e:
            e["addenda"] = [convert_stage(a) for a in e["addenda"]]
        entries.append(e)
    total += len(entries)
    with open(os.path.join(OUT, os.path.basename(f)), "w") as o:
        json.dump({"entries": entries}, o, indent=1)
        o.write("\n")
print(f"converted {total} entries")
