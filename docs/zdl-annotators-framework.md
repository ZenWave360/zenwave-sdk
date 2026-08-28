# ZDL Annotator Framework

**Status:** design settled — implementation in progress.

An annotation meta-framework for the ZenWave SDK. Annotators attach Java annotations to generated
code without hard-coding annotation packs into Handlebars templates. `JSpecifyAnnotator` was the
first pack; `JMoleculesAnnotator` is the pack that forced the framework to become general.

---

## 1. The problem

**One ZDL element produces many generated files, and the same Java model object needs different
annotations in each of them.**

A ZDL `entity` produces the domain POJO, a repository, transition classes, tests and fixtures.
A ZDL `service` produces an inbound port *and* an implementation. jMolecules wants `@AggregateRoot`
on the POJO but `@Repository` on the repository — never both on the same file.

Two further complications rule out the obvious solutions:

- Scoping by **ZDL building block** is too coarse: one block, many templates.
- Scoping by **template registration** is too rigid: advanced users replace templates, and
  `ProjectTemplates` is a subclassing API whose signatures must not break.

The answer is to let **each template declare what it generates**, and let annotators target that.

---

## 2. Core concept: artifact type

A template declares its artifact type inline, as the first argument to every annotation-emitting
helper:

```handlebars
{{annotate 'domain.entity' entity.javaEntity entity}}
```

Artifact types name **what the template generates**, never what the model happens to be. One
template renders aggregate roots, plain entities and value objects alike — deciding between them is
the annotator's job, from ZDL facts.

Nothing changes in `ProjectTemplates`, `TemplateInput`, or any `addTemplate(...)` registration.

### Why not an id on `TemplateInput`

Rejected: it requires either changing the return type of `addTemplate(...)` — a binary-incompatible
change to a public subclassing API (the JVM method descriptor changes, so externally compiled
plugins would throw `NoSuchMethodError`) — or rewriting every `new TemplateInput(...)` registration.
Declaring it in the `.hbs` keeps custom templates self-describing, with nothing to keep in sync on
the Java side.

### `ArtifactType` is extensible, not a closed enum

Multiple templates deliberately share one value (`core/domain/{jpa,mongodb,vo}/Entity.java.hbs` all
declare `domain.entity`), so this is a *classification*, not an identity — hence `ArtifactType`,
not `ArtifactId`.

```java
public interface ArtifactType {
    String id();
}

public enum CoreArtifactType implements ArtifactType {
    DOMAIN_ENTITY("domain.entity"),
    // ...
}
```

Custom annotators and templates define their own without becoming second-class:

```java
enum AcmeArtifactType implements ArtifactType { COMMAND_HANDLER("acme.command-handler"); /* ... */ }

Annotation.on("com.acme.Handled", AcmeArtifactType.COMMAND_HANDLER);
Annotation.on("com.acme.Handled", () -> "acme.command-handler");   // ad-hoc
```

Matching happens on the raw string, so a single factory covers both and there is no varargs overload
ambiguity.

---

## 3. Framework changes

Seven changes, all in `zenwave-sdk-cli` so every generator benefits — not only
`BackendApplicationDefaultGenerator`.

### 3.1 `Annotation` carries its artifact types

```java
public record Annotation(String name, String value, Map<String, Object> options, Set<String> artifactTypes) {

    /** empty artifactTypes = renders wherever the owning node renders */
    public boolean appliesTo(String artifactType) {
        return artifactTypes == null || artifactTypes.isEmpty()
                || (artifactType != null && artifactTypes.contains(artifactType));
    }

    public static Annotation of(String name) { ... }                         // artifact-independent
    public static Annotation on(String name, ArtifactType... types) { ... }  // artifact-scoped
}
```

Names are always fully qualified: `jakarta.persistence.Entity` and
`org.jmolecules.ddd.annotation.Entity` cannot share a simple name, and FQNs remove any need for an
import-collection pass.

### 3.2 `Annotated` — a marker so the helper is genuinely generic

Required by the helper, and the place idempotence is enforced (see §3.7):

```java
public interface Annotated {
    List<Annotation> annotations();

    default void addAnnotation(Annotation annotation) {
        if (!annotations().contains(annotation)) {
            annotations().add(annotation);
        }
    }
}
```

Implemented by `Entity`, `Field`, `Relationship`, `Service`, `ServiceMethod`, `MethodParameter`,
`ReturnType`, `Event`, `Enum`, `EnumValue`, `Input`, `Output`.

