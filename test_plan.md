1. **Analyze Fcitx5 Android User Data Import**
   - Investigated `org.fcitx.fcitx5.android.data.UserDataManager.kt`.
   - The user data zip requires a `metadata.json` with the structure:
     ```json
     {
       "packageName": "org.fcitx.fcitx5.android",
       "versionCode": <number>,
       "versionName": "<string>",
       "timestamp": <number>
     }
     ```
   - It extracts directories `shared_prefs/`, `databases/`, `external/`, and `recently_used/`.
   - `external/` maps to the app's external files dir (`getExternalFilesDir(null)`).
   - Rime's user data directory is retrieved in `rimeengine.cpp` using `StandardPaths::global().userDirectory(StandardPathsType::PkgData) / "rime"`.
   - `PkgData` maps to `$FCITX_DATA_HOME`, which is set in `native-lib.cpp` as `extData_ / "data"`.
   - `extData_` is passed from Kotlin as `(getExternalFilesDir(null) ?: filesDir).absolutePath`.
   - Therefore, Rime's user data directory is located at `external/data/rime/` inside the user data zip.

2. **Create Fcitx5 User Data Package Script**
   - Create `frontends/android-ime/rime-package/package-fcitx5-userdata.sh`.
   - This script will structure the zip as follows:
     ```
     metadata.json
     external/
       data/
         rime/
           <contents of shared/rime/>
     ```
   - Generate `metadata.json` with `packageName: "org.fcitx.fcitx5.android"`.

3. **Create Validation Script**
   - Create `tools/validate/validate_fcitx5_userdata_package.py`.
   - Ensure the zip exists, contains `metadata.json`, and the `external/data/rime/` path exists with Rime configs.

4. **Update Documentation**
   - Update `frontends/android-ime/README.md`, `frontends/android-ime/TESTING.md`, `docs/android-import-strategy.md`, `docs/artifact-download.md`, and `README.md` to reflect the new workflow for Trime and Fcitx5 Android.
   - Create `docs/android-phone-only-test.md` with explicit instructions.

5. **Update GitHub Actions**
   - Add the Fcitx5 user data packaging step to `.github/workflows/validate-and-package.yml`.
   - Upload the new artifact.

6. **Complete Pre-Commit Steps**
   - Call `pre_commit_instructions` to ensure testing and checks pass.

7. **Submit Changes**
   - Commit and submit.
