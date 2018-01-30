# OverlayHelper
[![Release](https://jitpack.io/v/adriangl/overlayhelper.svg)](https://jitpack.io/#adriangl/overlayhelper)

This utility library aims to help Android developers handle "draw overlays" (also called "draw over other apps") permissions
and queries in a version-agnostic way.

## Installation
Add the following dependencies to your main `build.gradle`:
```groovy
allprojects {
    repositories {
        maven { url "https://jitpack.io" }
    }
}
```

Add the following dependencies to your app's `build.gradle`:

* For Gradle < 4.0
    ```groovy
    dependencies {
        compile "com.github.adriangl:overlayhelper:1.0.0"
    }
    ```

* For Gradle 4.0+
    ```groovy
    dependencies {
        implementation "com.github.adriangl:overlayhelper:1.0.0"
    }
    ```

## Example usage

* Create a new _OverlayHelper_ with a _OverlayPermissionChangedListener_.
* Start watching settings changes with _OverlayHelper.startWatching()_, for example in _onCreate()_.
* Stop watching changes with _OverlayHelper.stopWatching()_ in _onDestroy()_.
* If you want to check if the app has their "draw overlays" permission enabled, use _OverlayHelper.canDrawOverlays()_.
* Call _OverlayHelper.requestDrawOverlaysPermission()_ wherever you want in your activity. You'll have to also add  _OverlayHelper.onRequestDrawOverlaysPermissionResult(int)_ in the activity's _onActivityResult()_, so the helper can retrieve the needed data.
* You'll receive your permission results in the _OverlayPermissionChangedListener_ that you registered when creating the helper.

Check the [example app](app) for more implementation details.

## License
```
Copyright (C) 2018 Adrián García

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
