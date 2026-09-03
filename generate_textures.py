import struct
import zlib
import os

def create_png(matrix, filepath):
    """
    matrix: 16x16 or 256x256 list of rows, each containing (R, G, B) or (R, G, B, A) tuples.
    """
    height = len(matrix)
    width = len(matrix[0])
    has_alpha = len(matrix[0][0]) == 4
    color_type = 6 if has_alpha else 2
    pixel_size = 4 if has_alpha else 3
    
    raw_data = b''
    for row in matrix:
        raw_data += b'\x00' # filter type None
        for px in row:
            if has_alpha:
                raw_data += struct.pack('4B', px[0], px[1], px[2], px[3])
            else:
                raw_data += struct.pack('3B', px[0], px[1], px[2])
                
    compress = zlib.compressobj()
    compressed = compress.compress(raw_data) + compress.flush()
    
    def chunk(tag, data):
        return struct.pack('>I', len(data)) + tag + data + struct.pack('>I', zlib.crc32(tag + data) & 0xffffffff)
        
    png = b'\x89PNG\r\n\x1a\n'
    png += chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, color_type, 0, 0, 0))
    png += chunk(b'IDAT', compressed)
    png += chunk(b'IEND', b'')
    
    os.makedirs(os.path.dirname(filepath), exist_ok=True)
    with open(filepath, 'wb') as f:
        f.write(png)

# ==========================================
# 1. ITEM PIPE (Steel & Brass Pneumatic Theme)
# ==========================================
def generate_item_pipe():
    # Palette
    D_STEEL = (38, 42, 48)     # Shadow frame
    M_STEEL = (68, 74, 84)     # Main steel plate
    L_STEEL = (110, 120, 135)  # Steel highlight
    H_STEEL = (160, 175, 195)  # Specular edge
    RIVET_H = (200, 210, 225)  # Rivet highlight
    RIVET_D = (25, 28, 32)     # Rivet shadow
    
    BRASS_D = (110, 75, 25)
    BRASS_M = (180, 135, 45)
    BRASS_L = (230, 190, 75)
    
    TUBE_BG = (24, 26, 30)     # Dark interior
    GLASS_H = (190, 220, 240)  # Glass glint
    GLASS_M = (85, 115, 135)
    
    m = [[M_STEEL for _ in range(16)] for _ in range(16)]
    
    # Outer frame & beveling
    for y in range(16):
        for x in range(16):
            # Outer border shading (Top/Left light, Bottom/Right dark)
            if y == 0 or x == 0:
                m[y][x] = L_STEEL
            elif y == 15 or x == 15:
                m[y][x] = D_STEEL
            elif (x in [1, 2, 3, 12, 13, 14]) and (y in [1, 2, 3, 12, 13, 14]):
                m[y][x] = M_STEEL
                
    # Corner Rivets
    corners = [(2,2), (2,13), (13,2), (13,13)]
    for cx, cy in corners:
        m[cy][cx] = RIVET_H
        m[cy+1][cx] = RIVET_D
        m[cy][cx+1] = RIVET_D
        
    # Vertical and horizontal pipe channel grooves
    for i in range(16):
        # Vertical arm channel (x: 5..10)
        m[i][5] = D_STEEL
        m[i][6] = BRASS_D
        m[i][7] = BRASS_M
        m[i][8] = BRASS_L
        m[i][9] = BRASS_M
        m[i][10] = D_STEEL
        
        # Horizontal arm channel (y: 5..10)
        m[5][i] = D_STEEL
        m[6][i] = BRASS_D
        m[7][i] = BRASS_M
        m[8][i] = BRASS_L
        m[9][i] = BRASS_M
        m[10][i] = D_STEEL
        
    # Ribbed connectors on arms
    for y in [1, 3, 12, 14]:
        for x in range(6, 10):
            m[y][x] = H_STEEL if y in [1, 12] else D_STEEL
    for x in [1, 3, 12, 14]:
        for y in range(6, 10):
            m[y][x] = H_STEEL if x in [1, 12] else D_STEEL
            
    # Center Chamber (x: 6..9, y: 6..9) - Pneumatic Glass Window
    for y in range(6, 10):
        for x in range(6, 10):
            m[y][x] = TUBE_BG
            
    # Brass chamber ring
    m[5][6] = BRASS_L; m[5][7] = BRASS_L; m[5][8] = BRASS_L; m[5][9] = BRASS_M
    m[10][6] = BRASS_D; m[10][7] = BRASS_D; m[10][8] = BRASS_D; m[10][9] = BRASS_D
    m[6][5] = BRASS_L; m[7][5] = BRASS_L; m[8][5] = BRASS_D; m[9][5] = BRASS_D
    m[6][10] = BRASS_M; m[7][10] = BRASS_M; m[8][10] = BRASS_D; m[9][10] = BRASS_D
    
    # Glass glint in center
    m[6][6] = GLASS_H
    m[7][6] = GLASS_M
    m[6][7] = GLASS_M
    m[8][8] = (45, 160, 220) # Sensor LED dot
    
    return m