### 3.3 Complete the Java model mirrors

Today `JavaZdlModel` only cross-links services and methods:

```java
service.put("javaService", javaService);
map.put("javaServiceMethod", serviceMethod);
```

Templates cannot reach an annotation list for anything else. All mirrors must exist:

```
entity.javaEntity      field.javaField        relationship.javaRelationship
event.javaEvent        input.javaInput        output.javaOutput
enum.javaEnum          service.javaService    method.javaServiceMethod
```

Plus a **synthetic identity field**: `id` and `version` are injected by templates and are not ZDL
fields, so `Entity` gains `idField` for `@Identity` to attach to.

`createRelationship` currently returns an all-null record and must be populated before relationship
annotations (or jMolecules `@Association`) are possible.

### 3.4 Expand `ZDLAnnotator` traversal — a headline change, not a detail

The current default traversal visits **services → methods → parameters only**. It must visit
entities, fields, the synthetic id field, relationships, events, external events, inputs, outputs,
enums, enum values, services, methods, parameters and return types. This is the bulk of the
framework work.

### 3.5 Artifact-level annotations for elementless artifacts

`EventPublisher`, `DefaultEventPublisher`, `EventListeners` and `package-info` have no backing ZDL
element. They get an artifact-keyed store rather than a second mechanism:

```java
public Map<String, List<Annotation>> artifactAnnotations = new LinkedHashMap<>();
public void addArtifactAnnotation(ArtifactType artifactType, Annotation annotation) { ... }
```

```handlebars
{{annotate 'outbound.event-publisher-port'}}
public interface EventPublisher {
```

### 3.6 One rewritten `AnnotationHelper`, registered globally

The existing helper is stub and dead code: `annotate(ServiceMethod, …)` returns the literal
`"// todo"`, `annotate(MethodParameter, …)` is private and unreachable, and `addImports` is never
called from any template. It is rewritten, moved to core, and registered in `HandlebarsEngine`
alongside `CustomHandlebarsHelpers` so `{{annotate}}` resolves for **every** generator.

```java
public static String annotate(String artifactType, Options options) {
    // elementless means no element argument at all. A null element argument is an absent model node
    // (a value object has no idField) and renders nothing, never the artifact level annotations.
    List<Annotation> annotations = options.params.length == 0
            ? artifactAnnotations(artifactType, options)
            : options.param(0) instanceof Annotated annotated ? annotated.annotations() : List.of();
    return annotations.stream()
            .filter(a -> a.appliesTo(artifactType))
            .map(AnnotationHelper::render)
            .distinct()
            .collect(Collectors.joining("\n"));
}
```

### 3.7 Annotate once, idempotently

Annotators currently run per-generator in `ZDLProjectGenerator`, mutating a model shared by every
generator in the chain — so annotations accumulate across runs.

**Structural fix:** run annotators in `ZDLProcessor`, immediately after `new JavaZdlModel(zdlModel)`.
The model is rebuilt on every `process()` call, so re-processing rebuilds rather than accumulates,
and every downstream generator renders an already-annotated model.

**Defensive fix:** `Annotated.addAnnotation` is contains-guarded (§3.2). `Annotation` is a record,
so `equals` is structural across all four components and `contains` is correct for free.

Because contribution is idempotent, `ZDLProjectGenerator` can keep running
`templates.getZDLAnnotators()` for backward compatibility — anyone who already overrides it in a
`ProjectTemplates` subclass keeps working, and double contribution is a no-op.

`.distinct()` in the helper is then belt-and-braces for the legitimate case of two different
annotators emitting the same annotation.

---

## 4. Scoping must reach signature helpers

Type and field annotations flow through `{{annotate}}`, but **parameter and return-type annotations
render through a different path**: `ZDLJavaSignatureUtils.methodParametersSignature` builds the
signature string and inlines `parameter.annotations()` directly. That path cannot see the artifact
type, so without a change the framework would support scoping at class and field level but silently
ignore it for parameters — a trap for any future pack.

Every helper that emits annotations therefore takes the artifact type first:

```handlebars
{{annotate                  'domain.entity'        entity.javaEntity entity}}
{{methodParametersSignature 'inbound.service-port' method}}
{{returnType                'inbound.service-port' method}}
```

`methodParametersCallSignature` is **not** affected — it renders parameter names only.

### The shared-partial wrinkle

`core/implementation/partials/serviceMethodSignature.hbs` is included by **both** the port and the
implementation (`Service.java.hbs:25`, `ServiceImpl.java.hbs:90`, and the Kotlin equivalents), so
the artifact type cannot be a literal inside the partial. It arrives as a partial hash parameter:

