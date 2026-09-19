# Contributing

Keep changes focused and explain their visual or behavioral effect. For UI changes, include a screenshot or short recording and the device/API level used to verify them.

- Use the official Kotlin style: four spaces, explicit imports, descriptive names, and trailing commas in multiline declarations.
- Keep wave/time calculations in `motion`, rendering in `ui`, and screen events in `MainActivity`.
- Document units and non-obvious rendering decisions. Avoid comments that repeat the code.
- Preserve the measured default parameters unless the change explicitly concerns the fit.
- Run `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`.
- Check pause/resume, slow playback, manual progress, rotation, and foreground/background transitions on a device.
- Do not commit SDK paths, recordings from personal devices, signing keys, build caches, or generated APKs.

No publication, repository push, or release is performed by the local build.
