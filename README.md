# Simple Logistic

**A TPS-friendly proportional pipe network mod for Minecraft 1.21.1 (NeoForge)**

Simple Logistic adds a lightweight but powerful logistics system to Minecraft. Transport items, fluids, and energy through a clean pipe network with proportional distribution, priority routing, and cross-dimensional support — all without killing your server's TPS.

---

## ✨ Features

### 🔧 4 Pipe Types
| Pipe | Transports | Recipe |
|------|-----------|--------|
| **Item Pipe** | Items | 2× Iron Ingot + Glass → 16× |
| **Fluid Pipe** | Fluids | 2× Copper Ingot + Glass → 16× |
| **Energy Pipe** | Forge Energy (FE) | 2× Copper Ingot + Redstone → 16× |
| **Universal Pipe** | All three | Item + Fluid + Energy Pipe + Redstone → 4× |

### ⚡ Smart Distribution
- **Proportional Routing** — Items are distributed evenly across all connected outputs, not just dumped into the first one
- **Priority System** — Set priorities (-∞ to +∞) on each connection to control which machine gets items first
- **Multi-Operation** — Up to 3 independent operations per pipe connection (e.g., INPUT items AND OUTPUT fluids on the same pipe)

### 🎛️ Intuitive 4-Column GUI
No more guessing compass directions! The configuration GUI shows:
1. **Connected Machines** — Displays actual block names (e.g., "Chest", "Furnace") with relative side info ("Front", "Back", "Left")
2. **Actions** — List of operations for the selected machine
3. **Configuration** — Mode, target side, redstone control, priority
4. **Filter** — Whitelist/blacklist, NBT matching, ghost item slots, tag & mod filters

### 🔍 Advanced Filtering
- **Whitelist / Blacklist** — Allow or block specific items
- **Ghost Item Slots** — Drag items into 9 filter slots (items are not consumed)
- **Tag Filters** — Filter by item tags (e.g., `#minecraft:logs`, `#c:ores`)
- **Mod Filters** — Filter by mod ID (e.g., `@create`)
- **NBT Matching** — Optionally match item components/NBT data

### 🌍 Cross-Dimensional Transport
- **Dimensional Node** — Link pipe networks across dimensions via named channels
- **Chunk Loading** — Dimensional Nodes automatically keep their chunks loaded
- **Channel System** — Name your channels by right-clicking with a renamed item (e.g., a paper named "Nether_Link")

### 🔴 Redstone Control
Each operation supports 4 redstone modes:
- **Always Active** — Ignores redstone
- **High** — Only active with redstone signal
- **Low** — Only active without redstone signal
- **Pulse** — Activates on redstone pulse

### 🔀 Side Spoofing
Access specific sides of machines using relative names:
- **Front / Back / Left / Right / Top / Bottom**
- Perfect for machines that accept different items on different faces (furnaces, modded machines)

### 🔧 Pipe Wrench
- **Sneak + Right-Click** to remove pipes instantly
- Crafted with 3 Iron Ingots + 1 Copper Ingot

---

## 📦 Crafting Recipes

### Item Pipe (×16)
```
[ Iron Ingot | Glass | Iron Ingot ]
```

### Fluid Pipe (×16)
```
[ Copper Ingot | Glass | Copper Ingot ]
```

### Energy Pipe (×16)
```
[ Copper Ingot | Redstone | Copper Ingot ]
```

### Universal Pipe (×4, Shapeless)
```
Item Pipe + Fluid Pipe + Energy Pipe + Redstone
```

### Dimensional Node (×2)
```
[ Obsidian      | Eye of Ender   | Obsidian      ]
[ Universal Pipe| Diamond        | Universal Pipe]
[ Obsidian      | Eye of Ender   | Obsidian      ]
```

### Pipe Wrench (×1)
```
[              | Iron Ingot |              ]
[              | Copper     | Iron Ingot   ]
[ Iron Ingot   |            |              ]
```

---

## 🚀 Getting Started

1. **Craft pipes** matching your transport needs (Item, Fluid, Energy, or Universal)
2. **Place pipes** between machines — they auto-connect to adjacent inventories
3. **Right-click a pipe** to open the configuration GUI
4. **Select a machine** from the left column
5. **Set the mode** to INPUT (extract from machine) or OUTPUT (insert into machine)
6. **Add filters** if needed — switch to the Filter column

### Example Setup: Auto-Smelting
```
[Chest A] ←── Pipe (INPUT) ──── Pipe (OUTPUT) ──→ [Furnace]
                                Pipe (INPUT)  ──→ [Furnace]
                                Pipe (OUTPUT) ──→ [Chest B]
```
1. Pipe next to Chest A: Set to **INPUT** (extracts raw ore)
2. Pipe next to Furnace: Set operation 1 to **OUTPUT** (inserts ore), operation 2 to **INPUT** (extracts smelted items)
3. Pipe next to Chest B: Set to **OUTPUT** (receives smelted items)

---

## 🎮 Compatibility

Simple Logistic is designed to work alongside popular tech mods:
- ✅ **GregTech** — Side-specific I/O via Side Spoofing
- ✅ **Create** — Compatible with Create's inventories
- ✅ **Applied Energistics 2** — Connect to ME interfaces
- ✅ **Ender IO** — Works with all Ender IO machines
- ✅ Any mod that uses **Forge Capabilities** (IItemHandler, IFluidHandler, IEnergyStorage)

---

## ⚙️ Technical Details

- **Tick Rate**: Networks tick every 10 game ticks (0.5 seconds)
- **Void Protection**: 6-phase simulate-first transfer system — items are never lost
- **Memory Efficient**: Uses ResourceKeys instead of level references to prevent memory leaks
- **Exploit Protected**: All network packets are validated with bounds checking
- **Proportional Distribution**: Fair distribution across all outputs, not first-come-first-served

---

## 🌐 Languages

- 🇬🇧 English
- 🇩🇪 Deutsch (German)

---

## 📋 Requirements

- **Minecraft**: 1.21.1
- **NeoForge**: 21.1.48+
- **Java**: 21+

---

## 📄 License

MIT License — Feel free to include this mod in any modpack!

---

## 🐛 Found a Bug?

Please report issues on our [GitHub Issues](https://github.com/GeneraBlack/SimpleLogistic/issues) page.
