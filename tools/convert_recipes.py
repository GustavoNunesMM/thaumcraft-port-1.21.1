"""Converts the Thaumcraft 1.12 recipes defined in Java (ConfigRecipes) into 1.21 datapack JSON.

Run from the repo root: python3 tools/convert_recipes.py [arcane] [crucible] [infusion]
(default: every kind whose recipe type is already ported, see KINDS below).

How it works: a small parser reads the Java expressions passed to ThaumcraftApi.addArcaneCraftingRecipe /
addCrucibleRecipe / addInfusionCraftingRecipe and evaluates them into items, tags, aspect lists and patterns.
1.12 names are mapped to 1.21 ids (flattened metadata, ore dictionary -> c: tags). Every recipe gets a
neoforge:conditions list with an item_exists condition per Thaumcraft item it uses, so recipes for things that
aren't ported yet are simply skipped until they are. Recipes the parser can't express are reported.
"""
import json, os, re, sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
REF = os.path.join(ROOT, "reference/thaumcraft")
OUT = os.environ.get("TC_RECIPE_OUT") or os.path.join(ROOT, "mod/src/main/resources/data/thaumcraft/recipe")
KINDS = {"arcane", "crucible", "infusion"}  # recipe types implemented in the port so far

# ---------------------------------------------------------------------------------------------- Java tokenizer/parser

TOKEN = re.compile(r'\s*(?:(?P<str>"(?:\\.|[^"\\])*")|(?P<chr>\'(?:\\.|[^\'\\])\')|(?P<num>-?\d+(?:\.\d+)?[FfLlDd]?)|'
                   r'(?P<id>[A-Za-z_$][\w$]*)|(?P<op>[(){}\[\],.;+\-*/?:<>=!&|]))')


def tokenize(src):
    toks, pos = [], 0
    while pos < len(src):
        m = TOKEN.match(src, pos)
        if not m or m.end() == pos:
            if src[pos:].strip() == "":
                break
            raise SyntaxError(src[pos:pos + 40])
        pos = m.end()
        kind = m.lastgroup
        toks.append((kind, m.group(kind)))
    return toks


class P:
    def __init__(self, toks):
        self.t, self.i = toks, 0

    def peek(self, k=0):
        return self.t[self.i + k] if self.i + k < len(self.t) else (None, None)

    def eat(self, v=None):
        tok = self.t[self.i]
        if v is not None and tok[1] != v:
            raise SyntaxError(f"expected {v}, got {tok}")
        self.i += 1
        return tok

    def args(self):
        self.eat("(")
        out = []
        while self.peek()[1] != ")":
            out.append(self.expr())
            if self.peek()[1] == ",":
                self.eat(",")
        self.eat(")")
        return out

    def expr(self):
        left = self.additive()
        if self.peek()[1] in ("<", ">"):
            op = self.eat()[1]
            left = ("cmp", op, left, self.additive())
        return left

    def additive(self):
        left = self.unary()
        while self.peek()[1] in ("+", "-"):
            op = self.eat()[1]
            left = ("concat" if op == "+" else "minus", left, self.unary())
        return left

    def unary(self):
        k, v = self.peek()
        if v == "(" and self.peek(1)[0] == "id" and self.peek(2)[1] == ")" and self.peek(1)[1][0].isupper():
            self.eat("(")
            typ = self.eat()[1]
            self.eat(")")
            return ("cast", typ, self.unary())
        if v == "(":
            self.eat("(")
            e = self.expr()
            self.eat(")")
            return self.postfix(e)
        if k == "str":
            self.eat()
            return self.postfix(("str", json.loads(v)))
        if k == "chr":
            self.eat()
            return ("chr", v[1:-1])
        if k == "num":
            self.eat()
            return ("num", float(v.rstrip("FfLlDd")) if "." in v else int(v.rstrip("FfLlDd")))
        if v == "new":
            self.eat()
            typ = self.eat()[1]
            while self.peek()[1] == ".":
                self.eat(".")
                typ += "." + self.eat()[1]
            if self.peek()[1] == "[" and self.peek(1)[1] != "]":
                self.eat("[")
                size = self.expr()
                self.eat("]")
                return ("array", typ, [None] * size[1] if size[0] == "num" else [])
            if self.peek()[1] == "[":
                self.eat("[")
                self.eat("]")
                self.eat("{")
                items = []
                while self.peek()[1] != "}":
                    items.append(self.expr())
                    if self.peek()[1] == ",":
                        self.eat(",")
                self.eat("}")
                return ("array", typ, items)
            if self.peek()[1] == "<":
                while self.eat()[1] != ">":
                    pass
            return self.postfix(("new", typ, self.args()))
        if k == "id":
            self.eat()
            return self.postfix(("name", v))
        raise SyntaxError(f"unexpected {self.peek()}")

    def postfix(self, e):
        while True:
            v = self.peek()[1]
            if v == ".":
                self.eat(".")
                name = self.eat()[1]
                if self.peek()[1] == "(":
                    e = ("call", e, name, self.args())
                else:
                    e = ("field", e, name)
            elif v == "(" and e[0] == "name":
                e = ("call", None, e[1], self.args())
            elif v == "[":
                self.eat("[")
                idx = self.expr()
                self.eat("]")
                e = ("index", e, idx)
            else:
                return e


