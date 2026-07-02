# BlockKo

A privacy-first, DNS-based ad blocker for Android. BlockKo blocks ads and trackers system-wide — across every installed app and browser — using a local `VpnService`. No root required, no external VPN servers: all traffic and filtering stays on-device.

## How it works

BlockKo establishes a local TUN interface via `VpnService` and routes only DNS traffic (UDP port 53, plus a handful of well-known public resolver IPs some apps hardcode) into the tunnel. Every DNS query is checked against an in-memory `HashSet` blocklist:

- If the domain (or a parent domain) is blocked, BlockKo answers immediately with `NXDOMAIN` — the query never leaves the device.
- Otherwise the query is forwarded to your chosen upstream resolver (Cloudflare, Google, Quad9, or a custom IP) and the response is relayed back.

Everything else — actual app traffic to real IPs — bypasses the VPN entirely and flows over your normal connection.

## Features

- Bundled offline starter blocklist (StevenBlack hosts format, ~83k domains), with in-app "Update Blocklist" from any hosts-file URL
- Custom block/allow rules with an allowlist override
- Per-app exclusions (e.g. banking apps) via `addDisallowedApplication`
- Quick Settings tile + notification Pause 5 min / Resume
- Stats dashboard: blocked today/all-time, queries today, 7-day chart, top blocked domains
- English / Taglish in-app language toggle
- Honest limitations screen (Settings → About) — no ambiguity about what DNS filtering can and can't block

## Tech stack

Kotlin, Jetpack Compose (Material 3, dynamic color), MVVM + StateFlow, Room, DataStore, Vico charts. Min SDK 26.

## Building

```
./gradlew assembleDebug
```

Tagging a commit `vX.Y.Z` and pushing the tag triggers `.github/workflows/build-release.yml`, which builds the debug APK, renames it to `BlockKo-vX.Y.Z.apk`, and publishes it as a GitHub Release asset. The workflow also supports manual `workflow_dispatch` runs.

## Permissions

`INTERNET`, `FOREGROUND_SERVICE`, `POST_NOTIFICATIONS`, `BIND_VPN_SERVICE`, and `RECEIVE_BOOT_COMPLETED` (only used if auto-start-on-boot is enabled). Nothing else — no location, contacts, or storage access. No analytics, crash reporters, or third-party tracking SDKs are bundled.