```handlebars
{{!-- core/inbound/Service.java.hbs --}}
{{~> (partial '../implementation/partials/serviceMethodSignature') artifactType='inbound.service-port'}};

{{!-- core/implementation/imperative/ServiceImpl.java.hbs --}}
{{~> (partial '../partials/serviceMethodSignature') artifactType='application.service-impl'}} {
```

```handlebars
{{!-- serviceMethodSignature.hbs — one file, both artifacts --}}
public {{{returnType artifactType method}}} {{method.name}}({{{methodParametersSignature artifactType method}}})
```

---

## 5. `CoreArtifactType` vocabulary

| Artifact type | Template |
|---|---|
| `domain.entity` | `core/domain/{jpa,mongodb,vo}/Entity.java.hbs` |
| `domain.aggregate` | `core/domain/common/Aggregate.java.hbs` |
| `domain.aggregate-transitions` | `core/domain/common/AggregateTransitions.java.hbs` |
| `domain.entity-transitions` | `core/domain/common/EntityTransitions.java.hbs` |
| `domain.event` | `core/domain/common/DomainEvent.java.hbs` |
| `domain.enum` | `core/domain/common/DomainEnum.java.hbs` |
| `inbound.service-port` | `core/inbound/Service.java.hbs` |
| `inbound.dto` | `core/inbound/dtos/InputOrOutput.java.hbs` |
| `inbound.dto-enum` | `core/domain/common/InputEnum.java.hbs` |
| `inbound.event-enum` | `core/domain/common/EventEnum.java.hbs` |
| `application.service-impl` | `core/implementation/{style}/ServiceImpl.java.hbs` |
| `application.service-mapper` | `core/implementation/mappers/ServiceMapper.java.hbs` |
| `application.events-mapper` | `core/implementation/mappers/EventsMapper.java.hbs` |
| `application.base-mapper` | `core/implementation/mappers/BaseMapper.java.hbs` |
| `outbound.repository-port` | `core/outbound/{persistence}/{style}/EntityRepository.java.hbs` |
| `outbound.event-publisher-port` | `core/outbound/events/EventPublisher.java.hbs` |
| `infrastructure.event-publisher` | `infrastructure/events/DefaultEventPublisher.java.hbs` |
| `infrastructure.repository` | infrastructure repository implementations |
| `adapter.web-controller` | `web/{webFlavor}/ServiceApiController.java.hbs` *(openapi-controllers)* |
| `adapter.web-mapper` | `web/mappers/*.java.hbs` *(openapi-controllers)* |
| `adapter.event-listener` | `adapters/events/EventListeners.java.hbs` |
| `adapter.event-listener-mapper` | `adapters/events/EventListenersMapper*.java.hbs` |
| `adapter.asyncapi-consumer` | `adapters/events/asyncapi/{style}/ConsumerService.java.hbs` |
| `adapter.asyncapi-mapper` | `adapters/events/asyncapi/EventsMap*.java.hbs` |
| `package-info.module` | `package-info.java.hbs` |
| `package-info.common` | `common-package-info.java.hbs` |
| `package-info.inbound-dtos` | `core/inbound/dtos/package-info.java.hbs` |
| `package-info.infrastructure` | `infrastructure/package-info.java.hbs` |

**Cross-plugin reach.** `adapter.web-controller` and `adapter.asyncapi-consumer` are operational with
no generator changes: `OpenAPIControllersGenerator` (line 314) and
`BackendApplicationAsyncApiAdaptersGenerator` (line 441) both already put the ZDL model — which
carries `javaModel` — into their template model. They only need the `{{annotate}}` call in the
template.

There is deliberately **no `adapter.asyncapi-producer`**: `SpringCloudStreams3Generator` builds no
ZDL template model (it generates from an AsyncAPI spec), so such a constant could never fire.

**Test templates declare no artifact type and never call `{{annotate}}`**, so annotations cannot leak
into generated test sources. That is a property, not an omission.

---

## 6. jMolecules pack

Annotation-based only. No `AggregateRoot<T,ID>` generics, no `Association<T,ID>` types.

### DDD

