# BlissLauncher3

**BlissLauncher3** is a modified version of the AOSP [Launcher3](https://android.googlesource.com/platform/packages/apps/Launcher3), with features inspired from [BlissLauncher v1](https://gitlab.e.foundation/e/os/BlissLauncher/-/tree/master).

It is the default launcher for [/e/ OS](https://e.foundation/e-os/).

## Building

- Clone the repository

  ```git
  git clone --recurse-submodules https://gitlab.e.foundation/e/os/BlissLauncher3 -b v1-s BlissLauncher3s
  ```

- To build this project, we need the following jars inside the `prebuilts` folder from our AOSP build directory:

  - **framework-13.jar**: `out/soong/.intermediates/frameworks/base/framework/android_common/turbine-combined/framework.jar`

  - We need to build the `Launcher3QuickStepLib` module using `m Launcher3QuickStepLib` and then copy the jar from the following directory:

    - **classes.jar**: `out/target/common/obj/JAVA_LIBRARIES/Launcher3QuickStepLib_intermediates/classes.jar`

- Now we need to run two tasks to generate the proper jars:

  ```bash
  # Unzip the classes.jar file
  ./gradlew unzipJar

  # Generate the final jar for Quickstep
  ./gradlew makeReleaseJar
  ```

- Launch Android Studio and Import the project

- Build the project through the IDE or run the following command:

  ```bash
  ./gradlew assembleBlissWithQuickstepDebug
  ```

## Installing

- Below conditions are required to install the app:

  - User should be on **Android 13** /e/OS or LineageOS
  - Rom should be signed with **test keys**

- Download and install the APK like any other normal app from the [pipeline](https://gitlab.e.foundation/e/os/BlissLauncher/-/pipelines/latest?ref=1021-launcher3-rewrite)

- Download and install the icon mask like normal app [SquircleMask.apk](https://gitlab.e.foundation/internal/wiki/-/wikis/uploads/320461a58f097993b29772abe0d2b0b9/KGLN4.apk)

- Go to _Settings > Apps > Default apps > Launcher_ and change launcher to `Blisslauncher` (with green icon)

- It will open a page about `Usage access`. Allow the new BlissLauncher (`Permit usage access`)

- Go to Settings > Display > Icon Shape > Select **Squircle**

- Clear the data of Blisslauncher3 manually through settings or run the command through adb:

  ```bash
  adb shell pm clear com.android.launcher3
  ```

- Reboot

- Now it is totally ready to use and play around with!

## Recurrent task
### Publish a new release for update through app lounge
For this, you need to create a protected tag
which follow this format: `v1.2.3-s`, `v1.2.3-t`

The Android letter is important to let app lounge know which release to use!

also note, that tag with `v*` format will all be protected.


### When a new android version is supported
#### Update .gitlab-ci.yml
When a new Android version is supported, we need to add the gitlab-ci's job that make release to update it through app lounge

1. Add Ci variables:
```
  APK_PATH: "build/outputs/apk/blissWithQuickstep/release"
  UNSIGNED_APK: "BlissLauncher-apiT-release.apk"
  COMMUNITY_APK: "BlissLauncher-apiT-community.apk"
  OFFICIAL_APK: "BlissLauncher-apiT-official.apk.apk"
``` 
but you need to adapt the letter after the api work for `UNSIGNED_APK`, `COMMUNITY_APK`, `OFFICIAL_APK`.

2. Add stage `gitlab_release` in `stages` entry

3. Copy paste jobs from one of a branch for a supported Android version : `init_submodules`, `generate-apks`, `create-json-files` and `create-release`

## License

```text
Copyright © MURENA SAS 2023.

All rights reserved. This program and the accompanying materials
are made available under the terms of the GNU Public License v3.0
which accompanies this distribution, and is available at
http://www.gnu.org/licenses/gpl.html
```
