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

Run `./gradlew build` for the server game tests and `./gradlew runClientGameTest` for the rendering and
interaction check. Client test screenshots are written to `build/run/clientGameTest/screenshots`.
