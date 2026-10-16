# Java 25 upgrade summary

## Result
- `mvn clean package` succeeds on JDK 25 (Corretto 25.0.4) with zero compilation errors; classes are emitted at bytecode level 69 (Java 25).
- 9/9 unit tests pass (`*Test`); Arquillian integration tests (`*IT`) compile but are skipped by default (`skipITs=true`).
- No manual code fixes were needed after the OpenRewrite pass.

## Changes applied by OpenRewrite (reviewed, kept)
- `pom.xml`: `maven.compiler.source/target` 1.8 -> 25; war plugin 3.5.1; surefire/failsafe 3.5.6; byte-buddy 1.17.8 managed (Java 21+ class-file support for Mockito/Arquillian).
- Added javax-namespace APIs removed from the JDK: `jakarta.xml.bind-api` 2.3.3 + `jaxb-runtime` 2.3.9, `jakarta.annotation-api` 1.3.5, `jakarta.inject-api` 1.0.3 (all still `javax.*` packages, matching Java EE 7).
- Source modernisation: unnamed catch variables (`_`), `instanceof` pattern matching, `Locale.of(...)`, `IO.println(...)` in `LoginContextProducer`.

## Next steps
- Runtime: the app targets Java EE 7 on WildFly 10–26. WildFly 26 is not certified on Java 25; verify deployment on a JDK 25 runtime, or plan a Jakarta EE 10 / WildFly 27+ migration (javax -> jakarta) as a separate step.
- `IO.println` in `LoginContextProducer` requires a Java 25 runtime; consider replacing it with the existing logging (log4j) if the runtime JDK may be older.
- `maven-compiler-plugin` is still 3.1 (works, but old); consider upgrading to a 3.13+ version and using `maven.compiler.release`.
- Run Arquillian ITs (`-Parquillian-wildfly-managed`) against the target server to validate end-to-end behaviour.
