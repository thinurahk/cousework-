# Smart Campus Sensor & Room Management API

## API Design Overview
This project provides a robust RESTful API built on JAX-RS (Jersey) to manage a "Smart Campus" infrastructure. The API handles two main entities: `Rooms` and `Sensors`, managing their physical relationships and history logs out of a singleton in-memory `DataStore`. 

Key design elements include:
- **Stateless interactions**: Following core RESTful specifications via standard HTTP methods (GET, POST, DELETE).
- **Sub-Resource Locators**: Abstracting complex nesting (e.g., `/sensors/{id}/readings`) into their own controller boundaries (`SensorReadingResource`).
- **Comprehensive Error Handling**: Explicit Exception Mapping handling business constraints (returning proper HTTP 403, 409, 422, and 500 error boundaries without leaking software stack traces).

## Build & Launch Instructions
Since this backend leverages the Maven build system and Jakarta EE libraries, it can be launched directly through standard Java IDEs via the servlet container.
1. Download or clone this GitHub repository to your local machine.
2. Open your IDE (e.g., Apache NetBeans or Eclipse Enterprise).
3. Select **File > Open Project** and choose the `SmartCampus` folder.
4. Right-click the project in the hierarchy menu and select **Clean and Build**.
5. Right-click the project again and select **Run** (or deploy it directly to your GlassFish/Tomcat server).
6. By default, API will be hosted at `http://localhost:8080/SmartCampus/api/v1/`.

## 5 Sample cURL Interactions

**1. Create a New Room**
```bash
curl -X POST http://localhost:8080/SmartCampus/api/v1/rooms \
-H "Content-Type: application/json" \
-d '{"id":"CLASS-101", "name":"Main Hall", "capacity":100}'
```

**2. List All Rooms**
```bash
curl -X GET http://localhost:8080/SmartCampus/api/v1/rooms \
-H "Accept: application/json"
```

**3. Register a Sensor (Links to Room)**
```bash
curl -X POST http://localhost:8080/SmartCampus/api/v1/sensors \
-H "Content-Type: application/json" \
-d '{"id":"TEMP-01", "type":"Temperature", "status":"ACTIVE", "currentValue":22.5, "roomId":"CLASS-101"}'
```

**4. Filter Sensors by Type**
```bash
curl -X GET "http://localhost:8080/SmartCampus/api/v1/sensors?type=Temperature" \
-H "Accept: application/json"
```

**5. Submit a Nested Sensor Reading**
```bash
curl -X POST http://localhost:8080/SmartCampus/api/v1/sensors/TEMP-01/readings \
-H "Content-Type: application/json" \
-d '{"value": 24.1}'
```

---

## Conceptual Report & Questions

### Part 1: Service Architecture & Setup
**Q: Explain the default lifecycle of a JAX-RS Resource class. Is a new instance instantiated for every incoming request, or does the runtime treat it as a singleton? Elaborate on how this impacts in-memory data structures.**
By default, JAX-RS operates on a "per-request" lifecycle. The runtime instantiates a completely new instance of the Resource class (e.g., `SensorRoomResource`) for every single incoming HTTP request, ensuring standard statelessness. To prevent data resetting to zero on every API call, in-memory structures (like our `DataStore`) must be defined as `static` or injected as Singletons so that the globally shared variables survive the short-lived controller destruction. To avoid race conditions in production environments, concurrent data structures (like `ConcurrentHashMap`) should be used over standard maps to safely handle simultaneous modifying threads.

**Q: Why is the provision of "Hypermedia" (HATEOAS) considered a hallmark of advanced RESTful design?**
Hypermedia transforms a static data API into a dynamic state engine. Instead of a developer having to rigorously read external documentation to know what URLs do what, HATEOAS directly injects logical navigational URLs directly inside the JSON response. This benefits developers heavily because if structural paths ever change on the server, the client will dynamically follow the newly updated hypermedia links automatically without failing.

### Part 2: Room Management
**Q: When returning a list of rooms, what are the implications of returning only IDs versus returning the full room objects?**
Returning only IDs saves massive amounts of network bandwidth and prevents over-fetching payloads, which creates much faster HTTP response times. However, the consequence is "under-fetching"; a client application might then be forced to fire hundreds of sequential `GET /rooms/{id}` requests sequentially to fetch the actual UI names, heavily throttling client-side processing speeds. Returning full objects solves UI drawing speeds immediately at the cost of heavier initial bandwidth loads.

