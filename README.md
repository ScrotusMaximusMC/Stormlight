# Stormlight

## Setup

For setup instructions, please see the [Fabric Documentation page](https://docs.fabricmc.net/develop/getting-started/creating-a-project#setting-up) related to the IDE that you are using.

## Gemcutting Table

Place the table with room for two adjacent blocks. Right-click either half while holding a diamond,
garnet, ruby, sapphire, or emerald to load one gem into the vise. Wait for the vise to close, then
right-click with a chisel ten times. Cracks appear as you work; the tenth strike releases nine matching
nuggets onto the bench. Each strike uses one of the chisel's 128 durability points. The gem and its
progress are saved when the world closes. Sneak-right-click with an empty hand to retrieve a gem.

Craft a chisel with an iron ingot diagonally above-right of a stick. Gem-to-nugget recipes use
the table instead of the crafting grid. Break and re-place single-block tables from older saves to
get both halves. The continuous bench has four outer legs and an animated vise, with no screen.

## Sphere Forging Table

A 3 wide, 2 deep, 2 tall glassworks: steps on one side, a brick hearth with an open crucible in the
middle, and a casting bench under a tap on the other side. Place it with that space clear. Fuel the
hearth by right-clicking with a lava bucket (or any furnace fuel). Climb the steps and throw glass
blocks into the crucible, or right-click with glass; each block melts into one unit of molten glass
while the fire burns. Right-click with a gem nugget, gem or large gem to set it in the mould, then
right-click the casting bench with an empty hand to turn the tap. The glass pours over the gem and
the sphere cools in the mould before you can take it: a nugget makes a chip (1 glass), a gem a mark
(2 glass) and a large gem a broam (3 glass). Sneak-right-click with an empty hand to take a gem back.
Craft it from iron ingots, a cauldron, a furnace and bricks (`ICI` / `BFB` / `BBB`).

Run `./gradlew build` for the server game tests and `./gradlew runClientGameTest` for the rendering and
interaction check. Client test screenshots are written to `build/run/clientGameTest/screenshots`.
