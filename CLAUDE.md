# Stormlight

Fabric mod (Minecraft 26.2). Run Gradle as `sh gradlew ...` (gradlew is not executable).

## Testing

Run only the tests affected by your change:

```
scripts/test-affected.sh            # branch vs main + uncommitted
scripts/test-affected.sh --dry-run  # show what it would run
```

Don't run the full `runGameTest runClientGameTest` suite unless the user asks or shared code changed (the script decides this). To target tests by hand: `sh gradlew runClientGameTest -Ptests=Highstorm` (comma-separated test class prefixes).

When adding tests for a new feature, register the class in `src/gametest/resources/fabric.mod.json` and add a rule to `RULES` in `scripts/test-affected.sh`.