# ==========================================
# 2. FLUID PIPE (Copper, Glass & Liquid Blue)
# ==========================================
def generate_fluid_pipe():
    # Palette
    D_COPPER = (60, 32, 22)
    M_COPPER = (135, 70, 42)
    L_COPPER = (185, 105, 65)
    H_COPPER = (225, 145, 95)
    VERDIGRIS = (45, 120, 105) # Oxidized patina
    
    GLASS_BORDER = (35, 65, 85)
    WATER_DEEP = (18, 65, 175)
    WATER_MID = (35, 115, 235)
    WATER_LIGHT = (95, 185, 255)
    WATER_SPECULAR = (220, 245, 255)
    
    m = [[M_COPPER for _ in range(16)] for _ in range(16)]
    
    # Outer frame
    for y in range(16):
        for x in range(16):
            if y == 0 or x == 0:
                m[y][x] = L_COPPER
            elif y == 15 or x == 15:
                m[y][x] = D_COPPER
                
    # Patina in corners
    m[1][1] = H_COPPER; m[2][2] = VERDIGRIS; m[1][14] = VERDIGRIS
    m[14][1] = VERDIGRIS; m[14][14] = D_COPPER
    
    # Fluid Pipe central cross
    for i in range(16):
        m[i][5] = D_COPPER; m[i][6] = GLASS_BORDER; m[i][7] = WATER_MID; m[i][8] = WATER_LIGHT; m[i][9] = WATER_DEEP; m[i][10] = D_COPPER
        m[5][i] = D_COPPER; m[6][i] = GLASS_BORDER; m[7][i] = WATER_MID; m[8][i] = WATER_LIGHT; m[9][i] = WATER_DEEP; m[10][i] = D_COPPER
        
    # Reinforced Copper rings / Flanges
    for y in [2, 13]:
        for x in range(5, 11):
            m[y][x] = H_COPPER if y == 2 else D_COPPER
    for x in [2, 13]:
        for y in range(5, 11):
            m[y][x] = H_COPPER if x == 2 else D_COPPER
            
    # Central Glass Liquid Viewing Dome (x: 6..9, y: 6..9)
    m[6][6] = WATER_SPECULAR
    m[6][7] = WATER_LIGHT
    m[6][8] = WATER_LIGHT
    m[6][9] = WATER_MID
    
    m[7][6] = WATER_LIGHT
    m[7][7] = WATER_MID
    m[7][8] = WATER_MID
    m[7][9] = WATER_DEEP
    
    m[8][6] = WATER_MID
    m[8][7] = WATER_DEEP
    m[8][8] = WATER_SPECULAR # Bubble!
    m[8][9] = WATER_DEEP
    
    m[9][6] = WATER_DEEP
    m[9][7] = WATER_DEEP
    m[9][8] = WATER_DEEP
    m[9][9] = (10, 40, 110)
    
    return m

