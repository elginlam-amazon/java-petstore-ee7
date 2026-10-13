# 📊 Executive Report: Petstore Java EE 7 → Java 25 / Jakarta EE 11  
  
## 1. 📝 Executive summary  
  
The Petstore WAR (`org.agoncal.application:petstoreee7`, iteration 2, commit `5d8d5c3`) was moved from Java 8 on Java EE 7 (javax, JSF 2.2, PrimeFaces 6.1) to Java 25 on Jakarta EE 11, with Jakarta Faces 4.1 and PrimeFaces 14.0.18 (`jakarta` classifier). Two OpenRewrite runs did the bulk of the work. They changed 144 files and the template `entity.ftl`, and both ended with `BUILD SUCCESS`. Kiro CLI then fixed the compile errors the recipes left behind and aligned the dependencies and descriptors. ✅ The final `mvn clean package` on Corretto 25 is green, and all 9 unit tests pass. 🌱 Spring upgrade: not applicable. The log says `Skipping Spring Boot upgrade: not requested (springBootTargetVersion=none)`, so no Spring version check ran and no Spring upgrade is claimed. 🗄️ Hibernate 7.1: the recipe ran, but the SourcesFileResults table has 0 Hibernate rows. The app has no Hibernate dependency and uses the server's Hibernate.  
  
## 2. 🏗️ Application changes  
  
- ☕ **Java:** `maven.compiler.source/target` 1.8 → **25**.  
- 🌐 **Namespace:** javax → jakarta for persistence, validation, ejb, enterprise/CDI, inject, interceptor, decorator, ws.rs, xml.bind, annotation and faces. 102 Java files changed.  
- 🛡️ **Kept on javax as intended:**  
  - JDK packages (`javax.security.auth.*`, `javax.naming`, `javax.sql`)  
  - jsr305 `javax.annotation.Nonnull/Nullable`, restored by `PreserveJsr305Annotations` in 4 places  
- 🖥️ **Faces 4.1:** 35 XHTML views moved to the `jakarta.faces.*`, `jakarta.tags.*` and `primefaces` URNs. `faces-config.xml` is at 4.1.  
- 📦 **Platform API:** the EE 7 `jboss-javaee-7.0` jar was replaced by `jakarta.platform:jakarta.jakartaee-api:11.0.0` (provided).  
- 📄 **Descriptors set to EE 11 levels:** `web.xml` 6.1, `beans.xml` 4.1 (`bean-discovery-mode="all"` kept), `persistence.xml` 3.2, `forge.taglib.xml` 4.1. `validation.xml` is at 3.0.  
- 🧩 **FreeMarker templates:** `entity.ftl` moved to `jakarta.persistence`. `datasource.ftl` keeps its JDK javax imports, as intended.  
- 🎨 **PrimeFaces:** 6.1 → 14.0.18 with the `jakarta` classifier. The UI-visible items are left for the human UI gate (see below).  
  
## 3. 🛠️ Tools used  
  
- ☕ **Java SDK:** Amazon Corretto **25.0.4.1** (build 25.0.4.1+10-LTS), Maven 3.9.16  
- 🔧 **OpenRewrite:**  
  - `rewrite-maven-plugin` **6.42.0**, `rewrite-migrate-java` **3.38.0**, `rewrite-hibernate` **2.22.0**  
  - Run 1, in order: `UpgradeToJakartaEE11` → `UpgradeToJakartaFaces41` → `UpgradeToHibernate71` → `UpgradeToJava25`. 1 min 39 s, BUILD SUCCESS.  
  - Run 2: `MigrateJakartaEeTemplates` on `*.ftl/*.ftlh/*.ftlx`. 47 s, BUILD SUCCESS.  
  - The template-marker check after the Java leg was clean.  
- 🤖 **Amazon Kiro CLI:** `kiro-cli` **2.14.2**, model **claude-opus-5.5**. It used the migration knowledge base (docs 30, 31, 32, 33). The build-fix session took 6 min 24 s.  
  
## 4. 💻 Code changes  
  
- 🔁 **OpenRewrite, Java leg** (SourcesFileResults: 144 files = 102 `.java`, 35 `.xhtml`, 7 `.xml`):  
  - `JavaxMigrationToJakarta` and its sub-recipes: imports and fully qualified names in all entities, services, REST endpoints, views and ITs  
  - `JakartaFacesXhtmlEE9/EE10` (XHTML namespaces), `JavaxFacesConfigXmlToJakartaFacesConfigXml`, `JakartaFacesConfigXml41`  
  - `JavaxWebXml`, `JavaxBeansXml`, `JavaxPersistenceXml` and `JavaxBeanValidationXml` descriptor migrations  
  - `UpgradeBuildToJava11/17/21/25`, `UpgradePluginsForJava25` (war 3.5.1, surefire/failsafe 3.5.6), Byte Buddy pinned to 1.17.8  
  - `ReplaceUnusedVariablesWithUnderscore` (8 sites)  
- 🔁 **OpenRewrite, template leg:** 1 file (`entity.ftl`), `javax.persistence` → `jakarta.persistence`  
- 🤖 **Kiro CLI fixes after OpenRewrite:**  
  - ❌ → ✅ Fixed the `package jakarta.faces.* does not exist` errors in `AbstractBean`, `ExceptionInterceptor`, `LocaleBean` and 7 admin beans. The cause was the missing Faces 4.1 API, now supplied by the EE 11 platform API.  
  - 🧹 Removed the EE 10-level API jars the recipe had added in compile scope (ejb, cdi, persistence, validation, ws.rs, annotation), so no API jars end up in `WEB-INF/lib`.  
  - 🔄 `jaxb-runtime` 2.3.9 (javax implementation) → 4.0.6 (provided). `jakarta.xml.bind-api` 4.0.5 is now provided.  
  - 📜 `swagger-maven-plugin` 3.1.8 is set to `skipSwaggerGeneration=true`. It only reads javax.ws.rs annotations and was overwriting `swagger.json` with an empty contract. The committed contract is kept.  
  - 📄 Bumped the EE 11 descriptor versions and replaced the Facelets 1.0 DOCTYPE in `forge.taglib.xml`.  
  
