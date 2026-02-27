# KoboCollect
![Platform](https://img.shields.io/badge/platform-Android-blue.svg)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

KoboCollect is part of [KoBoToolbox](https://www.kobotoolbox.org/) and based on [ODK Collect](https://github.com/getodk/collect).

## Local setup (macOS)

1. Install Java 17:

```
brew install openjdk@17
```

2. Install Android Studio:

```
brew install --cask android-studio
```

3. Set `JAVA_HOME` (zsh) and verify:

```
echo 'export JAVA_HOME="$(/usr/libexec/java_home -v 17)"' >> ~/.zshrc
source ~/.zshrc
java -version
```

If `java -version` fails, point `JAVA_HOME` to the Homebrew path:

```
echo 'export JAVA_HOME="/opt/homebrew/opt/openjdk@17"' >> ~/.zshrc
echo 'export PATH="$JAVA_HOME/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
java -version
```

4. Open Android Studio and install required SDKs if prompted.

5. Verify Gradle build:

```
./gradlew :collect_app:assembleDebug
```

6. Optional: add `secrets.properties` in the repo root with:

```
MAPBOX_DOWNLOADS_TOKEN=your_token_here
```

This is used to download Mapbox artifacts (see `secrets.gradle`).

## Packaging and distribution

### Building an APK for the team

**Option A: Self-signed release (no keystore setup)**  
Best for internal distribution when you don’t have a release keystore. Uses your machine’s default Android debug keystore (`~/.android/debug.keystore`, created by Android Studio/SDK), or a `debug.keystore` in the project root if you add one.

```bash
./gradlew :collect_app:assembleSelfSignedRelease
```

APK output: `collect_app/build/outputs/apk/selfSignedRelease/ODK-Collect-<versionName>.apk`

**Option B: Signed release (for production or Play Store)**  
Requires a `secrets.properties` in the repo root with:

```properties
RELEASE_STORE_FILE=path/to/your/keystore.jks
RELEASE_STORE_PASSWORD=...
RELEASE_KEY_ALIAS=...
RELEASE_KEY_PASSWORD=...
```

Then:

```bash
./gradlew :collect_app:assembleRelease
# or, to match the existing release script:
./gradlew :collect_app:assembleOdkCollectRelease
```

APK output: `collect_app/build/outputs/apk/release/` or `.../odkCollectRelease/`.

**Bumping the version**  
Edit `collect_app/build.gradle`: update `versionCode` (integer, must increase for each release) and ensure `versionName` from `getVersionName()` (or override) is what you want. Then share the single APK (e.g. via link or distribution channel below).

### Distributing so tablets receive updates

| Approach | Auto-updates? | Effort | Best for |
|----------|----------------|--------|-----------|
| **Manual APK** (link to file) | No – users re-download and install | Low | Quick one-off handoff |
| **Firebase App Distribution** | Yes – testers get notified of new builds and install from the same channel | Low | Internal team; no store |
| **Google Play (internal/closed testing)** | Yes – like normal app updates | Medium | Teams comfortable with Play Console |
| **MDM (Intune, Workspace One, etc.)** | Yes – you push and update the app | Medium–High | Enterprise-managed tablets |

**Recommended for “everyone gets updates”:**

1. **Firebase App Distribution** (free, no store)
   - Add the [Firebase App Distribution](https://firebase.google.com/docs/app-distribution) Gradle plugin and upload each new APK (or wire a CI step).
   - Invite testers by email; they install once, then get notifications and one-tap install for new builds. All tablets on the list receive updates when you publish a new build.

2. **Google Play internal or closed testing**
   - Create an app in [Google Play Console](https://play.google.com/console), upload the first AAB/APK, then add testers (email list or internal).
   - New versions you upload are offered as updates to all testers. Requires a developer account and some setup.

3. **MDM**
   - If your organization already manages tablets with Intune, VMware Workspace One, etc., you can deploy and update the app through the MDM. Build a signed release APK (Option B above) and distribute via your MDM’s app catalog.

The app does **not** currently check for its own APK updates (it only has form/schema auto-update). So for “everyone receives updates,” use one of the channels above rather than relying on in-app update checks.
