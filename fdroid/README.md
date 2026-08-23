# F-Droid publishing

Pageless is a good fit for the official F-Droid repository: it is GPL-3.0-or-later
(see `/LICENSE`), builds from source with Gradle, and has **no proprietary
dependencies** (no Google Play Services, Firebase, or other non-free SDKs).

## How F-Droid builds it

F-Droid builds and signs the app on its own infrastructure from a tagged commit
— it does **not** use your Play upload key. The release APK is produced with:

    ./gradlew :app:assembleRelease

The release workflow writes the `versionName` (a clean date string) and
`versionCode` (minutes since 2025-01-01 UTC) to `version.properties`, commits
that file, and tags the release commit. Gradle and F-Droid therefore read the
same static values from the tagged source.

## Submitting to the official repo

1. Merge the release code and ensure CI passes.
2. Run the Release workflow from the default branch with concise release notes
   (500 characters or fewer). It commits `version.properties` and the matching
   Fastlane changelog, builds the APK, pushes the tag, and creates the GitHub
   Release.
3. Record the generated `versionName`, `versionCode`, and full release commit SHA.
4. Fork https://gitlab.com/fdroid/fdroiddata.
5. Copy `fdroid/live.pageless.mobile.yml` (in this repo) to
   `metadata/live.pageless.mobile.yml` in your fork, replacing the example first
   build and `CurrentVersion` values with the generated release values.
6. Validate locally with the fdroidserver tools:

       fdroid lint live.pageless.mobile
       fdroid build live.pageless.mobile

7. Open a merge request against fdroiddata.

After the initial recipe is merged, `UpdateCheckMode: Tags` finds each new
release tag and `AutoUpdateMode: Version` appends the build entry
automatically. Manual fdroiddata merge requests are only needed if detection or
a build fails, or the build recipe changes.

**`UpdateCheckData` is load-bearing — do not remove it.**

```yaml
UpdateCheckData: version.properties|VERSION_CODE=(\d+)|.|VERSION_NAME=(.+)
```

F-Droid's update check never executes build logic; it scrapes. Its FAQ warns
against versions computed at build time, and `app/build.gradle.kts` derives
both values through a `versionProp(...)` call that the scraper cannot evaluate.
That line sidesteps the problem by reading the literals straight out of
`version.properties` in the tagged commit. Without it, detection would have to
parse the Gradle file and would fail — silently, with no new versions ever
appearing. Keep `version.properties` a flat `KEY=value` file for the same
reason.

Verified working on 2026-08-23: tag `20260822130925-a79739e9d` was detected and
its build entry added upstream automatically about 22 hours after the push.
Building and publishing the APK takes longer — F-Droid's build cycle runs once
a day and publication typically takes a few days. Check with:

```sh
curl -s https://f-droid.org/api/v1/packages/live.pageless.mobile
```

Store listing text and screenshots come from `fastlane/metadata/android/en-US/`
in this repo, which both F-Droid and Play read.

## Notes

- Release builds forbid cleartext HTTP (`app/src/main/res/xml/network_security_config.xml`),
  which aligns with F-Droid's expectations.
- The APK produced by the Release workflow is currently **unsigned** (Play upload
  signing is deferred); this does not affect F-Droid, which signs its own builds.
- The release workflow pushes a small release-metadata commit directly to the
  default branch. Branch protection must allow the workflow's `GITHUB_TOKEN` to
  make that push.