def dotted(e):
    if e[0] == "name":
        return e[1]
    if e[0] == "field":
        return dotted(e[1]) + "." + e[2]
    return None


DYE_KEYS = ["white", "orange", "magenta", "lightBlue", "yellow", "lime", "pink", "gray", "silver", "cyan", "purple", "blue", "brown",
            "green", "red", "black"]  # 1.12 EnumDyeColor translation keys, in ordinal order


def subst(e, env):
    """Replaces local variables in an expression with their current values."""
    if not isinstance(e, tuple):
        return e
    if e[0] == "name" and e[1] in env:
        return env[e[1]]
    if e[0] in ("str", "chr", "num", "dye", "aspect"):
        return e
    if e[0] == "array" and isinstance(e[2], list):
        return (e[0], e[1], [subst(x, env) for x in e[2]])
    return tuple(subst(x, env) if isinstance(x, tuple) else [subst(y, env) for y in x] if isinstance(x, list) else x for x in e)


def const(e):
    """Evaluates int/string expressions (loop counters, recipe names built by concatenation)."""
    k = e[0]
    if k in ("num", "str"):
        return e[1]
    if k == "concat":
        l, r = const(e[1]), const(e[2])
        return str(l) + str(r) if isinstance(l, str) or isinstance(r, str) else l + r
    if k == "minus":
        return const(e[1]) - const(e[2])
    if k == "cmp":
        return const(e[2]) < const(e[3]) if e[1] == "<" else const(e[2]) > const(e[3])
    if k == "call" and e[2] == "toLowerCase":
        return const(e[1]).lower()
    if k == "call" and e[2] == "getTranslationKey" and e[1][0] == "dye":
        return DYE_KEYS[e[1][1]]
    if k == "call" and e[2] == "getTag" and e[1][0] == "aspect":
        return e[1][1]
    if k == "index" and dotted(e[1]) == "ConfigAspects.dyes":
        return DYES[const(e[2])]
    raise Unsupported(f"constant {e}")


DYES = ["dyeBlack", "dyeRed", "dyeGreen", "dyeBrown", "dyeBlue", "dyePurple", "dyeCyan", "dyeLightGray", "dyeGray", "dyePink", "dyeLime",
        "dyeYellow", "dyeLightBlue", "dyeMagenta", "dyeOrange", "dyeWhite"]


