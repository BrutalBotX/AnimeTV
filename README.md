# AnimeTV

A cross-platform anime streaming app for Android TV, Google TV, Fire TV, smartphones, Windows, Linux, and macOS.

Optimized for TV interfaces with full remote control navigation. No ads. No data collection. No analytics. Completely free and open source.

<p align="center">
  <img src="https://github.com/BrutalBotX/AnimeTV/assets/1386831/d05c7e5d-8abc-4fed-9183-0c58aa815c44" alt="Homescreen" width="45%">
  <img src="https://github.com/BrutalBotX/AnimeTV/assets/1386831/c8854596-1984-4c54-993d-d358d9943e7d" alt="Anime Popup" width="45%">
</p>

## Features

- **Multi-source streaming** — browse and stream anime from multiple providers
- **TV-first interface** — designed for 10-foot screens, full remote control support
- **AniList integration** — rich metadata, descriptions, genres, banners, and recommendations
- **Skip Intro & Skip Filler** — automatically skip opening themes and filler content
- **Playback controls** — adjustable speed, quality selection, subtitle/audio track switching
- **Soft subtitles** — styled subtitles with language selection
- **Watchlist & History** — track what you watch with MAL and AniList sync
- **Profiles** — multiple user profiles with custom avatars and wallpapers
- **Customizable themes** — purple, green, teal, blue, brown, red, orange, and dark themes
- **Airing schedule** — see what's airing each day
- **Search** — search by title with genre filters
- **Multiple languages** — subtitle and audio language preferences

## About This Fork

This fork replaces all non-functional anime sources with a new streaming backend powered by the [Anilili](https://github.com/Anilili) provider system.

### What's Changed

| Area | Details |
|---|---|
| **Streaming backend** | Rebuilt from scratch using Anilili providers (Senshi, AniBD, AnimeKai, Anivexa + 8 more) |
| **AniList bridge** | Direct AniList GraphQL client with DNS-over-HTTPS for blocked networks |
| **Source count** | Removed 8 dead sources, replaced with 16+ working providers |
| **UI** | Modernized with rounded corners, glassmorphism sidebars and dialogs, improved shadows |
| **Performance** | All visual effects respect the existing "UI & Performance" toggle settings |

### Technical Stack

- **Android:** Java + Kotlin 2.1.20, WebView-based UI (HTML/CSS/JS)
- **Streaming:** kotlinx-coroutines 1.9.0, kotlinx-serialization 1.7.3
- **Providers:** HLS/m3u8 streaming, multi-provider fallback, VTT subtitles
- **Desktop:** Electron wrapper with identical WebView engine

## Credits

- **[amarullz](https://github.com/amarullz)** — original creator of AnimeTV. The TV-optimized UI, navigation system, profile management, theming engine, and overall application architecture are his work. This project would not exist without his vision and years of development.
- **[Anilili](https://github.com/Anilili)** — the streaming provider backend that powers the episode sources in this fork. Their multi-provider system with HLS extraction and playlist decryption enables reliable anime streaming.

## Disclaimer

- AnimeTV scrapes links from publicly available websites and does not host any content
- All images and anime information are sourced from public APIs
- The developers have no affiliation with any source websites
- The developers are not liable for any misuse of content found through the app
- For copyright concerns, contact the source website directly

## License

Licensed under the Apache License, Version 2.0.

```
Copyright 2023 Ahmad Amarullah

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
