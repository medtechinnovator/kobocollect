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