class Interp:
    """Walks the statements of ConfigRecipes, keeping locals and expanding loops, and reports recipe registrations."""

    def __init__(self, toks, emit):
        self.p, self.emit = P(toks), emit

    def skip_stmt(self):
        p, depth = self.p, 0
        while True:
            v = p.eat()[1]
            if v in ("(", "{"):
                depth += 1
            elif v in (")", "}"):
                depth -= 1
                if depth == 0 and v == "}":
                    return
            elif v == ";" and depth == 0:
                return

    def block(self, env):
        while self.p.peek()[1] not in ("}", None):
            self.stmt(env)

    def loop(self, env, var, values):
        start = self.p.i
        for val in values:
            self.p.i = start
            env[var] = val
            self.stmt(env)
        if not values:
            self.skip_stmt()

    def stmt(self, env):
        p = self.p
        k, v = p.peek()
        if v == "{":
            p.eat()
            self.block(env)
            p.eat("}")
            return
        if v == "for":
            p.eat()
            p.eat("(")
            if p.peek(2)[1] == ":":
                p.eat()
                var = p.eat()[1]
                p.eat(":")
                it = dotted(p.expr()[1]) if p.peek()[1] else None
                p.eat(")")
                if it == "EnumDyeColor":
                    values = [("dye", i) for i in range(16)]
                elif it == "Aspect.aspects":
                    values = [("aspect", a) for a in ASPECTS.values()]
                else:
                    values = []  # loops over runtime registries: nothing to convert
                self.loop(env, var, values)
                return
            p.eat("int")
            var = p.eat()[1]
            p.eat("=")
            i = const(p.expr())
            p.eat(";")
            cond = p.expr()
            p.eat(";")
            while p.eat()[1] != ")":
                pass
            values = []
            while const(subst(cond, {**env, var: ("num", i)})):
                values.append(("num", i))
                i += 1
            self.loop(env, var, values)
            return
        if v in ("if", "while", "return", "try"):
            p.eat()
            if p.peek()[1] == "(":
                p.args()
            if v == "return":
                self.skip_stmt()
            else:
                self.skip_stmt()
                if p.peek()[1] == "else":
                    p.eat()
                    self.skip_stmt()
            return
        start = p.i
        try:
            if k == "id" and (p.peek(1)[0] == "id" or p.peek(1)[1] in ("<",) or p.peek(1)[1] == "[" and p.peek(2)[1] == "]"):
                p.eat()
                if p.peek()[1] == "<":
                    depth = 0
                    while True:
                        t = p.eat()[1]
                        depth += t == "<"
                        depth -= t == ">"
                        if depth == 0:
                            break
                while p.peek()[1] == "[":
                    p.eat("[")
                    p.eat("]")
                name = p.eat()[1]
                if p.peek()[1] == "=":
                    p.eat("=")
                    env[name] = subst(p.expr(), env)
                p.eat(";")
                return
            if k == "id" and p.peek(1)[1] == "+" and p.peek(2)[1] == "+":
                name = p.eat()[1]
                p.eat()
                p.eat()
                p.eat(";")
                env[name] = ("num", const(env[name]) + 1)
                return
            if k == "id" and p.peek(1)[1] == "[":
                name = p.eat()[1]
                p.eat("[")
                idx = const(subst(p.expr(), env))
                p.eat("]")
                p.eat("=")
                env[name][2][idx] = subst(p.expr(), env)
                p.eat(";")
                return
            e = subst(p.expr(), env)
            p.eat(";")
            if e[0] == "call":
                self.emit(e)
        except (SyntaxError, IndexError, Unsupported, KeyError, TypeError):
            p.i = start
            self.skip_stmt()


def methods(text):
    """Bodies of the static methods of a class, as token lists."""
    out = {}
    for m in re.finditer(r"\n   public static void (\w+)\([^)]*\) \{\n", text):
        depth, j = 1, m.end()
        while depth:
            depth += {"{": 1, "}": -1}.get(text[j], 0)
            j += 1
        out[m.group(1)] = tokenize(text[m.end():j - 1])
    return out


# ---------------------------------------------------------------------------------------------- names


def class_source(cls):
    for d, _, files in os.walk(os.path.join(REF, "common")):
        if cls + ".java" in files:
            return open(os.path.join(d, cls + ".java")).read()
    return ""


def registry_names(config, holder):
    """Field name -> (registry name, [variants]) for ItemsTC/BlocksTC, from Config*.java and the classes."""
    text = open(os.path.join(REF, "common/config", config)).read()
    names = {}
    for m in re.finditer(holder + r"\.(\w+) = (.*?);\n", text, re.S):
        field, rhs = m.group(1), m.group(2)
        cm = re.search(r"new (\w+)(?:\.(\w+))?\((.*)", rhs, re.S)
        if not cm:
            continue
        lits = re.findall(r'"([a-z0-9_]+)"', cm.group(3).split(")")[0] if cm.group(1) != "ItemTCBase" else cm.group(3))
        if lits and lits[0] != "thaumcraft":
            names[field] = (lits[0], lits[1:] if cm.group(1) == "ItemTCBase" else [])
            continue
        src = class_source(cm.group(1))
        sm = re.search(r'super\(\s*(?:Material\.\w+,\s*)?(?:\w+\.class,\s*)?"([a-z0-9_]+)"((?:\s*,\s*"[a-z0-9_]+")*)', src)
        if sm:
            names[field] = (sm.group(1), re.findall(r'"([a-z0-9_]+)"', sm.group(2) or ""))
            continue
        rm = re.search(r'setRegistryName\((?:"thaumcraft", )?"([a-z0-9_]+)"\)', rhs) or re.search(r'setRegistryName\((?:"thaumcraft", )?"([a-z0-9_]+)"\)', src) \
            or re.search(r'super\([^;]*?"([a-z0-9_]+)"', src)
        if rm:
            names[field] = (rm.group(1), [])
    return names


