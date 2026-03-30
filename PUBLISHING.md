# Publishing to Maven Central

This guide describes the one-time setup and the release process for publishing
the MAX Bot API Java library to Maven Central via the
[Central Portal](https://central.sonatype.com).

---

## Prerequisites

### 1. Create a Maven Central Account

1. Go to [central.sonatype.com](https://central.sonatype.com) and sign in
   (or create a new account).
2. Navigate to **Namespaces** and register the `ru.etsft.max` namespace.
3. Verify namespace ownership by one of:
   - **DNS TXT record**: Add a TXT record to the `etsft.ru` domain with the
     verification code provided by the portal.
   - **GitHub**: If using a `io.github.*` namespace, create a temporary
     verification repo.

### 2. Generate a GPG Signing Key

Maven Central requires all artifacts to be signed with GPG/PGP.

```bash
# Generate a new GPG key pair (use RSA, 4096 bits)
gpg --full-generate-key

# List your keys — note the key ID (last 8 characters of the fingerprint)
gpg --list-keys --keyid-format short

# Upload your public key to a keyserver
gpg --keyserver keyserver.ubuntu.com --send-keys <YOUR_KEY_ID>

# Also upload to other keyservers for redundancy
gpg --keyserver keys.openpgp.org --send-keys <YOUR_KEY_ID>
```

Save the following values — you will need them for signing:
- **Key ID** (last 8 hex characters of the fingerprint)
- **Key passphrase** (the password you set during key generation)
- **Secret key ring** file location, or export the key for CI:

```bash
# Export the secret key in ASCII armor (for CI environments)
gpg --export-secret-keys --armor <YOUR_KEY_ID> > secret-key.gpg
```

### 3. Generate Maven Central API Token

1. Go to [central.sonatype.com](https://central.sonatype.com) → **Account** →
   **Generate User Token**.
2. Save the **username** and **password** values (these are the API tokens,
   not your login credentials).

---

## Local Configuration

Create or edit `~/.gradle/gradle.properties` (NOT the project's
`gradle.properties`) to add your secrets:

```properties
# Maven Central credentials (user tokens from the Central Portal)
mavenCentralUsername=<your-token-username>
mavenCentralPassword=<your-token-password>

# GPG signing — option A: key ring file (local development)
signing.keyId=<last-8-hex-chars>
signing.password=<your-key-passphrase>
signing.secretKeyRingFile=/home/<you>/.gnupg/secring.gpg

# GPG signing — option B: in-memory key (CI environments)
# signingInMemoryKeyId=<last-8-hex-chars>
# signingInMemoryKeyPassword=<your-key-passphrase>
# signingInMemoryKey=<ascii-armored-secret-key>
```

> **Important**: Never commit these secrets to version control.

### GPG Key Ring Note

Modern GPG versions (2.1+) don't create `secring.gpg` by default. Export it:

```bash
gpg --export-secret-keys -o ~/.gnupg/secring.gpg
```

---

## Building and Publishing

### Step 1: Set the Release Version

Edit `gradle.properties` in the project root and remove the `` suffix:

```properties
version=0.1.0
```

### Step 2: Build and Publish to Local Staging

```bash
./gradlew clean build publishAllPublicationsToStagingRepository
```

This will:
- Compile all modules
- Run tests and quality gates (checkstyle, spotbugs, jacoco)
- Generate sources and javadoc JARs
- Sign all artifacts with GPG
- Write everything to `build/staging-deploy/`

### Step 3: Verify the Staging Output

Inspect the staging directory to confirm all artifacts are present:

```bash
find build/staging-deploy -type f | sort
```

Each module should have:
- `<artifactId>-<version>.jar`
- `<artifactId>-<version>-sources.jar`
- `<artifactId>-<version>-javadoc.jar`
- `<artifactId>-<version>.pom`
- `.asc` signature for each file above
- `.md5` and `.sha1` checksums for each file above

### Step 4: Create the Upload Bundle

```bash
cd build/staging-deploy
zip -r ../max-bot-api-java-0.1.0.zip ru
```

### Step 5: Upload to Maven Central

1. Go to [central.sonatype.com/publishing](https://central.sonatype.com/publishing).
2. Click **Publish Component**.
3. Upload the ZIP bundle.
4. Set a deployment name (e.g., `max-bot-api-java 0.1.0`).
5. Click **Publish Component**.
6. Wait for validation (this may take several minutes).
7. Once validated, click **Publish** to release to Maven Central.

> Artifacts typically appear in Maven Central within 10–30 minutes after
> publishing.

### Step 6: Post-Release

1. Commit the version change and tag the release:

```bash
git tag -a v0.1.0 -m "Release 0.1.0"
git push origin v0.1.0
```

2. Bump the version to the next SNAPSHOT:

```properties
version=0.2.0
```

3. Commit the SNAPSHOT version bump.

---

## CI/CD Integration (GitLab CI)

For automated publishing from GitLab CI, add these **CI/CD variables**
(Settings → CI/CD → Variables, masked and protected):

| Variable | Description |
|---|---|
| `ORG_GRADLE_PROJECT_signingInMemoryKeyId` | GPG key ID (last 8 hex chars) |
| `ORG_GRADLE_PROJECT_signingInMemoryKeyPassword` | GPG key passphrase |
| `ORG_GRADLE_PROJECT_signingInMemoryKey` | ASCII-armored secret key |
| `ORG_GRADLE_PROJECT_mavenCentralUsername` | Central Portal token username |
| `ORG_GRADLE_PROJECT_mavenCentralPassword` | Central Portal token password |

Example `.gitlab-ci.yml` job:

```yaml
publish:
  stage: deploy
  image: eclipse-temurin:21-jdk
  only:
    - tags
  script:
    - ./gradlew clean build publishAllPublicationsToStagingRepository
    - cd build/staging-deploy
    - zip -r ../bundle.zip ru
    # Upload via Central Portal API
    - |
      curl -X POST "https://central.sonatype.com/api/v1/publisher/upload" \
        -H "Authorization: Bearer ${MAVEN_CENTRAL_TOKEN}" \
        -F "bundle=@../bundle.zip" \
        -F "name=max-bot-api-java-$(git describe --tags)" \
        -F "publishingType=USER_MANAGED"
```

> **Note**: The `MAVEN_CENTRAL_TOKEN` can be generated from the Central Portal
> API settings. See
> [Central Portal API docs](https://central.sonatype.org/publish/publish-portal-api/)
> for details.

---

## Module Artifacts

The following modules are published to Maven Central:

| Module | Artifact ID | Description |
|---|---|---|
| `max-bot-api-core` | `max-bot-api-core` | Models and interfaces (zero dependencies) |
| `max-bot-api-client` | `max-bot-api-client` | HTTP transport and MaxClient |
| `max-bot-api-jackson` | `max-bot-api-jackson` | Jackson serialization adapter |
| `max-bot-api-gson` | `max-bot-api-gson` | Gson serialization adapter |
| `max-bot-api-webhook` | `max-bot-api-webhook` | Webhook server |
| `max-bot-api-longpolling` | `max-bot-api-longpolling` | Long polling consumer |
| `max-bot-api-test-support` | `max-bot-api-test-support` | WireMock test utilities |
| `max-bot-api-spring-boot` | `max-bot-api-spring-boot` | Spring Boot auto-configuration |

The `max-bot-api-examples` module is **not** published.

---

## Troubleshooting

### "Signing is required but no key is configured"

Ensure `signing.keyId` is set in `~/.gradle/gradle.properties` or the
`signingInMemoryKey` environment variable is set.

### "Namespace not verified"

Complete the namespace verification on the Central Portal before uploading.

### "POM validation failed"

Run locally to inspect the generated POM:

```bash
./gradlew :max-bot-api-core:generatePomFileForMavenJavaPublication
cat max-bot-api-core/build/publications/mavenJava/pom-default.xml
```

Ensure it contains: name, description, url, license, developer, and scm sections.

### GPG key not found on keyserver

It may take a few minutes for keys to propagate. Try uploading to multiple
keyservers:

```bash
gpg --keyserver keyserver.ubuntu.com --send-keys <YOUR_KEY_ID>
gpg --keyserver keys.openpgp.org --send-keys <YOUR_KEY_ID>
```
