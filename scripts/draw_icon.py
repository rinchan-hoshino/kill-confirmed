#!/usr/bin/env python3
"""Generate the 64x64 pixel-art Kill Confirmed 2.0 badge icon (8x nearest-neighbor -> 512x512)."""
from pathlib import Path
import math
import struct
import zlib

ROOT = Path(__file__).resolve().parents[1]
SIZE = 64
SCALE = 8


def hex_rgba(h: str, a: int = 255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def blend(dst, src):
    sr, sg, sb, sa = src
    if sa == 0:
        return dst
    if sa == 255:
        return src
    dr, dg, db, da = dst
    inv = 255 - sa
    out_a = sa + (da * inv) // 255
    if out_a == 0:
        return (0, 0, 0, 0)
    r = (sr * sa + dr * da * inv // 255) // out_a
    g = (sg * sa + dg * da * inv // 255) // out_a
    b = (sb * sa + db * da * inv // 255) // out_a
    return (r, g, b, out_a)


def write_png(path: Path, width: int, height: int, rgba_bytes: bytes):
    def chunk(tag: bytes, data: bytes) -> bytes:
        return (
            struct.pack("!I", len(data))
            + tag
            + data
            + struct.pack("!I", zlib.crc32(tag + data) & 0xFFFFFFFF)
        )

    raw = bytearray()
    stride = width * 4
    for y in range(height):
        raw.append(0)
        raw.extend(rgba_bytes[y * stride : (y + 1) * stride])

    png = bytearray(b"\x89PNG\r\n\x1a\n")
    png.extend(chunk(b"IHDR", struct.pack("!IIBBBBB", width, height, 8, 6, 0, 0, 0)))
    png.extend(chunk(b"IDAT", zlib.compress(bytes(raw), 9)))
    png.extend(chunk(b"IEND", b""))
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(bytes(png))


def inside_rounded_rect(x: int, y: int, x0: int, y0: int, x1: int, y1: int, r: int) -> bool:
    if x < x0 or x > x1 or y < y0 or y > y1:
        return False
    cx = x0 + r if x < x0 + r else (x1 - r if x > x1 - r else x)
    cy = y0 + r if y < y0 + r else (y1 - r if y > y1 - r else y)
    dx = x - cx
    dy = y - cy
    return dx * dx + dy * dy <= r * r


def draw_icon():
    canvas = [[(0, 0, 0, 0) for _ in range(SIZE)] for _ in range(SIZE)]

    def put(x: int, y: int, color):
        if 0 <= x < SIZE and 0 <= y < SIZE:
            canvas[y][x] = blend(canvas[y][x], color)

    def fill_rect(x0: int, y0: int, x1: int, y1: int, color):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                put(x, y, color)

    # 1. Deep military olive-green & dark-steel rounded riveted badge
    outer_rim = hex_rgba("#141918")
    steel_hi = hex_rgba("#768783")
    steel_mid = hex_rgba("#4C5B57")
    steel_bot = hex_rgba("#2D3835")
    inner_groove = hex_rgba("#1B2421")
    bg_top = hex_rgba("#24322B")
    bg_bot = hex_rgba("#17201D")

    for y in range(SIZE):
        for x in range(SIZE):
            if not inside_rounded_rect(x, y, 2, 2, 61, 61, 11):
                continue
            if not inside_rounded_rect(x, y, 3, 3, 60, 60, 10):
                put(x, y, outer_rim)
            elif not inside_rounded_rect(x, y, 5, 5, 58, 58, 8):
                if y < 20 or x < 16:
                    put(x, y, steel_hi if (x + y) % 6 != 0 else steel_mid)
                elif y > 44 or x > 48:
                    put(x, y, steel_bot)
                else:
                    put(x, y, steel_mid)
            elif not inside_rounded_rect(x, y, 6, 6, 57, 57, 7):
                put(x, y, inner_groove)
            else:
                t = (y - 6) / 51.0
                r = int(bg_top[0] * (1 - t) + bg_bot[0] * t)
                g = int(bg_top[1] * (1 - t) + bg_bot[1] * t)
                b = int(bg_top[2] * (1 - t) + bg_bot[2] * t)
                # Subtle brushed olive-steel radial lift in center
                dx = x - 31
                dy = y - 31
                dist2 = dx * dx + dy * dy
                if dist2 < 400:
                    lift = max(0.0, 1.0 - dist2 / 400.0)
                    r = min(255, int(r + 22 * lift))
                    g = min(255, int(g + 30 * lift))
                    b = min(255, int(b + 24 * lift))
                # Subtle diagonal tactical weave
                if (x + y) % 8 == 0:
                    r = max(0, r - 3)
                    g = max(0, g - 3)
                    b = max(0, b - 3)
                put(x, y, (r, g, b, 255))

    # Corner rivets on badge plate
    for rx, ry in ((10, 10), (53, 10), (10, 53)):
        fill_rect(rx - 1, ry - 1, rx + 1, ry + 1, hex_rgba("#1A221F"))
        put(rx, ry, hex_rgba("#7D8E88"))
        put(rx - 1, ry - 1, hex_rgba("#A8BCB5"))
        put(rx + 1, ry + 1, hex_rgba("#313D39"))

    # Helper to draw an angled rounded dog tag plate
    def draw_dog_tag(cx: float, cy: float, angle_deg: float, half_w: float, half_h: float,
                     palette: dict, draw_lines: bool = True, draw_notch: bool = True):
        rad = math.radians(angle_deg)
        cos_a = math.cos(rad)
        sin_a = math.sin(rad)

        # Drop shadow first
        for y in range(6, 58):
            for x in range(6, 58):
                dx = (x - (cx + 2.0))
                dy = (y - (cy + 2.5))
                u = dx * cos_a + dy * sin_a
                v = -dx * sin_a + dy * cos_a
                if abs(u) <= half_w + 1.0 and abs(v) <= half_h + 1.0:
                    # Rounded corners in local space
                    ru = max(0.0, abs(u) - (half_w - 3.5))
                    rv = max(0.0, abs(v) - (half_h - 3.5))
                    if ru * ru + rv * rv <= 4.3 * 4.3:
                        put(x, y, hex_rgba("#0D1311", 125))

        for y in range(6, 58):
            for x in range(6, 58):
                dx = x - cx
                dy = y - cy
                u = dx * cos_a + dy * sin_a
                v = -dx * sin_a + dy * cos_a

                ru = max(0.0, abs(u) - (half_w - 3.5))
                rv = max(0.0, abs(v) - (half_h - 3.5))
                dist_corner = math.hypot(ru, rv)
                if abs(u) > half_w + 1.2 or abs(v) > half_h + 1.2 or dist_corner > 4.6:
                    continue

                # Side military notch at bottom-right edge of tag
                if draw_notch and u > half_w - 2.3 and abs(v - 4.0) < 1.6:
                    continue

                # Chain eyelet hole near top-center (v around -half_h + 3.8)
                hole_dist = math.hypot(u, v - (-half_h + 4.0))
                if hole_dist < 1.45:
                    continue
                if hole_dist < 2.4:
                    put(x, y, palette["outline"])
                    continue

                # Outer dark steel outline
                is_edge = (abs(u) > half_w - 0.1 or abs(v) > half_h - 0.1 or dist_corner > 3.5)
                if is_edge:
                    put(x, y, palette["outline"])
                    continue

                # Inner bevel rim
                is_bevel = (abs(u) > half_w - 1.5 or abs(v) > half_h - 1.5 or dist_corner > 2.2)
                if is_bevel:
                    if u + v < 0:
                        put(x, y, palette["hi"])
                    else:
                        put(x, y, palette["dark"])
                    continue

                # Brushed metallic sheen (diagonal specular band)
                sheen = u * 0.65 + v * 0.35
                if -4.0 <= sheen <= -0.5:
                    base = palette["hi"]
                elif -7.0 <= sheen <= 3.5:
                    base = palette["light"]
                elif sheen > 7.0:
                    base = palette["dark"]
                else:
                    base = palette["mid"]

                # Subtle horizontal brushed grain
                if int(round(v)) % 3 == 0 and base == palette["mid"]:
                    base = palette["light"]

                put(x, y, base)

                # Embossed dog tag text lines
                if draw_lines and -half_w + 3.2 <= u <= half_w - 3.2:
                    if -3.0 <= v <= -1.9 and u <= half_w - 4.2:
                        put(x, y, palette["engrave"])
                    elif 0.5 <= v <= 1.6 and u <= half_w - 5.8:
                        put(x, y, palette["engrave"])
                    elif 4.0 <= v <= 5.1 and u <= half_w - 7.5:
                        put(x, y, palette["engrave"])

    # 2. Rear dog tag (darker gunmetal steel, tilted slightly right)
    rear_palette = {
        "outline": hex_rgba("#1A2024"),
        "dark": hex_rgba("#4C565E"),
        "mid": hex_rgba("#6E7B85"),
        "light": hex_rgba("#8D9BA6"),
        "hi": hex_rgba("#B1BFC9"),
        "engrave": hex_rgba("#384148"),
    }
    draw_dog_tag(37.5, 34.5, -14.0, 9.5, 14.5, rear_palette, draw_lines=True, draw_notch=False)

    # 3. Front dog tag (bright brushed silver-steel, tilted diagonally left-to-right)
    front_palette = {
        "outline": hex_rgba("#1C2329"),
        "dark": hex_rgba("#7C8A96"),
        "mid": hex_rgba("#AEB9C4"),
        "light": hex_rgba("#CFD9E2"),
        "hi": hex_rgba("#F1F6FA"),
        "engrave": hex_rgba("#52606B"),
    }
    draw_dog_tag(27.5, 33.0, -26.0, 10.0, 15.0, front_palette, draw_lines=True, draw_notch=True)

    # 4. Metallic bead chain draping cleanly from upper-left to both tag holes
    chain_beads = [
        # Upper arc draping to rear tag eyelet
        (11, 14), (13, 12), (16, 11), (19, 11), (22, 11), (25, 12),
        (28, 13), (31, 15), (34, 17), (36, 19), (37, 22),
        # Lower strand dropping to front tag eyelet
        (13, 16), (15, 18), (18, 20), (20, 22), (22, 23),
    ]
    bead_outline = hex_rgba("#1A2126")
    bead_dark = hex_rgba("#6C7A85")
    bead_mid = hex_rgba("#B4C2CC")
    bead_hi = hex_rgba("#F4F8FB")

    for bx, by in chain_beads:
        for dy in (-1, 0, 1):
            for dx in (-1, 0, 1):
                if abs(dx) + abs(dy) <= 1:
                    put(bx + dx, by + dy, bead_outline)
    for bx, by in chain_beads:
        put(bx, by, bead_mid)
        put(bx - 1, by, bead_dark)
        put(bx, by - 1, bead_hi)

    # 5. Bottom-right small warm brass clasp / latch emblem
    # Positioned around (46..56, 46..56) inside the badge
    brass_outline = hex_rgba("#2B1C0E")
    brass_dark = hex_rgba("#8A5D24")
    brass_mid = hex_rgba("#C8923B")
    brass_hi = hex_rgba("#F4D27A")

    # Clasp shadow
    fill_rect(47, 48, 56, 56, hex_rgba("#0E1412", 160))
    # Clasp body (compact rounded rectangular brass plate + keyhole/stud)
    for y in range(46, 56):
        for x in range(46, 56):
            if (x in (46, 55) and y in (46, 55)):
                continue
            if x in (46, 55) or y in (46, 55):
                put(x, y, brass_outline)
            elif y == 47 or x == 47:
                put(x, y, brass_hi)
            elif y == 54 or x == 54:
                put(x, y, brass_dark)
            else:
                put(x, y, brass_mid)
    # Clasp horizontal latch groove & bright center stud
    for x in range(48, 54):
        put(x, 51, brass_dark)
    fill_rect(50, 49, 51, 50, brass_hi)
    put(50, 52, brass_outline)
    put(51, 52, brass_outline)

    # Nearest-neighbor 8x upscale -> 512x512
    out_w = SIZE * SCALE
    out_h = SIZE * SCALE
    buf = bytearray(out_w * out_h * 4)
    for y in range(SIZE):
        row = canvas[y]
        for x in range(SIZE):
            r, g, b, a = row[x]
            for sy in range(SCALE):
                base = ((y * SCALE + sy) * out_w + x * SCALE) * 4
                for sx in range(SCALE):
                    idx = base + sx * 4
                    buf[idx] = r
                    buf[idx + 1] = g
                    buf[idx + 2] = b
                    buf[idx + 3] = a

    data = bytes(buf)
    write_png(ROOT / "common/src/main/resources/icon.png", out_w, out_h, data)
    write_png(ROOT / "docs/icon.png", out_w, out_h, data)
    print("Generated 512x512 Kill Confirmed icons from 64x64 pixel art.")


if __name__ == "__main__":
    draw_icon()
