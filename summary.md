# Java 25 upgrade – build verification

## Result
`mvn clean verify` on JDK 25 (Corretto 25.0.4): BUILD SUCCESS, 0 compilation errors, 9/9 unit tests pass.
Integration tests (`*IT`) are skipped by default (`skipITs=true`) and need a WildFly instance (`-Parquillian-wildfly-*`).

No further code or POM changes were required in this cycle. The OpenRewrite pass already:
- set `maven.compiler.source/target` to 25 and pinned byte-buddy 1.17.8 (needed by equalsverifier on Java 21+ class files)
- added `jakarta.inject-api` 1.0.3, `jakarta.xml.bind-api` 2.3.3, `jakarta.annotation-api` 1.3.5 and `jaxb-runtime` 2.3.9. These all still use the `javax.*` packages, so the app stays on Java EE 7 / javax.
- applied language modernisations: unnamed catch variables (`_`), pattern-matching `instanceof`, `Locale.of`, `@Serial`, `Integer.valueOf` in ITs, `IO.println` in `LoginContextProducer`

## Next steps
- Runtime target: WildFly 26.1.x (the server in `version.wildfly` and the last javax-based WildFly) officially supports only Java 11/17. Bytecode compiled for Java 25 will not deploy on a JVM older than 25. Before deploying, check that WildFly 26 starts on JDK 25, or lower `maven.compiler.release` to the server's JDK.
- `LoginContextProducer` uses `java.lang.IO.println`, which exists only on Java 25+. Consider using a logger instead.
- `maven-compiler-plugin` is still 3.1. It works, but upgrading it to a current 3.x and using `maven.compiler.release` would be cleaner.
- The ITs were not run in this cycle. Run `mvn verify -Parquillian-wildfly-managed` to validate them.
