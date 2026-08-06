# Submitting Unmark to F-Droid

F-Droid does not accept pushed builds. It builds every app itself, from source, on its own
servers, using a metadata file that lives in F-Droid's own repository (`fdroiddata`), not in
this repo. Getting listed is a one-time, manually reviewed step; after that, new versions can
publish with no manual work on either side.

## One-time setup (you do this, it can't be automated)

1. Create a GitLab account if you don't have one, then fork
   [gitlab.com/fdroid/fdroiddata](https://gitlab.com/fdroid/fdroiddata).
2. In your fork, add **two** files:
   - [`com.unmark.app.yml`](com.unmark.app.yml) from this folder, as `metadata/com.unmark.app.yml`
   - [`com.unmark.app/en-US/summary.txt`](com.unmark.app/en-US/summary.txt) from this folder, as
     `metadata/com.unmark.app/en-US/summary.txt`

   The `Summary` field is not part of the main yml, F-Droid keeps per-locale short descriptions
   in separate files. Skipping this file makes CI's `tools check scripts` job fail (it tries to
   auto-migrate an inline `Summary:` field and treats that as an error).
3. Optional but recommended: install `fdroidserver` and run `fdroid lint com.unmark.app` and
   `fdroid checkupdates com.unmark.app` locally to catch metadata problems before submitting.
4. Open a merge request against `fdroiddata` with those files.
5. F-Droid's reviewers will comment if anything needs fixing (common asks: reproducible builds,
   exact license match, no anti-features). Respond on the MR; this can take anywhere from days
   to weeks depending on reviewer availability.
6. Once merged, F-Droid's build server does a test build from the tag/commit pinned in the
   metadata file's `Builds` entry. If it succeeds, Unmark goes live on F-Droid within the next
   publish cycle (typically up to a few days).

**Before submitting, make sure the metadata's `Builds` entry points at the current release tag**,
not an old one, this repo's copy at [`com.unmark.app.yml`](com.unmark.app.yml) is kept up to date,
but if you copied it earlier, re-copy it now.

**Watch out for line endings when copy-pasting into GitLab's web editor on Windows.** If the pasted
file ends up with CRLF line endings, `fdroid rewritemeta` will reject it and F-Droid's build script
can fail to locate the `versionCode` line. After committing, open the file's raw view on GitLab and
confirm it looks like a normal LF file (no stray `^M` visible if you check with `git diff` locally).

## What's automated after that

The metadata sets `AutoUpdateMode: Version` and `UpdateCheckMode: Tags ^v[0-9]+\.[0-9]+\.[0-9]+$`.
That means F-Droid's own bot periodically scans this GitHub repo, and any new tag matching
`vX.Y.Z` is picked up and built automatically, no new merge request needed.

## What you need to do for every future release

F-Droid reads `versionName`/`versionCode` from `app/build.gradle.kts` at the tagged commit, so
before tagging a new release:

1. Bump `versionCode` (always increases by at least 1) and `versionName` in
   [`app/build.gradle.kts`](../app/build.gradle.kts).
2. Commit that change, then tag and push, e.g.:
   ```bash
   git tag v0.2.0
   git push origin v0.2.0
   ```
3. This repo's [release workflow](../.github/workflows/release.yml) publishes the GitHub Release
   automatically. F-Droid's bot picks up the same tag on its own schedule and builds/publishes
   there too, independently.
