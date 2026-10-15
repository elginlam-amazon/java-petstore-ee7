# Java 25 upgrade – summary

## Result
`mvn clean package` builds with zero compilation errors on JDK 25 (Corretto 25.0.4); all 9 unit tests pass. Integration tests (`*IT`) compile and stay skipped by default (`skipITs=true`).

## Changes from the OpenRewrite pass (kept)
- `maven.compiler.source/target` 1.8 -> 25; surefire/failsafe 3.5.6; war plugin 3.5.1; byte-buddy 1.17.8 managed (for Java 25 class files).
- Java language modernisations: unnamed catch variables (`_`), `instanceof` pattern matching, `Locale.of(...)`, `@Serial` on `serialVersionUID`, `IO.println` in `LoginContextProducer`.
- Added `jakarta.inject-api` 1.0.3, `jakarta.xml.bind-api` 2.3.3, `jakarta.annotation-api` 1.3.5, `jaxb-runtime` 2.3.9. These versions still use the `javax.*` packages.

## Manual fix in this cycle
- Changed `jakarta.inject-api`, `jakarta.xml.bind-api` and `jaxb-runtime` to `provided` scope. WildFly (Java EE 7) already supplies these APIs at runtime, and with the recipe's default scopes they were bundled into `WEB-INF/lib`, which can cause classloader conflicts. The WAR now only contains the UI/logging/swagger jars it had before.

## Next steps
- WildFly 26 does not officially support running on Java 25. Check the target runtime JDK, or plan a move to a Jakarta EE 10 server (WildFly 27+), which would need the javax -> jakarta migration.
- `maven-compiler-plugin` is still 3.1 and `maven-dependency-plugin` 2.10. Both work, but they could be bumped (for example to compiler 3.13.x with `<release>25</release>`).
- `IO.println` in `LoginContextProducer` needs JDK 25 at runtime. A logger would be more appropriate.
- The Arquillian ITs were not run (they need a WildFly instance).
