# Portfolio NLU Pipeline (Spring Boot)

Rule-based natural-language pipeline that turns a free-text portfolio query
("How much is my portfolio worth?", "Show my large cap MF purchased last yr")
into a structured execution plan and runs it against a sample holdings store —
no LLM required, matching the flow you specified.

## Pipeline

```
User Query
   │
   ▼
NormalizationService   → lowercase, strip punctuation, expand abbreviations,
   │                      lightweight spell-correction, remove stopwords
   ▼
IntentClassifier        → keyword-set matching against intent-keywords.json
   │
   ▼
EntityExtractor          → dictionary lookup (asset class, market cap, metric, company)
   │
   ▼
FilterExtractor          → regex over dates ("last year", "before 2023") and
   │                        amounts ("above 1 lakh")
   ▼
RuleEngine                → business validation (e.g. XIRR not valid for Fixed Deposit),
   │                         throws InvalidRequestException on violation
   ▼
ExecutionPlanner          → assembles {intent, entities, filters} into ExecutionPlan
   │
   ▼
PortfolioService          → filters the in-memory holdings and shapes the result
   │                         per intent (sum, list, XIRR map, report stub)
   ▼
IntentResponse (JSON)
```

## Project layout

```
src/main/java/com/portfolio/nlu
├── controller/   IntentController.java
├── service/      NormalizationService, IntentClassifier, EntityExtractor,
│                 FilterExtractor, RuleEngine, ExecutionPlanner,
│                 PortfolioService, IntentEngineService (orchestrator)
├── model/        IntentRequest, IntentResponse, Entity, Filter,
│                 ExecutionPlan, Holding
├── repository/   HoldingRepository (in-memory, backed by data/holdings.json)
├── util/         DictionaryLoader (loads all dictionaries once at startup)
└── exception/    InvalidRequestException

src/main/resources
├── application.yml
├── dictionaries/ abbreviations.json, stopwords.json, entities.json, intent-keywords.json
└── data/         holdings.json (sample portfolio)
```

Dictionaries are plain JSON, not hardcoded in Java, so you can extend
vocabulary, intents, or entities without touching a single class — the same
separation the flow describes ("stored in a dictionary").

## Running

```bash
mvn spring-boot:run
```

Then:

```bash
curl -X POST http://localhost:8080/api/nlu/query \
  -H "Content-Type: application/json" \
  -d '{"query": "Show my large cap MF purchased last yr"}'
```

Response:

```json
{
  "rawQuery": "Show my large cap MF purchased last yr",
  "normalizedQuery": "show large cap mutual fund purchased last year",
  "intent": "LIST_HOLDINGS",
  "entities": [
    {"type": "assetClass", "value": "Mutual Fund"},
    {"type": "marketCap", "value": "Large Cap"}
  ],
  "filters": [
    {"field": "purchaseDate", "operator": "=", "value": "Last Year"}
  ],
  "executionPlan": { "...": "..." },
  "result": ["HDFC Top 100", "ICICI Bluechip", "Axis Bluechip"]
}
```

Swagger UI: `http://localhost:8080/swagger-ui.html`

Try the query from your original example too:

```bash
curl -X POST http://localhost:8080/api/nlu/query \
  -H "Content-Type: application/json" \
  -d '{"query": "How much is my portfolio worth?"}'
```

A rule violation (e.g. asking for XIRR on a Fixed Deposit) returns HTTP 400:

```bash
curl -X POST http://localhost:8080/api/nlu/query \
  -H "Content-Type: application/json" \
  -d '{"query": "Show XIRR of Fixed Deposit"}'
```

## Tests

```bash
mvn test
```

`IntentEngineServiceTest` runs the exact end-to-end example from the spec
("Show my large cap MF purchased last yr" → LIST_HOLDINGS + Mutual Fund +
Large Cap + Last Year) plus a portfolio-value query and a rule-violation case.

## Extending it

- **New intent**: add a phrase-set array to `intent-keywords.json` — no code change.
- **New entity type**: add a section to `entities.json`, then handle it in
  `PortfolioService.matchesEntities` if it should also act as a filter.
- **New filter pattern**: add a regex + branch in `FilterExtractor`.
- **New business rule**: add a check in `RuleEngine.validate`.
- **Real database**: swap `HoldingRepository`'s in-memory list for a
  Spring Data JPA repository — nothing else in the pipeline needs to change,
  since `PortfolioService` is the only consumer of it.
- **Smarter classification/spell-correction**: the `IntentClassifier` and
  `NormalizationService` are isolated beans specifically so you can later
  swap keyword-matching for an embedding classifier or SymSpell without
  touching entity extraction, filters, or rules.

## Note on this environment

This sandbox has no network access to Maven Central, so I wasn't able to run
`mvn test` here to confirm a live build — I hand-verified brace/syntax
consistency across every file instead. Run `mvn test` on your machine (with
normal internet access) to confirm; flag anything that doesn't compile and
I'll fix it immediately.
