This is a Kotlin Multiplatform project targeting Android, iOS, Web, Server.

* [/composeApp](./composeApp/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./composeApp/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./composeApp/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./composeApp/src/jvmMain/kotlin)
    folder is the appropriate location.

* [/iosApp](./iosApp/iosApp) contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/server](./server/src/main/kotlin) is for the Ktor server application.

* [/shared](./shared/src) is for the code that will be shared between all targets in the project.
  The most important subfolder is [commonMain](./shared/src/commonMain/kotlin). If preferred, you
  can add code to the platform-specific folders here too.

* [/webApp](./webApp) contains web React application. It uses the Kotlin/JS library produced
  by the [shared](./shared) module.

### Build and Run Android Application

To build and run the development version of the Android app, use the run configuration from the run widget
in your IDE’s toolbar or build it directly from the terminal:
- on macOS/Linux
  ```shell
  ./gradlew :composeApp:assembleDebug
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:assembleDebug
  ```

### Build and Run Server

To build and run the development version of the server, use the run configuration from the run widget
in your IDE’s toolbar or run it directly from the terminal:
- on macOS/Linux
  ```shell
  ./gradlew :server:run
  ```
- on Windows
  ```shell
  .\gradlew.bat :server:run
  ```

### Build and Run Web Application

To build and run the development version of the web app, use the run configuration from the run widget
in your IDE’s toolbar or run it directly from the terminal:
1. Install [Node.js](https://nodejs.org/en/download) (which includes `npm`)
2. Build Kotlin/JS shared code:
   - on macOS/Linux
     ```shell
     ./gradlew :shared:jsBrowserDevelopmentLibraryDistribution
     ```
   - on Windows
     ```shell
     .\gradlew.bat :shared:jsBrowserDevelopmentLibraryDistribution
     ```
3. Build and run the web application
   ```shell
   npm install
   npm run start
   ```

### Build and Run iOS Application

To build and run the development version of the iOS app, use the run configuration from the run widget
in your IDE’s toolbar or open the [/iosApp](./iosApp) directory in Xcode and run it from there.

### Production-like local stack (Docker)

Brings up Postgres, LocalStack S3, and the Ktor server, all wired together via Docker Compose.

```shell
cp .env.prod.example .env.prod
# edit .env.prod — set JWT_SECRET, DATABASE_PASSWORD, GOOGLE_CLIENT_ID
docker compose -f docker-compose.prod.yml --env-file .env.prod up --build
```

The server boots on `http://localhost:${SERVER_PORT:-8081}` and writes media to a LocalStack-hosted
S3 bucket reachable at `http://localhost:4566/thykra-media`. State persists across `up`/`down`
cycles in named Docker volumes (`thykra_postgres_data`, `thykra_localstack_data`); `docker compose
down -v` wipes everything.

For real production, swap `STORAGE_TYPE=s3` to point at a real AWS S3 bucket — leave `S3_ENDPOINT`
unset, drop the `localstack` service, and use the AWS default credential chain (instance role) by
clearing `S3_ACCESS_KEY`/`S3_SECRET_KEY`.

### Headless Compose UI tests

Compose Multiplatform UI behaviour is verified on the JVM via Robolectric — no Android emulator
required. Run locally:

```shell
./gradlew :composeApp:testDebugUnitTest
```

Tests live under `composeApp/src/androidUnitTest/kotlin/`. CI runs these on every PR.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…