# Pageless Mobile — Privacy Policy

**Last updated: 23 August 2026**

This policy covers the **Pageless Mobile** Android app (package
`live.pageless.mobile`), the open-source client for Pageless, a self-hosted
audiobook server. It does not cover any particular Pageless *server*; see
[Who is responsible for what](#who-is-responsible-for-what).

## Summary

- Pageless Mobile has **no backend of its own**. We operate no servers that
  receive your data, and we cannot see your library, your listening, or your
  account.
- The app talks to **one server: the one whose address you type in when you sign
  in**. That is normally a server you or someone you trust runs.
- The app **does** send data to that server — your email address at sign-in, and
  your playback progress, bookmarks and listening history while you use it. It is
  not accurate to say nothing leaves your device, so we don't say it.
- **No advertising, no analytics, no tracking, no crash reporting.** There are no
  third-party SDKs of that kind in the app at all.
- Nothing is sold, and nothing is shared with any third party.

## Who is responsible for what

Pageless is self-hosted, so two different parties are involved and it matters
which is which.

**Us — the app publisher.** We write and distribute the Android app. We run no
service that collects your data. We never receive your email address, password,
library, progress, bookmarks or listening history. Removing the app removes our
involvement entirely.

**The server operator — usually you.** Every piece of information the app sends
goes to the Pageless server you configure. That server stores your account and
your listening data, and whoever runs it controls that data, its retention, its
backups and its deletion. If you run your own server, that is you. If someone
else runs it for you, their practices apply, and you should ask them about them.

## What the app sends to your server

Only to the server address you entered, and only over the network:

| Data | When | Why |
| --- | --- | --- |
| Email address and password | When you sign in | To authenticate you. The password is used for that one request and is never stored on your device |
| Session token | On every subsequent request | Proves you are signed in. Issued by your server |
| Device name (manufacturer, model, Android version) | At sign-in and with listening history | So your server can label the session, e.g. in a device list |
| Playback progress | While you listen | So you can resume on another device |
| Bookmarks, including any notes you type | When you create, edit or delete one | So bookmarks follow your account |
| Listening history (play, pause and seek events, positions, timestamps, and the book's title and authors) | While you listen | So your server can show listening statistics |

All of this is sent over **HTTPS**. Release builds of the app refuse to send it
over unencrypted HTTP.

## What the app does *not* do

- **No search terms leave your device.** Searching, filtering and sorting your
  library happen entirely on the device, from data already stored locally.
- **No ads, no analytics, no telemetry, no crash or performance reporting.** No
  advertising identifier, no device identifier such as IMEI, MAC address or
  Android ID, and no third-party analytics, attribution, or crash-reporting
  library is present in the app.
- **No audio is uploaded.** Audiobooks travel one way — downloaded from your
  server to your device.
- **No location, contacts, photos, microphone, camera, calendar, SMS, or
  external-storage access.** The app requests none of those permissions.
- **No account creation, and no third-party sign-in.** The app can only sign in
  to an account that already exists on your server.

## What is stored on your device

All of it in the app's private storage, which other apps cannot read:

- Your server address, email address, first name, and the session token.
- Your display and playback preferences, and your theme choice.
- A local copy of your library — books, chapters, series, collections,
  playlists, and their metadata — so the app works offline.
- Progress, bookmarks and listening history, kept locally until they sync.
- Audiobook files and cover images you explicitly download for offline
  listening.

**Android backup is switched off for this app**, so none of the above —
including the session token — is copied into Google cloud backups or
device-to-device transfers.

One limitation we'd rather state than hide: the session token is stored in the
app's private storage without additional encryption. On a normal Android device
other apps cannot read it, but it is not protected against someone with root
access or physical control of an unlocked device. Signing out removes it.

## Deleting your data

**On your device.** Signing out deletes the session token, your email address and
first name, and the entire local copy of your library, progress, bookmarks and
history. Audiobook files and covers you downloaded are **not** removed by signing
out — remove a download individually from the book screen, or clear the app's
storage in Android settings, or uninstall the app. Uninstalling always removes
everything the app stored.

**On your server.** We cannot delete anything there, because we never have it.
Your account and its listening data live on the server you configured, and only
the account owner or that server's administrator can delete them. If you run the
server, this is entirely under your control.

Because we hold no copy of your data, there is no deletion request for us to
receive or act on.

## Permissions the app requests

| Permission | Why |
| --- | --- |
| Internet, network state | Reach your server, and show whether it is reachable |
| Foreground service (media playback) | Keep playing when the app is in the background |
| Foreground service (data sync) | Finish downloads reliably |
| Notifications | Playback controls on the lock screen and download progress |

## Third-party components

The app is built with open-source libraries — AndroidX and Jetpack Compose,
Kotlin, Media3/ExoPlayer for playback, Room for local storage, OkHttp and
Retrofit for networking, Coil for cover images, and Hilt for dependency
injection. They run inside the app and send nothing anywhere on their own; cover
images are fetched only from your server. None of them is an advertising,
analytics or tracking SDK.

## Children

Pageless Mobile is not directed at children and collects nothing about age.

## Distribution

The app is distributed through Google Play and F-Droid. Those stores' own
practices apply to your interaction with them and are outside our control. Google
Play may also collect information about installs and crashes independently of the
app; see Google's privacy policy.

## Changes to this policy

If the app's data behaviour changes, this page will be updated and the date at
the top will change. Because the app is open source, the underlying behaviour can
always be verified in the source at
<https://github.com/dcaixinha/pageless-mobile>.

This policy needs revisiting whenever the app starts sending a new kind of
information to a server, adds any third-party library that talks to a network,
introduces a persistent device or user identifier, or gains a backend operated by
the publisher.

## Contact

Questions about this policy, bugs and feature requests all belong in the issue
tracker: <https://github.com/dcaixinha/pageless-mobile/issues>.

If you have found a security or privacy defect and would rather not describe it
in public first, report it privately through GitHub:
<https://github.com/dcaixinha/pageless-mobile/security/advisories/new>.

There is no support mailbox, because there is no service behind it. Nothing you
do in the app reaches us, so anything we could help with is either a question
about the app — which belongs in the open, where the next person can find the
answer — or a matter for whoever runs your server.
