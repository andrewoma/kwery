The transactional-jersey module provides transactional interceptors for kwery with Jersey 2.

Registering [TransactionListener](src/main/kotlin/com/github/andrewoma/kwery/transactional/jersey/transactional.kt)
as a Jersey provider allows the `transactional` attribute to declare resource classes (or methods) as transactional.

Any `ManagedThreadLocalSessions` will then automatically participate in a transaction. Transactions will
be rolled back automatically if the status of the method is not successful.
 
```kotlin
val session = ManagedThreadLocalSession(dataSource, HsqlDialect())

Path("/films")
transactional public class FilmResource(session: Session) : Resource {
    GET fun find(): List<Film> {
        session.select(...)
    }
}
```

The `transaction` annotation accepts 2 parameters:
- `manual`. If true, a session will be initialised but no transaction will be started. This allows the use
of `Session's` transactional methods to manually control transaction boundaries.
- `name`. Multiple datasources can be supported via the `name` attribute (name underlying `ManagedThreadLocalSessions` to match).