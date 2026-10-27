# Brief: zdl-spring-mvc-thymeleaf

## Problem

We want self-contained services with their own UI, without an OpenAPI contract between a backend and a separate frontend. That extra contract slows the team down. It also stops a coding agent from working in one stack and one module. REST APIs stay for integrations only.

Today, REST controllers come almost finished from OpenAPI Generator, and nobody argues about their structure. We want the same for a server-side UI: one clear convention and a skeleton that removes the repetitive work.

## Goal

From an existing ZDL service, generate a Spring MVC controller and draft Thymeleaf templates with htmx. The result is a skeleton that compiles and makes sense. It does not have to reproduce the screen flow the customer expects. The developer completes that.

First iteration: Spring MVC, Thymeleaf and htmx.

## Principles

- No new ZDL constructs. We start from the service and its current HTTP annotations.
- One service, one controller. No controller per screen and no God class.
- Generated code is a starting point, like the services we already generate. The developer customizes it.
- Deterministic and lightly opinionated. There is one convention, and you change it by editing the code.

## Plugin

Name: `zdl-spring-mvc-thymeleaf`.

It follows the `input-technology` pattern of `openapi-controllers` and `openapi-spring-webtestclient`. The name is tied to the template engine on purpose. Supporting another engine (JTE, Pebble) means a sibling plugin, not an option inside this one.

## Input

The ZDL service as it exists today. `@ui` on the service decides which adapter is generated. The business contract is the same one a REST adapter would use, so services are not duplicated.

```zdl
@ui("/customers")
service CustomerService for (Customer) {
  @post("/customers")
  createCustomer(CustomerInput) Customer withEvents CustomerEvent [CustomerCreated|CustomerCreatedFailed]

  @put("/customers/{customerId}")
  updateCustomer(id, CustomerInput) Customer? withEvents CustomerEvent CustomerUpdated

  @delete("/customers/{customerId}")
  deleteCustomer(id) withEvents CustomerDeleted

  @get("/customers/{customerId}")
  getCustomer(id) Customer?

  @get({path: "/customers", params: {search: "string"}})
  @paginated
  listCustomers() Customer[]
}
```

## Generation conventions

| ZDL method | Generated handler |
|---|---|
| `@get` with `id` and return type `Customer?` | Renders the edit form. If the result is empty, 404. |
| `@get` with a null `id` | Not a real path. The generator creates `GET /customers/new`, which renders the same form empty. |
| `@get` with `@paginated` and `search` | List with search box. With the `HX-Request` header, it returns only the results fragment. |
| `@post` with input | `@Valid` and `BindingResult`. On errors, it re-renders the form. On success, it redirects. |
| `@put` with input | Same as POST. |
| `@delete` | Calls the service and redirects. |

`withEvents` does not affect the controller. It belongs to the service.

One form serves both create and edit.

### Redirects

The skeleton generates a reasonable redirect so the flow compiles and works. After create or update, it goes to the entity detail. After delete, it goes to the list. This is not a product decision. The developer changes it to fit the flow they want.

### Control type

The control is deduced from the input data type. `@widget` stays as an override for unusual cases.

| ZDL field | Control |
|---|---|
| String | text |
| String with `@email` | email |
| String with a large maxlength | textarea |
| Integer, Long, Decimal | number |
| LocalDate | date |
| Instant | datetime-local |
| Boolean | checkbox |
| enum | select |

### Validation

ZDL validations (required, maxlength, min, pattern) are generated as Bean Validation on the input and as HTML attributes on the form.

## Lookups

In this version, lookups are not defined in ZDL. The generator does not know which fields need select options, or where those options come from.

This is developer work. The developer decides two things on the generated code:

- Where the data comes from: database, static JSON, external API or cache.
- Where it is rendered: on the server, when the handler builds the model, or on the client, with an htmx fragment requested from the browser.

The generator makes neither decision. The draft templates render each field with the control that matches its type, and the developer replaces it with a select when needed.

## Generated code structure

- **Controller:** one class per service, with the seven handlers. Generated once, as a skeleton.
- **Thymeleaf templates:** `customerList.html` (with a `results` fragment) and `customerForm.html`. Generated once, as a draft.
- **Input types and validations:** the types that already exist in ZDL.

### Controller skeleton

```java
@Controller
class CustomersController {

    private final CustomerService customerService;
    private final CustomerMapper mapper;

    CustomersController(CustomerService customerService, CustomerMapper mapper) {
        this.customerService = customerService;
        this.mapper = mapper;
    }

    @GetMapping("/customers")
    String listCustomers(@RequestParam(required = false) String search,
                         Pageable pageable,
                         @RequestHeader(name = "HX-Request", required = false) String htmx,
                         Model model) {
        model.addAttribute("page", customerService.listCustomers(search, pageable));
        model.addAttribute("search", search);
        return htmx != null ? "customers/customerList :: results" : "customers/customerList";
    }

    @GetMapping("/customers/new")
    String newCustomer(Model model) {
        model.addAttribute("customer", new CustomerInput());
        return "customers/customerForm";
    }

    @GetMapping("/customers/{customerId}")
    String getCustomer(@PathVariable UUID customerId, Model model) {
        var customer = customerService.getCustomer(customerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("customerId", customerId);
        model.addAttribute("customer", mapper.toInput(customer));
        return "customers/customerForm";
    }

    @PostMapping("/customers")
    String createCustomer(@Valid @ModelAttribute("customer") CustomerInput input,
                          BindingResult errors) {
        if (errors.hasErrors()) {
            return "customers/customerForm";
        }
        var created = customerService.createCustomer(input);
        return "redirect:/customers/" + created.getId();
    }

    @PutMapping("/customers/{customerId}")
    String updateCustomer(@PathVariable UUID customerId,
                          @Valid @ModelAttribute("customer") CustomerInput input,
                          BindingResult errors, Model model) {
        if (errors.hasErrors()) {
            model.addAttribute("customerId", customerId);
            return "customers/customerForm";
        }
        customerService.updateCustomer(customerId, input)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return "redirect:/customers/" + customerId;
    }

    @DeleteMapping("/customers/{customerId}")
    String deleteCustomer(@PathVariable UUID customerId) {
        customerService.deleteCustomer(customerId);
        return "redirect:/customers";
    }
}
```

## Assumptions

- `listCustomers` returns `Page<Customer>` and accepts `search` and `Pageable`.
- A `CustomerMapper` exists with `toInput(Customer)`.
- For `PUT` and `DELETE` from plain HTML forms, set `spring.mvc.hiddenmethod.filter.enabled=true`. With htmx, use `hx-put` and `hx-delete` directly.

## Work left for the developer

Estimate: one day to get a presentable customer CRUD. This is an estimate, not a measurement.

- Lookups: data source and rendering location.
- Template design, texts, i18n and error messages.
- Security: who can edit or delete.
- Screen flow and redirects, according to the product.
- Fields that do not follow the type convention, and list columns that should go.

## Ownership

Controllers and templates are generated once. They are not regenerated, so a change in the ZDL does not update them. This is the same cost we already accept with generated services.

## Out of scope

- Vaadin Flow. It will be a separate plugin, `zdl-vaadin`, in a later iteration. Resolution of fields, controls and validations is shared, and should move to a common module only when that plugin starts, not before. Vaadin-specific parts are the `Grid`, the `Binder`, the `@Route` and the shape of the views, which are classes and not templates.
- Any screen or layout language.
- Two-way sync with edited HTML or code.

## Pending decisions

- Output package name and location.
- Which style library the templates assume by default, if any.
- Pagination in the list template: custom component or the Spring Data one.
