The transactional module provides general purpose transactional interceptors for kwery.

Note: the use of this module is discouraged as it may be removed - using a `ThreadLocalSession` with
[transaction blocks](../core#transactions) is preferred as they work without interceptor
configuration.

Transaction interceptors are useful to automatically start transactions for method calls,
rolling back on exceptions.

The key advantage of interceptors is they allow services to be composed and nested in
different combinations while implicitly managing transactions at the outer service layer.
 
#### Usage

Transactional interceptors are created by the `transactionalFactory` object. 
`transactionalFactory` supports any object annotated by `transactional` that uses
`ManagedThreadLocalSessions` internally. e.g.

```kotlin
@Transactional open class MyService(val session: Session) {
    open fun foo() {}
}

val session = ManagedThreadLocalSession(dataSource, HsqlDialect(), LoggingInterceptor())
val service = transactionalFactory.fromClass(MyService(session), MyService::session)

// Now calls to service automatically occur within a transaction
service.foo()
```

When creating a transactional proxy from a concrete class (as shown above) it is necessary
to make the class and functions it defines `open` to permit the proxy to subclass. A default
constructor or constructor arguments are also required.

These restrictions aren't required if the object being proxied has an interface.

#### Configuration

By default, any exception (including checked) results in the transaction rolling back.

However the `rollbackOn` and `ignore` parameters can be used to control the behaviour.
e.g. The following will only rollback on instances of `RuntimeException` excluding `IllegalArgumentException`.

```kotlin
@Transactional(rollbackOn = arrayOf(RuntimeException::class), ignore = arrayOf(IllegalArgumentException::class)) 
open class MyService(val session: Session) {
    open fun foo() {}
}
```

Transactional annotations can be applied at either the class or method level. If applied at the method
level, they override the class level annotations.

Sometimes it's convenient to manually manage transactions. This can be done via the `manual` parameter. e.g.  
 
```kotlin
open class MyService(val session: Session) {
    @Transactional(manual = true) open fun foo() {
        // Use Session's transaction functions ...
        session.transaction {
        }
    }
}
```

Finally, multiple datasources can be supported via the `name` attribute (name underlying `ManagedThreadLocalSessions` to match).