| Source | Annotation | Artifact type |
|---|---|---|
| `@aggregate` entity | `@AggregateRoot` | `domain.entity` |
| plain entity | `@Entity` | `domain.entity` |
| `@vo` / `@embedded` | `@ValueObject` (opt-in) | `domain.entity` |
| `input` / `output` | `@ValueObject` | `inbound.dto` |
| synthetic `id` field | `@Identity` | `domain.entity` |
| entity with a repository | `@Repository` | `outbound.repository-port` |
| Spring Modulith module | `@Module` | `package-info.module` |
| non-`@asyncapi` event | `@DomainEvent` | `domain.event` |

`@AggregateRoot` is meta-annotated with `@Entity` — emit one or the other, never both.

### Hexagonal

| Artifact type | Annotation |
|---|---|
| `inbound.service-port` | `@PrimaryPort` |
| `application.service-impl` | `@Application` |
| `outbound.event-publisher-port` | `@SecondaryPort` |
| `infrastructure.event-publisher` | `@SecondaryAdapter` |
| `adapter.event-listener` | `@PrimaryAdapter` |
| `adapter.web-controller` | `@PrimaryAdapter` |
| `adapter.asyncapi-consumer` | `@PrimaryAdapter` |
| `outbound.repository-port` | `@SecondaryPort` |

### Layered

`LayeredProjectLayout` is the three tier `web -> service -> repository`, with `domain` models used
by all three — not Evans' four layers. Its own package map gives it away: `outboundRepositoryPackage`
and `infrastructureRepositoryPackage` are the same package, so the repository *is* the persistence
tier.

The annotations name those tiers as they are:

| tier | annotation |
|---|---|
| entities, domain events | `@DomainLayer` |
| service port and impl | `@ApplicationLayer` |
| repository | `@InfrastructureLayer` |
| event publisher implementation | `@InfrastructureLayer` |
| controllers, inbound listeners | `@InterfaceLayer` |

**`ensureLayering()` is deliberately not generated for these projects.** That rule encodes Evans'
layering, where infrastructure is the bottom substrate:

```java
.whereLayer(DOMAIN).mayOnlyBeAccessedByLayers(APPLICATION, INTERFACE)
.whereLayer(INFRASTRUCTURE).mayOnlyBeAccessedByLayers(DOMAIN, APPLICATION, INTERFACE)
```

`INFRASTRUCTURE` may not reference `DOMAIN`. But a Spring Data repository names its aggregate in its
own type signature — `JpaRepository<Customer, Long>` — so in a three tier project the rule cannot
pass for any model. The two ways to make it green are to relabel the repository as `@DomainLayer`,
which misdescribes the architecture, or to split persistence and domain into mirror types, which is
a large structural cost for a check. Neither is worth it, so the annotations stay truthful and this
one rule is omitted. `HEXAGONAL` is unaffected: `@SecondaryPort` and `@SecondaryAdapter` fit ZenWave's
structure, and `ensureHexagonal(STRICT)` passes on generated output.

### Deliberate omissions

- **No ddd `@Service`.** It denotes a *domain* service; ZenWave services are application/use-case
  services, so emitting it would misrepresent the model.
- **`@SecondaryPort` on repositories.** The repository interface is generated into the outbound
  package, which is what makes it the aggregate's secondary port. It also extends `JpaRepository`,
  so the port is not technology-free; splitting the port from the Spring Data adapter remains the
  real fix, but the artifact's role in the layout is unambiguous.
- **No `@Association`.** Blocked on `createRelationship` returning an all-null record.
- **`@ValueObject` on inputs, outputs, `@vo` and `@embedded`.** All of them are identity-free and
  attribute-defined. An earlier draft gated the entity case behind a `jmoleculesValueObjects` flag,
  on the grounds that a mutable JPA `@Embeddable` is a weak value object — but the generated DTOs are
  equally mutable, so the argument applied to both cases or neither. It also left `@vo` entities with
  no stereotype at all despite the model saying exactly what they are.
- **No `@BoundedContext`.** ZenWave generates one application per model, so the marker would be
  redundant, and there is no base-package `package-info.java` to host it without generating a new
  file for every project.

---

## 7. Rendered result

`customer-address-relational`, hexagonal, `useJMolecules=true`, `useJSpecify=true`:

```java
@org.jmolecules.ddd.annotation.AggregateRoot
public class Customer implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @org.jmolecules.ddd.annotation.Identity
    private Long id;

    @Version
    private Integer version;                     // deliberately unannotated
}

@SuppressWarnings("unused")
@Repository
@org.jmolecules.ddd.annotation.Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {}

@org.jmolecules.architecture.hexagonal.PrimaryPort
@org.jspecify.annotations.NullMarked
public interface CustomerService {
    Optional<Customer> updateCustomer(@org.jspecify.annotations.Nullable Long id, Customer input);
}

@Service
@Transactional
@org.jmolecules.architecture.hexagonal.Application
@org.jspecify.annotations.NullMarked
public class CustomerServiceImpl implements CustomerService {}
```

