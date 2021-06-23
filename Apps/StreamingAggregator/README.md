
# Problem Statement
<p>In reality, a single processor or machine cannot handle the input volume. Input RPS can be in the 10,000s or 100,000s. How do you scale your solution in a 
distributed environment?</p>

# Solution
<p>Before looking into the system design aspects behind such large-scale stream processing systems, it would be worthwhile to define some concrete terminologies relevant 
to this problem statement.</p>


## Terminology:
<li>
Streams - set of messages that usually belongs to a common category. For instance, a stream can be composed of Netflix titles streamed by the user 
along with related dimensions like country, device, etc. Streams can be broadly categorized into two types :
<ol>
<li>Unbounded streams - emanating from a streaming source like Kinesis, Kafka, changelog events from a DB etc.</li>
<li>Bounded streams - emanating from a static batch source like HDFS, File system, etc.</li>
</ol>
</li>
<li>
Partitions - represents a set of logical grouping that contains an ordered, immutable stream of events. Events are grouped before writing to the 
source channel by the producers. Partitions can be treated as independent streams on the processing side, and this property can be leveraged to process 
these partitions of a given stream in parallel.
</li>
<li>
Task - independent processing unit in the system, that can execute the operation(s) defined by the user in an efficient manner. 
Usually, a task can be thought of as an OS thread, which can operate independently in the system without sharing context and without the need to coordinate with 
other threads in the system. This properly allows tasks to be scheduled and scaled easily without the need to worry about external coordination.
</li>
<li>
Task Shell - represents a collection of one or more tasks that can be run in parallel. These could be OS or JVM processes, which can be scheduled on any given host.
</li>
<li>
State - represents the internal states maintained by operators that work together as part of a running task. Note that, states can also refer to external states 
used by the streaming job to enrich or augment events in the stream. Internal states can be used by operators like window, joins, etc, to perform stateful operations 
that can span across event boundaries. These states can be retained in-memory or on external state-stores that can be useful while task re-scheduling and job restarts
</li>


## Assumptions :

<li>Source under consideration is a streaming source, whose data is partitioned based on producer’s partitioning logic. Also, each partition contains ordered, 
immutable sequence of records - each with a unique offset within the partition.</li> 
<li>Traffic is equally distributed among the partitions of the stream.</li>
<li>Existence of an external resource manager like Mesos, YARN, that can help schedule workloads with desired compute and memory specifications.</li>
<li>Stream processing engine supports “atleast-once” processing guarantee.</li>
<li>The streaming job under consideration is throughput intensive and not compute intensive. Compute intensive jobs would need per-task optimizations, 
whereas throughput intensive streams are amenable to horizontal scaling techniques. </li>
<li>Streams consist of offsets, which can be used to replay the stream from any given point. This assumption helps replay the data if the job needs any re-processing logic.</li>
<li>The streaming application/job under consideration does not have `m:1` style operator logic which involves data shuffling stage.</li>
<li>A given task consumes data only from “one” partition and does not have the need to read from multiple partitions and/or different streams in order to satisfy the job 
requirements. Effectively this would mean that, we are not considering a job that can perform `JOIN` operation from multiple input streams.
Presence of an efficient, persistent local state-store like RocksDB, Redis, or MongoDB that can support efficient management of states per task.</li>
<li>Notifications about host/container downtime from the resource managers. </li>
<li>The workload orchestrator allows workloads and tasks to be scheduled on any desired host within the given cluster of hosts. 
This assumption is sometimes hard to achieve since container orchestration frameworks pack containers based on available resources in a node. However, this property can 
help relaunch the workload on the same host thereby eliminating the need for a remote state-store. 
Also, we are assuming that node and disk failures are rare events, and the common scenario is job/task level failures. If the node/disk failures are the norm, 
then using a remote state-store would be the obvious choice, which results in increased bootstrapping time for the tasks that are relaunched and increased job restart times. 
Another technique can be to use log-compacted Kafka topic to track state changes per task/job. </li>  

## Design Considerations

<p>Building a scalable high-throughput stream processing engine poses several design challenges. Design of one such system should subsume the following key design aspects </p>

### Scalable Task Assignment Strategy

<p>Based on our assumptions, we have established that the streaming source is partitioned, which denotes the unit of parallelism for the underlying stream. Also, 
the job doesn't require a cross-partition processing logic. If required, the input stream can be re-partitioned before feeding the results into the next stage pipeline/task. 
These common assumptions can help us design an external task assignment layer, called a task-coordinator, which can assign one task per stream partition. Note that, 
these tasks belonging to a given job are identical and execute an identical DAG of operations on the incoming stream events. </p>

<p>As a side note, in order to compute the “sps” metric, we can define a pipeline that involves a filter, window, and map operators that can help us determine the 
“sps” metric in a streaming fashion. However, in order to guarantee independent task execution for this application, there is a need to re-partition the incoming events 
based on the application defined key function, which could feed into a new output stream. This re-partitioned output stream of events can be consumed by the second stage of 
operators to compute the “sps” metric in a streaming fashion without any need for inter-task coordination.</p>

