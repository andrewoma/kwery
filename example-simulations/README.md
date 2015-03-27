## Kwery Performance Tests

This module uses [Gatling](http://gatling.io/) to run a few performance tests against the example module.

It requires `sbt` to build and run (I use [Paul Phillips's sbt-extras script](https://github.com/paulp/sbt-extras).

### Start the example server

Before running a simulation, make sure the example server is up and running in another terminal window.

```bash
$ cd ..
$ ./gradlew :example:run
```

### Running simulations

Start `sbt` to run simulations:

```bash
$ sbt
```

#### Compile the simulations

```bash
> compile
```

#### Run all simulations

```bash
> test
```

#### Run a single simulation

```bash
> testOnly kwery.QuerySimulation
```

#### Open the last report

```bash
> lastReport
```

#### Available simulations

* kwery.QuerySimulation: Runs a few queries for 4 minutes at around 300 requests/s