**Q: Is the DELETE operation idempotent in your implementation? Provide a detailed justification.**
Yes, my `DELETE /rooms/{roomId}` endpoint is idempotent. Idempotency guarantees that executing the same network call explicitly multiple times will safely yield the exact same server state as executing it just once. If a client mistakenly sends the exact same DELETE request for a room multiple times, the first call will successfully remove the mapping and return `204 No Content`. Any subsequent duplicate calls will merely identify the target mapping is missing and harmlessly return `404 Not Found`; crucially, it will never trigger a destructive loop or crash the system map. 

### Part 3: Sensor Operations & Linking
**Q: Explain the technical consequences if a client attempts to send data in a different format than specified by @Consumes(MediaType.APPLICATION_JSON). How does JAX-RS handle this mismatch?**
If a client attempts to shoot text (`text/plain`) or XML data into our POST endpoint while ignoring our strictly defined `@Consumes` rule, JAX-RS's dispatcher mechanism drops the payload before it even physically hits our Java method. JAX-RS intercepts the routing mapping automatically and returns an implicit `415 Unsupported Media Type` to the physical client, defending the system from parsing errors natively.

**Q: Contrast @QueryParam filtering with alternative URL path designs (e.g., /api/v1/sensors/type/CO2). Why is the query parameter approach generally superior?**
Path variables (`/{variable}`) are strictly geared towards locating absolute, unique hierarchical resources (like fetching a highly specific ID). Query parameters (`?type=..`) map logically against filtering, modifying, sorting, or paginating data that already exists cleanly in a singular collection. Queries are superior here because they are inherently optional. If a client wants all sensors, they omit the query. In path-based routing, you would likely be forced to write duplicative java methods to handle both logic variations cleanly.

### Part 4: Deep Nesting with Sub-Resources
**Q: Discuss the architectural benefits of the Sub-Resource Locator pattern. How does delegating logic help manage complexity APIs compared to massive controller classes?**
Delegating nested paths using Sub-Resource locators solves massive "controller bloat". In huge APIs, shoving paths like `sensors/{id}/readings/...` into one massive `SensorResource.java` creates a gigantic monolithic class loaded with thousands of unreadable lines mixing up different business operations. By using locators, `SensorResource` only handles basic sensor actions, and explicitly hands off the heavy nested state logic cleanly to `SensorReadingResource`. This significantly boosts codebase readability, separates coding concerns, and drastically simplifies localized debugging tasks.

### Part 5: Advanced Error Handling & Logging
**Q: Why is HTTP 422 often considered more semantically accurate than a standard 404 when the issue is a missing reference inside a valid JSON payload?**
A `404 Not Found` functionally suggests that the top-level URI the client attempted to ping physically doesn't map to anything on the server. In our scenario, the client reached `POST /sensors` accurately, and their JSON syntax was perfect. The issue was that a string inside the structurally valid body pointed to a "ghost" foreign key (a missing room target). `422 Unprocessable Entity` exactly screams: "I read and understood your perfect JSON payload perfectly, but the semantic logic of what you asked me to do inside it is impossible."

**Q: What specific information could an attacker gather from exposing internal Java stack traces to external API consumers?**
Raw stack traces literally map the internal blueprint of the backend server out for hackers. They expose absolute server file directory pathways (`/var/www/...`), explicitly reveal third-party dependency names alongside their rigid version numbers (allowing hackers to lookup known CVE exploits against them), expose precise package nomenclatures, and sometimes leak database credential variables wrapped in error chains. Intercepting these via `500` mappers ensures data leaks are crushed. 

**Q: Why is it advantageous to use JAX-RS filters for cross-cutting concerns like logging, rather than manually inserting Logger statements inside every resource method?**
It is a massive operational violation of the "Don't Repeat Yourself" (DRY) principle to drop manual `Logger.info()` statements inside 50 different API endpoints. If your network analytics standard changes later, you'd be forced into opening 50 different controller methods to rewrite the code. Utilizing JAX-RS `ContainerRequestFilter` guarantees that logic is globally wrapped transparently over the entire architecture without a single controller method ever needing to be touched, drastically reducing error and codebase clutter.
