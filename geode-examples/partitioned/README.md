# Geode partitioned region example

This basic example demonstrates the properties of a partitioned region. 
Two servers host a partitioned region, which has no redundancy.
The producer puts 50 entries into the partitioned region.
The consumer prints the number of entries in the region.
Due to partitioning,
the entries are distributed among the two servers hosting the region.
Since there is no redundancy of the data within the region,
when one of the servers goes away,
the entries hosted within that server are also gone.

This example is a simple demonstration of some basic Geode APIs,
as well how to write tests using mocks for Geode applications.

## Steps
1. From the ```geode-examples/partitioned``` directory,
run a script that starts a locator and two servers:

        $ scripts/startAll.sh

    Each of the servers hosts the partitioned region called ```myRegion```.

2. Run the producer to put 50 entries into ```myRegion```:

        $ ../gradlew run -Pmain=Producer
        ...
        ... 
        INFO: Done. Inserted 50 entries.

3. Run the consumer to observe that there are 50 entries in ```myRegion```:

        $ ../gradlew run -Pmain=Consumer
        ...
        ...
        INFO: Done. 50 entries available on the server(s).

    Note that this observation may also be made with ```gfsh```:
 
        $ $GEODE_HOME/bin/gfsh
        ...
        gfsh>connect
        gfsh>describe region --name=myRegion
        ..........................................................
        Name            : myRegion
        Data Policy     : partition
        Hosting Members : server2
                          server1

        Non-Default Attributes Shared By Hosting Members  

         Type  |    Name     | Value
        ------ | ----------- | ---------
        Region | size        | 50
               | data-policy | PARTITION

        gfsh>quit

4. Kill one of the servers:

        $ $GEODE_HOME/bin/gfsh
        ...
        gfsh>connect
        gfsh>stop server --name=server1
        gfsh>quit

5. Run the consumer a second time, and notice that only approximately half of
the entries are still available: 

        $ ../gradlew run -Pmain=Consumer
        ...
        ...
        INFO: Done. 25 entries available on the server(s).

    Again, this observation may also be made with ```gfsh```:

        $ $GEODE_HOME/bin/gfsh
        ...
        gfsh>connect
        gfsh>describe region --name=myRegion
        ..........................................................
        Name            : myRegion
        Data Policy     : partition
        Hosting Members : server2

        Non-Default Attributes Shared By Hosting Members  

         Type  |    Name     | Value
        ------ | ----------- | ---------
        Region | size        | 25
               | data-policy | PARTITION

        gfsh>quit

6. Shut down the system:

        $ scripts/stopAll.sh

