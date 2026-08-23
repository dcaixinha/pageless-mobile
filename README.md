# Pageless Mobile (Android)

Native Android client for [Pageless](https://github.com/dcaixinha/pageless), a
self-hosted audiobook server. The app is Android-only and offline-first: browse
your library, download `.m4b` files and covers, play without a connection, and
sync playback progress, bookmarks, and listening history back to the server over
its token-authenticated JSON API.

Built with Kotlin, Jetpack Compose, Material 3, Hilt, Retrofit/OkHttp, Room,
DataStore, WorkManager, Coil, and Media3/ExoPlayer.

## Status

The app is in active early development, but the core listening flow is working:

- Login against a configurable Pageless server URL.
- Browse Home and Library from Room-backed offline-first data. The Library can
  filter offline by authors, narrators, genres, series, collections, playlists,
  publishers, language, libraries, and progress, and supports the same
  metadata/progress sorting options as the web app. Library search filters the
  grid while presenting grouped books and metadata facets from the local cache.
- Open book details with metadata, chapters, progress, bookmarks, downloads, and
  listening history. Normalized metadata values link back to the Library with
  the corresponding offline filter applied.
- Download books for offline playback, including detail metadata, chapters,
  cover image, and `.m4b` audio. Cover caching is required for a successful
  offline download.
- Play streamed or downloaded audio with Media3 background playback,
  notification/lock-screen controls, mini-player, and full Now Playing screen.
- Save playback progress locally every 10 seconds while playing and on pause,
  seek, task removal, and service shutdown.
- Sync progress, bookmarks, and listening history to the server while playing,
  during pull-to-refresh, and periodically with WorkManager.
- Add/delete bookmarks, preview bookmarks without changing book progress, and
  configure bookmark preview context time.
- Show connection/sync warning icons for no internet and server-unavailable
  states.
- Configure player behavior in Settings, including jump intervals, notification
  seeking, mini-player chapter track, Now Playing total/chapter tracks, and
  bookmark context time.

## Requirements

- **JDK 17 or newer** (Gradle 9.6.1 runs on JDK 17–26). Android Studio's
  bundled JBR works. Point `JAVA_HOME` at it or at any other JDK 17+.
- **Android SDK** with:
  - Platform `android-36`
  - Build Tools `36.0.0` (the minimum for AGP 9)
  - Platform Tools (`adb`)
- Set `ANDROID_HOME` to the SDK root, or create a git-ignored `local.properties`
  with `sdk.dir=/home/you/Android/Sdk`. Keep every package under **one** root:
  if `platform-tools` lives in a different root than `platforms`/`build-tools`,
  `assembleDebug` still works but `:app:installDebug` fails with
  `Cannot run program ".../platform-tools/adb"`.

Install SDK packages headlessly with Android command-line tools:

```sh
sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0"
```

## Build, Test, Install

From the repository root:

```sh
export JAVA_HOME=/path/to/jdk-17-or-newer
export ANDROID_HOME=/path/to/android-sdk
./gradlew testDebugUnitTest      # run JVM unit tests
./gradlew assembleDebug          # build a debug APK
./gradlew :app:installDebug      # install on a connected device/emulator
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

`./gradlew assembleRelease` produces an **unsigned** release APK. That is what
CI and F-Droid build; F-Droid signs it with its own key on its own
infrastructure. See [`fdroid/README.md`](fdroid/README.md) for how releases
reach F-Droid.

## Connecting To A Server

The Pageless server lives in the sibling repository `../pageless`.

**Debug builds** default to `http://10.0.2.2:5050`, Android Emulator's alias for
the host machine's `localhost:5050`. On a physical device, enter your computer's
LAN IP on the login screen, for example `http://192.168.50.96:5050`.

For physical-device testing, start the Phoenix server bound to all interfaces:

```sh
PHX_HOST_IP=0.0.0.0 mix phx.server
```

Debug builds allow cleartext HTTP for LAN development via
`app/src/debug/res/xml/network_security_config.xml`.

**Release builds** forbid cleartext HTTP. Users must enter an HTTPS server URL
with a valid certificate. Release defaults to `https://` and uses
`app/src/main/res/xml/network_security_config.xml`.

## Server Compatibility

Use this app with a current Pageless server checkout. Mobile sync currently
depends on API support for:

- bearer-token sessions
- books, home shelves, covers, and audio download
- structured authors, narrators, genres, series, a singular nullable publisher,
  and library metadata used by offline filtering and sorting
- progress sync
- bookmark sync
- listening history sync (`POST /api/listening-history`)

If you recently pulled server changes, run server migrations before testing:

```sh
cd ../pageless
mix ecto.migrate
```

## Offline And Sync Behavior

- Room is the local source of truth for books, metadata facets, libraries,
  chapters, progress, bookmarks, downloads, and listening history.
- Repositories read from Room and refresh/sync against the server.
- A successful Library refresh authoritatively replaces the scoped book/facet
  catalog. Cache writes and account/server cache clearing are serialized so an
  old session cannot repopulate Room after logout or a server switch.
- Progress and bookmarks use dirty/tombstone flags for offline-first sync.
- Playback progress uses last-write-wins by `lastPlayedAt`, matching the server.
- Listening history is captured locally as sessions/events and pushed to the
  server with stable client UUIDs so retries are idempotent.
- Periodic sync is scheduled with WorkManager every 15 minutes when network is
  connected. Active playback also attempts server progress sync every 60 seconds.
- Pull-to-refresh on Home, Library, and Book screens refreshes server data; the
  Library refresh also pushes queued progress/bookmark/history sync.
- Offline downloads store audio under app-private files and track completed
  downloads in Room. Removing a download removes both audio and cached cover
  paths.

## Playback

- `PlaybackService : MediaSessionService` hosts the main ExoPlayer and
  MediaSession.
- `PlayerConnection` exposes player state and commands to Compose screens.
- The mini-player owns the bottom of the app; there is no bottom navigation bar.
- Now Playing can be opened from the mini-player and dismissed by tapping the
  chevron or swiping down on the top/cover area.
- Media notification seeking is controlled by the setting “Allow position seeking
  on media notification controls”. When disabled, external controllers get a
  reduced seek command set so the notification/lock-screen scrub bar is hidden;
  the in-app player still has full seek support.
- Bookmark previews use a separate ExoPlayer instance inside the bookmark dialog
  and do not affect normal book progress.

## Shared Logic With The Server

A small amount of pure logic is intentionally duplicated from the Elixir server
instead of shared as a package. Keep these in sync; mirrored tests help catch
drift:

| Kotlin (`core/`)        | Elixir (server)                             |
| ----------------------- | ------------------------------------------- |
| `PlaybackRules`         | `Pageless.Playback.finished_at_position?/2` |
| `Chapters.currentIndex` | `Pageless.Library.Chapters.current_index/2` |
| `TimeFormat`            | `Pageless.Format`                           |
| `ProgressMerge`         | server progress merge (last-write-wins)     |
| `Iso8601`               | server ISO-8601 timestamp handling          |

Keep `core/` free of Android, Compose, Retrofit, and Room imports so it remains
JVM-testable.

## Project Layout

```text
app/src/main/java/live/pageless/mobile/
  core/            # pure, layer-agnostic logic mirrored from server rules
  data/download/   # WorkManager downloads + offline audio/cover file caching
  data/local/      # Room entities/DAOs, database, DataStore stores
  data/remote/     # Retrofit API, DTOs, auth + base-url interceptors
  data/repository/ # offline-first repositories + mappers
  data/sync/       # WorkManager sync worker/scheduler
  di/              # Hilt modules
  playback/        # Media3 PlaybackService + PlayerConnection
  ui/              # Compose screens, components, navigation, theme
```

## Useful Notes

- `PagelessDatabase` currently uses
  `fallbackToDestructiveMigration(dropAllTables = true)`. Room schema bumps wipe
  local data, which is acceptable during this dev stage but can orphan
  downloaded files.
- The app-wide authenticated `OkHttpClient` is reused for Retrofit, Coil cover
  loading, ExoPlayer streaming, and download/cover caching so protected assets
  carry the bearer token.
- Brand icon assets live in `res/drawable-*/ic_brand.png`; notification small
  icon is `R.drawable.ic_stat_pageless`; launcher assets live under `mipmap-*`.
- Native splash/window background is `@color/splash_background`, matching the
  app's dark purple surface tint.

## License

Copyright (C) 2026 Pageless contributors

This program is free software: you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free Software
Foundation, either version 3 of the License, or (at your option) any later
version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program. If not, see <https://www.gnu.org/licenses/>.
