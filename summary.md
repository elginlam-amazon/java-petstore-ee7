# Upgrade summary: Java 25 + Jakarta EE 11 (Faces 4.1, POI 5)

`mvn clean package` passes on Corretto 25: zero compilation errors and 9/9 unit tests green. Arquillian ITs are still skipped by default (`skipITs=true`).

## Changes made in this cycle (on top of the OpenRewrite output)

- pom.xml
  - Replaced the javax `org.jboss.spec:jboss-javaee-7.0` API with `jakarta.platform:jakarta.jakartaee-api` 11.0.0 (`provided`), declared in dependencyManagement and dependencies. Without this the Faces, Servlet, CDI, Interceptor and Inject APIs were not on the classpath (`package jakarta.faces.* does not exist`).
  - Removed the separate Jakarta API jars the recipe had added with compile scope (annotation, ejb, cdi, persistence, validation, ws.rs). They duplicated the platform API and would have been packaged into the WAR next to WildFly's own modules. `jakarta.xml.bind-api` 4.0.5 stays, now `provided`, because XML Binding is not part of the EE 11 platform API jar.
  - Removed `org.glassfish.jaxb:jaxb-runtime` 2.3.9. It was the javax line, and WildFly provides the JAXB implementation.
  - Moved log4j from 2.19.0 to 2.24.3 so log4j-core matches the log4j-api 2.24.3 that POI 5.5.1 pulls in.
  - Pinned commons-lang3 to 3.18.0, which commons-compress 1.28 (POI 5) needs. The Arquillian BOM was managing it down to 3.5.
  - swagger-maven-plugin 3.1.8: generation is now skipped (`swagger.skip=true`). The plugin only reads `javax.ws.rs` annotations, so on the jakarta endpoints it overwrote `src/main/webapp/swagger.json` with an empty contract (it also failed on Java 25 because JAXB is missing). The committed swagger.json is kept unchanged.
- Descriptors bumped to the Jakarta EE 11 versions (doc 33): `web.xml` 5.0 to 6.1, `beans.xml` 4.0 to 4.1 (`bean-discovery-mode="all"` kept). `forge.taglib.xml` had a Facelets 1.0 DOCTYPE with the jakarta namespace; it now uses the `web-facelettaglibrary_4_1.xsd` header, version 4.1.
- `ReportStyles.java`: whitespace only, re-indented the lines the recipe rewrote. I checked every POI mapping against the doc 35 tables: named constants map to the same-code enum, short literals, parameters and `TOTAL_ALIGN` go through `forInt` (same value as POI 3.x), `setBoldweight(BOLD)` became `setBold(true)`, `setCellType(0)` became `CellType.NUMERIC` (code 0). Nothing needed fixing.

I checked these and they are correct as the recipes left them: persistence.xml (3.2, `org.hibernate.jpa.HibernatePersistenceProvider`), faces-config.xml 4.1, every XHTML `xmlns` on the jakarta URNs, no `@ManagedBean` or `javax.faces` left, jsr305 `javax.annotation.Nonnull/Nullable` kept on javax, the JDK `javax.naming` and `javax.security.auth` imports and `datasource.ftl` kept on javax, and `entity.ftl` moved to jakarta.persistence. The application uses no Hibernate API (no native queries, no `StatelessSession`, no `refresh`/`lock`), so none of the Hibernate 7 silent behavior changes apply.

## PrimeFaces human UI gate

PrimeFaces is now 14.0.18 with the `jakarta` classifier. I did not change these sites; each is left for human UI review:

- `src/main/webapp/shopping/createaccount.xhtml:49`: `<p:calendar id="dateOfBirth" ...>`. PrimeFaces 15 replacement: `<p:datePicker>` (monthNavigator/yearNavigator, showIcon). Left for human UI review.
- `src/main/webapp/shopping/updateaccount.xhtml:53`: `<p:calendar id="dateOfBirth" ...>`. PrimeFaces 15 replacement: `<p:datePicker>`. Left for human UI review.
- `src/main/webapp/WEB-INF/web.xml:33`: `primefaces.THEME` = `bootstrap` (legacy ThemeRoller theme). PrimeFaces 15 replacement: a built-in theme (`saga-blue`, `arya-blue`, `vela-blue`), `primefaces-themes.jar`, or a Theme Designer theme. Left for human UI review.
- `pom.xml:119` (dependencyManagement) and `pom.xml:184` (dependencies): `org.primefaces.themes:all-themes` 1.1.0. Remove it once the theme above is replaced. Left for human UI review.
- `src/main/webapp/resources/templates/template.xhtml:86, 92, 100, 107`: FontAwesome `fa fa-user`, `fa fa-sign-out`, `fa fa-sign-in`, `fa fa-shopping-cart` (with `primefaces.FONT_AWESOME=true` in web.xml). PrimeFaces 15 replacement: PrimeIcons (`pi pi-user`, `pi pi-sign-out`, `pi pi-sign-in`, `pi pi-shopping-cart`), or keep supplying FontAwesome from the app (the webjar is already bundled). Left for human UI review.

## Next steps

- Manual deployment test (CDI 4): `src/main/webapp/WEB-INF/beans.xml` explicitly keeps `bean-discovery-mode="all"`, which preserves the old behavior. Deploy and open the pages to confirm that the injected beans, the producers (`util/*Producer`) and the interceptors listed in beans.xml (`LoggingInterceptor`, `ExceptionInterceptor`) still resolve.
- Target server: WildFly 40/41 (EE 11). WildFly 26, which the README and `version.wildfly` (26.1.3.Final) still name, cannot run a jakarta app. WildFly 40/41 are EE 11 certified only on Java SE 17/21, not on SE 25. A human needs to choose between SE 25 (recommended but not certified) and SE 21 (certified).
- The Arquillian IT stack is still on javax and EE 7: `version.wildfly` 26.1.3, `wildfly-arquillian-container-*` 3.0.1.Final, `resteasy-client`/`resteasy-jackson-provider` 3.15.3, and `arquillian-rest-client-impl-3x`. The ITs compile, but running them (`-Parquillian-wildfly-*`) needs a WildFly 40+ distribution, a current wildfly-arquillian, and RESTEasy 6.2+/7 with `resteasy-jackson2-provider`. This was not done because I could not verify it without a server.
- Swagger: moving `io.swagger:swagger-annotations` 1.6 / swagger-maven-plugin 3.1.8 to swagger-core v3 (`swagger-annotations-jakarta` + `io.swagger.core.v3:swagger-maven-plugin-jakarta`) means rewriting `@Api`/`@ApiOperation` on 5 endpoints and changes the published contract format (OpenAPI 3). Until then, generation is skipped and the committed swagger.json is served.
- POI: the mappings are value-preserving (`forInt`), but the workbooks have not been compared before and after. Optionally use the golden-master capture in doc 36 for `WriteWorkbooks`. The recipe also turned `WriteWorkbooks.main` into a Java 25 instance `void main(String[])`; nothing in the repo calls it directly.
- `src/main/forge/generate.fsh` (a JBoss Forge script, not compiled) still names `javax.persistence.*` / `javax.enterprise.*`. Update it only if the scaffolding is still used.
- `maven-compiler-plugin` is still 3.1 (it works with source/target 25). Consider 3.13+ with `<maven.compiler.release>25</maven.compiler.release>`.
