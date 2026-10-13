# 📊 Executive Report: Petstore Upgrade to Java 25 / Jakarta EE 11  
  
## 1. 🧭 Executive summary  
  
The Petstore WAR (`org.agoncal.application:petstoreee7`) moved from Java 8 / Java EE 7 (`javax`) to Java 25 and Jakarta EE 11 (`jakarta`). The upgrade included Jakarta Faces 4.1, PrimeFaces 14 (jakarta classifier), Jakarta Persistence 3.2 and Apache POI 5.5.1. OpenRewrite changed 146 files in its main run. The resulting build had about 120 compile errors, because the Jakarta Faces API was missing from the classpath. The Kiro CLI fixed these with POM and descriptor changes only. ✅ The final `mvn clean package` on JDK 25 is green: 0 compile errors and 9/9 unit tests passing. ⚠️ The integration tests compile but were not run; they need a Jakarta EE 11 server.  
  
- 🎯 **Targets selected in the log:** `jakartaEeTarget=11`, `facesTarget=4.1`, `hibernateTarget=7.1`, `poiTarget=5`, Java 25.  
- 🌱 **Spring:** not in scope. The log says "Skipping Spring Boot upgrade: not requested (springBootTargetVersion=none)". The app has no Spring dependencies, so the log has no Spring version-verification line. Spring was neither upgraded nor left incomplete.  
- 🐘 **Hibernate 7.1 recipe:** ran, but changed 0 files. The app does not bundle Hibernate (the server provides it) and has no `org.hibernate` code.  
- 🖼️ **PrimeFaces UI gate:** 9 sites left unchanged for human UI review (see the last section).  
- ⏱️ **Run time:** about 18 minutes end to end. OpenRewrite took about 4 minutes of Maven time and the Kiro build-fix leg took 7 min 4 s.  
  
## 2. 🏗️ Application changes  
  
- ☕ **Java:** `maven.compiler.source`/`target` 1.8 → 25, built and tested on Amazon Corretto 25.0.4.1.  
- 🔁 **Namespace:** `javax.*` → `jakarta.*` for the EE APIs. Changed packages include persistence, inject, ejb, validation, ws.rs, enterprise, interceptor, decorator, annotation, xml.bind and faces.  
  - These stay `javax`, as intended: the JDK `javax.*` packages (`javax.security.auth.*`, `javax.naming`, `javax.sql` in `datasource.ftl`) and the jsr305 `javax.annotation.Nonnull`/`Nullable`.  
- 📦 **Platform API:** `org.jboss.spec:jboss-javaee-7.0` → `jakarta.platform:jakarta.jakartaee-api:11.0.0` (`provided`).  
- 🎨 **Faces:**  
  - All 35 XHTML views now use the `jakarta.faces.*` and `primefaces` URNs.  
  - `faces-config.xml` is now version 4.1.  
  - PrimeFaces 6.1 → 14.0.18 with the `jakarta` classifier.  
- 🗄️ **Persistence:**  
  - `persistence.xml` → Jakarta Persistence 3.2 (`persistence_3_2.xsd`).  
  - Property names moved from `javax.persistence.*` to `jakarta.persistence.*`.  
- 📑 **Apache POI:** 3.14 → 5.5.1. Short-code style calls were migrated to the `HorizontalAlignment` / `FillPatternType` / `CellType` enums.  
- 🧾 **Descriptors:**  
  - `web.xml` → 6.1.  
  - `beans.xml` → 4.1, keeping `bean-discovery-mode="all"`.  
  - `validation.xml` → 3.0.  
  - `forge.taglib.xml` → Facelets taglib schema 4.1.  
- 🧩 **FreeMarker templates:** `entity.ftl` moved from `javax.persistence` to `jakarta.persistence`. `datasource.ftl` (JDK `javax.naming` / `javax.sql`) was left alone, correctly.  
  
## 3. 🛠️ Tools used  
  
- ☕ **Java SDK:** Amazon Corretto `openjdk 25.0.4.1` (2026-08-18 LTS), with Apache Maven 3.9.16.  
- 🔧 **OpenRewrite:**  
  - Plugin: `rewrite-maven-plugin` 6.42.0.  
  - Recipe libraries: `rewrite-migrate-java` 3.38.0, `rewrite-hibernate` 2.22.0, `rewrite-apache` 2.27.1.  
  - Recipes, in the order the log ran them:  
    1. `PreparePoiStyleEnums`, own run (2 files).  
    2. `MigratePoiStyleShortArguments`, own run (1 file).  
    3. Main run, one pass: `UpgradeToJakartaEE11` → `UpgradeToJakartaFaces41` → `UpgradeToHibernate71` → `UpgradePersistenceXmlToJakartaEE11` → `UpgradeApachePoi5` → `UpgradeToJava25` (146 files).  
    4. `MigrateJakartaEeTemplates`, own run (1 file).  
  - "OpenRewrite template-marker check after the Java leg: clean."  
