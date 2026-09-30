# tigergate-java-ant

A **legacy Java web application** used as a reproduction fixture for a TigerGate SCA/SBOM gap.

Its dependencies are **committed directly as 41 JAR files** under `Web/WEB-INF/lib/` instead of
being declared in a package manifest. There is **no `pom.xml`**, **no Gradle build script**, and
**no lockfile of any kind**. The JARs *are* the dependency manifest.

This mirrors a real customer repository: an old Ant/Eclipse WTP project where third-party
libraries were downloaded once and checked into version control.

> **Warning**
> Everything vulnerable here is intentional and every credential is fabricated. Nothing in this
> repository authenticates to anything, and none of this code should be copied anywhere.

## The bug being reproduced

TigerGate runs `trivy fs` on the repository. `trivy fs` discovers Java components by parsing
dependency **manifests** (`pom.xml`, `build.gradle`, `gradle.lockfile`). It does **not** open
committed `.jar` files. A project with no manifest therefore produces an empty SBOM and zero
Java vulnerabilities — not because it is clean, but because nothing told the scanner where to look.

```text
41 JAR files exist            Same repository
        |                             |
   No pom.xml                         |
   No Gradle files                    |
        |                             |
     trivy fs                    trivy rootfs
        |                             |
  JARs not inspected           JAR analysis runs
        |                             |
  Empty Java SBOM             41 Maven components
        |                             |
  0 Java vulnerabilities       77 vulnerabilities
```

### Observed results

Measured on Trivy **0.68.1**, vulnerability DB version 2 (updated 2026-09-30), working tree clean
(`ant clean` run first):

| | `trivy fs .` | `trivy rootfs .` |
| --- | --- | --- |
| Java components in CycloneDX SBOM | **0** | **41** |
| Java vulnerabilities | **0** | **77** |
| CRITICAL / HIGH / MEDIUM / LOW | 0 / 0 / 0 / 0 | 11 / 33 / 30 / 3 |
| Vulnerable packages | 0 | 24 |
| Secret findings | 3 | 3 |
| Java result target | *(absent)* | `Java` (type `jar`) |

Both scans read the **same directory**. The only difference is the subcommand.

Critical CVEs that `trivy fs` misses entirely and `trivy rootfs` reports:

```text
CVE-2021-44228  org.apache.logging.log4j:log4j-core@2.14.1   (Log4Shell)
CVE-2021-45046  org.apache.logging.log4j:log4j-core@2.14.1
CVE-2022-42889  org.apache.commons:commons-text@1.9          (Text4Shell)
CVE-2015-7501   commons-collections:commons-collections@3.2.1 (deserialization RCE)
CVE-2016-1000027 org.springframework:spring-web@5.3.18
CVE-2016-1000031 commons-fileupload:commons-fileupload@1.3.1
CVE-2019-17571  log4j:log4j@1.2.17
CVE-2022-23305  log4j:log4j@1.2.17
CVE-2022-23307  log4j:log4j@1.2.17
CVE-2020-10683  dom4j:dom4j@1.6.1
CVE-2021-23926  org.apache.xmlbeans:xmlbeans@2.6.0
```

## Reproduction commands

Run these from the repository root. Run `ant clean` first so `build/` and `dist/` do not add a
WAR that changes the component counts.

```bash
ant clean

# TigerGate-like scan: finds no Java components
trivy fs --quiet --format json . \
  | jq -c '[.Results[]? | {Target, Type, vulns: (.Vulnerabilities|length)}]'

trivy fs --quiet --format cyclonedx . \
  | jq -c '[.components[]? | .purl]'

# Comparison scan: finds the committed JARs
trivy rootfs --quiet --format json . \
  | jq -c '[.Results[]? | {Target, Type, vulns: (.Vulnerabilities|length)}]'

trivy rootfs --quiet --format cyclonedx . \
  | jq -c '[.components[]? | .purl]'
```

Observed output:

```console
$ trivy fs --quiet --format json . | jq -c '[.Results[]? | {Target, Type, vulns: (.Vulnerabilities|length)}]'
[{"Target":"config/application.properties","Type":null,"vulns":0},{"Target":"config/aws-credentials","Type":null,"vulns":0},{"Target":"config/id_rsa","Type":null,"vulns":0}]

$ trivy fs --quiet --format cyclonedx . | jq -c '[.components[]? | .purl]'
[]

$ trivy rootfs --quiet --format json . | jq -c '[.Results[]? | {Target, Type, vulns: (.Vulnerabilities|length)}]'
[{"Target":"Java","Type":"jar","vulns":77},{"Target":"config/application.properties","Type":null,"vulns":0},{"Target":"config/aws-credentials","Type":null,"vulns":0},{"Target":"config/id_rsa","Type":null,"vulns":0}]

$ trivy rootfs --quiet --format cyclonedx . | jq '[.components[]? | .purl] | length'
41
```

Note that `trivy fs` still reports the three planted **secret** findings. Secret scanning walks
files directly, so it is unaffected by the missing manifest. Only the **Java component**
discovery is empty, which is precisely the gap.

## Committed dependencies

All 41 JARs are unmodified artifacts from Maven Central, deliberately including outdated
versions with published CVEs.

| # | JAR file | Maven coordinates |
| --- | --- | --- |
| 1 | `commons-beanutils-1.9.3.jar` | `pkg:maven/commons-beanutils/commons-beanutils@1.9.3` |
| 2 | `commons-codec-1.12.jar` | `pkg:maven/commons-codec/commons-codec@1.12` |
| 3 | `commons-collections-3.2.1.jar` | `pkg:maven/commons-collections/commons-collections@3.2.1` |
| 4 | `commons-compress-1.20.jar` | `pkg:maven/org.apache.commons/commons-compress@1.20` |
| 5 | `commons-dbcp2-2.9.0.jar` | `pkg:maven/org.apache.commons/commons-dbcp2@2.9.0` |
| 6 | `commons-fileupload-1.3.1.jar` | `pkg:maven/commons-fileupload/commons-fileupload@1.3.1` |
| 7 | `commons-io-2.6.jar` | `pkg:maven/commons-io/commons-io@2.6` |
| 8 | `commons-lang-2.6.jar` | `pkg:maven/commons-lang/commons-lang@2.6` |
| 9 | `commons-logging-1.2.jar` | `pkg:maven/commons-logging/commons-logging@1.2` |
| 10 | `commons-pool2-2.12.0.jar` | `pkg:maven/org.apache.commons/commons-pool2@2.12.0` |
| 11 | `commons-text-1.9.jar` | `pkg:maven/org.apache.commons/commons-text@1.9` |
| 12 | `dom4j-1.6.1.jar` | `pkg:maven/dom4j/dom4j@1.6.1` |
| 13 | `gson-2.8.5.jar` | `pkg:maven/com.google.code.gson/gson@2.8.5` |
| 14 | `guava-19.0.jar` | `pkg:maven/com.google.guava/guava@19.0` |
| 15 | `httpclient-4.5.13.jar` | `pkg:maven/org.apache.httpcomponents/httpclient@4.5.13` |
| 16 | `httpcore-4.4.13.jar` | `pkg:maven/org.apache.httpcomponents/httpcore@4.4.13` |
| 17 | `jackson-annotations-2.13.5.jar` | `pkg:maven/com.fasterxml.jackson.core/jackson-annotations@2.13.5` |
| 18 | `jackson-core-2.13.5.jar` | `pkg:maven/com.fasterxml.jackson.core/jackson-core@2.13.5` |
| 19 | `jackson-databind-2.13.5.jar` | `pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.13.5` |
| 20 | `jackson-datatype-jsr310-2.13.5.jar` | `pkg:maven/com.fasterxml.jackson.datatype/jackson-datatype-jsr310@2.13.5` |
| 21 | `jakarta.transaction-api-1.3.1.jar` | `pkg:maven/jakarta.transaction/jakarta.transaction-api@1.3.1` |
| 22 | `javax.servlet-api-3.1.0.jar` | `pkg:maven/javax.servlet/javax.servlet-api@3.1.0` |
| 23 | `jcl-over-slf4j-1.7.36.jar` | `pkg:maven/org.slf4j/jcl-over-slf4j@1.7.36` |
| 24 | `jstl-1.2.jar` | `pkg:maven/javax.servlet/jstl@1.2` |
| 25 | `jxl.jar` | `pkg:maven/net.sourceforge.jexcelapi/jxl@2.6.12` |
| 26 | `log4j-1.2.17.jar` | `pkg:maven/log4j/log4j@1.2.17` |
| 27 | `log4j-api-2.14.1.jar` | `pkg:maven/org.apache.logging.log4j/log4j-api@2.14.1` |
| 28 | `log4j-core-2.14.1.jar` | `pkg:maven/org.apache.logging.log4j/log4j-core@2.14.1` |
| 29 | `lombok.jar` | `pkg:maven/org.projectlombok/lombok@1.18.30` |
| 30 | `mysql-connector-java-5.1.49.jar` | `pkg:maven/mysql/mysql-connector-java@5.1.49` |
| 31 | `poi-3.11-beta2.jar` | `pkg:maven/org.apache.poi/poi@3.11-beta2` |
| 32 | `poi-ooxml-3.11-beta2.jar` | `pkg:maven/org.apache.poi/poi-ooxml@3.11-beta2` |
| 33 | `poi-ooxml-schemas-3.11-beta2.jar` | `pkg:maven/org.apache.poi/poi-ooxml-schemas@3.11-beta2` |
| 34 | `slf4j-api-1.7.36.jar` | `pkg:maven/org.slf4j/slf4j-api@1.7.36` |
| 35 | `snakeyaml-1.30.jar` | `pkg:maven/org.yaml/snakeyaml@1.30` |
| 36 | `spring-beans-5.3.18.jar` | `pkg:maven/org.springframework/spring-beans@5.3.18` |
| 37 | `spring-context-5.3.18.jar` | `pkg:maven/org.springframework/spring-context@5.3.18` |
| 38 | `spring-core-5.3.18.jar` | `pkg:maven/org.springframework/spring-core@5.3.18` |
| 39 | `spring-web-5.3.18.jar` | `pkg:maven/org.springframework/spring-web@5.3.18` |
| 40 | `stax-api-1.0.1.jar` | `pkg:maven/stax/stax-api@1.0.1` |
| 41 | `xmlbeans-2.6.0.jar` | `pkg:maven/org.apache.xmlbeans/xmlbeans@2.6.0` |