`@NullMarked` lands on **both** port and impl from a single unscoped `Annotation.of(...)`, while
`@PrimaryPort` and `@Application` land on exactly one artifact each — all three from the same
`JavaZdlModel.Service`.

---

## 8. Configuration

`useJSpecify` and `useJMolecules` are `ZDLProcessor` options, so they apply across every generator
whose chain includes `ZDLProcessor` — which is what makes the cross-plugin artifacts of §5 work with
no generator changes.

Options reach the processor through `MainGenerator.applyConfiguration`, which binds the plugin's flat
option map onto **every** chain element with a matching public field. The same method also binds it
onto a generator's `templates` object, so a `@DocumentedOption` declared on a `ProjectTemplates`
subclass is configurable too.

That second binding is why **a flag must not construct annotators in two places**. The previous
`BackendApplicationProjectTemplates.useJSpecify` was therefore removed along with its
`getZDLAnnotators()` override: with the same field name on both the processor and the templates,
`useJSpecify true` bound to both and added `JSpecifyAnnotator` twice. Idempotent contribution (§3.7)
made that harmless, but correctness should not depend on the safety net.

`ProjectTemplates.getZDLAnnotators()` remains as the extension point for custom annotators added by a
Java subclass; `ZDLProjectGenerator` still calls it.

A flag may legitimately be read in both places when the second reader does something other than build
an annotator. `BackendApplicationProjectTemplates.useJMolecules` is such a case: it only decides
whether to emit the generated `JMoleculesArchitectureTest`, and constructs nothing.

The architecture vocabulary **is derived from `ProjectLayout`**, by
`JMoleculesAnnotator.architectureOf`. There is no option to set or override it:

| layout | derived architecture |
|---|---|
| `HexagonalProjectLayout`, `CleanHexagonalProjectLayout`, `DefaultProjectLayout` | `HEXAGONAL` |
| `LayeredProjectLayout` | `LAYERED` |
| `SimpleDomainProjectLayout`, `CleanArchitectureProjectLayout` | `NONE` |

A layout that needs a different vocabulary is mapped here, or extends the layout whose vocabulary it
wants — which is more robust than a flag, since the layout is the thing that decides where ports and
adapters actually live. `useJMolecules` is therefore the only jMolecules option.

The layout reaches the processor the same way it reaches every other chain element:
`MainGenerator.applyConfiguration` writes it into any plugin with a public `layout` field. (An earlier
draft of this document claimed the processor could not know the layout. That was wrong — the
injection has always been there.)

Deriving matters because the two settings are not independent. `SimpleDomainProjectLayout` defines
`outboundPackage` as the base package itself, so a repository generated under it sits in no port
package at all; pairing that layout with `HEXAGONAL` would stamp `@SecondaryPort` on a class that
holds no such role. Layouts without a port boundary therefore derive `NONE` and keep only the DDD
annotations.

`useJSpecify` and `useJMolecules` are listed in
`BackendApplicationDefaultPlugin.mainOptions` so they show up in plugin help and IDE completion.

---

## 8b. Generated verification

With `useJMolecules`, `BackendApplicationProjectTemplates` also emits
`src/test/java/JMoleculesArchitectureTest.java`, kept **separate** from the existing
`ArchitectureTest`. The two verify different things: `ArchitectureTest` matches package names and is
therefore gated to `CleanHexagonalProjectLayout`, while these rules read the annotations themselves
and hold for any layout.

The architecture-specific rule is chosen from `JavaZdlModel.jmoleculesArchitecture`, which the
annotator records once it has resolved the vocabulary.

| rule | scope |
|---|---|
| `annotatedEntitiesAndAggregatesNeedToHaveAnIdentifier` | always |
| `valueObjectsMustNotReferToIdentifiables` | always |
| `entitiesShouldBeDeclaredForUseInSameAggregate` | always |
| `aggregateReferencesShouldBeViaIdOrAssociation` | always, opt out by hand |
| `ensureHexagonal(STRICT)` | `HEXAGONAL` |
| `ensureLayering()` | *not generated* — see below |

