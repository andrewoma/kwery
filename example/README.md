#### Kwery Example

The example is a [DropWizard](http://dropwizard.io/) project that demonstrates using Kwery
to expose a simple model via RESTful web services.

Features:
* Model persistence using Kwery's mapper module (e.g. [ActorDao](src/main/kotlin/com/github/andrewoma/kwery/example/film/dao/ActorDao.kt))
* Transactions using a [Jersey interceptor](src/main/kotlin/com/github/andrewoma/kwery/example/film/jersey/Transactions.kt) and Kwery's ThreadLocalSession
* Filtering using Kwery's `Dao.findByExample`
* Graph fetching of related entities on a per request basis
* Logging of either full statements or summaries on a per request basis
* [Partial serialisation](src/main/kotlin/com/github/andrewoma/kwery/example/film/jackson/JacksonExtensions.kt) of objects to JSON (i.e. only serialise the id if the object hasn't been fetched)

Here's a snippet from [ActorResource](src/main/kotlin/com/github/andrewoma/kwery/example/film/resources/ActorResource.kt) showing a typical resource:
```kotlin
Path("/actors")
Produces(MediaType.APPLICATION_JSON)
public class ActorResource(val actorDao: ActorDao, override val fetcher: GraphFetcher) : Resource {

    Transaction Timed GET
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

##### Building & Running

Assuming you've built Kwery as per [Building](../README.md#building), you can run the example via gradle:
```bash
./gradlew :example:run
```
You can then browse at example home page at http://localhost:9090 and try out some queries.

##### Graph Fetching
...

##### Logging
...