### JARs with no version in the filename

Two entries are deliberately stored without a version, because the customer repository does the
same and it is the hardest case for JAR identification:

| File | Real coordinates | `pom.properties`? | Version in filename? |
| --- | --- | --- | --- |
| `jxl.jar` | `net.sourceforge.jexcelapi:jxl:2.6.12` | no | no |
| `lombok.jar` | `org.projectlombok:lombok:1.18.30` | no | no |

Neither filename heuristics nor manifest parsing can identify these. Trivy resolves them by
hashing the archive and looking the digest up against Maven Central, which requires network
access — in an air-gapped scan expect both to drop out of the `rootfs` results.

## Project layout

```text
tigergate-java-ant/
├── src/com/example/
│   ├── App.java                    # POI, JExcelAPI, Commons Codec/Lang/Collections
│   ├── ReportServlet.java          # Servlet API, Log4j, Commons IO, Jackson
│   ├── VulnerableDao.java          # SAST: SQL injection, hardcoded DB credentials
│   ├── InsecureCrypto.java         # SAST: MD5/SHA-1, DES & AES-ECB, java.util.Random
│   ├── UnsafeIO.java               # SAST: command injection, path traversal, XXE, deserialization
│   └── ApiClient.java              # SAST + secrets: hardcoded tokens, cleartext HTTP
├── config/                         # secrets fixture
│   ├── application.properties
│   ├── aws-credentials
│   └── id_rsa
├── Web/
│   ├── index.html
│   └── WEB-INF/
│       ├── web.xml
│       └── lib/                    # 41 committed JARs - the dependency source
├── build.xml                       # Apache Ant
├── .classpath                      # Eclipse, lists all 41 JARs
├── .project                        # Eclipse
├── .gitignore
└── README.md
```

## Building with Ant

Requires a JDK and Apache Ant. **No network access is needed** — the build resolves nothing and
downloads nothing. `Web/WEB-INF/lib/*.jar` is the entire compile classpath.

```bash
ant clean       # remove build/ and dist/
ant classpath   # print the JARs used as the dependency source
ant compile     # compile to build/classes
ant war         # package dist/java-jar-dependency-test.war
ant run         # run com.example.App to prove the libraries are linked
ant             # default: compile + war
```

`ant war` packages the JARs into `WEB-INF/lib/` of the WAR, so the same components are present
whether a scanner looks at the source tree or the packaged artifact.

