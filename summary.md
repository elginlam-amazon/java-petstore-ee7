# Upgrade summary: Java 25 + Jakarta EE 11 + Jakarta Faces 4.1 + Apache POI 5

Build status: `mvn clean verify` is green on Corretto 25 (9 unit tests pass; Arquillian ITs are skipped by default with `skipITs=true`).

## Changes made in this cycle (after the OpenRewrite pass)

- `pom.xml`
  - Replaced the javax umbrella `org.jboss.spec:jboss-javaee-7.0` (provided) with `jakarta.platform:jakarta.jakartaee-api:11.0.0` (provided). Removed the separate compile-scope `jakarta.annotation/ejb/enterprise/persistence/validation/ws.rs` API jars the recipe added (they were EE 10 versions and got bundled into `WEB-INF/lib`). The platform API also supplies `jakarta.faces`, which was missing and caused the compile errors.
  - Added `jakarta.xml.bind:jakarta.xml.bind-api:4.0.5` as provided, because XML Binding is not in the EE 11 platform API jar. WildFly provides it.
  - Removed `org.glassfish.jaxb:jaxb-runtime:2.3.9` (runtime). It is a javax JAXB implementation, and WildFly provides the jakarta one.
  - `version.log4j` went from 2.19.0 to 2.24.3. POI 5.5.1 brings in log4j-api 2.24.3, so log4j-core had fallen out of step with it.
  - `swagger-maven-plugin` (kongchen 3.1.8, swagger-core 1.x) now runs with `skipSwaggerGeneration=true`. It only scans `javax.ws.rs` annotations and would have overwritten `src/main/webapp/swagger.json` with an empty contract. The committed `swagger.json` is kept; the endpoints did not change.
- Descriptors moved to the EE 11 versions: `WEB-INF/web.xml` is 6.1, `WEB-INF/beans.xml` is 4.1 (keeping `bean-discovery-mode="all"`), and `forge.taglib.xml` is a 4.1 schema document (the Facelets 1.0 DOCTYPE was removed). `faces-config.xml` 4.1 and `persistence.xml` 3.2 came from the recipe and were checked.
- `CatalogExportService` (Hibernate 7 silent breaks, made safe at the call sites):
  - `lastCatalogChange()` now accepts `java.time.LocalDateTime` (Hibernate 7) as well as `java.sql.Timestamp` from the native query.
  - `reload(Item)` now loads the managed instance with `find` before `refresh`. A detached entity would now throw `IllegalArgumentException`. A missing row still throws `EntityNotFoundException`.
- Arquillian ITs (25 files under `src/test/java`): `EmptyAsset` `beans.xml` was replaced by the new `src/test/resources/test-beans.xml` (`bean-discovery-mode="all"`). Under CDI 4 an empty `beans.xml` means `annotated`, so the unscoped producers (`LoggingProducer`, `DatabaseProducer`, `NumberProducer`, `ConfigPropertyProducer`, `LoginContextProducer`) would no longer be discovered.
- Apache POI: `CatalogExcelExport` already uses `HorizontalAlignment.CENTER/RIGHT`, `FillPatternType.SOLID_FOREGROUND` and `Font.setBold(true)`. No short codes are left, so no mapping was needed (checked against doc 35).

## PrimeFaces human UI gate

None of these were changed. They do not block the build, because PrimeFaces 14.0.18-jakarta still ships `p:calendar`.

