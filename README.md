# Not Sorry

A tiny Android app that replaces the word "sorry" with a random food or animal as you type, in any app. It uses an AccessibilityService, the same way Texpand works.

Edit the banned word and the replacement list at the top of `app/src/main/java/com/notsorry/ReplaceService.kt`.

## Build and install

1. Push this repo to GitHub.
   ```
   git init
   git add .
   git commit -m "not sorry"
   git branch -M main
   git remote add origin https://github.com/<you>/not-sorry.git
   git push -u origin main
   ```
2. On GitHub, open the **Actions** tab. Wait for the **build** workflow to finish.
3. Open the finished run. Under **Artifacts**, download **app-debug**. Unzip it. You get `app-debug.apk`.
4. Copy the APK to your phone (USB, Google Drive, email to yourself, or download it straight from GitHub in the phone browser).
5. Open the APK from the Files app or your browser's downloads.
6. Android will say your browser or Files app is not allowed to install unknown apps. Tap **Settings**, turn on **Allow from this source**, then go back and tap **Install**.
   You can also find this under Settings > Apps > Special app access > Install unknown apps.

## Turn the service on

1. Open the **Not Sorry** app and tap the button. It opens the Accessibility settings page.
2. Find **Not Sorry** in the list (often under **Downloaded apps** or **Installed apps**) and tap it.
3. Turn the toggle on and confirm.

On Android 13 and newer the toggle is greyed out for sideloaded apps and says "Restricted setting". Do this first:

1. Long press the **Not Sorry** app icon and tap **App info** (or go to Settings > Apps > Not Sorry).
2. Tap the three-dot menu in the top right.
3. Tap **Allow restricted settings** and confirm.
4. Go back to Accessibility settings and turn the toggle on.

Android may turn the service off after an update or reinstall. Repeat these steps if replacement stops.

## Where it will not work

- **Apps that block accessibility text edits.** Some apps set a text field to not accept `ACTION_SET_TEXT`, or use a custom text view that does not report itself as editable. Nothing is replaced there.
- **Password fields and secure screens.** Banking apps, password managers, and any screen flagged as secure hide their content from accessibility services, so the service never sees the text.
- **Apps with custom editors.** Apps that draw their own text (many code editors, some note apps, games, and canvas-based web apps) do not use a standard Android text field, so there is nothing to read or rewrite.
- **Web pages inside browsers.** Text fields in Chrome and other browsers sometimes fire the events and sometimes do not, and setting text can reset the cursor or break in-page autocomplete. Results vary by site.
- **Rich text or formatted fields.** `ACTION_SET_TEXT` writes plain text. In an editor with bold, links, or inline images, the formatting can be lost when the field is rewritten.
- **Keyboard suggestion strips and search bars in launchers.** These are often not real editable fields, so no event is sent.
- **Voice typing.** Speech input inserts whole phrases at once. The word is replaced only if the inserted text ends with a space or punctuation after "sorry".