- 🤖 **Amazon Kiro CLI:** `kiro-cli` 2.14.2, model **claude-opus-5.5**, running non-interactively for the automated build fix and this report. It used 18.84 credits in 7 min 4 s.  
  
## 4. 💻 Code changes  
  
### 🔄 Changed by OpenRewrite (from the SourcesFileResults data tables)  
  
- 📈 **Main run:** 146 files changed (104 `.java`, 35 `.xhtml`, 7 `.xml`). Files changed per recipe:  
  - `UpgradeToJakartaEE11`: 127  
  - `UpgradeToJava25`: 105  
  - `PreserveJsr305Annotations`: 4  
  - `UpgradeApachePoi5`: 2  
  - `UpgradeToJakartaFaces41` / `JakartaFacesConfigXml41`: 1  
  - `UpgradePersistenceXmlToJakartaEE11`: 1  
  - `UpgradeToHibernate71`: 0  
- 📑 **POI** (`ReportStyles.java`, 16 call sites in `poi-short-style-calls.txt`):  
  - Named constants → enums, e.g. `CellStyle.ALIGN_CENTER` → `HorizontalAlignment.CENTER`, `SOLID_FOREGROUND` → `FillPatternType.SOLID_FOREGROUND`.  
  - Literals and variables → `HorizontalAlignment.forInt(...)` / `FillPatternType.forInt(...)`.  
  - `setBoldweight` → `setBold(true)`.  
  - `setCellType(Cell.CELL_TYPE_STRING)` → `setCellType(CellType.STRING)`.  
- 🆕 **Java 25:** `WriteWorkbooks.main` became an instance `void main(String[])` (`MigrateMainMethodToInstanceMain`). The recipe also pinned Byte Buddy to 1.17.8 and upgraded surefire/failsafe to 3.5.6 and the war plugin to 3.5.1.  
  
### 🤖 Fixed by Kiro CLI (pom.xml and descriptors only, no Java source changes)  
  
- 🧱 **About 120 compile errors fixed** (`package jakarta.faces.* does not exist`): replaced the javax `jboss-javaee-7.0` API with `jakarta.jakartaee-api:11.0.0` (`provided`). This brings in Faces 4.1.  
- 🧹 **Removed the separate compile-scope Jakarta API jars** the recipe had added at EE 10 levels: annotation, ejb, cdi, persistence, validation, ws.rs. The WAR's `WEB-INF/lib` now has no EE API jars.  
- 📦 **JAXB:** `jakarta.xml.bind-api` 4.0.5 → `provided` scope. XML Binding is not in the EE 11 platform API, and WildFly supplies it. Removed the javax `jaxb-runtime` 2.3.9.  
- 🪵 **Logging:** `log4j-core` 2.19.0 → 2.24.3, to match the `log4j-api` 2.24.3 that POI 5.5.1 brings in.  
- 📘 **Swagger:** on Java 25 + jakarta, `swagger-maven-plugin` 3.1.8 failed (missing `javax.xml.bind`). With JAXB added it ran, but wrote an empty contract because it only reads `javax.ws.rs`. Set `skipSwaggerGeneration=true` so the committed `swagger.json` is kept.  
- 🧾 **Descriptors:**  
  - `web.xml` 5.0 → 6.1.  
  - `beans.xml` 4.0 → 4.1.  
  - `forge.taglib.xml`: Facelets 1.0 DTD → `web-facelettaglibrary_4_1.xsd`.  
- 🔍 **Checked, no change needed:**  
  - Every POI `forInt` code matches doc 35: 2 → CENTER, 3 → RIGHT, 4 → FILL, 6 → CENTER_SELECTION, 1 → SOLID_FOREGROUND, 18 → LEAST_DOTS.  
  - No `@ManagedBean` or `javax.faces.bean` is left.  
  - No JDK `javax` package was wrongly renamed to `jakarta` (false positives).  
  
## 5. ⏱️ Time savings estimate  
  
- 🔧 **OpenRewrite's own estimate (from the log):** 35m (POI constants) + 10m (POI short arguments) + 32h 13m (main run) + 5m (templates) = **about 33 h**.  
- 🤖 **Kiro build-fix leg:** diagnosing the missing Faces API, reconciling the APIs, finding the swagger regression, updating the descriptors and verifying the POI mappings would take an engineer an estimated **1–2 days** by hand. Kiro did it in about 7 min.  
- 📉 **Total:** about **5–6 engineer-days** of manual migration work, done in about 18 min of automated run time. Human effort is now limited to review, deployment testing and the UI decisions below.  
  