- `src/main/webapp/shopping/createaccount.xhtml:49`: current `<p:calendar id="dateOfBirth" ... navigator="true" showOn="button" pattern="d/M/yyyy">`. PrimeFaces 15 replacement: `<p:datePicker monthNavigator="true" yearNavigator="true" showIcon="true" pattern="d/M/yyyy">`. Left for human UI review.
- `src/main/webapp/shopping/updateaccount.xhtml:53`: current `<p:calendar id="dateOfBirth" ... navigator="true" showOn="button">`. PrimeFaces 15 replacement: `<p:datePicker>` with the attributes above. Left for human UI review.
- `src/main/webapp/WEB-INF/web.xml:33`: current `primefaces.THEME` = `bootstrap` (legacy ThemeRoller theme). PrimeFaces 15 replacement: a built-in theme (`saga-blue` / `arya-blue` / `vela-blue`), `primefaces-themes.jar`, or a Theme Designer theme. Left for human UI review.
- `pom.xml:113` and `pom.xml:166`: current dependency `org.primefaces.themes:all-themes:1.1.0`. PrimeFaces 15 replacement: drop it once a PF 15 theme is chosen. Left for human UI review.
- `src/main/webapp/WEB-INF/web.xml:37`: current `primefaces.FONT_AWESOME=true`. PrimeFaces 11+ no longer bundles FontAwesome; the replacement is PrimeIcons, or keep the app-supplied `org.webjars.bower:fontawesome`. Left for human UI review.
- `src/main/webapp/resources/templates/template.xhtml:86` (`fa fa-user`), `:92` (`fa fa-sign-out`), `:100` (`fa fa-sign-in`), `:107` (`fa fa-shopping-cart`): current FontAwesome 4.7 icons. PrimeFaces 15 replacement: PrimeIcons `pi pi-user`, `pi pi-sign-out`, `pi pi-sign-in`, `pi pi-shopping-cart`. Left for human UI review.

## Next steps

- Manual deployment test (CDI 4): `src/main/webapp/WEB-INF/beans.xml` keeps `bean-discovery-mode="all"` (now version 4.1). The app relies on it for the unscoped producer classes (`util/LoggingProducer`, `util/DatabaseProducer`, `util/NumberProducer`, `util/ConfigPropertyProducer`, `security/LoginContextProducer`) and for `view/AbstractBean` subclasses. `src/test/resources/test-beans.xml` does the same for the IT archives. Deploy the app and open its pages to confirm; the build does not prove this.
- Manual integration test (Hibernate 7, native query java.time): `CatalogExportService.lastCatalogChange()` (`select max(CURRENT_TIMESTAMP) from Item`). The code now accepts both `LocalDateTime` and `Timestamp`. Check it on the target WildFly.
- Manual integration test (Hibernate 7, refresh of a detached entity): `CatalogExportService.reload(Item)`. It now does `find` and then `refresh`. Check it with an `Item` passed in from a view.
- StatelessSession L2 cache change: does not apply. There is no `StatelessSession` in the code, and `persistence.xml` has `shared-cache-mode NONE`.
- Target server: WildFly 26 (`version.wildfly`) cannot run a `jakarta.*` app. Use WildFly 40/41 for Jakarta EE 11 (doc 33). WildFly 40/41 are EE 11-certified on Java SE 17/21 only, not on SE 25. A human needs to choose between SE 25 (recommended, not certified) and SE 21 (certified).
- The IT stack is still on the javax/WildFly 26 line and was not run in this cycle: `version.wildfly` 26.1.3.Final, `wildfly-arquillian-container-*` 3.0.1.Final, `resteasy-client` / `resteasy-jackson-provider` 3.15.3.Final, `arquillian-rest-client-impl-3x`, `wildfly-maven-plugin` 4.0.0.Final, and the `wildfly-11.0.0.Final` paths in the managed profile and `arquillian.xml`. These need moving to WildFly 41 / wildfly-arquillian 5.x / a jakarta RESTEasy client before `-Parquillian-wildfly-*` can pass.
- Swagger: move from `io.swagger` 1.x annotations and the kongchen plugin to swagger-core 2.x (`*-jakarta` artifacts, OpenAPI 3; `@Api`/`@ApiOperation` become `@Tag`/`@Operation` in the five REST endpoints), then re-enable contract generation. Until then `swagger.json` is maintained by hand.
- PrimeFaces is on 14.0.18-jakarta (recipe choice). Moving to 15.0.x is a single version bump (doc 31), but it is tied to the theme decision above, because legacy themes no longer render on PF 15.
- `src/main/forge/generate.fsh` (JBoss Forge script, not part of the build) still references javax / Java EE 7 coordinates. It was left unchanged.
