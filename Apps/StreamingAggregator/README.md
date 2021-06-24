
# Problem Statement 1
<p>In reality, a single processor or machine cannot handle the input volume. Input RPS can be in the 10,000s or 100,000s. How do you scale your solution in a 
distributed environment?</p>

# Solution
<p>Before looking into the system design aspects behind such large-scale stream processing systems, it would be worthwhile to define some concrete terminologies relevant 
to this problem statement.</p>


## Terminology:
<li>
Streams - set of messages that usually belongs to a common category. For instance, a stream can be composed of Netflix titles watched by the user 
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
<li>Data sources are persistent and consist of offsets, which can be used to replay the stream from any given point. This assumption helps replay the data if the job needs any re-processing logic.</li>
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

<p>Data exchanges of streams transformed by one task/pipeline to the other can be made efficient by adding intermediate buffering layer to compensate for spikes in the traffic. Also,
it can help in inducing a clean backpressure mechanism between consuming tasks and producing tasks when the pipelines are chained together.</p>

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
The above design outlines the requirements and design principles to achieve a high-throughput stream processing system that can scale horizontally. 
The framework can be run on a shared multi-core computing infrastructure, which can schedule new task-shells during the runtime depending on the application requirements. 
Parallelizing task execution with minimal to no inter-task coordination, along with efficient operator state management can help realize a high-throughput streaming system.



# Problem Statement 2
 
Input volume can vary significantly throughout the day, i.e. many customers watch Netflix in the evening, 
there can be a spike with the launch of a new show, etc. How do you handle such variations in data volume while balancing 
resource utilization efficiency?

# Solution

The solution to this problem builds on top of the previous problem. Even though the load profile of the streams can 
be assumed to be the same as the one used in the above problem, there are some additional characterizations that need to be 
made before sketching a clean design for this classic dynamic scaling problem. Before getting into the actual design 
considerations, the following helps us characterize the problem space.

## System Characterization and Assumptions
<ol>
<li>
The stream processing jobs that are running in the system are primarily long-lived jobs that can span days or months if 
left uninterrupted. 
</li>
<li>
Events originate from a partitioned streaming source whose partitions are fairly balanced avoiding any “hot partition” 
in the stream. Consequently, all the parallel tasks operating on these partitions are dealing with almost equal load from 
the streaming source.
</li>
<li>
Stream processing jobs in the system can emit metrics to capture the record lag, resource utilization (CPU, memory), 
along with per-operator idleness per sec. Also, the assumption here is that these metrics can be published in near 
real-time to external systems, which can proactively take scaling decisions based on these input metrics.
</li>
<li>
A single task can consume from multiple partitions of a single stream. This slightly deviates from the model used 
in the previous problem. However, this model is equally valid and can be seen in real-world stream processing engines 
like Apache Flink, which can have a single task consume data from multiple topic partitions. 
</li>
<li>
We assume that the streaming job’s parallelism cannot be changed during the runtime of the job execution. Parallelism 
needs to be specified during the job startup phase. 
</li>
<li>
Stream processing tasks need to have the ability to take a savepoint of their current operator’s state via external triggers, 
and these tasks should have the capability to reload their states from a savepoint during a job restart.
</li>
<li>
State size acts as the primary bottleneck among stateful stream processing jobs. 
</li>
<li>
A stream processing job cannot be considered active and running until all the operators/tasks have loaded their states 
from a previous savepoint.
</li>
<li>
We assume that the majority of the tasks that get auto-scaled are stateless tasks, which can be rebalanced and started in a 
new task-shell or host with minimal downtime.
</li>
<li>
The traffic profile is fairly predictable with known patterns during the day with occasional outliers and burstiness like a 
sudden popular Netflix show, etc. 
</li>
<li>
The final assumption made in the system is to prefer over-provisioning over under-provisioning of resources. 
This assumption and design choice allows us to meet the strict SLAs in the event of spikes and unpredictable traffic bursts. 
</li>
</ol>

