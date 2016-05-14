#### Kwery Overview

Kwery is an SQL library for Kotlin.

Kwery consists of three major modules (core, mapper and fetcher) that when combined provide similar
functionality to a traditional ORM.

Kwery's manifesto:
* **Your domain model is sacred.** No annotations or modifications to your model are required. Immutable models are fully supported.
* **No implicit fetching.** Joins and graph fetches are explicit for predictable performance.
* **No magic.** No proxies, interceptors, reflection or implicit saves. Explicit functions with sensible defaults control everything.
* **Useful logging.** Logged statements are valid SQL with inline parameters for your dialect.

[![Build Status](https://travis-ci.org/andrewoma/kwery.svg?branch=master)](https://travis-ci.org/andrewoma/kwery)

#### Core

The [core module](core) is a fairly thin wrapper over JDBC, providing support for named parameters, logging
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

The [mapper module](mapper) module builds on core to provide typical DAO (Data Access Object) functionality.

As Kwery believes your domain model shouldn't be tainted by mapping annotations,
it uses a ``Table`` object to define the mapping between rows and objects.

```kotlin
// We'll map to standard immutable classes, grouping name fields into a class
class Name(val firstName: String, val lastName: String)
class Actor(val id: Int, val name: Name, val lastUpdate: LocalDateTime)

// A table object defines the mapping between columns and models
// Conversions default to those defined in the configuration but may be overridden
object actorTable : Table<Actor, Int>("actor"), VersionedWithTimestamp {
    val ActorId    by col(Actor::id, id = true)
    val FirstName  by col(Name::firstName, Actor::name)
    val LastName   by col(Name::lastName, Actor::name)
    val LastUpdate by col(Actor::lastUpdate, version = true)

    override fun idColumns(id: Int) = setOf(ActorId of id)

    override fun create(value: Value<Actor>) = Actor(value of ActorId,
            Name(value of FirstName, value of LastName), value of LastUpdate)
}

// Given a table object, a generic dao is a one-liner, including standard CRUD operations
class ActorDao(session: Session) : AbstractDao<Actor, Int>(session, actorTable, Actor::id)

// Now we can use the DAO
val dao = ActorDao(session)
val inserted = dao.insert(Actor(1, Name("Kate", "Beckinsale"), LocalDateTime.now())
val actors = dao.findAll()
```

See [`FilmDao.kt`](/mapper/src/test/kotlin/com/github/andrewoma/kwery/mappertest/example/FilmDao.kt) for
a more comprehensive example.

#### Graph Fetcher

DAOs only fetch data from their linked table by default. To fetch an object graph, using
a [graph fetcher](fetcher) is the recommended method.

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
fun <T> Collection<T>.fetch(node: Node) = graphFetcher.fetch(this, Node(node))

// We can now efficiently fetch various graphs for any list of films
// The following fetches the films with actors and languages in 3 queries
val filmsWithAll = filmDao.findFilmsReleasedAfter(2010).fetch(Node.all)

// The graph specification can also be built using properties
val filmsWithActors = filmDao.findFilmsReleasedAfter(2010).fetch(Film::actors.node())
```

DAOs and graph fetching aim to cover 95% of a typical application data retrievals. For the
remaining performance critical sections, use specialised methods on the DAOs using
partial selects and joins as required.

#### Example

The [example module](example) demonstrates using Kwery
to expose a simple model via RESTful web services via [Dropwizard](http://dropwizard.io/).

#### Transactional

The [transactional module](transactional) adds general purpose transaction interceptors. e.g.

```kotlin
@Transactional open class MyService(val session: Session) {
    open fun foo() {}
}

val session = ManagedThreadLocalSession(dataSource, HsqlDialect())
val service = transactionalFactory.fromClass(MyService(session), MyService::session)
service.foo() // Now calls to service automatically occur within a transaction
```

See the [readme](transactional) for more information.

#### Transactional for Jersey

The [transactional-jersey module](transactional-jersey) adds transaction annotations for Jersey.

Registering [`TransactionListener`](transactional-jersey/src/main/kotlin/com/github/andrewoma/kwery/transactional/jersey/transactional.kt)
as a Jersey provider allows the `transactional` attribute to declare resource classes or methods as transactional.  

```kotlin
Path("/films")
@Transactional class FilmResource() : Resource {
    GET fun find(): List<Film> {
        ...
    }
}
```

See the [readme](transactional-jersey) for more information.

#### Status

Kwery is unstable. It's currently being developed for a side project, so features are added as required.

Kwery is available in [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Candrewoma.kwery)

`0.10` Compatible with Kotlin 1.0.2.

`0.9` Compatible with Kotlin 1.0.0.
* Mapper: add `Table.optionalCol` to construct optional types via paths

`0.8` Compatible with Kotlin 1.0.0-rc-1036.
* Mapper: support PreUpdate and PreInsert events (thanks @davemaple)
* Remove tomcat pool module as Postgres drivers now support prepared statement caching

`0.7` Compatible with Kotlin 1.0.0-beta-3595.
* Add MySQL dialect

`0.6` Compatible with Kotlin 1.0.0-beta-1038.

`0.5` Compatible with Kotlin M14.

`0.4` Compatible with Kotlin M13:
* Provide a consistent set of defaults and converters for mapping standard types
* Add defaults and converters for OffsetDateTime and ZonedDateTime

`0.3` Compatible with Kotlin M13:
* Improved docs
* Simplified transaction listeners
* Made transactions re-entrant
* Renamed ThreadLocalSession to ManagedThreadLocalSession and introduced a new ThreadLocalSession for
  use without interceptors and annotations.

`0.2` Compatible with Kotlin M12, adding transactional interceptors.

`0.1` Compatible with Kotlin M11.

#### Building

```bash
git clone https://github.com/andrewoma/kwery.git
cd kwery
./gradlew check install
```

Note: The tests require a local postgres and mysql database named `kwery`. e.g. On OS X
```
brew install postgres
launchctl load ~/Library/LaunchAgents/homebrew.mxcl.postgresql.plist
createdb kwery

brew install mysql
ln -sfv /usr/local/opt/mysql/*.plist ~/Library/LaunchAgents
launchctl load ~/Library/LaunchAgents/homebrew.mxcl.mysql.plist
mysql -uroot -e 'create database kwery'
mysql -uroot -e "create user 'kwery'@'localhost' identified by 'kwery'"
mysql -uroot -e "grant all privileges on *.* to 'kwery'@'localhost'"
```

To open in IntelliJ, just open the `build.gradle` file and IntelliJ will generate the project automatically.

#### Roadmap

Core:
* Support direct execution (currently everything is via a PreparedStatement)
* Add more robust named parameter replacement (ignore patterns inside comments, strings, etc)

DAO:
* Documentation

Fetcher:
* Documentation
* General review - code seems overly complicated for what it does

Modules:
* Dropwizard metrics integration
* Generator - Generate initial `Table` and domain objects from reading JDBC metadata

Robustness/Performance:
* Soak test - check for leaking connections/resources over extended usage
* Profile array based in clauses on large tables

Misc:
* Better IDE support for highlighting inline SQL. Vote for [KT-6610](https://youtrack.jetbrains.com/issue/KT-6610)

#### License
This project is licensed under a MIT license.
