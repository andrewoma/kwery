#### Kwery Overview

Kwery is an SQL library for Kotlin.

Kwery consists of three major modules (core, mapper and fetcher) that when combined provide similar
functionality to a traditional ORM.

Kwery's manifesto:
* **Your domain model is sacred.** No annotations or modifications to your model are required. Immutable models are fully supported.
* **No implicit fetching.** Joins and graph fetches are explicit for predictable performance.
* **No magic.** No proxies, interceptors, reflection or implicit saves. Explicit functions with sensible defaults control everything.
* **Useful logging.** Logged statements are valid SQL with inline parameters for your dialect.

#### Core

The core module is a fairly thin wrapper over JDBC, providing support for named parameters, logging
and transactions.
```kotlin
class Actor(val firstName: String, val lastName: String, val lastUpdate: Timestamp)

val session = DefaultSession(connection, HsqlDialect()) // Standard JDBC connection

val sql = "select * from actor where first_name = :first_name"

val actors = session.select(sql, mapOf("first_name" to "Brad")) { row ->
    Actor(row.string("first_name"), row.string("last_name"), row.timestamp("last_update"))
}
```

#### Mapper

The mapper module builds on core to provide typical DAO (Data Access Object) functionality.

As Kwery believes your domain model shouldn't be tainted by mapping annotations,
it uses a ``Table`` object to define the mapping between rows and objects.

```kotlin
// We'll map to standard immutable classes, grouping name fields into a class
class Name(val firstName: String, val lastName: String)
class Actor(val id: Int, val name: Name, val lastUpdate: LocalDateTime)

// Table configuration defines defaults, converters and naming conventions
// In this case, it includes converters for LocalDateTime <-> java.sql.Timestamp
val tableConfig = TableConfiguration(defaults, converters, camelToLowerUnderscore)

// A table object defines the mapping between columns and models
// Conversions default to those defined in the configuration but may be overridden
object actorTable : Table<Actor, Int>("actor", tableConfig), VersionedWithTimestamp {
    val ActorId    by col(Actor::id,                     id = true)
    val FirstName  by col(Name::firstName, { it.name },  notNull = true)
    val LastName   by col(Name::lastName,  { it.name },  notNull = true)
    val LastUpdate by col(Actor::lastUpdate,             version = true)

    override fun idColumns(id: Int) = setOf(ActorId of id)

    override fun create(value: Value<Actor>) = Actor(value of ActorId,
            Name(value of FirstName, value of LastName), value of LastUpdate)
}

// Given a table object, a generic dao is a one-liner, including standard CRUD operations
class ActorDao(session: Session) : AbstractDao<Actor, Int>(session, actorTable, { it.id })

// Now we can use the DAO
val dao = ActorDao(session)
val inserted = dao.insert(Actor(1, Name("Kate", "Beckinsale"), LocalDateTime.now())
val actors = dao.findAll()
```

See [FilmDao.kt](/mapper/src/test/kotlin/com/github/andrewoma/kwery/mappertest/example/FilmDao.kt) for
a more comprehensive example.

#### Graph Fetcher

DAOs only fetch data from their linked table by default. To fetch an object graph, using
a graph fetcher is the recommended method.

Given a graph specification, the fetcher attempts to fetch the graph in the minimum
number of queries possible. It does this by batching together requests for the same
type into a single query. As it fetches by ids, it also provides an ideal
mechanism to insert a cache layer.

```kotlin
// Given the following domain model
data class Actor(val id: Int, val firstName: String, val lastName: String)

data class Language(val id: Int, val name: String)

data class Film(val id: Int, val language: Language, val actors: Set<Actor>,
                val title: String, val releaseYear: Int)

// Define types with functions describing how to fetch a batch by ids
val language = Type(Language::id, { languageDao.findByIds(it) })
val actor = Type(Actor::id, { actorDao.findByIds(it) })

// For types that reference other types describe how to apply fetched values
val film = Type(Film::id, { filmDao.findByIds(it) }, listOf(
        // 1 to 1
        Property(Film::language, language, { it.language.id }, {(f, l) -> f.copy(language = l) }),

        // 1 to many requires a function to describe how to fetch the related objects
        CollectionProperty(Film::actors, actor, { it.id },
                {(f, a) -> f.copy(actors = a.toSet()) },
                { actorDao.findByFilmIds(it) })
))

val fetcher = GraphFetcher(setOf(language, actor, film))

// Extension function to fetch the graph for any List using fetcher defined above
fun <T> Collection<T>.fetch(node: Node) = graphFetcher.fetch(this, node)

// We can now efficiently fetch various graphs for any list of films
// The following fetches the films with actors and languages in 3 queries
val filmsWithAll = filmDao.findFilmsReleasedAfter(2010).fetch(Node.all)

// The graph specification can also be built using properties
val filmsWithActors = filmDao.findFilmsReleasedAfter(2010).fetch(Node(Film::actors.node()))
```

DAOs and graph fetching aim to cover 95% of a typical application data retrievals. For the
remaining performance critical sections, use specialised methods on the DAOs using
partial selects and joins as required.

#### Example

The [example module](example) demonstrates using Kwery
to expose a simple model via RESTful web services via [DropWizard](http://dropwizard.io/).

#### Status

Kwery is unstable. It's currently being developed for a side project, so features are added as required.

#### Building

Kwery depends on kommon which is also currently unreleased. To build kommon and install to your local repository:
```bash
git clone https://github.com/andrewoma/kommon.git
cd kommon
./gradlew check install
```

To build kwery itself:
```bash
git clone https://github.com/andrewoma/kwery.git
cd kwery
./gradlew check install
```

To open in IntelliJ, just open the ``build.gradle`` file and IntelliJ will generate the project automatically.

#### Roadmap

General cleanup and improve test coverage.

Core:
* Better support for Blobs and Clobs
* Oracle dialect
* Support direct execution (currently everything is via a PreparedStatement)

DAO:
* Bulk update by example?
* Bulk delete by example?

Fetcher:
* General review - code seems overly complicated for what it does
* Review methods for specifying graphs
* Make node specs `*` (all) and `**` (all descendants) nest within a root node
* Clarify position on fetching cycles. Forbidden?

Example:
* Documentation
* Caching and automatic cache invalidation
* Add create, update and delete

Misc:
* Create a test module, factoring out common code like AbstractSessionTest
* Better IDE support for highlighting inline SQL. Vote for [KT-6610](https://youtrack.jetbrains.com/issue/KT-6610)
* Create a guava backed statement cache for Tomcat's connection pool (standard one just caches the first _n_
  statements and then ignores anything else).

#### License
This project is licensed under a MIT license.

[![Build Status](https://travis-ci.org/andrewoma/kwery.svg?branch=master)](https://travis-ci.org/andrewoma/kwery)
