# Submitting Unmark to F-Droid

F-Droid does not accept pushed builds. It builds every app itself, from source, on its own
servers, using a metadata file that lives in F-Droid's own repository (`fdroiddata`), not in
this repo. Getting listed is a one-time, manually reviewed step; after that, new versions can
publish with no manual work on either side.

## One-time setup (you do this, it can't be automated)

1. Create a GitLab account if you don't have one, then fork
   [gitlab.com/fdroid/fdroiddata](https://gitlab.com/fdroid/fdroiddata). It needs to be a
   **public** fork, GitLab may restrict new accounts from setting a project to Public; if so,
   grant the reviewing maintainer direct Member access as a workaround, or contact GitLab
   support.
2. In your fork, add exactly **one** file: [`com.unmark.app.yml`](com.unmark.app.yml) from this
   folder, as `metadata/com.unmark.app.yml`.

   Do not add `Summary`, `Description`, or any per-locale files under `metadata/com.unmark.app/`
   in fdroiddata. That listing text lives in *this* repo instead, in the standard fastlane
   layout at [`../fastlane/metadata/android/en-US/`](../fastlane/metadata/android/en-US/)
   (`short_description.txt`, `full_description.txt`). F-Droid pulls it from there automatically
   at build time. Adding it in fdroiddata too gets flagged by CI and by reviewers.
3. Optional but recommended: install `fdroidserver` and run `fdroid lint com.unmark.app` and
   `fdroid checkupdates com.unmark.app` locally to catch metadata problems before submitting.
4. Open a merge request against `fdroiddata`. Edit the MR description and pick the **"App
   Inclusion"** template from GitLab's template dropdown; read through it and check off the
   task boxes that apply.
5. F-Droid's reviewers will comment if anything needs fixing (common asks: reproducible builds,
   exact license match, no anti-features, commit hashes instead of tags, listing text living in
   the app's own repo instead of fdroiddata). Respond on the MR; this can take anywhere from
   days to weeks depending on reviewer availability.
6. Once merged, F-Droid's build server does a test build from the commit pinned in the metadata
   file's `Builds` entry. If it succeeds, Unmark goes live on F-Droid within the next publish
   cycle (typically up to a few days).

**The `commit:` field in `Builds:` must be a full commit hash, never a tag or branch name.**
Get it with:

```bash
git rev-list -n 1 vX.Y.Z
```

and use that 40-character hash, not `vX.Y.Z` itself. (F-Droid's own bot resolves this
automatically for versions it adds later via `AutoUpdateMode`; this only matters for entries
you hand-write, like the first one.)

**Watch out for line endings when copy-pasting into GitLab's web editor on Windows.** If the
pasted file ends up with CRLF line endings, `fdroid rewritemeta` will reject it. Prefer
downloading the raw file from GitHub and using GitLab's "Replace file" (upload) action instead
of pasting text.

## Reproducible builds

`Binaries` and `AllowedAPKSigningKeys` in the metadata let F-Droid verify its own build matches
our signed GitHub Release APK byte-for-byte, and if so, distribute our signed APK directly
instead of resigning with F-Droid's own key. Both are one-time, permanent settings, nothing to
redo per release:

- `Binaries` is a URL template (`%v`/`%c` expand to versionName/versionCode) pointing at our
  GitHub Release asset, which is always named `Unmark.apk` regardless of version.
- `AllowedAPKSigningKeys` is the lowercase hex SHA-256 fingerprint of our release signing
  certificate (the same keystore documented in `unmark-release-keystore-README.txt`, kept out
  of git). Get it with:
  ```bash
  keytool -list -v -keystore unmark-release.jks -storepass <password> | grep SHA256
  ```
  then strip the colons and lowercase it.

If verification ever fails (different build environments producing non-identical output isn't
unheard of), F-Droid just falls back to signing the APK with its own key, no harm done, this
isn't something that can break the submission.

**If the signing key is ever lost or rotated, `AllowedAPKSigningKeys` must be updated to match**,
otherwise F-Droid will stop trusting upstream binaries entirely (still not fatal, just reverts to
F-Droid resigning it).

## What's automated after that

The metadata sets `AutoUpdateMode: Version` and `UpdateCheckMode: Tags ^v[0-9]+\.[0-9]+\.[0-9]+$`.
That means F-Droid's own bot periodically scans this GitHub repo, and any new tag matching
`vX.Y.Z` is picked up and built automatically, no new merge request needed.

## What you need to do for every future release

F-Droid reads `versionName`/`versionCode` from `app/build.gradle.kts` at the tagged commit, so
before tagging a new release:

1. Bump `versionCode` (always increases by at least 1) and `versionName` in
   [`app/build.gradle.kts`](../app/build.gradle.kts).
2. Update the listing text in [`../fastlane/metadata/android/en-US/`](../fastlane/metadata/android/en-US/)
   if anything changed, and optionally add a changelog note at
   `../fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`.
3. Commit those changes, then tag and push, e.g.:
   ```bash
   git tag v0.3.0
   git push origin v0.3.0
   ```
4. This repo's [release workflow](../.github/workflows/release.yml) publishes the GitHub Release
   automatically. F-Droid's bot picks up the same tag on its own schedule and builds/publishes
   there too, independently, no changes needed in fdroiddata.
