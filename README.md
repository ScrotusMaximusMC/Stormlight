# Stormlight

## Setup

For setup instructions, please see the [Fabric Documentation page](https://docs.fabricmc.net/develop/getting-started/creating-a-project#setting-up) related to the IDE that you are using.

## Gemcutting Table

Place the table with room for two adjacent blocks. Right-click either half while holding a diamond,
garnet, ruby, sapphire, or emerald to load one gem into the vise. Wait for the vise to close, then
right-click with a chisel ten times. Cracks appear as you work; the tenth strike releases matching raw chips or raw marks onto the bench. Each strike uses one of the chisel's 128 durability points. The gem and its
progress are saved when the world closes. Sneak-right-click with an empty hand to retrieve a gem.

Craft a chisel with an iron ingot diagonally above-right of a stick. Cutting recipes use
the table instead of the crafting grid. Gemstone nuggets have been replaced by raw chips and marks.
Old nugget item IDs are removed; existing nuggets in saves will no longer load. Break and re-place single-block tables from older saves to
get both halves. The continuous bench has four outer legs and an animated vise, with no screen.

## Sphere Forging Table

A 3 wide, 2 deep, 2 tall glassworks: steps on one side, a brick hearth with an open crucible in the
middle, and a casting bench under a tap on the other side. Place it with that space clear. Fuel the
hearth by right-clicking with a lava bucket (or any furnace fuel). Climb the steps and throw glass
blocks into the crucible, or right-click with glass; each block melts into one unit of molten glass
while the fire burns. Set a mould on the casting bench, then right-click with a raw chip, raw mark or raw broam to set it in the mould, and right-click the casting bench with an empty hand to turn the tap. The glass pours over
the cut gem and the sphere cools in the mould before you can take it. Each size needs its own mould:

| Gem | Mould | Sphere | Glass |
|---|---|---|---|
| Raw chip | Chip Mould (`C C` / ` I `) | Chip | 1 |
| Raw mark | Mark Mould (`C C` / `CIC`) | Mark | 2 |
| Raw broam | Broam Mould (`C C` / `CIC` / `CCC`) | Broam | 3 |

Moulds are crafted from clay balls (`C`) and an iron ingot (`I`) and stay on the bench between
pours. Sneak-right-click with an empty hand to take the gem back, then again for the mould.
Craft the forge from iron ingots, a cauldron, a furnace and bricks (`ICI` / `BFB` / `BBB`).

Run `./gradlew build` for the server game tests and `./gradlew runClientGameTest` for the rendering and
interaction check. Client test screenshots are written to `build/run/clientGameTest/screenshots`.

## Gem Cutting progression

The player landing the final strike earns one completed cut. Creative cuts produce items but do not
award progress. Rank and practice persist through logout and death. Sneak-right-click an empty
cutting table with an empty hand to see your rank, progress and odds. There are ten ranks;
Ranks 2–8 unlock every ten cuts, followed by fifteen cuts each for ranks 9 and 10.
Existing saved practice counts are retained.

| Rank | Total cuts | 1–3 raw chips | 1 raw mark | 2 raw marks |
| --- | --- | --- | --- | --- |
| 1 | 0 | 95% | 5% | 0% |
| 2 | 10 | 90% | 10% | 0% |
| 3 | 20 | 85% | 13% | 2% |
| 4 | 30 | 80% | 16% | 4% |
| 5 | 40 | 75% | 19% | 6% |
| 6 | 50 | 70% | 22% | 8% |
| 7 | 60 | 65% | 25% | 10% |
| 8 | 70 | 60% | 28% | 12% |
| 9 | 85 | 55% | 31% | 14% |
| 10 | 100 | 50% | 35% | 15% |

Chip outcomes split evenly between one, two and three. Raw gems stack to 64 and hold no Stormlight;
forge them to create spheres. At level 5, large gemstones can be cut into raw broams on the gemcutting table using a diamond
chisel (crafted like the iron chisel, with a diamond in place of iron; 1,561 durability). Each cut
takes ten strikes and awards one completed cut. The diamond chisel also cuts ordinary gemstones.
Large gems cannot be forged directly; use a raw broam with a broam mould and three glass.

| Level | One raw broam | Two raw broams | Three raw broams |
| --- | --- | --- | --- |
| 5 | 95% | 5% | 0% |
| 6 | 86% | 12% | 2% |
| 7 | 77% | 19% | 4% |
| 8 | 68% | 26% | 6% |
| 9 | 59% | 33% | 8% |
| 10 | 50% | 40% | 10% |

## The Way of Kings

Craft a book with lapis lazuli to make The Way of Kings. Put it on a lectern and open it to read the
Field Guide contents page with clickable chapters for Stormlight, Crafting Spheres, Highstorms,
Storage & Lighting, Radiants & Lashings, and Spren & Ore Seeking. Each chapter returns to Contents
and supports scrolling when needed. Separate tabs retain your current Radiant order and next XP
milestone, and a visual Gem Cutting tree with
all ten levels, cut requirements, and chip, mark, and broam odds on one Gem Cutting tree.
The same page shows the level 5 broam unlock and diamond chisel requirement. Scroll or use the
arrow buttons to explore the tree on smaller screens. The displayed progress belongs to the reader,
so two players using the same lectern see their own levels. Use **Take Book** to retrieve it.

## Sphere wear visuals

Sphere items use a shared crack overlay over their existing gemstone and charge model. Cracks
appear at 1% wear, branch at 40%, and become heavy at 75%. Three shared models in
`assets/stormlight/models/item/sphere_cracks_*.json` draw the cracks as small faces using the
vanilla gray concrete texture, so no gemstone, denomination, or charge-specific sprites are needed.
The overlay reads normalized item damage, including wear already saved on existing spheres.

## UI debug commands

Enable cheats in singleplayer or use an operator account. Commands affect the player running them.

| Command | Use |
| --- | --- |
| `/stormlightdebug guide` | Open the Way of Kings using your current progress, without a lectern. |
| `/stormlightdebug guide 5 3` | Preview Gem Cutting level 5 and Radiant level 3; saved progression stays unchanged. Use Radiant level 0 to preview no order. |
| `/stormlightdebug spheres broam 100` | Get twenty broams: every gemstone at pristine, light, moderate and heavy wear, fully charged. |
| `/stormlightdebug spheres chip 0` | Get the same comparison set of dun chips. `mark` also works; charge accepts 0–100 and defaults to 100. |
| `/stormlightdebug wear 95` | Set the main-hand sphere to 95% wear without changing charge. Accepts 0–99; 0 restores pristine health. |
| `/stormlightdebug gemlevel 5` | Set your saved Gem Cutting rank to 5 so you can test the broam unlock. Accepts 1–10 and resets completed cuts to that rank's threshold. |
| `/stormlightdebug books` | Get The Way of Kings, a Mysterious Book and Words of Radiance for lectern/ritual checks. |

For the complete crack comparison, run the sphere command with charge values 0, 25, 50, 75 and 100.
Sample items that do not fit in your inventory drop at your feet. `/stormlightdebug` lists the shortcuts.
