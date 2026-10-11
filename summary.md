# Java 25 upgrade – build fix cycle

## Result
`mvn clean verify` passes on JDK 25 (Corretto 25.0.4): zero compilation errors, 9/9 unit tests green, `applicationPetstore.war` produced. The application stays on Java EE 7 / `javax.*` (WildFly 10–26 target), so no jakarta namespace migration was done.

## Changes made in this cycle
- `security/SimpleCallbackHandler.java`: the OpenRewrite "pattern matching switch" recipe left unexpanded template placeholders (`__P__...`) in the file, which broke compilation. It is rewritten as a valid Java 21+ pattern `switch` with the same behaviour as the original `instanceof` chain. Name and password callbacks are filled from `CredentialsBean`. Any other callback, including `null`, still throws `UnsupportedCallbackException`.
- `pom.xml`: `equalsverifier` 3.12.3 → 3.19.4. The Byte Buddy bundled in 3.12.3 only supports up to Java 20, so all entity tests failed on Java 25.
- `OrderLineTest`, `PurchaseOrderTest`: added `Warning.JPA_GETTER` to `suppress(...)`. EqualsVerifier 3.15+ adds a JPA check that version 3.12.3 did not run. It flags `equals()` reading the `item`/`customer` fields directly instead of through their getters. Both associations are `EAGER`, so this does not change behaviour, and the entity code was left as is.

## Next steps
- Integration tests (`*IT`, Arquillian/WildFly) are skipped by default (`skipITs=true`) and were not run in this cycle. Run them with `-Parquillian-wildfly-managed`.
- Deployment risk: WildFly 26 does not officially support Java 25 as a runtime. Check which JDK the target server runs on: bytecode version 69 needs a JVM that supports Java 25.
- `maven-compiler-plugin` resolves to 3.15.0, which works with Java 25. The unused `version.maven.compiler.plugin` property (3.1) is now stale and could be removed or aligned.
- OpenRewrite added `jakarta.inject-api` 1.0.3, `jakarta.xml.bind-api` 2.3.3, `jakarta.annotation-api` 1.3.5 and `jaxb-runtime` 2.3.9. These are the javax-namespace (EE 8) coordinates, so they are consistent with the app. Consider making `jakarta.inject-api` and `jakarta.xml.bind-api` `provided`, because WildFly supplies them.
- `rewrite.yml` (untracked) is the recipe file used by the automated pass. Remove it once the upgrade is final.
