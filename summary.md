# Upgrade summary: Java 25 + Jakarta EE 11 + Jakarta Faces 4.1

Result: `mvn clean package` is green on JDK 25 (Corretto 25.0.4): 0 compilation errors, 9/9 unit tests pass. The integration tests (`*IT`) compile but did not run, because `skipITs=true` and they need a running WildFly.

## What was fixed after the OpenRewrite pass

pom.xml
- Removed `org.jboss.spec:jboss-javaee-7.0` (javax Java EE 7 APIs) from dependencyManagement and dependencies, together with the unused `version.javaee` and `version.jboss-javaee` properties. It put `javax.faces`, `javax.ws.rs` and the other javax APIs on the classpath next to the jakarta ones.
- Added `jakarta.faces:jakarta.faces-api` 4.1.2 with `provided` scope. This was the cause of every compilation error (`package jakarta.faces.* does not exist`).
- Set all Jakarta EE API dependencies to `provided` (before, they had compile scope and were bundled into the WAR) and aligned them to Jakarta EE 11: CDI 4.1.0, Persistence 3.2.0, Validation 3.1.1, REST 4.0.0. EJB 4.0.1, Annotations 3.0.0 and XML Binding 4.0.5 were already at the EE 11 level.
- Removed `org.glassfish.jaxb:jaxb-runtime` 2.3.9. That is the javax JAXB implementation, which conflicts with `jakarta.xml.bind-api` 4. WildFly provides JAXB at runtime.
- `swagger-maven-plugin` 3.1.8: added `skipSwaggerGeneration=true`. The plugin (Swagger 1.5) cannot load without javax JAXB, and even with JAXB added it only reads `javax.ws.rs`. Run against the jakarta endpoints, it replaced `src/main/webapp/swagger.json` with an empty spec. The committed `swagger.json` is kept as it is.
- PrimeFaces stays at the version the recipe set: 14.0.18 with the `jakarta` classifier.

Descriptors (doc 33, Jakarta EE 11 versions)
- `WEB-INF/web.xml`: 5.0 → 6.1 (`web-app_6_1.xsd`).
- `WEB-INF/beans.xml`: 4.0 → 4.1 (`beans_4_1.xsd`). `bean-discovery-mode="all"` is kept.
- `META-INF/persistence.xml`: 3.0 → 3.2 (`persistence_3_2.xsd`).
- `WEB-INF/classes/META-INF/forge.taglib.xml`: replaced the Facelets 1.0 DOCTYPE with the `web-facelettaglibrary_4_1.xsd` schema, version 4.1.
- `faces-config.xml` was already 4.1. All XHTML views already use the `jakarta.faces.*`, `jakarta.tags.*` and `primefaces` URNs. No `@ManagedBean`, `@ManagedProperty` or `javax.faces.bean` is left.

Namespace checks
- The javax names that remain are correct and were left alone: jsr305 `javax.annotation.Nonnull` and `javax.annotation.Nullable`, and the JDK packages `javax.security.auth.*`, `javax.naming` and `javax.sql`. This includes the `datasource.ftl` template. `entity.ftl` is on `jakarta.persistence`.

## PrimeFaces human UI gate