## 6. 🚀 Next steps  
  
- 🧪 **Deployment test (CDI 4):** `WEB-INF/beans.xml` keeps `bean-discovery-mode="all"`. Deploy and open the shopping and admin pages to confirm these still work: the producers (`util/*Producer.java`, `LoginContextProducer`), the interceptors (`LoggingInterceptor`, `ExceptionInterceptor`) and the EL bean names.  
- 🖥️ **Target server:**  
  - Deploy on standard WildFly 40/41 (Jakarta EE 11). WildFly 26 cannot run a `jakarta` app.  
  - Update `version.wildfly` (still 26.1.3.Final) and the README.  
  - ⚠️ WildFly 40/41 are EE 11-certified on Java SE 17/21 only, not on SE 25. Choose between SE 25 (recommended, not certified) and SE 21 (certified).  
- 🔬 **Integration tests (not run):** the IT stack is still on javax: WildFly 26, `wildfly-arquillian-container-*` 3.0.1, RESTEasy 3.15.3 client and jackson-provider, and `arquillian-rest-client-impl-3x`. Move it to Jakarta versions (WildFly 40+, wildfly-arquillian 5.x, RESTEasy 6.2+/7), then run `mvn verify -Parquillian-wildfly-managed`.  
- 🐘 **Hibernate 7 (server-provided):** a search found no native queries, `StatelessSession`, `refresh()` or `lock()`, so none of the three silent behavior changes applies. Still run the `*ServiceIT` tests on WildFly 40+ and confirm `init_db.sql` loads with `drop-and-create`.  
- 📘 **Swagger / OpenAPI:** replace `swagger-maven-plugin` 3.1.8 with a Jakarta-aware generator (e.g. `io.swagger.core.v3:swagger-maven-plugin-jakarta`), move `rest/*Endpoint.java` to OpenAPI 3 annotations, then remove `skipSwaggerGeneration`.  
- 📑 **POI output check:**  
  - Write the workbooks with `WriteWorkbooks` and compare each cell's `getAlignment()` / `getFillPattern()` with the doc 35 tables.  
  - Callers of the static `WriteWorkbooks.main(...)` must now use the launcher.  
- 🧹 **Improvements:**  
  - Migrate JUnit 4.13.2 → JUnit 5.  
  - Optionally move `validation.xml` to the 3.1 schema.  
  - Consider PrimeFaces 15.0.x together with the theme decision below.  
  
## PrimeFaces human UI gate  
  
Each site below changes what users see. All were **left unchanged for human UI review**. The build stays green because PrimeFaces 14/15 still ship these components.  
  
- 🎨 **`pom.xml:114`** (`<dependencyManagement>`; line 112 in the current POM): `org.primefaces.themes:all-themes` 1.1.0, a legacy ThemeRoller theme bundle. PrimeFaces 15 replacement (doc 31): remove it and use a built-in theme (`saga-blue` / `arya-blue` / `vela-blue`), `primefaces-themes.jar` (Luna, Nova, Material, Bootstrap) or a Theme Designer theme. Left unchanged for human UI review.  
- 🎨 **`pom.xml:209`** (`<dependencies>`; line 178 in the current POM): `org.primefaces.themes:all-themes`, a legacy ThemeRoller theme bundle. Same PrimeFaces 15 replacement as above. Left unchanged for human UI review.  
- 🎨 **`src/main/webapp/WEB-INF/web.xml:33`**: `primefaces.THEME` = `bootstrap`, a legacy ThemeRoller theme. PrimeFaces 15 replacement: a PF 15 theme such as `saga-blue`, `arya-blue` or `vela-blue`, or a `primefaces-themes.jar` / Theme Designer theme. Left unchanged for human UI review.  
- 🔣 **`src/main/webapp/resources/templates/template.xhtml:86`**: FontAwesome icon `fa fa-user`. PrimeFaces 15 replacement: PrimeIcons `pi pi-user`, or keep an app-supplied FontAwesome. Left unchanged for human UI review.  
- 🔣 **`src/main/webapp/resources/templates/template.xhtml:92`**: FontAwesome icon `fa fa-sign-out`. PrimeFaces 15 replacement: PrimeIcons `pi pi-sign-out`, or keep an app-supplied FontAwesome. Left unchanged for human UI review.  
- 🔣 **`src/main/webapp/resources/templates/template.xhtml:100`**: FontAwesome icon `fa fa-sign-in`. PrimeFaces 15 replacement: PrimeIcons `pi pi-sign-in`, or keep an app-supplied FontAwesome. Left unchanged for human UI review.  
- 🔣 **`src/main/webapp/resources/templates/template.xhtml:107`**: FontAwesome icon `fa fa-shopping-cart`. PrimeFaces 15 replacement: PrimeIcons `pi pi-shopping-cart`, or keep an app-supplied FontAwesome. Left unchanged for human UI review.  
- 📅 **`src/main/webapp/shopping/createaccount.xhtml:49`**: `<p:calendar id="dateOfBirth" display="inline" navigator="true" showOn="button" pattern="d/M/yyyy">` (deprecated since PF 10). PrimeFaces 15 replacement: `<p:datePicker>` with `monthNavigator`/`yearNavigator`, `showIcon` and `pattern="d/M/yyyy"`. Left unchanged for human UI review.  
- 📅 **`src/main/webapp/shopping/updateaccount.xhtml:53`**: `<p:calendar id="dateOfBirth" display="inline" navigator="true" showOn="button">` (deprecated since PF 10). PrimeFaces 15 replacement: `<p:datePicker>` with `monthNavigator`/`yearNavigator` and `showIcon`. Left unchanged for human UI review.

