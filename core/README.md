The `core` module is a fairly thin wrapper over JDBC, providing support for named parameters, logging
and transactions.

#### Obtaining a Session

[`Sessions`](src/main/kotlin/com/github/andrewoma/kwery/core/Session.kt) are kwery's core interface for querying
and updating databases. e.g.

```kotlin
val session = DefaultSession(connection, PostgresDialect(), LoggingInterceptor())

val sql = "select * from actor where first_name = :first_name"

class Actor(val firstName: String, val lastName: String, val lastUpdate: Timestamp)

val actors = session.select(sql, mapOf("first_name" to "Brad")) { row ->
    Actor(row.string("first_name"), row.string("last_name"), row.timestamp("last_update"))
}
```

A `Session` is bound to a single JDBC connection. For simple use cases `DefaultSession` can be used
supplying the `connection` directly (as shown above). 


When using a pooled `DataSource`, as [`SessionFactory`](src/main/kotlin/com/github/andrewoma/kwery/core/SessionFactory.kt)
allows for automatically obtaining and returning a connection from the pool. e.g.

```kotlin
val factory = SessionFactory(hsqlDataSource, PostgresDialect(), LoggingInterceptor())

factory.use { session ->
    ...
}
```

A [`ThreadLocalSession`](src/main/kotlin/com/github/andrewoma/kwery/core/ThreadLocalSession.kt) is the most flexible option to use server-side. 
A single instance of ThreadLocalSession can be shared globally. By default, it obtains and releases
connections from a pooled DataSource automatically for each statement. 
However, when a transaction is started via `Session.transaction` the underlying connection
will be shared by all session uses on the thread for the duration of the transaction.

```kotlin
val session = ThreadLocalSession(hsqlDataSource, PostgresDialect(), LoggingInterceptor())
session.select(...)
```

Finally, `kwery` supports annotation-based transaction management using
a [`ManagedThreadLocalSession`](src/main/kotlin/com/github/andrewoma/kwery/core/ManagedThreadLocalSession.kt).
These require interceptors to automatically manage transactions. They have the advantage of defaulting
to a sensible strategy, however they often hold connections open longer than necessary. 

See the [transactional-jersey module](../transactional-jersey) and [transactional module](../transactional)
for more details.


##### Dialects

Kwery supports logging of SQL statements with parameters bound inline so statements can be copied and
pasted into database terminal and executed without modification.

To do this it requires a database [`Dialect`](src/main/kotlin/com/github/andrewoma/kwery/core/dialect/Dialect.kt)
to define the database specific literal formats for types such as timestamps and blobs.

Dialects also allow use of database specific features in the `mapper` module.

To use a dialect, specify it during `Session` creation as shown above.

##### Statement Interceptors
 
[`StatementInterceptors`](src/main/kotlin/com/github/andrewoma/kwery/core/interceptor/StatementInterceptor.kt) allow
tracking (and potential modification) of statement execution. 

The main use cases at present are to log statements and collection performance metrics. There's a couple of
in-built logging interceptors.

[`LoggingInterceptor`](src/main/kotlin/com/github/andrewoma/kwery/core/interceptor/LoggingInterceptor.kt) logs
full statements. Varying the log level controls when it logs statements and it includes a threshold to work
as a "slow query log". 
```
19:50:40.049 [main] DEBUG c.g.a.k.c.i.LoggingInterceptor - 
insert into film(title, release_year, language_id, original_language_id, length, rating, last_update, special_features) 
values ('Underworld: Evolution', 2006, 1003, null, 6360000, 'R', '2015-07-22 19:50:40.038', array['Behind the Scenes']);
Successfully executed FilmDao.insert in 0.311 ms (0.437 ms). Rows affected: 1. TXN: 1
```