ITEMS = registry_names("ConfigItems.java", "ItemsTC")
BLOCKS = registry_names("ConfigBlocks.java", "BlocksTC")
COLORS = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue",
          "brown", "green", "red", "black"]
DYE_ENUM = {c.upper(): c for c in COLORS} | {"SILVER": "light_gray"}

# Port names for 1.12 metadata variants (default: name_variant).
VARIANT_NAME = {("nugget", "rareearth"): "rare_earth", ("nugget", "iron"): "minecraft:iron_nugget",
                ("thaumonomicon", "normal"): "thaumonomicon", ("thaumonomicon", "cheat"): "thaumonomicon_cheat"}
VARIANT_ONLY = {"baubles"}  # items whose variants become the item names by themselves


def tc_item(field, meta=0, holder="ItemsTC"):
    table = ITEMS if holder == "ItemsTC" else BLOCKS
    if field not in table:
        raise KeyError(f"{holder}.{field}")
    name, variants = table[field]
    if variants:
        v = variants[meta] if meta < len(variants) else variants[0]
        name = VARIANT_NAME.get((name, v), v if name in VARIANT_ONLY else f"{name}_{v}")
    return name if ":" in name else "thaumcraft:" + name


# Vanilla Items/Blocks fields to 1.21 ids (field.lower() unless listed), with metadata variants.
VANILLA = {"REEDS": "sugar_cane", "PLANKS": "#minecraft:planks", "LOG": "#minecraft:logs", "LOG2": "#minecraft:logs",
           "WOOL": "#minecraft:wool", "STONE_SLAB": "stone_slab", "WOODEN_SLAB": "#minecraft:wooden_slabs", "GLASS_PANE": "glass_pane",
           "STAINED_GLASS": "#c:glass_blocks", "SKULL": "skeleton_skull", "DYE": "#c:dyes", "COAL": "#minecraft:coals",
           "WEB": "cobweb", "SNOWBALL": "snowball", "FISH": "#minecraft:fishes", "BOAT": "oak_boat", "SIGN": "oak_sign",
           "WOODEN_DOOR": "oak_door", "BED": "#minecraft:beds", "NETHERBRICK": "nether_bricks", "HARDENED_CLAY": "terracotta",
           "SPECKLED_MELON": "glistering_melon_slice", "MELON": "melon_slice", "GOLDEN_RAIL": "powered_rail", "CARPET": "#minecraft:wool_carpets",
           "TALLGRASS": "short_grass", "RED_FLOWER": "#minecraft:small_flowers", "YELLOW_FLOWER": "dandelion", "MAGMA": "magma_block",
           "FIREWORKS": "firework_rocket", "SAPLING": "#minecraft:saplings", "LEAVES": "#minecraft:leaves", "NETHER_WART": "nether_wart",
           "QUARTZ_BLOCK": "quartz_block", "LIT_PUMPKIN": "jack_o_lantern", "STONEBRICK": "stone_bricks", "PRISMARINE": "prismarine",
           "CHORUS_FRUIT_POPPED": "popped_chorus_fruit", "MOB_SPAWNER": "spawner", "END_BRICKS": "end_stone_bricks",
           "POTIONITEM": "potion", "EXPERIENCE_BOTTLE": "experience_bottle", "FIRE_CHARGE": "fire_charge"}
VANILLA_META = {("DYE", 0): "ink_sac", ("DYE", 15): "bone_meal", ("DYE", 4): "lapis_lazuli", ("DYE", 3): "cocoa_beans",
                ("COAL", 1): "charcoal", ("COAL", 0): "coal", ("SKULL", 1): "wither_skeleton_skull", ("SKULL", 2): "zombie_head",
                ("SKULL", 3): "player_head", ("SKULL", 4): "creeper_head", ("SKULL", 5): "dragon_head", ("GOLDEN_APPLE", 1): "enchanted_golden_apple",
                ("WOOL", 0): "white_wool", ("PLANKS", 0): "oak_planks", ("LOG", 0): "oak_log", ("STONE", 0): "stone",
                ("SAPLING", 0): "oak_sapling", ("FISH", 0): "cod", ("STONEBRICK", 0): "stone_bricks"}