## 5. ⏱️ Time savings estimate  
  
- 🔧 **OpenRewrite's own estimate:** 31 h 33 min (Java/Jakarta/Faces leg) + 5 min (templates) ≈ **31.6 h**  
- 🤖 **Kiro CLI build fix, dependency alignment, descriptor review and report:** about **1–1.5 engineer-days** of manual work, done in about 6.5 min. This is an estimate based on the scope: around 60 compile errors, POM rework, 5 descriptors, and a swagger regression found and handled.  
- 📈 **Total:** about **5 engineer-days** saved. The pipeline ran in about 10 minutes, plus the human review still needed (see Next steps).  
  
## 6. 🚀 Next steps  
  
- 🧪 **Deploy and test, CDI 4:** `beans.xml` keeps `bean-discovery-mode="all"`. `NumberProducer` (`@Produces @Named` fields, no scope) only works because of that. Deploy and open the shopping and admin pages.  
- 🖧 **Server:**  
  - Deploy on WildFly **40/41** (Jakarta EE 11). WildFly 26 cannot run this WAR.  
  - ⚠️ WildFly 41 is EE 11-certified only on Java SE 17/21. Choose SE 25 (recommended by WildFly, not certified) or SE 21 (certified).  
- 🔬 **Integration tests (not run, still on the javax line):**  
  - WildFly 26.1.3, wildfly-arquillian 3.0.1, RESTEasy 3.15.3 with `resteasy-jackson-provider`, and `arquillian-rest-client-impl-3x`  
  - `arquillian.xml` `jbossHome`  
  - Upgrade to WildFly 41, wildfly-arquillian 5.x and RESTEasy 6.2+, then run `mvn verify -Parquillian-wildfly-managed`.  
- 🗄️ **Hibernate 7 (server-provided):**  
  - No native queries, `StatelessSession`, `refresh()` or `lock()` were found, so none of the three silent changes applies.  
  - Smoke-test the JPQL named queries on WildFly's Hibernate 7.  
- 📜 **Swagger:** replace the kongchen plugin with a Jakarta-aware generator (e.g. `io.swagger.core.v3:swagger-maven-plugin-jakarta`), then turn generation back on.  
- 🎨 **PrimeFaces 15:** optional bump from 14.0.18, best done together with the UI gate items below.  
- 🧰 **Housekeeping:**  
  - Consider log4j 2.19.0 → a current 2.x.  
  - Consider JUnit 4 → JUnit 5.  
  - Remove the unused `version.maven.compiler.plugin` property.  
  
## PrimeFaces human UI gate  
  
Each site below changes what users see. All were **left unchanged for human UI review**. The build stays green, because PrimeFaces 14/15 still ships `p:calendar`.  
  
- 🎨 `pom.xml:112`: `org.primefaces.themes:all-themes` 1.1.0, managed dependency (legacy ThemeRoller themes) → a built-in PrimeFaces 15 theme (`saga-blue` / `arya-blue` / `vela-blue`), `primefaces-themes.jar` (Luna, Nova, Material, Bootstrap) or a Theme Designer theme, then drop `all-themes`. **Left unchanged for human UI review.**  
- 🎨 `pom.xml:196`: `org.primefaces.themes:all-themes` dependency (line 167 after this cycle's POM cleanup) → same PrimeFaces 15 theme replacement, then drop `all-themes`. **Left unchanged for human UI review.**  
- 🎨 `src/main/webapp/WEB-INF/web.xml:33`: `primefaces.THEME` = `bootstrap` (legacy theme) → a PrimeFaces 15 theme such as `saga-blue`, or a `primefaces-themes.jar` / Theme Designer theme. **Left unchanged for human UI review.**  
- 🔣 `src/main/webapp/resources/templates/template.xhtml:86`: FontAwesome `fa fa-user` → PrimeIcons `pi pi-user`, or keep an app-supplied FontAwesome. **Left unchanged for human UI review.**  
- 🔣 `src/main/webapp/resources/templates/template.xhtml:92`: FontAwesome `fa fa-sign-out` → PrimeIcons `pi pi-sign-out`. **Left unchanged for human UI review.**  
- 🔣 `src/main/webapp/resources/templates/template.xhtml:100`: FontAwesome `fa fa-sign-in` → PrimeIcons `pi pi-sign-in`. **Left unchanged for human UI review.**  
- 🔣 `src/main/webapp/resources/templates/template.xhtml:107`: FontAwesome `fa fa-shopping-cart` → PrimeIcons `pi pi-shopping-cart`. **Left unchanged for human UI review.**  
- 📅 `src/main/webapp/shopping/createaccount.xhtml:49`: `<p:calendar id="dateOfBirth" display="inline" navigator="true" showOn="button" pattern="d/M/yyyy">` → `<p:datePicker>` (`monthNavigator`/`yearNavigator`, `showIcon`, re-check `pattern`). **Left unchanged for human UI review.**  
- 📅 `src/main/webapp/shopping/updateaccount.xhtml:53`: `<p:calendar id="dateOfBirth" display="inline" navigator="true" showOn="button">` → `<p:datePicker>` (`monthNavigator`/`yearNavigator`, `showIcon`). **Left unchanged for human UI review.**  

## Kiro credit usage

💳 Kiro CLI credits used: 43.10  
