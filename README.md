# java-jar-dependency-test

A deliberately **legacy** Java web application used as a test fixture for SCA / SBOM scanners.

Its dependencies are **committed directly as JAR files** under `Web/WEB-INF/lib/` instead of
being declared in a package manifest. There is **no `pom.xml`**, **no `build.gradle`**, and
**no lockfile of any kind**. The JARs *are* the dependency manifest.

This layout is common in older Ant/Eclipse WTP projects, and it is exactly the shape that
manifest-driven dependency scanning tends to miss.

## Why this repository exists

The purpose is to test whether a security scanner can detect vulnerabilities in:

```text
Web/WEB-INF/lib/*.jar
```

Many scanners discover Java components by parsing dependency manifests (`pom.xml`,
`build.gradle`, `gradle.lockfile`). When a project has none, those scanners report
**zero Java components and zero vulnerabilities** — not because the project is clean, but
because nothing told them where to look. A scanner that inspects JAR files themselves will
instead identify the components below and report their published CVEs.

Concretely, this fixture reproduces a TigerGate issue where `trivy fs` reports no Java
components while `trivy rootfs` identifies the committed JARs.

## Dependencies (committed, intentionally outdated)

| File in `Web/WEB-INF/lib/` | Maven coordinates | How the version is discoverable |
| --- | --- | --- |
| `poi-3.11-beta2.jar` | `org.apache.poi:poi:3.11-beta2` | Filename + `MANIFEST.MF` (`Implementation-Version`) |
| `commons-collections-3.2.1.jar` | `commons-collections:commons-collections:3.2.1` | Embedded `META-INF/maven/.../pom.properties` |
| `commons-lang-2.6.jar` | `commons-lang:commons-lang:2.6` | Embedded `META-INF/maven/.../pom.properties` |
| `commons-codec-1.12.jar` | `commons-codec:commons-codec:1.12` | Embedded `META-INF/maven/.../pom.properties` |
| `jxl.jar` | `net.sourceforge.jexcelapi:jxl:2.6.12` | **Neither** — no `pom.properties`, no version in the filename, no version in `MANIFEST.MF` |

These are unmodified artifacts downloaded from Maven Central. SHA-1 digests:

```text
5b89faba0fd879a6a7eca16e81a47a2fd008738a  poi-3.11-beta2.jar
761ea405b9b37ced573d2df0d1e3a4e0f9edc668  commons-collections-3.2.1.jar
0ce1edb914c94ebc388f086c6827e8bdeec71ac2  commons-lang-2.6.jar
47a28ef1ed31eb182b44e15d49300dee5fadcf6a  commons-codec-1.12.jar
7faf62e0697f7a88954622dfe8c8de33ed142ac7  jxl.jar
```

### The `jxl.jar` case

`jxl.jar` is stored **without a version in its filename**, and the artifact carries no
embedded Maven metadata. Filename heuristics and manifest parsing both fail on it. Identifying
it as `net.sourceforge.jexcelapi:jxl:2.6.12` requires a digest lookup against an artifact index
(e.g. matching the SHA-1 above). It is included specifically to test that fallback path.

Versions were chosen to be old enough to carry published CVEs — notably
`commons-collections:3.2.1` (unsafe deserialization) and `org.apache.poi:poi:3.11-beta2`.
Nothing here should be used as a dependency baseline for real work.

## Project layout

```text
java-jar-dependency-test/
├── src/
│   └── com/example/App.java        # actually calls into all five libraries
├── Web/
│   ├── index.html
│   └── WEB-INF/
│       ├── web.xml
│       └── lib/                    # the dependency source
│           ├── poi-3.11-beta2.jar
│           ├── commons-collections-3.2.1.jar
│           ├── commons-lang-2.6.jar
│           ├── commons-codec-1.12.jar
│           └── jxl.jar
├── build.xml                       # Apache Ant
├── .classpath                      # Eclipse
├── .project                        # Eclipse
├── .gitignore
└── README.md
```

`App.java` is not a dead placeholder — it writes a BIFF8 workbook with POI, reads it back with
JExcelAPI, hashes with commons-codec, and uses the pre-generics commons-collections 3.2.1 and
commons-lang 2.6 APIs. The libraries are genuinely linked and exercised.

Note that `App` is a plain class with a `main` method rather than a servlet. Compiling a servlet
would require adding `servlet-api.jar` to `WEB-INF/lib`, which would put an extra component in
the fixture's dependency set. The five JARs are kept exact so scan results stay easy to compare.

## Building with Ant

Requires a JDK and Apache Ant. No network access is needed — the build resolves nothing and
downloads nothing; `Web/WEB-INF/lib/*.jar` is the entire compile classpath.

```bash
ant classpath   # print the JARs used as the dependency source
ant compile     # compile to build/classes
ant war         # package dist/java-jar-dependency-test.war
ant run         # run com.example.App to prove the libraries are linked
ant clean       # remove build/ and dist/
```

`ant war` produces a WAR with the JARs under `WEB-INF/lib/`, so the same components are present
whether a scanner looks at the source tree or at the packaged artifact.

## Validation

Confirm the JARs are present and that no Maven or Gradle descriptor has crept in:

```bash
find . -name "*.jar"
find . -name "pom.xml" -o -name "build.gradle" -o -name "settings.gradle" -o -name "gradle.lockfile"
```

The first must list five JARs. **The second must return nothing** — if it prints anything, the
fixture has been contaminated and is no longer testing the JAR-only case.

Confirm the JARs are real archives rather than placeholders:

```bash
for j in Web/WEB-INF/lib/*.jar; do unzip -t "$j" >/dev/null && echo "OK   $j"; done
sha1sum Web/WEB-INF/lib/*.jar
unzip -p Web/WEB-INF/lib/commons-lang-2.6.jar 'META-INF/maven/*/*/pom.properties'
```

## Maintaining the fixture

- Do not add `pom.xml`, any Gradle file, or any lockfile.
- Do not rename or upgrade the JARs; expected findings are tied to these exact versions.
- Keep `jxl.jar` named without its version — that is the point of including it.
- `build/` and `dist/` are gitignored. The JARs under `Web/WEB-INF/lib/` are **source**, not
  build output, and must stay committed.
# tigergate-java-ant
