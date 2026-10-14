"""Extracts the block placements of WorldGenMound (whose generate/generate2 failed to decompile: only the bytecode is in
reference/thaumcraft/common/world/objects/WorldGenMound.java) by simulating the simple stack pattern
    new BlockPos(x+a, y+b, z+c); getstatic Blocks.X; getDefaultState()|getStateFromMeta(m); World.setBlockState
and writes the structure template mod/src/main/resources/data/thaumcraft/structure/mound.nbt (1.12 metas mapped to 1.21
block states). The random tail of generate2 (loot crates/urns, trapped chest + TNT or chest, spawners) is coded in
MoundFeature.java.

Usage: python3 tools/mound_extract.py [--dump]
"""
import gzip, os, re, struct, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "reference/thaumcraft/common/world/objects/WorldGenMound.java")
OUT = os.path.join(ROOT, "mod/src/main/resources/data/thaumcraft/structure/mound.nbt")
TAIL_START = 0x74af  # generate2: from here on the placements depend on the random rolls

INSN = re.compile(r"^\s*// ([0-9a-f]{4}): (\S+)(?: (.*))?$")


def methods(lines):
    """Yields (method name, [(addr, op, arg)])."""
    cur, name = None, None
    for ln in lines:
        m = re.search(r"public boolean (generate2?)\(", ln)
        if m:
            if cur is not None:
                yield name, cur
            name, cur = m.group(1), []
            continue
        if cur is not None:
            mm = INSN.match(ln)
            if mm:
                cur.append((int(mm.group(1), 16), mm.group(2), mm.group(3) or ""))
    if cur is not None:
        yield name, cur


def simulate(name, insns):
    # local slots holding the base x/y/z
    xyz = {"generate": {4: 0, 5: 1, 6: 2}, "generate2": {3: 0, 4: 1, 5: 2}}[name]
    out = []
    stack = []
    for addr, op, arg in insns:
        if name == "generate2" and addr >= TAIL_START:
            break
        if name == "generate" and addr < 0x9e:
            continue  # the LocationIsValidSpawn checks
        if op == "iload":
            stack.append(("var", xyz.get(int(arg), None), 0))
        elif op == "bipush" or op == "sipush":
            stack.append(int(arg))
        elif op in ("iadd", "isub"):
            b, a = stack.pop(), stack.pop()
            if isinstance(a, tuple) and isinstance(b, int):
                stack.append(("var", a[1], a[2] + (b if op == "iadd" else -b)))
            else:
                raise ValueError(f"{name}@{addr:x}: {op} on {a} {b}")
        elif op == "invokespecial" and arg.startswith("net/minecraft/util/math/BlockPos.<init>"):
            z, y, x = stack.pop(), stack.pop(), stack.pop()
            stack.append(("pos", x[2], y[2], z[2]))
        elif op == "getstatic" and arg.startswith("net/minecraft/init/Blocks."):
            stack.append(("block", arg.split()[0].split(".")[-1]))
        elif op == "getstatic" and arg.startswith("thaumcraft/api/blocks/BlocksTC."):
            stack.append(("block", "TC:" + arg.split()[0].split(".")[-1]))
        elif op == "invokevirtual" and ".getDefaultState" in arg:
            b = stack.pop()
            stack.append(("state", b[1], None))
        elif op == "invokevirtual" and ".getStateFromMeta" in arg:
            meta = stack.pop()
            b = stack.pop()
            stack.append(("state", b[1], meta))
        elif op == "invokevirtual" and "World.setBlockState" in arg:
            st = stack.pop()
            pos = stack.pop()
            out.append((pos[1], pos[2], pos[3], st[1], st[2]))
        elif op in ("aload", "new", "dup", "pop", "ireturn"):
            pass
        elif op == "invokevirtual" and "generate2" in arg:
            stack.clear()
        else:
            raise ValueError(f"{name}@{addr:x}: unexpected {op} {arg}")
    return out


STAIR_FACING = {0: "east", 1: "west", 2: "south", 3: "north"}


