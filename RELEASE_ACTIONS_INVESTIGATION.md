# GitHub Actions Release Investigation — 2026-08-02

## Incident

- Failed run: [Release APK #30731711672](https://github.com/gansxx/labs-ArchiveAssistant/actions/runs/30731711672)
- Failed job: `release` / `91453131509`
- Trigger: manual dispatch for `v0.0.1`
- Result: failure before the Gradle build step.

## Root causes

The `Validate release configuration` step rejected the run because
`RELEASE_KEYSTORE_BASE64` was not configured. The run log also showed the
other required signing secrets as empty:

- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

Consequently, `Build signed release APK` was skipped. The GitHub Actions
summary message **“No Gradle build results detected”** is a consequence of
that early exit, not a Gradle, JDK, or dependency failure.

After signing configuration was repaired, a second failure became visible in
the real Gradle build. `:app:compileReleaseKotlin` failed because the Compose
BOM pinned by the project does not provide the `overscrollEffect` named
parameter used by `LazyColumn`, `LazyRow`, and `verticalScroll`.

## Remediation

1. Created a dedicated RSA-4096 Android release signing key and configured
   all four required repository Actions secrets. Secret values and key material
   are intentionally not stored in this repository.
2. Increased the Gradle Wrapper download timeout from 10 seconds to 60
   seconds. A cold Gradle 8.13 distribution download exceeded the previous
   limit in local verification, so this avoids an unrelated pre-build failure
   on slower runners.
3. Prepared the local release build environment with JDK 17, Android SDK
   Platform 36, Platform Tools, and Build Tools 36.0.0.
4. Removed the three incompatible `overscrollEffect = null` arguments. This
   retains the locked Compose dependency set and restores Kotlin compilation;
   the standard platform overscroll behavior is used instead.

## Verification plan

The source fix is pushed to `main` and the repaired workflow is dispatched
again for `v0.0.1`. A successful run must complete these steps: configuration
validation, `assembleRelease`, APK signature verification, artifact upload,
and GitHub Release publication.

## Operational note

The GitHub Actions signing key is now the release identity for this app.
Preserve it through the repository's secret-management/backup process; changing
it later prevents Android from accepting updates signed by the original key.
