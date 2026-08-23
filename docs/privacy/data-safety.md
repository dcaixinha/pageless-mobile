# Data flow audit and Google Play Data safety answers

Source-backed inventory of every data type Pageless Mobile reads, stores, or
transmits, plus a proposed answer for each Google Play Data safety question.

Tracked by beads issue **pm-a6l.3**; feeds **pm-a6l.7** (privacy policy) and
**pm-a6l.10** (Play Console app-content declarations).

| | |
| --- | --- |
| Audited revision | `16d1e06` (master), `applicationId` `live.pageless.mobile` |
| Audited on | 2026-08-04 |
| Taxonomy source | Play Console Help, *Provide information for Google Play's Data safety section* (retrieved 2026-08-04) |

## 1. Method

Every claim below was read out of the source, not inferred from product copy:

- `app/src/main/AndroidManifest.xml` — permissions, components, backup flag.
- `data/remote/PagelessApi.kt` + `Dtos.kt` — the complete set of requests the
  app can make and the exact fields in each body.
- `data/local/SessionStore.kt`, `PlayerSettingsStore.kt` — DataStore contents.
- `data/local/Entities.kt` — the 15 Room tables and their columns.
- `data/download/AudioDownloader.kt`, `CoverCache.kt` — on-disk file locations.
- `data/repository/*.kt` — what is transmitted, and when.
- `res/xml/network_security_config.xml` (main and debug) — transport security.
- `./gradlew :app:dependencies --configuration releaseRuntimeClasspath` — the
  actual shipped dependency graph, used to verify SDK behaviour.

The AGP 9 toolchain upgrade currently in `stash@{0}` changes dependency
*versions* only (Hilt, Room, KSP, ktlint). It adds and removes no libraries, so
it does not affect any conclusion here.

## 2. Conclusions

1. **There is no developer-operated backend.** All network traffic goes to a
   Pageless server whose URL the user types in at login. The publisher operates
   no server and receives no data.
2. **No ads, analytics, telemetry, crash reporting, or attribution SDKs.**
   Verified against the release classpath, not just intent.
3. **Data is nevertheless "collected" under Play's definition**, because Play
   defines collection as transmitting data off the device "irrespective of
   whether data is transmitted to you or a third-party server". The store
   listing must therefore *not* claim that no data is collected.
4. **Nothing is "shared."** The only recipient is the server the user chose and
   operates, reached by a specific user-initiated action the user plainly
   expects — Play's user-initiated-transfer exemption.
5. **Encrypted in transit: yes** for the Play artifact. Release builds forbid
   cleartext; the cleartext relaxation is confined to the debug source set,
   which is never distributed through Play.
