# Unmark

Unmark — we erase stuff that big corpos won't.

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![Build Status](https://img.shields.io/github/actions/workflow/status/EmreO33/unmark/build.yml)](https://github.com/EmreO33/unmark/actions)

## About

In recent years, generative AI's have been on a high. Misinformation on the internet have skyrocketed also. To combat this, companies like Google, OpenAI, xAI and more have put watermarks on the images they generate. Some people (like me) do not like this. This tool lets you paint over a watermark and erase it, right on your phone. You select the mark yourself — there's no AI or model involved, just a small on-device algorithm (see [How it works](#how-it-works)).

**Unmark does NOT:**
- Circumvent copy protection. Invisible provenance metadata like SynthID is not removed - a lot of social media websites can still detect whether an image is AI-generated regardless of what Unmark does. This only removes the visible watermark from the pixels of the photo.
- Upload your photos anywhere, nor collect your data
- Require an account or internet connection - everything is processed entirely on your device. You could disconnect your internet and the app would still work.

## Features

- [x] On-device watermark/object removal (no cloud upload, no network permission)
- [x] Manual brush selection tool with adjustable brush size
- [x] Nearest-fill + smoothing inpainting -- no AI/ML model, no native code
- [x] Undo last stroke / reset mask, non-destructive editing (original photo is never overwritten)
- [x] Save to gallery or share result directly
- [x] No ads, no trackers, no analytics
- [ ] Batch processing (not yet implemented)

## Screenshots

_Coming soon - not yet captured._

## Installation

### F-Droid

Not yet submitted/published. This section will get a real badge and link once it's live there.

### Manual APK

Every push builds a debug APK automatically — grab the latest one from the
[Actions tab](https://github.com/EmreO33/unmark/actions) (artifact `unmark-debug-apk`), or check the
[Releases page](https://github.com/EmreO33/unmark/releases) once tagged releases start.

## Building from source

**Requirements:**
- Android Studio Koala (2024.1) or newer
- JDK 17
- Android SDK 29–34

```bash
git clone https://github.com/EmreO33/unmark.git
cd unmark
./gradlew assembleDebug
```

Output APK: `app/build/outputs/apk/debug/`

### Verifying builds

No reproducible-build/signing setup yet - the only builds published right now are the unsigned debug
APKs produced by the [GitHub Actions workflow](.github/workflows/build.yml) on every push, so you can
compare the build log against the source at the same commit.

## How it works

Unmark uses a small, pure-Kotlin inpainting algorithm - there's no AI/ML model, no bundled weights,
and no native (C/C++) code. See [`Inpainter.kt`](app/src/main/java/com/unmark/app/inpaint/Inpainter.kt):

1. **Nearest-fill**: a multi-source breadth-first search fills every pixel you painted over with the
   color of the closest pixel you didn't paint over.
2. **Smoothing**: a few averaging passes over just the painted region soften the hard edges that
   nearest-fill leaves behind.

It won't match a proper diffusion/inpainting model on large or complex regions, but it's fast, has
zero dependencies, and works well for small marks like corner watermarks and logos on reasonably
textured backgrounds.

## Permissions

Unmark requests **no permissions at all**. Picking a photo goes through the system Photo Picker
(`ActivityResultContracts.PickVisualMedia`), and saving goes through `MediaStore` with scoped
storage — both work without any `READ_MEDIA_IMAGES`/`WRITE_EXTERNAL_STORAGE` grant on Android 10+
(minSdk 29). No network permission is requested or used; all processing happens on-device.

## Privacy

Unmark does not collect, transmit, or store any personal data. All processing happens locally
on your device. See [PRIVACY.md](PRIVACY.md) for the full policy.

## Tech stack

- Language: Kotlin
- UI: Jetpack Compose (Material 3)
- Image processing: custom, pure-Kotlin (no OpenCV, no TensorFlow Lite, no native code)
- Min SDK: 29 (Android 10) — Target SDK: 34

## Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) before opening a PR.

- Bug reports / feature requests: [Issues](https://github.com/EmreO33/unmark/issues)
- Translations: not set up yet

## License

This project is licensed under **GPL-3.0-or-later** — see [LICENSE](LICENSE) for details.

No third-party or non-free components are bundled — no ML model weights, no proprietary libraries.
All dependencies are standard AndroidX/Jetpack libraries (Apache-2.0), and the app icon is an
original vector drawable in this repo.

## Acknowledgments

- Built with [Jetpack Compose](https://developer.android.com/jetpack/compose) and AndroidX libraries
