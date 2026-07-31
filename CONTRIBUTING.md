# Contributing to Unmark

Thanks for considering a contribution.

## Reporting bugs / requesting features

Open an issue with steps to reproduce (for bugs) or a clear description of the use case (for
feature requests). Screenshots help.

## Sending a pull request

1. Fork the repo and create a branch off `master`.
2. Keep changes focused, one logical change per PR.
3. Make sure the app still builds: `./gradlew assembleDebug`.
4. Open the PR with a short description of what changed and why.

## Licensing

By contributing, you agree your contributions are licensed under the project's GPL-3.0-or-later
license (see [LICENSE](LICENSE)). Only contribute code/assets you have the right to license this
way: this matters for F-Droid inclusion, which requires everything in the app to be free/libre.

## Project constraints to keep in mind

- No network permission, ever. All processing must stay on-device.
- No proprietary or non-free dependencies (blocks F-Droid distribution).
- Keep the app lightweight. Avoid heavy dependencies (e.g. full ML frameworks) unless the
  size/quality tradeoff is discussed in an issue first.
