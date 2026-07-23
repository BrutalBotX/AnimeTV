# Build & Commit Rules

## Version Bumping

**Every commit MUST include a version bump.** No exceptions.

Before committing any code change:
1. Increment `versionCode` and `versionName` in `app/build.gradle`
   - Patch fixes (no new features): bump last digit (e.g. `6.0.0` → `6.0.1`)
   - New features: bump minor (e.g. `6.0.0` → `6.1.0`)
   - Breaking changes: bump major
2. Mirror the version in `server.json`:
   - `appnum` = `versionCode`
   - `appver` = `versionName`
   - `appurl` = correct release download URL with new version
3. Update `CHANGELOGS.md` with the new version entry
4. Commit all three files together with the code change

## Commit Flow

```
1. Make code changes
2. Bump version in build.gradle + server.json
3. Update CHANGELOGS.md
4. git add <all changed files>
5. git commit -m "vX.Y.Z: <description>"
6. Build APK: ./gradlew assembleRelease
7. git push origin master
```

## After Push

- Create a GitHub Release for the new tag
- Upload the APK to the release
- APK is at: `app/build/outputs/apk/release/animetv-<version>-release.apk`

## Project Files

| File | Purpose |
|---|---|
| `app/build.gradle` | versionCode, versionName |
| `server.json` | Update manifest (appnum, appver, download URL) |
| `CHANGELOGS.md` | Release notes |
| `AGENTS.md` | This file — build rules |
