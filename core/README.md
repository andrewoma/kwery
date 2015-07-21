The `core` module is a fairly thin wrapper over JDBC, providing support for named parameters, logging
and transactions.

#### Obtaining a Session

[Sessions](src/main/kotlin/com/github/andrewoma/kwery/core/Session.kt) are kwery's core interface for querying
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


When using a pooled `DataSource`, as [SessionFactory](src/main/kotlin/com/github/andrewoma/kwery/core/SessionFactory.kt)
allows for automatically obtaining and returning a connection from the pool. e.g.

```kotlin
val factory = SessionFactory(hsqlDataSource, PostgresDialect(), LoggingInterceptor())

factory.use { session ->
    ...
}
```

When using `kwery` in traditional services on the server-side it's more appropriate to use 
[ThreadLocalSession](src/main/kotlin/com/github/andrewoma/kwery/core/ThreadLocalSession.kt) in combination
with transaction interceptors provided in the `transactional` modules.

##### Dialects

Kwery supports logging of SQL statements with parameters bound inline so statements can be copied and
pasted into database terminal and executed without modification.

To do this it requires a database [Dialect](src/main/kotlin/com/github/andrewoma/kwery/core/dialect/Dialect.kt)
to define the database specific literal formats for types such as timestamps and blobs.

Dialects also allow use of database specific features in the `mapper` module.

To use a dialect, specify it during `Session` creation as shown above.

##### Statement Interceptors
 
[StatementInterceptors](src/main/kotlin/com/github/andrewoma/kwery/core/interceptor/StatementInterceptor.kt) allow
tracking (and potential modification) of statement execution. 

The main use cases at present are to log statements and collection performance metrics. There's a couple of
in-built logging interceptors.

[LoggingInterceptor](src/main/kotlin/com/github/andrewoma/kwery/core/interceptor/LoggingInterceptor.kt) logs
full statements. Varying the log level controls when it logs statements and it includes a threshold to work
as a "slow query log".
 
#### Using a Session
 
##### Queries
##### Binding Parameters
##### Statement Options
##### Updates
##### Transactions
##### Batching