<p>Since the tasks are throughput-bound and not compute-intensive, parallel execution of tasks on the input data stream is extremely effective to achieve high-throughput. 
However, there is a need for an external task coordination system that can help schedule the tasks and map them to the input stream partitions. Also, the task coordinator 
can help schedule these tasks via an external resource manager to get the desired amount of compute and memory resources to execute the data pipeline. Note that, the number 
of tasks spawned per job is bound by the number of partitions in the incoming data stream. Increasing the number of tasks beyond the partition count would not be beneficial 
and might lead to wasted resources in the system. </p>

<p>Task-coordinators can help monitor the health of all the tasks belonging to a job by simple monitoring mechanisms like periodic heartbeat exchange with the tasks, etc. 
Potentially, task coordinators can be notified about changes in the input stream partitions, which can allow task-coordinators to spawn additional tasks to maintain the 
right degree of parallelism. Importantly, coordinators can help evenly distribute tasks among multiple task-shells or hosts to achieve better resource utilization. 
This aspect becomes particularly compelling when the number of jobs/tasks are growing in the organization. </p>

<p>Tasks for a given job are independent entities in the system, and can be executed asynchronously within a task-shell by scheduling these tasks via a dedicated thread-pool. 
Also, external sink operators are bound to invoke remote calls, and making these sink operators asynchronous will greatly benefit the overall processing rates of each 
independent task.</p>

### Avoid Inter-Task Coordination

<p>Given the partitioned nature of the data in a stream, composing pipelines/tasks that operate on independent partitions makes the most sense to achieve high processing rates. 
Also, all these tasks which belong to a job execute the same computations on their assigned partitions of the stream. 
Contention among threads/tasks can lead to severe performance penalties in a distributed system. 
The task-coordinator takes the role of assigning tasks to independent partitions in the stream, thereby realizing parallel execution flows for a given stream. 
Avoiding coordination among tasks gives us the benefit of scheduling the tasks on any of the available hosts in the system and greatly helps in task distribution and better 
utilization of available shared compute resources. Also, this approach avoids the need to have any centralized coordination system in the processing path. </p>

### Stateful Processing and Fault-Tolerance 

<p>Stateless applications, like filtering a data stream based on a user-defined predicate followed by a simple map operation, are easy to scale when compared to scaling 
stateful applications. Stateful applications like joins, aggregations, and windowing have to retain state across event boundaries. Most of these stateful operators retain 
the state information in-memory during the lifecycle of the task. However, the state information needs to be checkpointed periodically to guarantee effective fault tolerance. 
Hence, a crash or a restart needs the newly created task to reload the state from a durable state-store. Since we are dealing with states that need to be retained across task 
restarts, the in-memory states could be persisted in a local state-store like RockDB, which could be re-used by the new task if they get rescheduled on the same host. 
Other options include saving the state in a remote shared K/V store, which could slowly start becoming a bottleneck owing to the high-throughput nature of the stream 
processing tasks that can overwhelm the external K/V store. And, the number of QPS to these stores can be very high when the number of tasks in the system increases. 
Also, these service calls incur network bandwidth as these are remote calls from the system. On the other hand, the upside for having shared K/V service is their in-built 
fault-tolerance and scaling guarantees.</p>

<p>Some of the tasks end up writing very large states (~100GB) to the state-store, hence a reasonable design would be to save the states periodically to a local state-store 
with an in-memory cache to load the recently accessed elements and to save on the deserialization cost of reading from the local state-store. This approach works very well 
if the node/disk failures are not common and the task-coordinator has the ability to relaunch the task on the same host node. However, a node crash would warrant the states 
to be written to a remote state-store that is accessible outside the node. There are streaming systems like “Kafka Streams” that rely on Kafka to store the partitioned local 
state information in a “changelog” topic. The state information stored in a “changelog” topic can mimic a replication log of the local state-store. Writes to the local 
state-store can be replicated onto these Kafka topics asynchronously in batches. Also, Kafka’s log compaction can allow the task consumers to read the latest value of 
the state when there are multiple state values for the same key, thereby expediting the reload times. </p>

<p>Hence, an LSM based  RocksDB (or) LevelDB state-store with an in-memory cache, with writes to RocksDB replicated to Kafka “changelog” topic with log-compaction 
enabled would work well for high-throughput stream processing jobs.</p>

### Fast Recovery from Failures and Restarts 
<p>To avoid building processing lags during restarts and to meet the demanding SLAs on data-freshness, it is desirable to minimize the restart times of jobs/tasks. 
One of the primary bottlenecks while re-balancing tasks in the system or restarting a job is the overhead incurred while bootstrapping the task from the previously 
restored state. This operation can be expedited by re-scheduling the task again on the same host/task-shell, thereby saving on the time taken to read the previously 
checkpointed state from a Kafka “changelog” topic. This functionality can be orchestrated by the task-coordinator that knows the previous task assignment states along 
with the host info. Note that, this would warrant task-coordinator service to negotiate the task placement information with the resource manager framework. 
Resource management frameworks like YARN expose these functionality to the applications that can be leveraged by our system to achieve reduced recovery times. </p>  

## Conclusion
The above design outlines the requirements and design principles to achieve a high-throughput stream processing system that can scale horizontally. The framework can be run on a shared multi-core computing infrastructure, which can schedule new task-shells during the runtime depending on the application requirements. Parallelizing task execution with minimal task coordination, along with efficient operator state management can help realize a high-throughput streaming system.
