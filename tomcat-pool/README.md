#### Tomcat JDBC Pool

This forks the StatementCache from tomcat-jdbc 8.0.20.

Initially, it just exposes the underlying statement for extracting metadata.

However, the intention is to replace the cache implementation with something
more sophisticated (probably Guava's cache).

The current implementation just caches the first _n_ statements it encounters,
there's no LRU/LFU eviction.
