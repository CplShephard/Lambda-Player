# Third-party references and notices

The app's About → Licenses list records the declared direct libraries and identified
code/design sources; it is shared by both UI themes. This file supplements that list.

* **QQMusicDecoder**: the QRC DES variant in `QqQrc.kt` follows WXRIW/QQMusicDecoder.
  Its MIT copyright and permission notice is reproduced in
  [third-party/QQMusicDecoder-MIT.txt](third-party/QQMusicDecoder-MIT.txt).
  Source: <https://github.com/WXRIW/QQMusicDecoder>.
* **Brad Conte's crypto-algorithms**: DES implementation reference; the upstream
  project states that its code is released into the public domain.
  <https://github.com/B-Con/crypto-algorithms>
* **PixelPlayer**: WavySliderExpressive was adapted from the supplied PixelPlayer
  source snapshot. That snapshot includes a GPL version 3 license; see this project's
  root `LICENSE`. Historical code/revision-specific provenance must be verified separately.
* **LiriQo, QQ Music, LRCLIB, lyrics.ovh** are online services, not licenses to use or
  redistribute lyrics. LiriQo can return material attributed to other upstream sources;
  availability, privacy, content rights and service terms depend on each service.
  Its source code license does not license the lyric text.
* Other code and library entries appear in `LicenseData.kt`. Their upstream links are
  provided in the in-app Licenses screens. The declared Gradle dependencies were reviewed
  statically; no resolved dependency graph, bundled binary license report, legal review, or
  complete transitive-license audit was produced. Check each release's actual dependency
  graph, license texts, notices and distribution requirements before shipping.