## Traditional Approach
<p>The traditional and naive approach to alleviate stream consumption lag buildup is to over-provision the servers needed to 
run the stream processing jobs. Users can configure a static size cluster with max servers at a level that can handle peak 
traffic plus some extra wiggle room to handle bursts. These approaches result in huge operational cost since extra provisioned 
resources remain un-utilized under low loads, which could have been turned off to save cost for the organization.
</p>

<p>To downscale the cluster under low-loads, we could resort to a manual restart of jobs depending on the load. This can 
avoid the excess operational cost on idle resources. However, this is a reactive approach that requires several humans to 
monitor and resize the jobs based on the observed load, and can be extremely cumbersome. 
</p>

## Dynamic Re-scaling of Streaming Jobs 

### Rationale
<p>We can resort to building a system around the stream processing framework that can dynamically make scaling decisions 
based on metrics generated by the job. The stream processing jobs can be instrumented to send measurements about the current 
state of the system. These metrics can then be analyzed and processed to make a scaling decision. Finally, this decision 
needs to be executed to scale up or scale down the stream processing job. Following sections describe the metrics that can 
be used to make scaling decisions.
</p>

### Custom Metrics Based Autoscaling Policy
Although CPU and memory utilization metrics provide useful insight into the running state of the tasks, they might not 
be the only metrics to choose while making a scaling decision. For example, it is not uncommon for a CPU-intensive task 
running in a single thread to take up more than 80% of the CPU core. However, scaling this task does not allow us to 
reduce the CPU utilization if the nature of the processing is CPU bound. Similarly, memory usage might not always be 
correlated to a backpressure scenario in a pipelined execution of operators. Hence, we can use the following two metrics as 
a reliable indicator of the stream processor state:

<ol>
<li> 
<b>Operator Idleness:</b>  
<p>This metric represents the percentage of time for which an operator in a task was idle in a second. In order to 
extract this metric from the task operator, we can have a lightweight instrumentation to monitor streaming applications 
at the operator level, specifically the proportion of time each operator instance spends doing useful computations. 
A higher value of operator idleness indicates an over-provisioning scenario and vice versa. 
</p>
</li>
<li>
<b>Consumer Lag:</b>
<p>This metric describes how far the consumer offsets are lagging behind when compared to the stream’s latest offset. This metric needs to be normalized to understand the overall trend, (i.e.), whether the lag is remaining constant or is it going up/down. These trends allow us to make a clear scaling decision. Note that, a constant lag can be a resultant of a job restart that is not over-provisioned, in which case, the system will not be able to process events in real-time because of the constant system lag. 
By using the above two metrics, viz. operator idleness and consumer record lag, we should be able to make accurate decisions around scaling needs for the running jobs. The scaling factor can be based on a desired value for the aforementioned metrics, and the system can be scaled up and down depending on whether the current value is higher or lower than the desired value.
For eg., a simple way to compute the desired degree of parallelism could be :
</p> 

desired_parallelism = current_parallelism × current_value/desired_value

<p>The above equation assumes that the job can handle tasks proportional to the “desired_parallelism”, and a scaling 
decision will bring the “current_value” to be closer to the “desired_value”.</p>
</li>
</ol>

### Impact of State Size on Scaling

<p>As alluded before, operator states can be a huge bottleneck when the tasks need to be scaled up and down. 
The majority of the downtime during a scaling operation is due to the loading of state from the persistent storage. 
This is especially true for long-running stateful operators that can incur huge operational states in the order of a 
few 10s of GB. Hence, the duration of initialization of the operators/tasks from a savepoint needs to be accounted for 
while making scaling decisions.
</p>
<p>
The operator state needs to be partitioned by key, and upon restarts, operators need to reload only from relevant key 
partitions. The operator’s state size can correlate linearly with the load times. Hence, a good estimate on downtime 
based on the state size will allow us to predict the approximate additional lag the restart will cause, and this needs 
to be factored in to ensure that the SLAs are not violated.
</p>

## Conclusion
<p>With a good auto-scaling architecture and policy, long-running stream processing jobs will be able to keep up with 
changing workloads while utilizing resources effectively. 
</p>

