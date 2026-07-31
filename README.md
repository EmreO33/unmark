<!--
  TEMPLATE README — fill in every [bracketed] placeholder.
  Sections are ordered the way F-Droid / typical FOSS Android repos expect them.
  Delete any section you don't need; keep License and Privacy — F-Droid reviewers check those first.
-->

# [App Name]

["Unmark, we erase stuff that big corpo's won't."]

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![F-Droid](https://img.shields.io/f-droid/v/[package.id].svg)](https://f-droid.org/packages/[package.id]/)
[![Build Status](https://img.shields.io/github/actions/workflow/status/[user]/[repo]/[workflow].yml)](https://github.com/[user]/[repo]/actions)

<p align="center">
  <img src="metadata/en-US/images/phoneScreenshots/1.png" width="200" />
  <img src="metadata/en-US/images/phoneScreenshots/2.png" width="200" />
  <img src="metadata/en-US/images/phoneScreenshots/3.png" width="200" />
</p>

## About

["In recent years, generative AI's have been on a high. Misinformation on the internet have skyrocketed also. To combat this, companies like: Google, OpenAI xAI and more have put watermarks on the images they generate. Some people (like me) do not like this. This tool has a very lightweight AI model that just removes watermarks. (You will have to select the watermarks yourself.)"]

**Unmark does NOT:**
- Circumvent copy protection (SynthID metadata are not removed. A lot of social media websites can still detect these images if they are AI or not. This only removes the watermark itself on the photo.)
- Upload your photos anywhere, nor collect your data
- Require an account or internet connection, Everything that is processed is processed on only your device. You could disconnect your internet and the app would still work.

## Features

- [ ] On-device object/watermark removal (no cloud upload required)
- [ ] [Inpainting method — e.g. OpenCV Telea/Navier-Stokes, or on-device ML model such as LaMa]
- [ ] Batch processing
- [ ] Manual brush/selection tool
- [ ] Undo/redo, non-destructive editing (keeps original file)
- [ ] Share/export result directly
- [ ] No ads, no trackers, no analytics

## Screenshots

| Home | Editor | Result |
|------|--------|--------|
| ![](metadata/en-US/images/phoneScreenshots/1.png) | ![](metadata/en-US/images/phoneScreenshots/2.png) | ![](metadata/en-US/images/phoneScreenshots/3.png) |

## Installation

### F-Droid (recommended)

<a href="https://f-droid.org/packages/[package.id]/">
  <img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" alt="Get it on F-Droid" height="80">
</a>

### Manual APK

Download the latest APK from the [Releases page](https://github.com/[user]/[repo]/releases).

> Reproducible builds: the APK signature published here matches the one F-Droid builds from source — see [Verifying builds](#verifying-builds).

## Building from source

**Requirements:**
- Android Studio [version] / Gradle [version]
- JDK [version]
- Android SDK [min]–[target]

```bash
git clone https://github.com/[user]/[repo].git
cd [repo]
./gradlew assembleDebug
```

Output APK: `app/build/outputs/apk/debug/`

### Verifying builds

[Explain reproducible-build steps if you support them, or link to F-Droid's build metadata/log for this app.]

## How it works

[Short technical explanation for contributors/curious users — e.g. algorithm used, model name +
license + size if bundled, whether processing happens fully offline. Important for F-Droid/FOSS
credibility since "how does it actually remove the mark" is the first question reviewers ask.]

## Permissions

| Permission | Why it's needed |
|---|---|
| `READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE` | [To let you pick a photo to edit] |
| `WRITE_EXTERNAL_STORAGE` (if applicable) | [To save the edited result] |
| [any others] | [reason] |

No network permission is requested. / [If network is needed, explain exactly why and what leaves the device.]

## Privacy

[App Name] does not collect, transmit, or store any personal data. All processing happens locally
on your device. See [PRIVACY.md](PRIVACY.md) for the full policy.

## Tech stack

- Language: [Kotlin]
- UI: [Jetpack Compose / XML Views]
- Image processing: [OpenCV / TensorFlow Lite / ONNX Runtime / custom]
- Min SDK: [level] — Target SDK: [level]


## Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) before opening a PR.

- Bug reports / feature requests: [Issues](https://github.com/[user]/[repo]/issues)
- Translations: [link to Weblate/Hosted Weblate project if used]

## License

This project is licensed under the **[GPL-3.0-or-later]** — see [LICENSE](LICENSE) for details.

[Note any third-party components with different licenses here, e.g. bundled ML model weights,
OpenCV (Apache-2.0/BSD), icon packs, etc. F-Droid requires all bundled components to be free/libre.]

## Acknowledgments

- [Library/model/algorithm credits]
- [Icon/design credits]