COLORED = {"WOOL": "wool", "CARPET": "carpet", "STAINED_GLASS": "stained_glass", "STAINED_GLASS_PANE": "stained_glass_pane",
           "STAINED_HARDENED_CLAY": "terracotta", "CONCRETE": "concrete", "CONCRETE_POWDER": "concrete_powder"}
# Ore dictionary to 1.21 tags.
ORE = {"stickWood": "#c:rods/wooden", "plankWood": "#minecraft:planks", "logWood": "#minecraft:logs", "slabWood": "#minecraft:wooden_slabs",
       "stone": "#c:stones", "cobblestone": "#c:cobblestones", "string": "#c:strings", "leather": "#c:leathers", "feather": "#c:feathers",
       "gemQuartz": "#c:gems/quartz", "gemDiamond": "#c:gems/diamond", "gemEmerald": "#c:gems/emerald", "gemLapis": "#c:gems/lapis",
       "gemAmber": "#c:gems/amber", "dustRedstone": "#c:dusts/redstone", "dustGlowstone": "#c:dusts/glowstone", "slimeball": "#c:slime_balls",
       "blockGlass": "#c:glass_blocks", "paneGlass": "#c:glass_panes", "chest": "#c:chests", "workbench": "#c:player_workstations/crafting_tables",
       "enderpearl": "#c:ender_pearls", "obsidian": "#c:obsidians", "netherStar": "#c:nether_stars", "bone": "#c:bones",
       "gunpowder": "#c:gunpowders", "egg": "#c:eggs", "sand": "#c:sands", "gravel": "#c:gravels", "blockRedstone": "#c:storage_blocks/redstone",
       "blockGold": "#c:storage_blocks/gold", "blockIron": "#c:storage_blocks/iron", "blockDiamond": "#c:storage_blocks/diamond",
       "nuggetMeat": "#thaumcraft:nuggets/meat", "quicksilver": "thaumcraft:quicksilver", "dyeBlack": "#c:dyes/black",
       "cropWheat": "#c:crops/wheat", "treeSapling": "#minecraft:saplings", "treeLeaves": "#minecraft:leaves", "torch": "minecraft:torch",
       "nitor": "#thaumcraft:nitor", "trapdoorWood": "#minecraft:wooden_trapdoors", "fenceWood": "#c:fences/wooden",
       "fenceGateWood": "#c:fence_gates/wooden", "doorWood": "#minecraft:wooden_doors", "chestWood": "#c:chests/wooden",
       "blockWool": "#minecraft:wool", "record": "#c:music_discs", "listAllseed": "#c:seeds", "cropNetherWart": "#c:crops/nether_wart"}
ORE_PREFIX = {"ingot": "ingots", "nugget": "nuggets", "plate": "plates", "ore": "ores", "dust": "dusts", "gem": "gems",
              "block": "storage_blocks", "cluster": "thaumcraft:clusters"}


def ore(name):
    if name in ORE:
        return ORE[name]
    if name.startswith("dye") and len(name) > 3:
        return "#c:dyes/" + re.sub(r"(?<=[a-z])([A-Z])", r"_\1", name[3:]).lower()
    for p, tagdir in ORE_PREFIX.items():
        if name.startswith(p) and name[len(p):len(p) + 1].isupper():
            mat = re.sub(r"(?<=[a-z])([A-Z])", r"_\1", name[len(p):]).lower()
            return f"#c:{tagdir}/{mat}" if ":" not in tagdir else f"#{tagdir}/{mat}"
    raise KeyError(f"oredict {name}")

# ---------------------------------------------------------------------------------------------- evaluation


class Unsupported(Exception):
    pass


ASPECTS = {"AIR": "aer", "EARTH": "terra", "FIRE": "ignis", "WATER": "aqua", "ORDER": "ordo", "ENTROPY": "perditio", "VOID": "vacuos",
           "LIGHT": "lux", "MOTION": "motus", "COLD": "gelum", "CRYSTAL": "vitreus", "METAL": "metallum", "LIFE": "victus", "DEATH": "mortuus",
           "ENERGY": "potentia", "EXCHANGE": "permutatio", "MAGIC": "praecantatio", "AURA": "auram", "ALCHEMY": "alkimia", "FLUX": "vitium",
           "DARKNESS": "tenebrae", "ELDRITCH": "alienis", "FLIGHT": "volatus", "PLANT": "herba", "TOOL": "instrumentum", "CRAFT": "fabrico",
           "MECHANISM": "machina", "TRAP": "vinculum", "SOUL": "spiritus", "MIND": "cognitio", "SENSES": "sensus", "AVERSION": "aversio",
           "PROTECT": "praemunio", "DESIRE": "desiderium", "UNDEAD": "exanimis", "BEAST": "bestia", "MAN": "humanus"}