## Runtime verification checklist (silent breaks)

The build passing does not prove any of these. Each one fails only at deployment or run time, so verify it on the target server and database before accepting the upgrade (migration knowledge doc 36).

- Applies because: Hibernate 7: the Hibernate 7 step ran (hibernateTarget=7.1); the module uses JPA (src/main/resources/META-INF/persistence.xml)
- Applies because: CDI 4: jakartaEeTarget=11 (CDI 4.1); the module uses CDI (src/main/webapp/WEB-INF/beans.xml)

- [ ] **Hibernate 7: Native queries return java.time types** (doc 10b, §5.1). Run every native query that returns date or time columns against the target database: Hibernate 7 returns java.time.LocalDate / LocalTime / LocalDateTime instead of java.sql.Date / Time / Timestamp, so a cast or a formatter can fail with ClassCastException. Fix the reads, or set hibernate.query.native.prefer_jdbc_datetime_types=true for the persistence unit.
  - No site found by the static scan; check code that builds these calls at run time.
- [ ] **Hibernate 7: StatelessSession now uses the second-level cache** (doc 10b, §5.2). Check every StatelessSession: in Hibernate 7 it reads from and writes to the L2 cache by default, so a batch job can read stale data with no error. Restore the old behavior with setCacheMode(CacheMode.IGNORE), or CacheMode.GET for bulk writers.
  - No site found by the static scan; check code that builds these calls at run time.
- [ ] **Hibernate 7: refresh() / lock() on a detached entity throws** (doc 10b, §5.3). Exercise every refresh() and lock() call whose entity may come from another transaction, an HTTP session, a JSF view-scoped bean or a cache: Hibernate 7 throws IllegalArgumentException on a detached entity. Work on a managed instance (find with a lock mode, or merge then refresh).
  - No site found by the static scan; check code that builds these calls at run time.
- [ ] **CDI 4: beans.xml discovery mode default flipped to annotated** (doc 32, "What changed"). Deploy the app and open its pages: under CDI 4 an empty beans.xml, or one without bean-discovery-mode, means annotated, so classes without a bean-defining annotation stop being beans and fail only at deployment (WELD-001408) or page render. Keep bean-discovery-mode="all" or add scopes.
  - `src/main/webapp/WEB-INF/beans.xml: bean-discovery-mode="all", version 4.1: old behavior kept`
- [ ] **CDI 4: @Named classes without a bean-defining annotation** (doc 32, "Detection patterns"). Render every view that uses these @Named classes and inject them once: @Named alone is not a bean-defining annotation, so they are beans only while discovery is "all". With annotated discovery EL resolves them to null (Target Unreachable). Add a scope, or keep discovery "all".
  - `src/main/java/org/agoncal/application/petstore/security/SimpleCallbackHandler.java:21`

## Golden-master output checks (optional)

This module writes PDF, Excel or JasperReports output, and a green build does not show that the output is unchanged. Optionally, capture each output file before and after the upgrade and compare the captures structurally: PDFBox text per page and Apache POI cell values, never the bytes (PDF and XLSX embed timestamps). Migration knowledge doc 36 has the steps.

High-risk output paths found by the static scan:

- `src/main/java/org/agoncal/application/petstore/export/poi/ReportStyles.java` (Excel: Apache POI)
- `src/main/java/org/agoncal/application/petstore/export/poi/WriteWorkbooks.java` (Excel: Apache POI)

No golden-master capture was made in this iteration (the scaffolding is optional).

## Kiro credit usage

💳 Kiro CLI credits used: 48.26  