def state_of(block, meta):
    """1.12 block + meta -> (1.21 name, properties)."""
    m = meta or 0
    if block == "AIR":
        return "minecraft:air", {}
    if block == "COBBLESTONE":
        return "minecraft:cobblestone", {}
    if block == "MOSSY_COBBLESTONE":
        return "minecraft:mossy_cobblestone", {}
    if block == "DIRT":
        return "minecraft:dirt", {}
    if block == "GRASS":
        return "minecraft:grass_block", {"snowy": "false"}
    if block == "STONEBRICK":
        return ["minecraft:stone_bricks", "minecraft:mossy_stone_bricks", "minecraft:cracked_stone_bricks",
                "minecraft:chiseled_stone_bricks"][m & 3], {}
    if block == "IRON_BARS":
        return "minecraft:iron_bars", {"east": "false", "north": "false", "south": "false", "west": "false", "waterlogged": "false"}
    if block == "TALLGRASS":
        return ["minecraft:dead_bush", "minecraft:short_grass", "minecraft:fern"][m % 3], {}
    if block == "STONE_STAIRS":
        if meta is None:
            m = 3  # BlockStairs' default state: facing north, bottom half
        return "minecraft:cobblestone_stairs", {"facing": STAIR_FACING[m & 3], "half": "top" if m & 4 else "bottom",
                                                 "shape": "straight", "waterlogged": "false"}
    if block == "LADDER":
        return "minecraft:ladder", {"facing": {2: "north", 3: "south", 4: "west", 5: "east"}.get(m, "north"), "waterlogged": "false"}
    raise ValueError(f"unmapped block {block}:{meta}")


# ------------------------------------------------------------------ NBT writer
def th(t, name):
    n = name.encode()
    return bytes([t]) + struct.pack(">H", len(n)) + n


def s_(name, v):
    b = v.encode()
    return th(8, name) + struct.pack(">H", len(b)) + b


def i_(name, v):
    return th(3, name) + struct.pack(">i", v)


def ilist(name, vals):
    return th(9, name) + bytes([3]) + struct.pack(">i", len(vals)) + b"".join(struct.pack(">i", v) for v in vals)


def clist(name, comps):
    return th(9, name) + bytes([10]) + struct.pack(">i", len(comps)) + b"".join(c + b"\x00" for c in comps)


def compound(name, body):
    return th(10, name) + body + b"\x00"


def main():
    lines = open(SRC, encoding="utf-8").read().splitlines()
    placed = {}
    for name, insns in methods(lines):
        for x, y, z, b, meta in simulate(name, insns):
            placed[(x, y, z)] = (b, meta)  # later writes win, as in the original
    if "--dump" in sys.argv:
        from collections import Counter
        print(Counter((b, m) for b, m in placed.values()))
        xs = [p[0] for p in placed]; ys = [p[1] for p in placed]; zs = [p[2] for p in placed]
        print("bounds", min(xs), max(xs), min(ys), max(ys), min(zs), max(zs), len(placed))
    xs = [p[0] for p in placed]; ys = [p[1] for p in placed]; zs = [p[2] for p in placed]
    assert min(xs) >= 0 and min(ys) >= 0 and min(zs) >= 0, "negative offsets"
    size = [max(xs) + 1, max(ys) + 1, max(zs) + 1]
    palette, pidx, blocks = [], {}, []
    for (x, y, z), (b, meta) in sorted(placed.items(), key=lambda kv: (kv[0][1], kv[0][2], kv[0][0])):
        name, props = state_of(b, meta)
        key = (name, tuple(sorted(props.items())))
        if key not in pidx:
            pidx[key] = len(palette)
            body = s_("Name", name)
            if props:
                body += compound("Properties", b"".join(s_(k, v) for k, v in sorted(props.items())))
            palette.append(body)
        blocks.append(ilist("pos", [x, y, z]) + i_("state", pidx[key]))
    root = th(10, "") + i_("DataVersion", 3955) + ilist("size", size) + clist("palette", palette) + clist("blocks", blocks) \
        + clist("entities", []) + b"\x00"
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with gzip.open(OUT, "wb") as f:
        f.write(root)
    print(f"mound.nbt: size {size}, {len(blocks)} blocks, {len(palette)} states")


if __name__ == "__main__":
    main()