# ==========================================
# 3. ENERGY PIPE (Obsidian, Gold & Plasma Core)
# ==========================================
def generate_energy_pipe():
    # Palette
    OBS_DARK = (20, 18, 26)
    OBS_MID = (38, 34, 48)
    OBS_LIGHT = (65, 58, 80)
    GOLD_D = (140, 95, 20)
    GOLD_M = (215, 165, 40)
    GOLD_L = (255, 225, 100)
    
    PLASMA_RED = (180, 25, 15)
    PLASMA_ORANGE = (255, 90, 20)
    PLASMA_YELLOW = (255, 200, 45)
    PLASMA_WHITE = (255, 255, 210)
    
    m = [[OBS_MID for _ in range(16)] for _ in range(16)]
    
    for y in range(16):
        for x in range(16):
            if y == 0 or x == 0:
                m[y][x] = OBS_LIGHT
            elif y == 15 or x == 15:
                m[y][x] = OBS_DARK
                
    # Gold corner inlays
    for (cx, cy) in [(1,1), (1,14), (14,1), (14,14)]:
        m[cy][cx] = GOLD_L
        m[cy][cx+1 if cx==1 else cx-1] = GOLD_M
        m[cy+1 if cy==1 else cy-1][cx] = GOLD_D
        
    # Energy Conduits
    for i in range(16):
        m[i][5] = OBS_DARK; m[i][6] = GOLD_D; m[i][7] = PLASMA_ORANGE; m[i][8] = PLASMA_YELLOW; m[i][9] = GOLD_D; m[i][10] = OBS_DARK
        m[5][i] = OBS_DARK; m[6][i] = GOLD_D; m[7][i] = PLASMA_ORANGE; m[8][i] = PLASMA_YELLOW; m[9][i] = GOLD_D; m[10][i] = OBS_DARK
        
    # Heat sink fins
    for y in [2, 13]:
        for x in range(6, 10):
            m[y][x] = GOLD_L if y == 2 else GOLD_D
    for x in [2, 13]:
        for y in range(6, 10):
            m[y][x] = GOLD_L if x == 2 else GOLD_D
            
    # Fusion Energy Core (x: 6..9, y: 6..9)
    for y in range(6, 10):
        for x in range(6, 10):
            m[y][x] = PLASMA_ORANGE
            
    m[6][6] = PLASMA_YELLOW; m[6][7] = PLASMA_WHITE; m[6][8] = PLASMA_YELLOW; m[6][9] = PLASMA_ORANGE
    m[7][6] = PLASMA_WHITE;  m[7][7] = (255, 255, 255); m[7][8] = PLASMA_WHITE;  m[7][9] = PLASMA_YELLOW
    m[8][6] = PLASMA_YELLOW; m[8][7] = PLASMA_WHITE; m[8][8] = PLASMA_YELLOW; m[8][9] = PLASMA_ORANGE
    m[9][6] = PLASMA_ORANGE; m[9][7] = PLASMA_YELLOW; m[9][8] = PLASMA_ORANGE; m[9][9] = PLASMA_RED
    
    return m

# ==========================================
# 4. UNIVERSAL PIPE (Quantum Dark Metal & Prismatic Matrix)
# ==========================================
def generate_universal_pipe():
    # Palette
    VOID_D = (16, 14, 24)
    VOID_M = (32, 28, 46)
    VOID_L = (58, 52, 82)
    IRID_CYAN = (40, 230, 240)
    IRID_PURPLE = (180, 50, 240)
    IRID_MAGENTA = (245, 60, 180)
    IRID_WHITE = (240, 250, 255)
    
    m = [[VOID_M for _ in range(16)] for _ in range(16)]
    
    for y in range(16):
        for x in range(16):
            if y == 0 or x == 0:
                m[y][x] = VOID_L
            elif y == 15 or x == 15:
                m[y][x] = VOID_D
                
    # Prismatic corners
    m[1][1] = IRID_CYAN; m[1][14] = IRID_PURPLE
    m[14][1] = IRID_MAGENTA; m[14][14] = IRID_CYAN
    
    # Conduits
    for i in range(16):
        m[i][5] = VOID_D; m[i][6] = IRID_PURPLE; m[i][7] = IRID_CYAN; m[i][8] = IRID_MAGENTA; m[i][9] = IRID_PURPLE; m[i][10] = VOID_D
        m[5][i] = VOID_D; m[6][i] = IRID_PURPLE; m[7][i] = IRID_CYAN; m[8][i] = IRID_MAGENTA; m[9][i] = IRID_PURPLE; m[10][i] = VOID_D
        
    # Quantum Containment Rings
    for y in [2, 13]:
        for x in range(6, 10):
            m[y][x] = IRID_CYAN if y == 2 else IRID_MAGENTA
    for x in [2, 13]:
        for y in range(6, 10):
            m[y][x] = IRID_CYAN if x == 2 else IRID_MAGENTA
            
    # Singularity Quantum Core
    m[6][6] = IRID_CYAN;    m[6][7] = IRID_WHITE;   m[6][8] = IRID_MAGENTA; m[6][9] = IRID_PURPLE
    m[7][6] = IRID_WHITE;   m[7][7] = (255,255,255);m[7][8] = IRID_WHITE;   m[7][9] = IRID_MAGENTA
    m[8][6] = IRID_MAGENTA; m[8][7] = IRID_WHITE;   m[8][8] = IRID_CYAN;    m[8][9] = IRID_PURPLE
    m[9][6] = IRID_PURPLE;  m[9][7] = IRID_MAGENTA; m[9][8] = IRID_PURPLE;  m[9][9] = (80, 20, 110)
    
    return m