6. The three open questions (declaring the password, declaring the session token,
   and the deletion-request answer) are **resolved** — see the decision log in
   [§7](#7-account-creation-and-deletion). The declared set is three data types:
   Email address, Other user-generated content, Other actions.
7. Five findings remain, two of them code changes tracked as their own issues;
   see [§8](#8-findings). None blocks the form.

## 3. Data inventory

`filesDir` = app-private internal storage. "Off device" = leaves the device, in
Play's sense. Every destination in this table is the user's own server.

| # | Data | Where it comes from | Stored on device | Sent off device | Purpose | Deletion |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | Server base URL | User input at login | DataStore `session` (`server_url`) | No — it *is* the destination | Reach the server | Logout |
| 2 | Email address | User input at login | DataStore `session` (`email`) | **Yes** — `LoginRequest.email`, `AuthRepository:56` | Authenticate | Logout |
| 3 | Password | User input at login | **Never persisted** — a function parameter only (`AuthRepository.login`) | **Yes** — `LoginRequest.password`, in-memory, one request | Authenticate | n/a (ephemeral) |
| 4 | Bearer token | Issued by the server | DataStore `session` (`token`), plaintext, excluded from backup | **Yes** — `Authorization` header on every request (`AuthInterceptor`) | Authenticate | Logout |
| 5 | First name | Server response (`user.first_name`) | DataStore `session` (`first_name`) | No — inbound only | Greeting in UI | Logout |
| 6 | Device model string | `Build.MANUFACTURER` + `Build.MODEL` | Room `playback_sessions.deviceInfo` | **Yes** — `device_name` at login (`AuthRepository:56`); `device_info` in history (`PlaybackHistoryRepository:142`, also `Build.VERSION.RELEASE`) | Name the session/device in server UI | Logout |
| 7 | Library metadata (titles, subtitles, authors, narrators, series, collections, playlists, genres, publisher, language, durations, descriptions) | Server | Room `books`, `chapters`, `book_facets`, `cached_libraries`, `series*`, `collections*`, `playlists*` | No — inbound only | Offline browsing | Logout |
| 8 | Audiobook files (`.m4b`) | Server | `filesDir/audiobooks/<bookId>.m4b` | No — download only, never uploaded | Offline playback | See finding **F2** |
| 9 | Cover images | Server | `filesDir/covers/<bookId>.<ext>` | No — download only | Offline artwork | See finding **F2** |
| 10 | Playback progress (position, duration, finished, timestamps) | Local playback | Room `progress` | **Yes** — `POST /api/progress/{bookId}` | Cross-device resume | Logout (local); server-side by account owner |
| 11 | Bookmarks, including free-text notes | User input | Room `bookmarks` | **Yes** — `PUT /api/bookmarks/{id}`, `DELETE` for tombstones | Sync bookmarks | Logout (local); server-side by account owner |
| 12 | Listening history (sessions and events: play/pause/seek, positions, timestamps, play method, title, authors) | Local playback | Room `playback_sessions`, `playback_events` | **Yes** — `POST /api/listening-history` | Listening stats on the server | Logout (local); server-side by account owner |
| 13 | Player and display settings, theme mode | User input | DataStore `player_settings` | **No** | Local preferences | Uninstall / clear data |
| 14 | Search, filter, and sort terms | User input | Not persisted | **No** — see below | Local browsing | n/a |
| 15 | HTTP logs | OkHttp | Logcat, **debug builds only** | No | Local debugging | n/a |

**Search never leaves the device.** `PagelessApi.books()` declares `search`,
`sort`, and `library_id` query parameters, but the sole caller
(`LibraryRepository:63`) invokes `api.books()` with **no arguments**. All
searching, filtering, and sorting is computed locally from Room by
`LibrarySearchEngine`, `LibraryFilterEngine`, and `LibrarySortEngine`. Play's
"In-app search history" is therefore *not* collected.

**Logging cannot leak the token.** The interceptor is installed only under
`if (BuildConfig.DEBUG)` at `di/AppModule.kt:64` and at
`HttpLoggingInterceptor.Level.BASIC`, which records the request line and
response status only — no headers, no bodies.

## 4. Third-party SDK verification

Release runtime classpath group list (945 resolved lines). Matches for
`gms`, `firebase`, `com.google.android.play`, `crashlytics`, `analytics`,
`adjust`, `appsflyer`, `facebook`, `bugsnag`, `sentry`, `amplitude`,
`mixpanel`: **0**.

Everything shipped is either AndroidX, Kotlin/kotlinx, or:

| Library | Role | Data behaviour |
| --- | --- | --- |
| `com.squareup.okhttp3` / `okio` | HTTP transport | Only reaches the user's server |
| `com.squareup.retrofit2`, `com.jakewharton.retrofit` | API binding | No independent network access |
| `io.coil-kt` | Cover image loading | Uses the app's authed `OkHttpClient` (`PagelessApp:40-44`), so it only ever fetches from the user's server |
| `com.google.dagger` | DI | Compile-time; no I/O |
| `com.google.accompanist:accompanist-drawablepainter` | Compose helper | UI only |
| `androidx.media3` | Playback | Streams from the user's server or local files |
| `androidx.work` | Download/sync scheduling | No network of its own |
| `androidx.profileinstaller` | Baseline profiles | Local only |
| `com.google.guava`, `jspecify`, `findbugs` `jsr305`, `jakarta.inject`, `javax.inject` | Transitive utilities | No I/O |

Also verified absent from the source: `WebView` (none), and every device
identifier API — `ANDROID_ID`, `Settings.Secure`, advertising ID, IMEI, MAC,
telephony (all grep-clean).

## 5. Permissions

None are data-collection permissions. There is no location, contacts, storage,
camera, microphone, or phone-state access.

| Permission | Why |
| --- | --- |
| `INTERNET` | Reach the user's server |
| `ACCESS_NETWORK_STATE` | Connection status indicator, WorkManager network constraints |
| `FOREGROUND_SERVICE`, `..._MEDIA_PLAYBACK` | Background audio (`PlaybackService`) |
| `..._DATA_SYNC` | Foreground download worker |
| `POST_NOTIFICATIONS` | Media notification, download progress |

## 6. Proposed Data safety form answers

### Data collection and security

| Question | Answer | Rationale |
| --- | --- | --- |
| Does your app collect or share any of the required user data types? | **Yes** | Email address and app activity are transmitted off device (§3) |
| Is all of the user data collected by your app encrypted in transit? | **Yes** | Release build sets `cleartextTrafficPermitted="false"`; debug-only relaxation is not distributed |
| Do you provide a way for users to request that their data is deleted? | **No** | The publisher holds no user data, so there is no deletion request to receive. See below |

**Why "No" on deletion, and what it is *not* about.** Play frames this question
as whether you "provide a mechanism to receive data deletion requests from your
users", and the badge may be claimed if you either provide such a mechanism or
"automatically initiate deletion or anonymization of **collected** data within 90
days". Both branches concern *collected* data — data transmitted off the device.

Every byte this app transmits goes to the user's own Pageless server. The
publisher never receives, stores, or can reach it, so there is no request it
could action and no retention window it controls. Deletion of server-side
progress, bookmarks, and history is performed by the account owner or server
administrator, outside this app.

Local on-device data is **out of scope for this question entirely**, because it
was never collected. That includes the downloaded `.m4b` files and cached covers:
they are the user's own audiobook content, pulled from the user's own server, and
never transmitted anywhere. Deleting them therefore cannot support a "Yes" here.
Finding **F2** is still worth fixing on shared-device privacy and storage
grounds, but it is not a Data safety lever.

### Data types

Declare **collected, not shared** for all of the following. None is shared,
under the user-initiated-transfer exemption (§2.4).

| Category → type | Declare? | Ephemeral | Required/optional | Purposes | Rationale |
| --- | --- | --- | --- | --- | --- |
| Personal info → **Email address** | **Yes** | No | Required | App functionality, Account management | Sent in `LoginRequest`; stored to show the signed-in account. The "Account management" purpose is what represents authentication, per Play's own example: "log in to your app, or verify their credentials" |
| Personal info → Other info (**password**) | **No** | — | — | — | Resolved decision **D2**. There is no credential data type in the taxonomy; "Other info" means "any other personal information such as date of birth, gender identity, veteran status", i.e. demographic attributes, not secrets. Credentials appear in Play's documentation only as an *example of the Account management purpose*, which is already declared on Email address. The password is also never persisted (`AuthRepository.login` takes it as a parameter) and would be ephemeral, so it would not surface on the listing even if filed. Declaring it under a type it does not match would misdescribe the app for zero transparency gain |
| Personal info → **User IDs** | **No** | — | — | — | Resolved decision **D3**. Play defines User IDs as "identifiers that relate to an identifiable person. For example, an account ID, account number, or account name". A bearer token is an authentication secret, not an account identifier: the app cannot resolve a user from it, and the publisher never receives it. It is returned only to the server that issued it and already knows the account. The identifying data in that exchange — the email address — is declared. See the residual-risk note below |
| Personal info → Name | **No** | — | — | — | First name is *received* from the server and never transmitted by the app; inbound data is not collection |
| App activity → **Other user-generated content** | **Yes** | No | **Optional** | App functionality | Bookmark notes. The app is fully usable without ever creating a bookmark, which meets Play's definition of optional |
| App activity → **Other actions** | **Yes** | No | **Required** | App functionality | Playback progress and listening history. Verified there is no opt-out: `PlayerSettings` exposes only display/playback preferences |
| App activity → In-app search history | **No** | — | — | — | Search is computed locally; `api.books()` is called with no query parameters |
| App activity → App interactions | **No** | — | — | — | Playback events are book actions, better described by "Other actions". Noted as an alternative reading |
| App activity → Installed apps | **No** | — | — | — | Never queried |
| **Device or other IDs** | **No** | — | — | — | Only `MANUFACTURER`/`MODEL`/`Build.VERSION.RELEASE` are sent — a model string, not an identifier "that relate[s] to an individual device". Re-evaluate if a per-install ID is ever added |
| Audio files → Music / Other audio files | **No** | — | — | — | Audiobooks are downloaded *to* the device; audio is never uploaded |
| Files and docs | **No** | — | — | — | No file access outside `filesDir` |
| Location, Financial info, Health and fitness, Messages, Photos and videos, Calendar, Contacts, Web browsing | **No** | — | — | — | No corresponding permission, API, or field exists |
| App info and performance → Crash logs / Diagnostics | **No** | — | — | — | No crash or performance SDK ships. Google's own Android Vitals collection is not developer collection |

So the declared set is exactly three types: **Email address**, **Other
user-generated content**, and **Other actions** — all collected, none shared.

**Residual risk on the two exclusions.** Play requires that pseudonymous data be
disclosed where it "can reasonably be re-associated with a user", and a session
token *can* be re-associated by the server that issued it. The counter-argument,
adopted here, is that the token is an authentication secret rather than an
identifier, that the publisher never holds it, and that the identifying element
of the same exchange (email) is already declared — so the listing already tells
users the truth about sign-in. Re-open **D2**/**D3** if any of these change:

- a stable per-user or per-install identifier starts being transmitted, or
- any identifier is used for analytics, profiling, or sent to a third party, or
- Play adds a credential data type to the taxonomy, or
- a developer-operated backend is ever introduced.

### Applying the answers

Play Console supports **Export to CSV / Import from CSV** on the Data safety
form. Filling the CSV offline maps these rows to question IDs (`PSL_EMAIL_ADDRESS`,
`PSL_DATA_USAGE_ONLY_COLLECTED`, `PSL_DATA_USAGE_EPHEMERAL`,
`PSL_DATA_USAGE_USER_CONTROL_REQUIRED`, `PSL_APP_FUNCTIONALITY`,
`PSL_ACCOUNT_MANAGEMENT`, …) and is more reviewable than clicking through the
wizard. Note the importer **overwrites** existing answers.

## 7. Account creation and deletion

The app **cannot create accounts**. `PagelessApi` has exactly one session
endpoint pair, `POST /api/session` (login) and `DELETE /api/session` (logout);
there is no registration call. Accounts are created by whoever administers the
Pageless server.

Consequences:

- Play's in-app account-deletion requirement is aimed at apps that let users
  create an account. It most likely does not apply, but the answer must be
  consistent with the privacy policy and the store listing.
- Server-side data (progress, bookmarks, history) can only be deleted by the
  account owner or server administrator, outside this app.
- On-device data is removed by logout, uninstall, or clear-data — *except* the
  downloaded audio and cover files (**F2**).

**Resolved decision D1** — answer **No** to the deletion-request question. The
publisher receives no user data, so it can neither hold a copy nor action a
deletion request, and it controls no retention window that would let it claim the
90-day automatic-deletion branch. The privacy policy (**pm-a6l.7**) must instead
explain plainly: data lives on the server the user operates and is deleted there
by the account owner or administrator, and logout plus uninstall clears the local
copy.

Answering "Yes" was considered and rejected. A contact form or deletion email
alias would be theatre when there is nothing on the publisher's side to delete,
and an earlier draft of this audit wrongly suggested that adding an in-app
"delete downloaded data" action could justify "Yes". It cannot: local files were
never collected, so they fall outside the question's scope.

### Decision log

| ID | Decision | Date | Rationale |
| --- | --- | --- | --- |
| **D1** | Answer **No** to the deletion-request question | 2026-08-04 | Publisher holds no collected data; local file deletion is out of scope |
| **D2** | Do **not** declare the password | 2026-08-04 | No credential data type exists; "Other info" is demographic; credentials are covered by the Account management purpose on Email address; also ephemeral |
| **D3** | Do **not** declare the token as User IDs | 2026-08-04 | A bearer token is an authentication secret, not an account identifier; unresolvable to a user by the app; never received by the publisher |

## 8. Findings

| ID | Severity | Finding |
| --- | --- | --- |
| ~~**F1**~~ | **Fixed** | `android:allowBackup` was `"true"` with no backup rules, making the bearer token and email in the session DataStore, plus the whole Room DB, eligible for Android Auto Backup to Google's cloud and for device-to-device transfer. Fixing this properly took **three** manifest attributes, not one. `allowBackup="false"` covers API 26–30, but Google's documentation is explicit that from API 31 it disables cloud backup while on some OEM devices **not** disabling D2D transfers, and that legacy `fullBackupContent` rules "don't affect D2D transfers" at all. So the app now also sets `dataExtractionRules=@xml/data_extraction_rules`, which excludes every domain from both `<cloud-backup>` and `<device-transfer>`, plus `fullBackupContent=@xml/backup_rules` as the pre-API-31 counterpart. Cross-platform (iOS) transfer, new in Android 16 QPR2, requires an explicit `<cross-platform-transfer>` element and is deliberately absent. Verified: Lint's `DataExtractionRules` and `FullBackupContent` checks are both clean, the merged manifest carries all three attributes, and `dumpsys package` on a real device reports no `ALLOW_BACKUP` flag. Closed as `pm-a6l.17`. |
| **F2** | Medium | Logout does not delete downloaded content. `AuthRepository.logout()` (`:88-95`) clears the session DataStore and calls `database.clearAllTables()`, but never removes `filesDir/audiobooks/*.m4b` or `filesDir/covers/*`. Because the Room rows are gone, the files become unreachable by the app yet persist until uninstall or clear-data — a privacy wart (another user's audiobooks remain on the device after account switch) and a storage leak. `AudioDownloader.delete` / `CoverCache.delete` already exist and are only wired to per-book removal. |
| **F3** | Low | No in-app "delete my downloaded data" affordance. Originally logged as a Data safety blocker; that was wrong (see **D1**) — local content was never collected, so this does not affect any form answer. It remains a reasonable usability feature for reclaiming storage, subsumed by **F2**. |
| ~~**F4**~~ | **Fixed** | Release `network_security_config.xml` trusted `<certificates src="system" />` only, while its own comment claimed user-added CAs were honoured — so self-hosters terminating TLS with a private CA could not connect at all on a release build. Resolved as `pm-a6l.19` by adding `<certificates src="user" />`, which also covers self-signed certificates installed as user CAs. Cleartext remains forbidden. The accepted trade-off: a CA the device owner installed can intercept the app's traffic, including the bearer token — but installing one is a deliberate action past explicit OS warnings that leaves a standing "network may be monitored" notification, and the users this unblocks otherwise cannot use the app at all. |
| **F5** | Low | The bearer token is stored unencrypted at rest in DataStore, as `SessionStore`'s KDoc already acknowledges. Play does not ask about encryption at rest, but the privacy policy must not overstate protection. Now partially mitigated by F1's fix: the token no longer leaves the device via backup, so the exposure is limited to a rooted or physically compromised device. |

## 9. Consistency requirements for downstream issues

- **pm-a6l.7 (privacy policy)** — source of truth at
  `docs/privacy/privacy-policy.md`, published at <https://pageless.live/privacy>.
  It is deliberately
  *more* detailed than the form: it discloses the password and session token even
  though neither is declared as a Data safety type (decisions **D2**/**D3**),
  states plainly that signing out does **not** remove downloaded audio (**F2**),
  and does not promise encryption at rest (**F5**). Being more transparent than
  the form is the safe direction; the reverse would not be.
- **pm-a6l.9 (store listing)** must not say "no data collected" — see §2.3.
- **pm-a6l.10 (declarations)** must match §6 field for field.
- **Sequencing:** per Play Console Help, apps active *only* on the internal
  testing track are exempt from the Data safety section. The form is therefore
  required before the closed test (**pm-a6l.13**), not before the internal test
  (**pm-a6l.12**) — though completing it earlier is harmless.
- Re-run this audit if a dependency is added, if any new field is added to a
  request body, or if a per-install identifier is introduced.
