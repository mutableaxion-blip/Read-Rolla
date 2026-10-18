# Read Rolla — the Android app (Lil ol Ax)

Read Rolla from Leeriana, floating over any app on your phone.

- **The wheel** floats over everything. Drag it anywhere. Tap it and it opens into a window running Read Rolla.
- **The window**: drag it by its title bar, resize it from the ◢ corner, – shrinks it back to the wheel, ✕ puts it away.
  ⌨ lets the window take typing (tap again to hand the keyboard back to the app behind it).
- **Put it away**: drag the wheel onto the ✕ that appears at the bottom, or use the notification.
- **Home-screen widget**: hold an empty spot on your home screen → Widgets → Read Rolla. Drag it anywhere, resize it;
  tap it and Read Rolla floats.
- **The app** opens all of Leeriana too, and can run your own Leeriana file (📄 Use my own Leeriana file). It ships with
  Leeriana v0.141 (books included). Everything you do in Read Rolla is kept in the app and shared between the floating
  window and the full Leeriana.

Sizes, places and limits are settings (SharedPreferences "float": bubbleDp, panelWdp, panelHdp, minWdp, minHdp).

## Build it

### On GitHub, from your phone (no computer)
1. Make a GitHub repository and put these files in it (or connect GitHub to Claude and let it push them).
2. The **Build the Read Rolla app** action runs on every push (or press *Run workflow* on the Actions tab).
3. When it finishes, open the repository's **Releases** page on your phone and download **ReadRolla.apk**.

### On a computer
Open the folder in Android Studio (Hedgehog or newer, JDK 17) and press Run, or run `gradle assembleDebug`;
the app is at `app/build/outputs/apk/debug/app-debug.apk`.

## Install it on the phone
Open ReadRolla.apk → allow installing from this source when Android asks → Install.
Open Read Rolla → **◉ Float Read Rolla** → turn on **Allow display over other apps** → come back.
Allow notifications too: Android needs a notification for anything that floats over other apps.

Built by Claude (claude-opus-5-5 · Opus 5.5), 17 Oct 2026, Edmonton. Concept and every Read Rolla behaviour: Lil ol Ax.