# ==========================================
# 5. CUSTOM GUI TEXTURE (256x256 Full Container Background)
# ==========================================
def generate_gui_texture():
    W, H = 256, 256
    BG = (198, 198, 198)
    BEVEL_L = (255, 255, 255)
    BEVEL_D = (85, 85, 85)
    PANEL_BG = (140, 140, 140)
    SLOT_DARK = (55, 55, 55)
    SLOT_LIGHT = (255, 255, 255)
    SLOT_BG = (139, 139, 139)
    
    m = [[(0, 0, 0, 0) for _ in range(W)] for _ in range(H)]
    
    # 176x166 window
    gw, gh = 176, 166
    for y in range(gh):
        for x in range(gw):
            if x == 0 or y == 0:
                m[y][x] = (*BEVEL_L, 255)
            elif x == gw-1 or y == gh-1:
                m[y][x] = (*BEVEL_D, 255)
            elif x == 1 or y == 1:
                m[y][x] = (*BEVEL_L, 255)
            elif x == gw-2 or y == gh-2:
                m[y][x] = (*BEVEL_D, 255)
            else:
                m[y][x] = (*BG, 255)
                
    def draw_sunken_box(bx, by, bw, bh):
        for y in range(by, by + bh):
            for x in range(bx, bx + bw):
                if x == bx or y == by:
                    m[y][x] = (*BEVEL_D, 255)
                elif x == bx + bw - 1 or y == by + bh - 1:
                    m[y][x] = (*BEVEL_L, 255)
                else:
                    m[y][x] = (*SLOT_BG, 255)
                    
    def draw_slot(sx, sy):
        for y in range(sy, sy + 18):
            for x in range(sx, sx + 18):
                if x == sx or y == sy:
                    m[y][x] = (*SLOT_DARK, 255)
                elif x == sx + 17 or y == sy + 17:
                    m[y][x] = (*SLOT_LIGHT, 255)
                else:
                    m[y][x] = (*SLOT_BG, 255)
                    
    # Left Operation Panel (List box)
    draw_sunken_box(8, 18, 72, 60)
    
    # Right Config Panel
    draw_sunken_box(84, 18, 84, 60)
    
    # Player Inventory Slots (3 rows of 9)
    inv_start_x = 8
    inv_start_y = 84
    for row in range(3):
        for col in range(9):
            draw_slot(inv_start_x + col * 18, inv_start_y + row * 18)
            
    # Hotbar Slots (1 row of 9)
    hotbar_y = 142
    for col in range(9):
        draw_slot(inv_start_x + col * 18, hotbar_y)
        
    return m

if __name__ == '__main__':
    base_dir = r'd:\projekt\SimpleLogistic\src\main\resources\assets\simplelogistic\textures\block'
    gui_dir = r'd:\projekt\SimpleLogistic\src\main\resources\assets\simplelogistic\textures\gui\container'
    
    create_png(generate_item_pipe(), os.path.join(base_dir, 'item_pipe.png'))
    create_png(generate_fluid_pipe(), os.path.join(base_dir, 'fluid_pipe.png'))
    create_png(generate_energy_pipe(), os.path.join(base_dir, 'energy_pipe.png'))
    create_png(generate_universal_pipe(), os.path.join(base_dir, 'universal_pipe.png'))
    create_png(generate_gui_texture(), os.path.join(gui_dir, 'pipe_gui.png'))
    
    print('All textures and GUI graphics generated with professional shading!')
