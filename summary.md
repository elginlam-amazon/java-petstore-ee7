# Upgrade summary: Petstore EE 7 → Java 25 / Jakarta EE 11 / Jakarta Faces 4.1 / POI 5

## Result

`mvn clean package` passes on JDK 25 (Corretto 25.0.4): zero compilation errors, 9/9 unit tests green. Integration tests (`*IT`) compile. They are skipped by default (`skipITs=true`) and were not run.

## What was wrong after the OpenRewrite pass

- The Jakarta Faces API was not on the compile classpath. The only provided API was `org.jboss.spec:jboss-javaee-7.0`, which is javax. That broke every `jakarta.faces.*` import, about 120 errors.
- The recipe added the separate Jakarta APIs (annotation, ejb, cdi, persistence, validation, ws.rs, xml.bind) at compile scope and at EE 10 versions. That bundles API jars into the WAR.
- `org.glassfish.jaxb:jaxb-runtime:2.3.9` (the javax JAXB implementation) was added alongside `jakarta.xml.bind-api` 4.
- On JDK 25, `swagger-maven-plugin` 3.1.8 failed (`javax/xml/bind/annotation/XmlElement` missing). With JAXB added to its classpath, it ran but wrote a `swagger.json` with no paths, because it only recognises `javax.ws.rs`.

## Changes made (pom.xml and descriptors only, no Java source changes)

- pom.xml:
  - Replaced `jboss-javaee-7.0` and the separate compile-scope Jakarta API dependencies with `jakarta.platform:jakarta.jakartaee-api:11.0.0` (`provided`). This brings in Faces 4.1, CDI 4.1, Persistence 3.2, REST 4.0, Validation 3.1, EJB 4.0 and Servlet 6.1. The property `version.javaee` / `version.jboss-javaee` became `version.jakartaee`.
  - Kept `jakarta.xml.bind-api` 4.0.5 but changed its scope to `provided`. XML Binding is no longer in the EE 11 platform API, and WildFly provides it.
  - Removed `jaxb-runtime` 2.3.9 (javax implementation; the server provides JAXB).
  - `version.log4j` 2.19.0 → 2.24.3, so log4j-core matches the log4j-api 2.24.3 that POI 5.5.1 brings in.
  - `swagger-maven-plugin`: set `skipSwaggerGeneration=true`, so the build no longer replaces the committed `src/main/webapp/swagger.json` with an empty contract.
- `WEB-INF/web.xml`: version 5.0 → 6.1 (`web-app_6_1.xsd`).
- `WEB-INF/beans.xml`: version 4.0 → 4.1 (`beans_4_1.xsd`). It keeps `bean-discovery-mode="all"`, which was already explicit in the original file, so the CDI 4 default change does not apply.
- `WEB-INF/classes/META-INF/forge.taglib.xml`: removed the Facelets 1.0 DOCTYPE and set it to the Jakarta 4.1 taglib schema (`web-facelettaglibrary_4_1.xsd`).

The WAR's `WEB-INF/lib` no longer contains any Java EE or Jakarta EE API jar.

## Verified without changes

- **Faces:** `faces-config.xml` is already 4.1 on the jakarta.ee namespace. Every XHTML view uses the `jakarta.faces.*` and `primefaces` URNs. No `@ManagedBean`, `@ManagedProperty` or `javax.faces.bean` is left.
- **Namespaces:** JDK `javax.*` packages stay javax (`javax.security.auth.*`, `javax.naming`, and `javax.sql` in `datasource.ftl`). The jsr305 `javax.annotation.Nonnull`/`Nullable` also stay javax.
- **persistence.xml:** already 3.2 with `jakarta.persistence.*` property names. It has no provider and no dialect set.
- **Apache POI** (`ReportStyles.java`): I checked every `forInt` against the doc 35 tables:
  - Alignment: A3 = 2 → CENTER, A7 = 4 → FILL, X1 = 3 → RIGHT, A5 = `TOTAL_ALIGN` 6 → CENTER_SELECTION.
  - Fill: F3 and X2 = 1 → SOLID_FOREGROUND, F5 = 18 → LEAST_DOTS.
  - `forInt` keeps the exact POI 3 codes, so I left the calls as they are.

## PrimeFaces human UI gate

None of these were changed; they change what users see. All are left for human UI review.