[`LoggingSummaryInterceptor`](src/main/kotlin/com/github/andrewoma/kwery/core/interceptor/LoggingSummaryInterceptor.kt)
logs a summary of statements executed. It's designed to collect timings for complex requests, giving a
breakdown per query. See the [LoggingListener](../example/src/main/kotlin/com/github/andrewoma/kwery/example/film/jersey/LoggingListener.kt)
in the example project to see how it can be used to wrap http requests:  
```
Executed 4 statements in 21.923 ms (closed in 52.205 ms) affecting 6,663 rows using 25.6% of request total (203.573 ms):
                                Calls    Exec   Close   Rows      
               FilmDao.findAll      1   3.525  27.283  1,000  52.3%
    FilmActorDao.findByFilmIds      1  15.701  21.679  5,462  41.5%
            ActorDao.findByIds      1   1.339   1.748    200   3.3%
         LanguageDao.findByIds      1   1.357   1.496      1   2.9%
```
 
#### Using a Session

##### Queries

`Session` provides several methods for querying data, but `select` is the most commonly used method.

###### select

`select` executes a query returning the results as `List`.

```kotlin
val sql = "select first_name, last_name from actor"

val actors = session.select(sql) { row ->
    row.string("first_name") to row.string("last_name")
}
```

As shown above, in addition to the query, `select` takes a row function that maps the result to objects.
The row function has a [`Row`](src/main/kotlin/com/github/andrewoma/kwery/core/Row.kt) parameter to extract data from the underlying `ResultSet`. 

`Row` is a thin wrapper over `ResultSet` providing a cleaner api for dealing with `null` results in Kotlin.

`select` also supports an optional `Map` of parameters.

```kotlin
val sql = "select first_name, last_name from actor where first_name = :name"
val params = mapOf("name" to "Bill")

val actors = session.select(sql, params) { row ->
    row.string("first_name") to row.string("last_name")
}
```

Finally, `select` accepts a [`StatementOptions`](src/main/kotlin/com/github/andrewoma/kwery/core/StatementOptions.kt)
to set some of less frequently used JDBC settings.

###### asSequence

`asSequence` executes a query, providing the results as a sequence for streaming.

This allows for flexible processing of large result sets without loading them into memory.

```kotlin
val sql = "select first_name, last_name from actor"

session.asSequence(sql) { rows ->
    val actors = rows.map { row.string("first_name") to row.string("last_name") }
    writeToFile(actors)    
}

fun writeToFile(actors: Sequence<Pair<String, String>) {
    ...
}
```

`Sequences` provide great flexibility for processing, in particular using `map` to lazily transform them into 
a sequence of objects.

###### forEach

`forEach` is an alternative method for streaming results.

It is slightly more concise than `asSequence`, but less flexible as processing must be done inline.

```kotlin
val sql = "select first_name, last_name from actor"

val outputStream = ...
session.forEach(sql) { row ->
    val actor = row.map { row.string("first_name") to row.string("last_name") }
    writeToFile(outputStream, actor)    
}
```

##### Updates

###### update

`update` is essentially identical to `select` except that it returns the count of rows affected instead
 of a result set.
 
```kotlin
val sql = "update actor set first_name = :first_name where last_name = 'Bennet'"
val count = session.update(sql, mapOf("first_name" to "Felicity"))
```

###### insert

`insert` is a variant of update that supports generated keys.

```sql
create table test (key serial, value varchar(1000))
```

```kotlin
val sql = "insert into test(value) values (:value)"
val (count, key) = session.insert(insertSql, mapOf("value" to "foo")) { it.int("key") }
```

##### Batching

To batch insert or update, use the `batchInsert` and `batchUpdate` functions respectively.

They work the same way as their single row counterparts, but accept and return lists instead.

##### Transactions

Session's `transaction` function starts a transaction for the lifetime of the supplied function block.

```kotlin
session.transaction {
    session.update(...)
    session.update(...)
}
```

In the above example, the transaction is implicitly committed unless an exception is thrown.

The `transaction` function also exposes a `Transaction` object that provides the ability 
force a roll back without throwing an exception.

```kotlin
session.transaction { t ->
    if (...) {
        t.rollbackOnly = true 
    }
}
```

The `transaction` object also allows the registration of handlers to be called back 
pre commit, post commit and on rollback.

These allow for use cases like synchronising caches post commit, or inserting to audit tables
pre commit.

```kotlin
val cache = ...

session.transaction { t ->
    val actor = insert(Actor("Kate", "Beckinsale"))

    t.postCommitHandler { committed, session ->
        if (committed) {
            cache[actor.id] = actor
        }
    }
}
```