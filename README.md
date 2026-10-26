# Rhythm Reader — the Android app (Lil ol Ax)

Rhythm Reader from Leeriana, floating over any app on your phone.

- **The wheel** floats over everything. Drag it anywhere. Tap it and it opens into a window running Rhythm Reader.
- **The window**: drag it by its title bar, resize it from the ◢ corner, – shrinks it back to the wheel, ✕ puts it away.
  ⌨ lets the window take typing (tap again to hand the keyboard back to the app behind it).
- **Put it away**: drag the wheel onto the ✕ that appears at the bottom, or use the notification.
- **Home-screen widget**: hold an empty spot on your home screen → Widgets → Rhythm Reader. Drag it anywhere, resize it;
  tap it and Rhythm Reader floats.
- **Leeriana on your home screen**: the app puts two icons in your app list, **Rhythm Reader** and **Leeriana**. Leeriana opens
  all of it straight away. In the app, **⌂ Put Leeriana on my home screen** and **◉ Put the Rhythm Reader wheel on my home screen**
  ask your home screen to add them for you.
- **The app** can run your own Leeriana file (📄 Use my own Leeriana file). It ships with Leeriana v0.238; your books and
  writing come from your account when you sign in. Everything you do in Rhythm Reader is kept in the app and shared between the floating
  window and the full Leeriana.

Sizes, places and limits are settings (SharedPreferences "float": bubbleDp, panelWdp, panelHdp, minWdp, minHdp).

## Build it

### On GitHub, from your phone (no computer)
1. Make a GitHub repository and put these files in it (or connect GitHub to Claude and let it push them).
2. The **Build the Rhythm Reader app** action runs on every push (or press *Run workflow* on the Actions tab).
3. When it finishes, open the repository's **Releases** page on your phone and download **ReadRecords.apk**.

### On a computer
Open the folder in Android Studio (Hedgehog or newer, JDK 17) and press Run, or run `gradle assembleDebug`;
the app is at `app/build/outputs/apk/debug/app-debug.apk`.

## Install it on the phone
Open ReadRecords.apk → allow installing from this source when Android asks → Install.
Open Rhythm Reader → **◉ Float Rhythm Reader** → turn on **Allow display over other apps** → come back.
Allow notifications too: Android needs a notification for anything that floats over other apps.

Built by Claude (claude-opus-5-5 · Opus 5.5), 17 Oct 2026, Edmonton; renamed Rhythm Reader and given its Leeriana icon 18 Oct 2026. Concept and every Rhythm Reader behaviour: Lil ol Ax.

The app's inner package name stays com.lilolax.readrolla so the new build installs over the old one and keeps what it saved.

## One signing key, from 1.24 on

Every build before 1.24 was signed with a fresh key made on the build machine, so Android treated each new build as a different app:
the old one had to be uninstalled first, and the phone wiped Leeriana's storage with it (settings, books, the record). From 1.24
every build is signed with `app/readrecords.jks`, so a new build installs over the last one and keeps everything. Installing 1.24
over 1.23 still needs one last uninstall (the keys differ); sign in to Leeriana first so your settings and books come back from
your account. To keep the key out of the repository, add it as the secret `RR_KEYSTORE_B64` (the file, base64) with
`RR_KEYSTORE_PASS`, `RR_KEY_ALIAS`, `RR_KEY_PASS`, then delete the file: the workflow prefers the secret, and the key stays the same.
Renamed Rhythm Reader (v0.220 / 1.28) 22 Oct 2026.