- `src/main/webapp/shopping/createaccount.xhtml:49`: `<p:calendar id="dateOfBirth" ... navigator="true" showOn="button" pattern="d/M/yyyy">`. PrimeFaces 15 replacement: `<p:datePicker>` (`monthNavigator`/`yearNavigator`, `showIcon`). Left for human UI review.
- `src/main/webapp/shopping/updateaccount.xhtml:53`: `<p:calendar id="dateOfBirth" ... navigator="true" showOn="button">`. PrimeFaces 15 replacement: `<p:datePicker>`. Left for human UI review.
- `src/main/webapp/WEB-INF/web.xml:33`: `primefaces.THEME` = `bootstrap` (legacy ThemeRoller theme). PrimeFaces 15 replacement: a built-in theme such as `saga-blue`/`arya-blue`/`vela-blue`, `primefaces-themes.jar`, or a Theme Designer theme. Left for human UI review.
- `pom.xml:112` (dependencyManagement) and `pom.xml:178` (dependencies): `org.primefaces.themes:all-themes:1.1.0`. PrimeFaces 15 replacement: remove it once a PF 15 theme is chosen. Left for human UI review.
- `src/main/webapp/WEB-INF/web.xml:37`: `primefaces.FONT_AWESOME` = `true`. PrimeFaces 15 replacement: PrimeIcons, or the FontAwesome the app already ships (`org.webjars.bower:fontawesome` 4.7.0). Left for human UI review.
- `src/main/webapp/resources/templates/template.xhtml:86`: FontAwesome `fa fa-user`. PrimeFaces 15 replacement: PrimeIcons `pi pi-user`. Left for human UI review.
- `src/main/webapp/resources/templates/template.xhtml:92`: FontAwesome `fa fa-sign-out`. PrimeFaces 15 replacement: PrimeIcons `pi pi-sign-out`. Left for human UI review.
- `src/main/webapp/resources/templates/template.xhtml:100`: FontAwesome `fa fa-sign-in`. PrimeFaces 15 replacement: PrimeIcons `pi pi-sign-in`. Left for human UI review.
- `src/main/webapp/resources/templates/template.xhtml:107`: FontAwesome `fa fa-shopping-cart`. PrimeFaces 15 replacement: PrimeIcons `pi pi-shopping-cart`. Left for human UI review.

PrimeFaces stays at 14.0.18 (`jakarta` classifier), which the recipe set. The move to 15.0.x is optional (doc 31). I did not make it in this cycle, because PF 15 makes the legacy `bootstrap` theme render incorrectly; do it together with the theme decision above.

## Next steps

1. **Manual deployment test (CDI):** `src/main/webapp/WEB-INF/beans.xml` keeps `bean-discovery-mode="all"`. Several classes depend on discovery mode `all`, for example producers in `util/*Producer.java`, `LoginContextProducer`, and the interceptors listed in `beans.xml`. Deploy the app and open the shopping and admin pages to confirm injection and EL names resolve.
2. **Target server:** the app now needs Jakarta EE 11, which means standard WildFly 40 or 41. The README and `version.wildfly` (26.1.3.Final, javax) are out of date. WildFly 40/41 are EE 11-certified only on Java SE 17/21, not on SE 25 (doc 33). A human needs to choose between SE 25 (recommended, not certified) and SE 21 (certified).
3. **Integration-test stack is still on javax (not run):**
   - `version.wildfly` 26.1.3.Final.
   - `wildfly-arquillian-container-*` 3.0.1.Final.
   - `resteasy-client` and `resteasy-jackson-provider` 3.15.3.Final.
   - `arquillian-rest-client-impl-3x`.
   - JUnit 4.
   
   The ITs compile but will not run against an EE 11 server until these move to Jakarta-based versions: WildFly 40+, wildfly-arquillian 5.x, RESTEasy 6.2+/7, and an Arquillian Jakarta REST client or plain `jakarta.ws.rs.client`.
4. **Hibernate 7 (server-provided ORM):** the app does not bundle Hibernate and has no `org.hibernate` code. A search found no native queries, `StatelessSession`, `refresh()` or `lock()` calls, so none of the three silent Hibernate 7 behavior changes applies. The integration-test items:
   - Run the `*ServiceIT` tests against WildFly 40+ (Hibernate 7.x).
   - Confirm that `init_db.sql` (`jakarta.persistence.sql-load-script-source`) still loads with `drop-and-create`.
5. **Swagger:** `swagger-maven-plugin` 3.1.8 cannot read `jakarta.ws.rs`, so generation is turned off and the committed `swagger.json` is kept. Replace it with a Jakarta-aware generator, such as `io.swagger.core.v3:swagger-maven-plugin-jakarta` with OpenAPI 3 annotations, then remove the skip flag. That changes the `@Api`/`@ApiOperation` annotations in `rest/*Endpoint.java`, so it needs a decision.
6. **POI behavior check:** write the workbooks with `WriteWorkbooks` and compare each styled cell's alignment and fill with the doc 35 tables. `WriteWorkbooks.main` is now a Java 25 instance `void main(String[])` (from `MigrateMainMethodToInstanceMain`). Callers that use it as `WriteWorkbooks.main(...)` must use the launcher instead.
7. `validation.xml` is on the Validation 3.0 schema, which is valid on EE 11. Moving it to 3.1 is optional.