CONFIG_CRYSTALS = {"AIR_CRYSTAL": "aer", "FIRE_CRYSTAL": "ignis", "WATER_CRYSTAL": "aqua", "EARTH_CRYSTAL": "terra", "ORDER_CRYSTAL": "ordo",
                   "ENTROPY_CRYSTAL": "perditio", "FLUX_CRYSTAL": "vitium"}


class Ctx:
    def __init__(self):
        self.groups = {}
        self.used = set()


def stack(item, count=1, components=None):
    if item.startswith("thaumcraft:"):
        CTX.used.add(item)
    s = {"item": item} if not item.startswith("#") else {"tag": item[1:]}
    if count != 1:
        s["count"] = count
    if components:
        s["components"] = components
    return s


def crystal(aspect):
    return stack("thaumcraft:crystal_essence", 1, {"thaumcraft:essentia": {aspect: 1}})


def resolve_holder(e, meta=0):
    """ItemsTC.x / BlocksTC.x / Items.X / Blocks.X / BlocksTC.nitor.get(EnumDyeColor.Y) -> id."""
    if e[0] == "call" and e[2] == "get" and dotted(e[1]) in ("BlocksTC.nitor", "BlocksTC.candles", "BlocksTC.banners"):
        arg = e[3][0]
        color = COLORS[arg[1]] if arg[0] == "dye" else DYE_ENUM[dotted(arg).split(".")[-1]]
        kind = dotted(e[1]).split(".")[1].rstrip("s")
        return f"thaumcraft:{'nitor' if kind == 'nitor' else kind}_{color}"
    d = dotted(e)
    if not d:
        raise Unsupported(f"holder {e}")
    holder, _, field = d.rpartition(".")
    if holder in ("ItemsTC", "BlocksTC"):
        return tc_item(field, meta if isinstance(meta, int) and meta != 32767 else 0, holder)
    if holder in ("Items", "Blocks"):
        if field in COLORED and isinstance(meta, int) and 0 <= meta < 16:
            return f"minecraft:{COLORS[meta]}_{COLORED[field]}"
        if field == "DYE" and isinstance(meta, int) and meta not in (0, 3, 4, 15, 32767):
            return f"minecraft:{COLORS[15 - meta]}_dye"
        if meta not in (0, None, 32767) and (field, meta) in VANILLA_META:
            return "minecraft:" + VANILLA_META[(field, meta)]
        if meta == 32767 and field in VANILLA:
            v = VANILLA[field]
            return v if v.startswith("#") else "minecraft:" + v
        if (field, 0) in VANILLA_META and meta in (0, None):
            return "minecraft:" + VANILLA_META[(field, 0)]
        v = VANILLA.get(field, field.lower())
        return v if v.startswith("#") or ":" in v else "minecraft:" + v
    raise Unsupported(f"holder {d}")


def aspects(e):
    """An AspectList expression. new AspectList(stack) / .remove(new AspectList(stack)) (the aspects of an item, known
    only at runtime) go to CTX.extra as aspects_of / remove_aspects_of."""
    out = {}
    while e and e[0] == "call" and e[2] in ("add", "merge", "remove"):
        if e[2] == "remove":
            arg = e[3][0]
            if arg[0] != "new" or arg[1] != "AspectList" or len(arg[2]) != 1:
                raise Unsupported(f"aspects remove {arg}")
            CTX.extra["remove_aspects_of"] = output(arg[2][0])["item"]
        else:
            a, n = e[3]
            out[a[1] if a[0] == "aspect" else ASPECTS[dotted(a).split(".")[-1]]] = n[1]
        e = e[1]
    if e is None or e[0] == "name" and e[1] == "null":
        return out
    if e[0] != "new" or e[1] != "AspectList":
        raise Unsupported(f"aspects {e}")
    if e[2]:
        CTX.extra["aspects_of"] = output(e[2][0])["item"]
    return dict(reversed(list(out.items())))


