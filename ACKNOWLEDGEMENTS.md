# Acknowledgements

OpenLoader builds on many open-source projects. Versions are pinned in [`libs.versions.toml`](gradle/libs.versions.toml). Highlights:

| Project | Use |
| --------| --- |
| [Android Open Source Project / Jetpack](https://developer.android.com/jetpack) | Compose, Lifecycle, DataStore, Navigation, etc. |
| [Kotlin](https://kotlinlang.org/) | Language & tooling |
| [Dagger / Hilt](https://dagger.dev/hilt/) | Dependency injection |
| [Google Protocol Buffers](https://developers.google.com/protocol-buffers) | Serialized preferences & install history |
| [Shizuku](https://github.com/RikkaApps/Shizuku) | API for running code with shell/ADB privileges when authorized |
| [libadb-android](https://github.com/MuntashirAkon/libadb-android) | ADB client for wireless installs |
| [Conscrypt](https://github.com/google/conscrypt) | TLS for ADB-related networking |
| [Material Kolor](https://github.com/jordond/material-kolor) | Material You-style dynamic colors |

If we missed a library you care about, open a PR to update this file.
