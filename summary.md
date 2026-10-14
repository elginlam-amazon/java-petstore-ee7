# Java 25 upgrade – build verification

## Result
`mvn clean package` succeeds on JDK 25 (Corretto 25.0.4): 68 main + 34 test sources compile with `--release`/target 25, 9 unit tests pass, `applicationPetstore.war` is produced. No compilation errors had to be fixed after the OpenRewrite pass.

## Changes made this cycle
- `pom.xml`: the recipe-added `jakarta.inject-api` 1.0.3, `jakarta.xml.bind-api` 2.3.3 (both still `javax.*` namespace) and `jaxb-runtime` 2.3.9 were changed to `provided` scope. WildFly already ships these APIs (they are also covered by `jboss-javaee-7.0`), so bundling them in `WEB-INF/lib` risked classloader conflicts at deploy time. The WAR now contains only the original application libraries.

## OpenRewrite changes (kept as-is)
- Compiler source/target 1.8 -> 25; surefire/failsafe 3.5.6, war plugin 3.5.1; Byte Buddy pinned to 1.17.8 for tests.
- Unnamed catch variables (`_`), pattern-matching `instanceof`, `Locale.of`, `@Serial`, `Integer/Float.valueOf` in ITs, `IO.println` in `LoginContextProducer`.
- The code stays on `javax.*` (Java EE 7); no jakarta namespace migration.

## Next steps
- Runtime target: the README targets WildFly 10–26. WildFly 26 does not officially support JDK 25, and Java 25 bytecode (class version 69) cannot run on an older JVM. Deploying needs a server that runs on JDK 25, which for the WildFly line means WildFly 27+ / Jakarta EE 10 (a jakarta namespace migration). Alternatively, lower `maven.compiler.release` to the JDK the server actually runs on.
- `IO.println` (java.lang.IO, final in JDK 25) is fine for compilation, but consider replacing it with the project's logger.
- Arquillian integration tests (`-Parquillian-wildfly-*`) were not run because they need a WildFly instance.
- `version.maven.compiler.plugin` (3.1) is declared but not bound. Maven 3.9's default compiler plugin is used, and it works with JDK 25.
