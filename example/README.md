#### Kwery Example

The example is a [DropWizard](http://dropwizard.io/) project that demonstrates using Kwery
to expose a simple model via RESTful web services.

Features:
* Model persistence using Kwery's mapper module (e.g. [ActorDao](src/main/kotlin/com/github/andrewoma/kwery/example/film/dao/ActorDao.kt))
* Transactions using the [transaction-jersey](../transaction-jersey) module
* Filtering using Kwery's `Dao.findByExample`
* Graph fetching of related entities on a per request basis
* Logging of either full statements or summaries on a per request basis
* [Partial serialisation](src/main/kotlin/com/github/andrewoma/kwery/example/film/jackson/JacksonExtensions.kt) of objects to JSON (i.e. only serialise the id if the object hasn't been fetched)

Here's a snippet from [ActorResource](src/main/kotlin/com/github/andrewoma/kwery/example/film/resources/ActorResource.kt) showing a typical resource:
```kotlin
Path("/actors")
Produces(MediaType.APPLICATION_JSON)
transactional public class ActorResource(val actorDao: ActorDao, override val fetcher: GraphFetcher) : Resource {

    Timed GET
    fun find(QueryParam("firstName") firstName: String?,
             QueryParam("lastName") lastName: String?,
             QueryParam("fetch") root: String?): List<Actor> {

        val filter = parameters(
                actorTable.FirstName + firstName,
                actorTable.LastName + lastName
        )

        return actorDao.findByExample(actorTable.copy(Actor(), filter), filter.keySet()).fetch(root)
    }

    Transaction Timed GET Path("/{id}")
    fun findById(PathParam("id") id: Int, QueryParam("fetch") root: String?): Actor {
        return actorDao.findById(id).fetch(root) ?: throw NotFoundException("$id not found")
    }
}
```

For all of the wiring and configuration of daos, caches and the fetcher,
see [FilmApplication.kt](src/main/kotlin/com/github/andrewoma/kwery/example/film/FilmApplication.kt).

##### Building & Running

Assuming you've built Kwery as per [Building](../README.md#building), you can run the example via gradle:
```bash
./gradlew :example:run
```
You can then browse the example home page at [http://localhost:9090](http://localhost:9090) and try out some queries.

##### Graph Fetching

The resource `GET` apis accept a `fetch` parameter that allows the user to control how much
data to fetch on each request. e.g.
* Fetch all actors: `http://localhost:9090/api/actors`
* Actors with films (with languages): `http://localhost:9090/api/actors?fetch=films(language,originalLanguage)`

This pattern means has two main advantages:

1. It removes the need for a proliferation of method names and DTOs returning different payloads
2. Clients are much more likely to request the data they actually need rather than abuse existing methods

With Kwery the pattern is trivial to implement - just pass the graph specification through to the fetcher
as shown in the resource example above.

##### Logging

Logging is a core concern in Kwery and is designed to be used in production (usually dormant by default).

The resource apis accept a `log` parameter that allows logging to be controlled on a per request basis.

Setting `log=summary` produces a summary of all statements made during the request. e.g.

`http://localhost:9090/api/actors?fetch=films(language,originalLanguage)&log=summary`
```
Executed 4 statements in 7.661 ms (closed in 12.597 ms) affecting 6,660 rows using 45.0% of request total (27.986 ms):
                                 Calls   Exec  Close   Rows
              FilmDao.findByIds      1  3.216  5.682    997  45.1%
    FilmActorDao.findByActorIds      1  3.213  5.295  5,462  42.0%
               ActorDao.findAll      1  0.603  0.944    200   7.5%
          LanguageDao.findByIds      1  0.629  0.675      1   5.4%
```

Setting `log=statements` produces a summary of all statements made during the request. e.g.

`http://localhost:9090/api/actors?fetch=films(language,originalLanguage)&log=statements`
```
select id, first_name, last_name, version
from actor;
Sucessfully executed ActorDao.findAll in 0.362 ms (0.550 ms). Rows affected: 200. TXN: 145

select film_id, actor_id from film_actor where actor_id in(unnest(array[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16 ... ]));
Sucessfully executed FilmActorDao.findByActorIds in 2.643 ms (4.776 ms). Rows affected: 5462. TXN: 145

select id, title, description, release_year, language_id, original_language_id, length, rating, special_features, version
from film
where id in(unnest(array[1,23,25,106,140,166,277,361,438,499,506,509,605,635,749,832,939,970,980,3,31,47,105 ... ]));
Sucessfully executed FilmDao.findByIds in 2.270 ms (6.612 ms). Rows affected: 997. TXN: 145

select id, name, version
from language
where id in(unnest(array[1]));
```

You can also set `log=none` to disable logging completely, or `log=all` for both statements and summaries.

##### Caching

The example caches Languages for a minute (after writing to the cache) using Guava's `LoadingCache`.

You can verify the caching behaviour from the logs. If you make multiple calls to any
method that graph fetches languages within a minute, you should only see a single call to
`LanguageDao.findByIds` in the logs.

Graph fetching works particularly well with caching as both fetch by ids. It is therefore
trivial to check the cache first before hitting the database.

Guava's caches also allow batch loading of misses, so it is often unnecessary to pre-warm caches.

The example uses [Dao Listeners](../mapper/src/main/kotlin/com/github/andrewoma/kwery/mapper/listener/DaoListener.kt)
to automatically invalidate the caches on update or delete so you don't have to rely on expiry for consistency.

##### CRUD Operations

[LanguageResource](src/main/kotlin/com/github/andrewoma/kwery/example/film/resources/LanguageResource.kt) supports standard CRUD operations.
[SqlExceptionMapper](src/main/kotlin/com/github/andrewoma/kwery/example/film/jersey/SqlExceptionMapper.kt)
converts SQLExceptions to their RESTful counterparts.

To create (returns the generated id):
```
$ curl -H "Content-Type: application/json" -X POST -d '{"id" : 0, "name" : "Chinese", "version" : 1 }' http://localhost:9090/api/languages
10
```
Attempts to create a language with a name that already exists returns a 409 (Conflict):
```
$ curl -H "Content-Type: application/json" -X POST -d '{"id" : 0, "name" : "Chinese", "version" : 1 }' http://localhost:9090/api/languages
{
  "code" : 409,
  "message" : "integrity constraint violation: unique constraint or index violation; LANGUAGE_NAME_IDX table: LANGUAGE",
  "details" : null
}
```
Update via PUT, ensuring you pass the version of the object you are updating (new version is returned):
```
$ curl -H "Content-Type: application/json" -X PUT -d '{"id" : 10, "name" : "Cantonese", "version" : 1 }' http://localhost:9090/api/languages/10
2
```
Attempts to update the same version twice will return a 428 (Precondition Required):
```
$ curl -H "Content-Type: application/json" -X PUT -d '{"id" : 10, "name" : "Cantonese", "version" : 1 }' http://localhost:9090/api/languages/10
{
  "code" : 428,
  "message" : "The same version (1) of language with id 10 has been updated by another transaction",
  "details" : null
}
```
Finally, a object can be deleted:
```
curl -H "Content-Type: application/json" -X DELETE http://localhost:9090/api/languages/10
```
Attempts to delete an object that is use via a foreign key with return 409 (Conflict):
```
$ curl -H "Content-Type: application/json" -X DELETE http://localhost:9090/api/languages/1
{
  "code" : 409,
  "message" : "integrity constraint violation: foreign key no action; FK_FILM_LANGUAGE table: FILM",
  "details" : null
}
```
Attempts to delete an object that doesn't exist with return a 404.

##### Cache invalidation

With the update method above we can also verify cache invalidation:

1. Run a query that fetches a language. e.g. `http://localhost:9090/api/films/1?fetch=language`
2. Update the language name: `$ curl -H "Content-Type: application/json" -X PUT -d '{"id" : 1, "name" : "Was English", "version" : 1 }' http://localhost:9090/api/languages/1`
3. Refresh the query - you should see `"Was English"` immediately without waiting for cache expiry.
An invalidation message will also be written to the server logs for inspection.
