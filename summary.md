# Upgrade summary: Java 25, Jakarta EE 11, Jakarta Faces 4.1

`mvn clean package` passes on Corretto 25: it compiles main and test code (ITs included) and the 9 unit tests pass. The Arquillian ITs are skipped by default (`skipITs=true`) and were not run.

## Changes made on top of the OpenRewrite output

- `pom.xml`
  - Replaced the javax `org.jboss.spec:jboss-javaee-7.0` umbrella with `jakarta.platform:jakarta.jakartaee-api:11.0.0` (`provided`). This brings in the Faces 4.1 API, which caused all the compile errors (`package jakarta.faces.* does not exist`).
  - Removed the separate EE 10-level API artifacts the recipe had added (`jakarta.annotation`, `ejb`, `cdi`, `persistence`, `validation`, `ws.rs`). Most were in `compile` scope, so they would have been packaged into the WAR. The EE 11 platform API now provides all of them.
  - `jakarta.xml.bind-api` 4.0.5 is now `provided`. It is not part of the EE 11 platform API, and WildFly ships it.
  - `jaxb-runtime` moved from 2.3.9 (the javax implementation, which does not work with `jakarta.xml.bind`) to 4.0.6, scope `provided`.
  - `swagger-maven-plugin` 3.1.8 now has `skipSwaggerGeneration=true`. The plugin only reads `javax.ws.rs` annotations, so on `jakarta.ws.rs` it overwrote `src/main/webapp/swagger.json` with an empty contract. The committed `swagger.json` is kept unchanged; the REST endpoints themselves did not change.
  - The WAR's `WEB-INF/lib` now holds only the application libraries (PrimeFaces 14.0.18-jakarta, themes, webjars, log4j, swagger-annotations, jsr305). No EE API jars are bundled.
- Descriptors set to the Jakarta EE 11 versions (doc 33):
  - `web.xml`: 6.1
  - `beans.xml`: 4.1, still `bean-discovery-mode="all"`
  - `persistence.xml`: 3.2 (validated against `persistence_3_2.xsd`)
  - `WEB-INF/classes/META-INF/forge.taglib.xml`: the Facelets 1.0 DOCTYPE replaced by the `web-facelettaglibrary_4_1.xsd` schema, version 4.1
  - `faces-config.xml` was already 4.1.
- Checked and left as they are:
  - All XHTML views already use the `jakarta.faces.*`, `jakarta.tags.*` and `primefaces` URNs.
  - There is no `@ManagedBean`, `@ManagedProperty` or `javax.faces.bean` left.
  - The JDK `javax.*` packages (`javax.security.auth.*`, `javax.naming`, `javax.sql`) and jsr305 `javax.annotation.Nonnull/Nullable` stay on javax.
  - `entity.ftl` is on jakarta. `datasource.ftl` keeps its JDK javax imports.

## PrimeFaces human UI gate

These sites change what users see. They were not edited and are left for human UI review (doc 31). The build is green with them, because PrimeFaces 14/15 still ships `p:calendar`.

- `src/main/webapp/shopping/createaccount.xhtml:49`: `<p:calendar id="dateOfBirth" ...>` → PrimeFaces 15 `<p:datePicker>` (`monthNavigator`/`yearNavigator`, `showIcon`, re-check `pattern`). Left for human UI review.
- `src/main/webapp/shopping/updateaccount.xhtml:53`: `<p:calendar id="dateOfBirth" ...>` → `<p:datePicker>`. Left for human UI review.
- `src/main/webapp/WEB-INF/web.xml:33`: `primefaces.THEME` = `bootstrap` (legacy ThemeRoller theme) → a built-in PF 15 theme (`saga-blue` / `arya-blue` / `vela-blue`), `primefaces-themes.jar`, or a Theme Designer theme. Left for human UI review.
- `pom.xml:112` and `pom.xml:167`: `org.primefaces.themes:all-themes` 1.1.0 (legacy themes) → drop once a PF 15 theme is chosen. Left for human UI review.
- `src/main/webapp/WEB-INF/web.xml:37`: `primefaces.FONT_AWESOME=true`. FontAwesome was removed from the PrimeFaces jar in 11.0 → PrimeIcons, or keep the app-supplied FontAwesome webjar. Left for human UI review.
- `src/main/webapp/resources/templates/template.xhtml:86`: `fa fa-user` → `pi pi-user`. Left for human UI review.
- `src/main/webapp/resources/templates/template.xhtml:92`: `fa fa-sign-out` → `pi pi-sign-out`. Left for human UI review.
- `src/main/webapp/resources/templates/template.xhtml:100`: `fa fa-sign-in` → `pi pi-sign-in`. Left for human UI review.
- `src/main/webapp/resources/templates/template.xhtml:107`: `fa fa-shopping-cart` → `pi pi-shopping-cart`. Left for human UI review.

## Next steps

- **Manual deployment test, CDI 4 (doc 32):** `src/main/webapp/WEB-INF/beans.xml` keeps the explicit `bean-discovery-mode="all"`, so behaviour should match EE 7. The producer class `org.agoncal.application.petstore.util.NumberProducer` has `@Produces @Named` fields but no bean-defining annotation, so it is a bean only because discovery is `all`. Deploy and open the shopping and admin pages to confirm. If `beans.xml` is ever switched to `annotated`, add a scope to `NumberProducer` (and to every other unannotated bean) first.
- **Server:** WildFly 26 (javax) cannot run this WAR. Use standard WildFly 40/41 (Jakarta EE 11). WildFly 41 is EE 11-certified only on Java SE 17/21, not on SE 25. Choose between SE 25 (recommended by WildFly, not certified) and SE 21 (certified).
- **Integration-test profiles are still on the EE 7/javax line and were not run:**
  - `version.wildfly` 26.1.3.Final
  - `wildfly-arquillian-container-*` 3.0.1.Final
  - `resteasy-client` / `resteasy-jackson-provider` 3.15.3.Final and `arquillian-rest-client-impl-3x` (javax JAX-RS 2)
  - `arquillian.xml` `jbossHome` (`target/wildfly-11.0.0.Final`)
  - To run the ITs against WildFly 40/41, move these to WildFly 41, wildfly-arquillian 5.x, and RESTEasy 6.2+/7 with `resteasy-jackson2-provider`, then run `mvn verify -Parquillian-wildfly-managed`.
- **Hibernate 7 (server-provided on WildFly 40+, doc 10b):** the app has no Hibernate dependency or API usage, and no native queries, `StatelessSession`, `refresh()` or `lock()`. None of the three silent Hibernate 7 behaviour changes applies. A smoke test of the JPQL named queries on the server's Hibernate 7 is still advisable.
- **Swagger:** generation is skipped (see above). Replace `com.github.kongchen:swagger-maven-plugin` with a Jakarta-aware generator, for example `io.swagger.core.v3:swagger-maven-plugin-jakarta` with OpenAPI 3 annotations, then re-enable generation.
- **PrimeFaces:** the recipe left it on 14.0.18-jakarta. A bump to 15.0.x (same `jakarta` classifier) is possible, but PF 15 no longer renders legacy themes correctly, so do it together with the UI gate items above.
- **Unchanged:**
  - The swagger, log4j 2.19.0, EqualsVerifier, JUnit 4 and Byte Buddy pins. They build and pass on Java 25.
  - The unused `version.maven.compiler.plugin` property (3.1); the build uses Maven's default compiler plugin.