def special(e):
    """Stacks made by helper methods: filled phials, golem seals, fluid buckets."""
    if e[0] != "call":
        return None
    if e[2] == "makeFilledPhial":
        a = e[3][0]
        return stack("thaumcraft:phial_filled", 1, {"thaumcraft:essentia": {a[1] if a[0] == "aspect" else ASPECTS[dotted(a).split(".")[-1]]: 10}})
    if e[2] == "getSealStack":
        return stack("thaumcraft:seal_" + const(e[3][0]).split(":")[1])
    if e[2] == "getFilledBucket":
        fluid = dotted(e[3][0][2][0]).split(".")[-2].removeprefix("Fluid")
        return stack({"FLUXGOO": "thaumcraft:bucket_flux_goo", "DEATH": "thaumcraft:bucket_death",
                      "PURIFYING": "thaumcraft:bucket_pure"}.get(fluid.upper(), "thaumcraft:bucket_" + fluid.lower()))
    return None


def ingredient(e):
    """An input in 1.21 ingredient form; stacks with components become neoforge:components ingredients."""
    i = raw_ingredient(e)
    return [as_ingredient(x) for x in i] if isinstance(i, list) else as_ingredient(i)


def as_ingredient(s):
    s = {k: v for k, v in s.items() if k != "count"}
    if "components" in s:
        return {"type": "neoforge:components", "items": s["item"], "components": s["components"], "strict": False}
    return s


def raw_ingredient(e):
    sp = special(e)
    if sp:
        return {k: v for k, v in sp.items() if k != "count"}
    k = e[0]
    if k == "str":
        return stack(ore(e[1]))
    if k == "cast":
        return raw_ingredient(e[2])
    if k == "new" and e[1] == "ItemStack":
        a = e[2]
        meta = a[2][1] if len(a) > 2 else 0
        count = a[1][1] if len(a) > 1 else 1
        return stack(resolve_holder(a[0], meta), 1 if count == 0 else 1)
    if k in ("name", "field"):
        d = dotted(e)
        if d and d.startswith("ConfigItems.") and d.split(".")[1] in CONFIG_CRYSTALS:
            return crystal(CONFIG_CRYSTALS[d.split(".")[1]])
        return stack(resolve_holder(e))
    if k == "call" and e[2] == "makeCrystal":
        a = e[3][0]
        return crystal(a[1] if a[0] == "aspect" else ASPECTS[dotted(a).split(".")[-1]])
    if k == "call" and dotted(e[1]) == "Ingredient" and e[2] in ("fromItem", "fromItems"):
        items = [raw_ingredient(x) for x in e[3]]
        return items[0] if len(items) == 1 else items
    if k == "call" and dotted(e[1]) == "Ingredient" and e[2] == "fromStacks":
        args = e[3][0][2] if len(e[3]) == 1 and e[3][0][0] == "array" else e[3]
        items = [raw_ingredient(x) for x in args]
        return items[0] if len(items) == 1 else items
    if k == "call" and e[2] == "getDefaultState":
        return raw_ingredient(e[1])
    raise Unsupported(f"ingredient {e}")


def output(e):
    sp = special(e)
    if sp:
        return sp
    k = e[0]
    if k == "new" and e[1] == "ItemStack":
        a = e[2]
        count = a[1][1] if len(a) > 1 else 1
        meta = a[2][1] if len(a) > 2 else 0
        return stack(resolve_holder(a[0], meta), count)
    if k in ("name", "field"):
        d = dotted(e)
        if d and d.startswith("ConfigItems.") and d.split(".")[1] in CONFIG_CRYSTALS:
            return crystal(CONFIG_CRYSTALS[d.split(".")[1]])
        return stack(resolve_holder(e))
    if k == "call" and e[2] == "makeCrystal":
        a = e[3][0]
        return crystal(a[1] if a[0] == "aspect" else ASPECTS[dotted(a).split(".")[-1]])
    raise Unsupported(f"output {e}")


def res_loc(e):
    if e[0] == "new" and e[1] == "ResourceLocation":
        parts = [const(a) for a in e[2]]
        if parts == [""]:
            return None
        return ":".join(parts) if len(parts) == 2 else (parts[0] if ":" in parts[0] else "minecraft:" + parts[0])
    if e[0] == "name" and e[1] in CTX.groups:
        return CTX.groups[e[1]]
    if e[0] == "name" and e[1] == "defaultGroup":
        return None
    raise Unsupported(f"resource location {e}")


def result_id(o):
    if isinstance(o, dict):
        r = {"id": o["item"]}
        if o.get("count", 1) != 1:
            r["count"] = o["count"]
        if "components" in o:
            r["components"] = o["components"]
        return r
    raise Unsupported("result")

