# StringsTranslator

StringsTranslator is a utility for translating string resources in your project to multiple locales with ease.

## Adding StringsTranslator to Your Project

Follow these steps to integrate StringsTranslator into your project:

1. **Initialize the Submodule**
   Open your project repository in the terminal and run the following commands:
   ```bash
   git submodule init
   git submodule add https://github.com/Coursiv/StringsTranslator.git
   ```

2. **Modify `settings.gradle.kts`**
   Open your `settings.gradle.kts` file and add the following line at the bottom:
   ```kotlin
   includeBuild("StringsTranslator")
   ```

3. **Update `gradle/libs.versions.toml`**
   In the `[plugins]` section of your `gradle/libs.versions.toml` file, add:
   ```toml
   strings-translator = { id = "io.zimran.strings-translator" }
   ```

4. **Apply the Plugin in `build.gradle.kts`**
   Inside the `plugins` section of your project's `build.gradle.kts`, add:
   ```kotlin
   alias(libs.plugins.strings.translator) apply true
   ```

5. **Sync the Project**
   Perform a Gradle sync to apply the changes.

6. **Obtain an OpenAI API Key**
   - Contact the OpenAI admin to request an API key. Currently, the admin is [Talgat Abdraimov](https://github.com/talgat-abdraimov).
   - Once you receive the key, add it to your `local.properties` file as follows:
     ```properties
     OPEN_AI_KEY=$YOUR_TOKEN
     ```

7. **Run the Translation Task**
   - After the sync is complete, open the Gradle tasks list.
   - Locate the task `translateStringResources` under `$projectName > tasks > zimran`.
   - Double-click the task to translate your strings into the locales specified in your `app/build.gradle.kts`.

## Cloning or Updating StringsTranslator

When cloning the parent project to a new machine or updating StringsTranslator:
1. Run the following command after cloning your project:
   ```bash
   git submodule update
   ```

## Future Improvements

Here are some planned enhancements for StringsTranslator:

1. **Support for Translating `string-arrays`**
   Extend functionality to handle `string-array` resources.

2. **Multi-Module Support**
   Enable translation for string resources in all modules by:
   - Iterating through all folders containing `build.gradle.kts` files.
   - Identifying and processing their `res` folders for translation.
     
3. **Indonesian locale**
   Take into account that "in" locale is treated as Hindi by ChatGPT and should be replaced with "id". Or provide some other solution that resolves this

4. **Plurals support**


