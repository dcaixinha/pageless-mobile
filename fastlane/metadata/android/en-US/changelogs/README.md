# Changelogs

F-Droid (and Play, via Fastlane) reads per-release notes from this directory,
one file per **versionCode**, named `<versionCode>.txt`.

This app uses a timestamp-derived `versionCode` (minutes since 2025-01-01 UTC;
see `.github/workflows/release.yml`). The Release workflow requires concise
release notes and creates the correctly named file automatically before tagging,
for example:

    fastlane/metadata/android/en-US/changelogs/804807.txt

Enter the final user-facing notes when dispatching the workflow; do not create
the numbered file manually. The workflow rejects empty notes and notes longer
than 500 characters. Because the changelog and `version.properties` are part of
the tagged release commit, published tags should never be amended to change
release notes; make corrections in the next release instead.