Also available in `jmolecules-archunit` but not generated: `ensureLayeringStrict()`,
`ensureOnionSimple()`, `ensureOnionClassical()`, the `StereotypeLookup` overloads, and the
`JMoleculesDddRules.all()` / `JMoleculesRules.all()` bundles.

`VerificationDepth` controls how much an adapter may reach past the ports: `LENIENT` lets adapters
and ports use non-port application code and lets primary adapters use secondary ports directly;
`SEMI_STRICT` withdraws that from primary adapters; `STRICT` withdraws it from every adapter.

The rules were checked twice, by compiling generated output and running them directly.

Against `customer-address-postgres-json`, whose `Address` is `@jsonb` embedded, all eleven pass
including `ensureHexagonal(STRICT)`. Against `customer-address-relational`, which has the
bidirectional `Customer{addresses} to Address{customer}` that four of the five e2e projects share,
one fails:

```
FAIL  ddd.aggregateReferencesShouldBeViaIdOrAssociation
   Field Address.customer refers to an aggregate root (Customer).
   Rather use an identifier reference or Association!
```

`entitiesShouldBeDeclaredForUseInSameAggregate` passes there, so jMolecules does place `Address`
inside `Customer`'s aggregate; it rejects the reference regardless, because only an identifier or an
`Association` may point at an aggregate root. The rule ships **enabled** anyway: a project that does
not want it comments out the `@ArchTest`, and since both test templates register with
`skipOverwrite = true` the generated file is never rewritten, so that edit is permanent.

The layered and onion rules pass vacuously on a hexagonal project, which is the remaining caveat on
this evidence.

Requires `org.jmolecules.integrations:jmolecules-archunit` (test scope), managed by the jMolecules
BOM. The five e2e projects that enable jMolecules already carried `archunit-junit5-api` and
`archunit-junit5-engine`, so only that one dependency was added to each.

### A bug this uncovered

Both `ArchitectureTest` templates, Java and Kotlin, interpolated a bare `{{moduleBasePackage}}`,
which resolves to nothing — the correct expression is `{{layout.moduleBasePackage}}`, which the very
same files already used for their `package` statement. Every generated project therefore shipped:

```java
@AnalyzeClasses(packages = "", importOptions = DoNotIncludeTests.class)
...
.consideringOnlyDependenciesInAnyPackage("..")
```

An empty `packages` imports no classes, so the rules passed over an empty set: a dead test that had
never verified anything. Replaying the same rules against the compiled
`customer-address-postgres-json` classes shows both readings:

| dependency scope | result |
|---|---|
| `"io.zenwave360.example.."` (fixed) | both rules pass |
| `".."` (as shipped) | layered rule reports 1116 violations |

So the empty scope was hiding in two directions at once — no classes imported, and every JDK and
Spring dependency considered. Fixed in both templates and pinned by
`architecture_tests_analyse_the_module_package` and its Kotlin counterpart.

Verified for `customer-address-postgres-json` only, since it is the one e2e project with compiled
output on disk. Other projects on the default layout also generate `ArchitectureTest` and now run it
for real; confirming those needs `-De2e.tests.skip=false`.

---

## 9. Migration notes

- `{{annotate service.javaService service}}` becomes
  `{{annotate 'inbound.service-port' service.javaService service}}` — two in-repo call sites.
- `{{{methodParametersSignature method}}}` and `{{{returnType method}}}` gain a leading artifact
  type; four `.hbs` call sites each, Java and Kotlin.
- Generated projects need `jmolecules-ddd`, `jmolecules-events` and the architecture artifact on the
  classpath. Adding them to the generated `pom.xml` is a separate plugin/BOM concern.

## 10. Known limitations

- Templates registered twice share one artifact type. `core/inbound/dtos/InputOrOutput.java.hbs`
  serves both inputs and outputs, so both get `inbound.dto`. A template needing two identities must
  branch on context.
- `SpringCloudStreams3Generator` output cannot be annotated (no ZDL model).
- `jpa/reactive/EntityRepository.java.hbs`, `mongodb/reactive/EntityRepository.java.hbs` and
  `web/webflux/ServiceApiController.java.hbs` are empty placeholder templates, so the reactive and
  webflux paths carry no annotations until those templates are written.
- Mapper artifacts (`application.*-mapper`, `adapter.web-mapper`) are in the vocabulary but no
  template calls `{{annotate}}` for them yet, so the layered `@ApplicationLayer` mapping does not
  reach them. `inbound.dto` is wired up.
- `@Association` is not implemented, though `createRelationship` is now populated so it is unblocked.