## Validation

```bash
# Build descriptors present - should list only build.xml and .classpath
find . -name .git -prune -o \( -name pom.xml -o -name "build.gradle*" \
  -o -name "settings.gradle*" -o -name "*.lockfile" -o -name build.xml \
  -o -name ivy.xml -o -name .classpath \) -print

# Must return NOTHING
find . -name .git -prune -o \( -name "pom.xml" -o -name "build.gradle" \
  -o -name "build.gradle.kts" -o -name "settings.gradle" \
  -o -name "settings.gradle.kts" -o -name "gradle.lockfile" \
  -o -name "gradlew" -o -name "gradlew.bat" \) -print

# Must print 41
find . -name .git -prune -o \( -name "*.jar" -o -name "*.war" -o -name "*.ear" \) -print | wc -l

# All must be valid archives
for j in Web/WEB-INF/lib/*.jar; do unzip -t "$j" >/dev/null && echo "OK   $j"; done
```

If the second command prints anything, the fixture is contaminated and no longer tests the
JAR-only case.

## Planted SAST findings

All in `src/com/example/`, all compiling against the committed JARs.

| Weakness | CWE | Location |
| --- | --- | --- |
| SQL injection (`SELECT` and `UPDATE`) | CWE-89 | `VulnerableDao` |
| Hardcoded database credentials | CWE-798 | `VulnerableDao` |
| Credentials written to stdout | CWE-532 | `VulnerableDao`, `ApiClient` |
| OS command injection | CWE-78 | `UnsafeIO.convertUpload`, `archiveRegion` |
| Path traversal | CWE-22 | `UnsafeIO.readUpload` |
| XXE | CWE-611 | `UnsafeIO.parseConfig` |
| Unsafe Java deserialization | CWE-502 | `UnsafeIO.loadSession` |
| TLS verification disabled | CWE-295 | `UnsafeIO.trustEverything` |
| Insecure temp file | CWE-377 / CWE-732 | `UnsafeIO.stageExport` |
| MD5 / SHA-1 password hashing | CWE-327 / CWE-916 | `InsecureCrypto` |
| DES and AES-ECB, hardcoded key | CWE-327 / CWE-321 | `InsecureCrypto` |
| `java.util.Random` for tokens | CWE-330 / CWE-338 | `InsecureCrypto.newSessionToken` |
| Hardcoded API tokens, creds in URL | CWE-798 | `ApiClient` |
| Cleartext HTTP for authenticated calls | CWE-319 | `ApiClient` |

`UnsafeIO.loadSession` is the cross-scanner case: a CWE-502 SAST finding that is genuinely
exploitable *because* SCA-visible `commons-collections:3.2.1` supplies a published gadget chain.
A scanner that surfaces both should correlate them.

## Planted secrets

Fabricated credentials live in `config/` and inline in Java source, because some scanners only
walk config files and some only walk source.

| Secret type | Location |
| --- | --- |
| AWS access key ID + secret (2 profiles) | `config/aws-credentials`, `config/application.properties` |
| RSA private key, PKCS#1 PEM | `config/id_rsa` |
| GitHub PAT, Slack token, Stripe key, SendGrid key, Twilio token | `config/application.properties`, `ApiClient` |
| JDBC / LDAP / service-account passwords | `config/application.properties`, `VulnerableDao` |
| JWT, HTTP Basic credentials in a URL | `ApiClient` |

`config/id_rsa` is a throwaway key generated for this fixture and used by nothing.

### Pushing to a hosted forge

These patterns are designed to be detected, so **GitHub push protection will block the first
push** and secret scanning will open alerts. That is the fixture working as intended. Allow each
flagged secret through the unblock URL GitHub prints, or keep the repository private.

## Maintaining the fixture

- Do not add `pom.xml`, any Gradle file, or any lockfile. That is what makes the bug reproduce.
- Do not add custom scanning scripts or SBOM files to make SCA work; this repo only demonstrates
  the existing behaviour.
- Do not upgrade the JARs; the expected findings are tied to these exact versions.
- Keep `jxl.jar` and `lombok.jar` named without versions.
- Do not "fix" `config/` or the four vulnerable classes; they are the expected SAST and secret findings.
- `build/` and `dist/` are gitignored. The JARs under `Web/WEB-INF/lib/` are **source**, not build
  output, and must stay committed.
