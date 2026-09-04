# TO DO widget

A dated to-do list home screen widget for Android, plus a small companion app.
Built with Jetpack Glance (widget), Compose + Material 3 (app), and Room (storage).

## Getting the APK without installing anything

1. Create a new **private** repo on GitHub.
2. Upload every file and folder from this project to it, keeping the structure.
   (Web UI: "Add file" > "Upload files", then drag the whole folder in. Chrome
   preserves subfolders. Make sure hidden folders `.github` and the file
   `debug.keystore` come along.)
3. Commit. Open the **Actions** tab. The `Build APK` workflow starts on its own.
4. When it finishes, open the run and download the `todo-widget-apk` artifact.
5. Unzip on your phone, tap `app-debug.apk`, allow install from unknown sources.

Every later change: edit the file in GitHub, commit, download the new APK.
Because `debug.keystore` is committed, updates install straight over the top
without wiping your to-dos.

## Adding the widget

Long-press the home screen > Widgets > TO DO. Resize by dragging the handles.
Default size is 3x3; anything from 2 cells wide upward works.

## Notes

- Background is transparent by default. If your wallpaper is light, either turn
  on "Dark text" or raise "Background dim" in the app's settings dialog.
- The `‹` `›` arrows move between dates. Tapping "TO DO" opens the app.
- Tapping a checkbox asks "Completed?", then the item leaves the widget and
  appears under Completed in the app. That prompt can be switched off.