# ---------------------------------------------------------------------------------------------- recipe kinds


def arcane(args):
    rid = res_loc(args[0])
    r = args[1]
    if r[0] != "new" or r[1] not in ("ShapedArcaneRecipe", "ShapelessArcaneRecipe"):
        raise Unsupported(f"arcane {r[1] if r[0] == 'new' else r}")
    a = r[2]
    group = res_loc(a[0])
    research, vis = a[1][1], a[2][1]
    crystals = aspects(a[3])
    out = result_id(output(a[4]))
    rest = a[5:]
    if len(rest) == 1 and rest[0][0] == "array":
        rest = rest[0][2]
    js = {"type": "thaumcraft:arcane_shaped" if r[1] == "ShapedArcaneRecipe" else "thaumcraft:arcane_shapeless",
          "research": research, "vis": vis}
    if crystals:
        js["crystals"] = crystals
    if group:
        js["group"] = group
    if r[1] == "ShapedArcaneRecipe":
        pattern = []
        while rest and rest[0][0] == "str":
            pattern.append(rest.pop(0)[1])
        key = {}
        while rest:
            ch = rest.pop(0)
            if ch[0] != "chr":
                raise Unsupported(f"pattern key {ch}")
            key[ch[1]] = ingredient(rest.pop(0))
        js["pattern"] = pattern
        js["key"] = key
    else:
        js["ingredients"] = [ingredient(x) for x in rest]
    js["result"] = out
    return rid, js


def crucible(args):
    rid = res_loc(args[0])
    r, group = args[1], None
    if r[0] == "call" and r[2] == "setGroup":
        group, r = res_loc(r[3][0]), r[1]
    if r[0] != "new" or r[1] != "CrucibleRecipe":
        raise Unsupported(f"crucible {r[1] if r[0] == 'new' else r}")
    research, out, catalyst, asp = r[2]
    js = {"type": "thaumcraft:crucible", "research": research[1]}
    if group:
        js["group"] = group
    js |= {"catalyst": ingredient(catalyst), "aspects": aspects(asp), **CTX.extra, "result": result_id(output(out))}
    return rid, js


def infusion(args):
    rid = res_loc(args[0])
    r = args[1]
    if r[0] != "new" or r[1] != "InfusionRecipe":
        raise Unsupported(f"infusion {r[1] if r[0] == 'new' else r}")
    research, out, instability, asp, central, *comps = r[2]
    if len(comps) == 1 and comps[0][0] == "array":
        comps = comps[0][2]
    js = {"type": "thaumcraft:infusion", "research": research[1], "instability": instability[1], "aspects": aspects(asp),
          "central": ingredient(central), "components": [ingredient(c) for c in comps], "result": result_id(output(out))}
    return rid, js


def write(rid, js):
    ns, path = rid.split(":")
    path = path.lower()
    conds = [{"type": "neoforge:item_exists", "item": i} for i in sorted(CTX.used)]
    if conds:
        js = {"neoforge:conditions": conds, **js}
    os.makedirs(OUT, exist_ok=True)
    with open(os.path.join(OUT, path + ".json"), "w") as f:
        json.dump(js, f, indent=2)
        f.write("\n")


def main():
    global CTX
    kinds = set(sys.argv[1:]) or KINDS
    text = open(os.path.join(REF, "common/config/ConfigRecipes.java")).read()
    CTX = Ctx()
    fns = {"ThaumcraftApi.addArcaneCraftingRecipe": ("arcane", arcane), "ThaumcraftApi.addCrucibleRecipe": ("crucible", crucible),
           "ThaumcraftApi.addInfusionCraftingRecipe": ("infusion", infusion)}
    ok, failed = 0, []

    def emit(e):
        nonlocal ok
        name = (dotted(e[1]) + "." if e[1] else "") + e[2]
        if name not in fns or fns[name][0] not in kinds:
            return
        CTX.used = set()
        CTX.extra = {}
        try:
            rid, js = fns[name][1](e[3])
            write(rid, js)
            ok += 1
        except (Unsupported, KeyError, IndexError, TypeError) as ex:
            failed.append(f"{e[3][0] if e[3] else '?'}: {ex}"[:200])

    for body in methods(text).values():
        Interp(body, emit).block({"defaultGroup": ("new", "ResourceLocation", [("str", "")])})
    print(f"converted {ok} recipes")
    for f in failed:
        print("  not converted:", f)


CTX = None
if __name__ == "__main__":
    main()