These sites were not changed. Each one is left for human UI review. Line numbers are for the current files.
- `src/main/webapp/shopping/createaccount.xhtml:49`: `<p:calendar id="dateOfBirth">`. PrimeFaces 15 replacement: `<p:datePicker>` (attribute changes: `navigator` → `monthNavigator`/`yearNavigator`, `showOn="button"` → `showIcon="true"`). Left for human UI review.
- `src/main/webapp/shopping/updateaccount.xhtml:53`: `<p:calendar id="dateOfBirth">`. Replacement: `<p:datePicker>`, same mapping as above. Left for human UI review.
- `src/main/webapp/WEB-INF/web.xml:33`: `primefaces.THEME` = `bootstrap` (legacy ThemeRoller theme). Replacement: a PrimeFaces built-in theme (`saga-blue`, `arya-blue`, `vela-blue`), `primefaces-themes.jar` (Bootstrap, Luna, Nova, Material) or a Theme Designer theme. Left for human UI review.
- `pom.xml:102` (dependencyManagement) and `pom.xml:199` (dependencies): `org.primefaces.themes:all-themes` 1.1.0. These were lines 112 and 196 before the POM cleanup. Replacement: drop it once a PrimeFaces 15 theme is chosen. Left for human UI review.
- `src/main/webapp/WEB-INF/web.xml:37`: `primefaces.FONT_AWESOME=true`. Replacement: PrimeIcons, or keep the FontAwesome that the app supplies itself (the `org.webjars.bower:fontawesome` 4.7.0 dependency). Left for human UI review.
- `src/main/webapp/resources/templates/template.xhtml:86` (`fa fa-user`), `:92` (`fa fa-sign-out`), `:100` (`fa fa-sign-in`), `:107` (`fa fa-shopping-cart`): FontAwesome icon classes. Replacement: `pi pi-user`, `pi pi-sign-out`, `pi pi-sign-in`, `pi pi-shopping-cart`. Left for human UI review.

## Next steps

- Manual deployment test (CDI 4, doc 32): `src/main/webapp/WEB-INF/beans.xml` is now version 4.1. It keeps `bean-discovery-mode="all"` explicitly, so the old discovery behavior should be preserved. These classes have no bean-defining annotation and depend on that setting: `model/Category`, `model/Product`, `model/Customer`, `model/Item`, `model/PurchaseOrder` (`@Named` entities) and `util/NumberProducer` (producer). Deploy the app and open the shopping and admin pages to confirm that injection and EL names still resolve.
- `PurchaseOrderDecorator` (`@Decorator`) has no `@Priority` and is not listed in `beans.xml` `<decorators>`, so it is disabled. It was disabled before the upgrade too, so behavior is unchanged. Confirm this is intended.
- Hibernate 7 (doc 10b): the app gets Hibernate from WildFly and has no `org.hibernate` dependency. No native queries, `StatelessSession`, `refresh()` or `lock()` calls were found in `src/main/java`, so none of the three silent Hibernate 7 behavior changes apply. Still run a smoke test of the persistence layer on the target server (`init_db.sql` load script, `drop-and-create`).
- Server: the app now needs WildFly 40 or 41 (Jakarta EE 11). WildFly 26 cannot run it. Jakarta EE 11 on Java SE 25 is not certified yet (WildFly 41 claims EE 11 compatibility on SE 17 and 21 only). Decide between SE 25 (recommended by WildFly, not certified) and SE 21 (certified).
- Arquillian profiles: `version.wildfly` is still 26.1.3.Final, `wildfly-arquillian-container-*` is 3.0.1.Final, and the REST test client (`resteasy-client` and `resteasy-jackson-provider` 3.15.3, `arquillian-rest-client-impl-3x`) is on javax. Move them to a WildFly 40+ / jakarta-compatible line before running `-Parquillian-wildfly-*`. `wildfly-maven-plugin` 4.0.0.Final should also move to a release that supports WildFly 40+.
- Swagger: generation is skipped (see the pom.xml section above). Replace `com.github.kongchen:swagger-maven-plugin` and `io.swagger:swagger-annotations` 1.x with a Jakarta-aware generator (for example `io.swagger.core.v3` `*-jakarta` with `swagger-maven-plugin-jakarta`). Then remove `skipSwaggerGeneration` and regenerate `swagger.json`. The annotations would need to move from Swagger 1.x to OpenAPI 3.
- `web.xml` still has the comment "To let Mojarra auto start it needs some JSF annotation (@ManagedBean)". It is out of date, but it is only a comment and was left alone.
- `log4j-core` 2.19.0 and `jsr305` are bundled in the WAR. They were not changed.
