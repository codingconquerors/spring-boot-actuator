The hierarchy of readiness, health check, startup probe, and liveness in Kubernetes defines how the system ensures that applications (pods/containers) are functioning properly and when to take action if they are not. Here’s the breakdown of these concepts in order:

### 1. **Startup Probe**
- **Purpose**: Checks whether the application inside a container has successfully started.
- **When It's Used**: Primarily used for slow-starting applications. It ensures that the container has completed its initialization phase before Kubernetes checks its readiness or liveness.
- **Behavior**: If the startup probe fails, Kubernetes will restart the container. Until the startup probe passes, Kubernetes will not perform any readiness or liveness checks. Once the startup probe succeeds, Kubernetes moves on to readiness and liveness probes.
- **Hierarchy**: **First in hierarchy**. It runs before the other probes to determine if the container has successfully started. Only after this passes will readiness or liveness probes be checked.

### 2. **Readiness Probe**
- **Purpose**: Determines whether the application is ready to serve traffic.
- **When It's Used**: This probe is especially useful when an application needs to complete initialization tasks (like loading data or connecting to a service) before it can serve requests.
- **Behavior**: If the readiness probe fails, the pod is removed from the service endpoints, meaning it won’t receive traffic. However, the pod remains running, and Kubernetes will continue checking readiness.
- **Hierarchy**: **Second in hierarchy**, after the startup probe. It only starts checking once the startup probe passes, ensuring the container is fully initialized. It continuously monitors whether the application is ready to handle traffic.

### 3. **Liveness Probe**
- **Purpose**: Checks whether the application is alive and functioning correctly.
- **When It's Used**: It monitors if the application inside the container has entered a failed or dead state (e.g., stuck, crashed, or unable to recover).
- **Behavior**: If the liveness probe fails, Kubernetes will restart the container. This probe is designed to detect runtime failures that would prevent the container from working properly.
- **Hierarchy**: **Third in hierarchy**, following the startup and readiness probes. It continuously checks whether the application is still "alive" after it has started successfully and become ready to serve traffic.

### Relationship and Flow:
1. **Startup Probe**: Runs first. Ensures the application has started. If it fails, the container is restarted. If it succeeds, Kubernetes moves on to readiness and liveness checks.
2. **Readiness Probe**: Once the startup probe passes, Kubernetes checks if the application is ready to handle traffic. If readiness fails, the container stays running but is temporarily removed from the load balancer until it is ready again.
3. **Liveness Probe**: If the application is running but later becomes unresponsive or encounters an issue, the liveness probe will trigger a container restart.

### Example Scenario:
- **Startup Probe**: A web server needs 60 seconds to initialize (e.g., load configuration, connect to a database), so the startup probe ensures it gets that time without triggering false restarts.
- **Readiness Probe**: After startup, the server may occasionally be too busy or in a temporary "warm-up" phase where it cannot handle traffic. The readiness probe ensures the server only receives traffic when it is ready.
- **Liveness Probe**: If the server later crashes or hangs (e.g., memory leak, deadlock), the liveness probe detects this and restarts the container.

### Key Points:
- **Startup probe** runs only during the container startup phase, ensuring it initializes correctly.
- **Readiness probe** ensures the application is ready to serve traffic.
- **Liveness probe** ensures the application stays alive and can recover if it fails.

In practice, each of these probes helps Kubernetes ensure the application is healthy and running efficiently in a robust and automated manner.


## What happens if start up probes done with all the failureThreshold values and the container still not started?

If the **Startup Probe** fails after reaching the defined `failureThreshold` and the container still has not started,
Kubernetes/OpenShift will **consider the container unhealthy**
and will take corrective actions based on the pod's restart policy. Here's what happens:

### 1. **Container Restart**:
- Once the **Startup Probe** reaches its maximum allowed failures (defined by `failureThreshold`), Kubernetes/OpenShift will **restart the container**.
- The idea is to give the application another chance to start correctly. This is particularly useful for scenarios where the container might have encountered a transient issue during startup.

### 2. **Restart Policy**:
Kubernetes has a **Restart Policy** for containers, which determines what happens when the container fails (in this case, because of the failing startup probe). There are three types of restart policies:
- **Always**: The container will always be restarted when it fails. This is the default policy for most deployments.
- **OnFailure**: The container will be restarted only if it exits with a non-zero exit code (indicating failure). This includes cases like failed startup probes.
- **Never**: The container will not be restarted, and the pod will move to a "Failed" state.

For example, if the `restartPolicy` is set to **Always**, Kubernetes will attempt to restart the container indefinitely after every failure.

### 3. **Backoff Mechanism**:
If the container continuously fails to start, Kubernetes will apply an **exponential backoff** strategy for restarts:
- After each failed restart, Kubernetes waits longer before trying to restart the container again (e.g., it might wait for 10 seconds, then 20 seconds, then 40 seconds, etc.).
- This backoff continues until the container either starts successfully or reaches the maximum number of retries (if specified).

### 4. **Pod Status**:
- The pod will remain in a **"CrashLoopBackOff"** state if the container continuously fails to start. This indicates that Kubernetes is repeatedly trying to restart the container but the startup probe (or the container itself) is failing.

### 5. **Intervention**:
- If the pod stays in a "CrashLoopBackOff" state for an extended period, it may require manual intervention. You might need to investigate the logs or fix issues related to the startup of the application (e.g., misconfiguration, missing dependencies, or external service failures).

### Key Points:
- The **Startup Probe** has a `failureThreshold` that specifies the number of times the probe can fail before the container is restarted.
- After reaching this threshold, Kubernetes/OpenShift **restarts the container**.
- If the container repeatedly fails to start (due to probe failures or other reasons), it will enter a **CrashLoopBackOff** state, and Kubernetes will exponentially back off from restarting the container.

### Example Scenario:
```yaml
startupProbe:
  httpGet:
    path: /actuator/health
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 10
  failureThreshold: 5
```

- In this configuration:
    - The probe starts 10 seconds after the container starts.
    - It checks every 10 seconds.
    - If the probe fails 5 consecutive times (50 seconds total), Kubernetes will **restart the container**.
    - If the container continues to fail after each restart, Kubernetes will keep restarting the container, potentially entering a **CrashLoopBackOff** state.

In conclusion, if the **Startup Probe** exhausts its `failureThreshold` without success, the container will be restarted. If the problem persists, Kubernetes will keep trying to restart the container, and the pod will likely enter a **CrashLoopBackOff** state until the issue is resolved or the container starts successfully.

## Does Openshift delete a pod with CrashLoopBackOff state?

No, **OpenShift does not automatically delete a pod in the `CrashLoopBackOff` state**. Instead, it continuously attempts to restart the container inside the pod according to its restart policy.

### What happens in `CrashLoopBackOff`:
- **CrashLoopBackOff** is a Kubernetes/OpenShift state that indicates the container inside the pod has repeatedly failed to start and is in a loop of crashing and restarting.
- OpenShift will continue trying to restart the container based on an **exponential backoff** (gradually increasing the delay between each restart attempt).
- OpenShift will **not delete the pod**; instead, it will keep the pod in the `CrashLoopBackOff` state until the underlying issue is resolved (such as fixing a configuration error or missing dependency in the application).

### How to resolve `CrashLoopBackOff`:
1. **Check logs**:
    - You can inspect the pod logs to identify why the container is failing to start:
      ```bash
      oc logs <pod-name> --previous
      ```
    - Use the `--previous` flag to see logs from the last failed container instance.

2. **Describe the pod**:
    - You can use the `oc describe pod <pod-name>` command to get detailed information, including events and reason for the failures.

3. **Fix the issue**:
    - Based on the logs or events, you might need to adjust your application configuration, ensure necessary services or dependencies are available, or fix errors in your code or environment.

4. **Delete or replace the pod (optional)**:
    - If you want to manually remove the pod, you can delete it:
      ```bash
      oc delete pod <pod-name>
      ```
    - OpenShift will automatically recreate the pod (if it's part of a **Deployment**, **DeploymentConfig**, or **StatefulSet**).

### In summary:
- **OpenShift does not delete pods in `CrashLoopBackOff` state** automatically. It simply keeps trying to restart the pod following the restart policy.
- Manual intervention is required to fix the issue causing the crashes. Once fixed, the pod will start successfully, or you can delete it manually if needed.

## Does readiness probe also runs after the container starts successfully and app is in ready state? if yes what is the reason

Yes, the **Readiness Probe** continues to run even after the container starts successfully and the application is in the ready state. The reason for this is to ensure that the application inside the container remains **ready** to serve traffic throughout its lifecycle, not just at the time of startup.

### Why the **Readiness Probe** continues to run:
1. **Dynamic Application State**:
    - Even after a container starts successfully, an application can temporarily become **unready** due to various reasons such as:
        - Overloaded resources (CPU/memory exhaustion)
        - External dependencies (databases, APIs) becoming unavailable
        - Configuration changes
    - The **Readiness Probe** ensures that the application is always ready to handle traffic. If the probe fails after the container has started, Kubernetes/OpenShift will temporarily stop routing traffic to that pod until the probe succeeds again.

2. **Graceful Handling of Temporary Failures**:
    - By continuing to run the **Readiness Probe**, OpenShift can detect **temporary disruptions** in the application and prevent traffic from being routed to a pod that cannot serve requests properly.
    - Once the issue is resolved and the probe starts succeeding again, the pod is marked as **ready** and traffic is routed back to it.

3. **Rolling Updates**:
    - During **rolling updates** or configuration changes, the **Readiness Probe** helps Kubernetes/OpenShift ensure that only the pods that have passed their readiness check are considered for handling requests.
    - This ensures smooth transitions between versions and avoids routing traffic to unready pods.

### Behavior of the **Readiness Probe**:
- The **Readiness Probe** typically runs at regular intervals (defined by `periodSeconds`) throughout the lifecycle of the container.
- If the readiness probe fails at any point, the pod is **marked as unready**, and the **service** stops sending traffic to the pod. Other healthy pods in the cluster will continue to serve traffic.
- Once the readiness probe succeeds again, the pod is marked as ready, and traffic resumes.

### Example:
In the following configuration, the readiness probe continues to run every 10 seconds, even after the container is in a ready state.

```yaml
readinessProbe:
  httpGet:
    path: /healthz
    port: 8080
  initialDelaySeconds: 5
  periodSeconds: 10
  failureThreshold: 3
```

- **InitialDelaySeconds**: 5 seconds after the container starts, the first probe runs.
- **PeriodSeconds**: Every 10 seconds, the readiness probe runs.
- If the application is **not ready** at any point (e.g., `/healthz` endpoint returns failure), the pod will be marked **unready** until it can respond successfully.

### In summary:
The **Readiness Probe** continues running even after the application has started successfully to monitor the ongoing health and availability of the application. It ensures that the pod is always ready to receive traffic, preventing issues such as routing requests to pods that cannot handle them temporarily due to internal or external factors.

## Readiness vs Liveness probe

The **Readiness Probe** and **Liveness Probe** serve distinct purposes in Kubernetes/OpenShift, even though they both monitor the health of a container. The key difference lies in **what they check** and **how Kubernetes responds to their results**.

Let's dig deeper into their core differences:

### 1. **Purpose**:
- **Readiness Probe**: Determines **if the application inside the container is ready to serve traffic**. Its goal is to prevent the application from receiving requests when it is temporarily unready (e.g., during initialization or after losing a connection to a database).
- **Liveness Probe**: Determines **if the application is still running and alive**. If this probe fails, it means the container is in a broken or unrecoverable state, and Kubernetes will **restart the container**.

### 2. **Behavior and Action**:
- **Readiness Probe**:
    - **Action**: When a readiness probe fails, the container is marked as **unready**, and Kubernetes stops routing traffic to it via the service. However, the container itself is **not restarted**.
    - **When to use**: If your application might need time to temporarily pause serving requests (e.g., during a rolling update, heavy load, or connection loss with an external service), but can recover without needing to restart the container.

- **Liveness Probe**:
    - **Action**: When a liveness probe fails, Kubernetes considers the container **unhealthy**, assuming it's stuck in a broken state, and will **restart the container**.
    - **When to use**: If your application could crash or hang in such a way that it needs to be completely restarted to recover. For example, if the app enters a deadlock or consumes all system resources and stops responding.

### 3. **Timing**:
- **Readiness Probe**:
    - Runs throughout the lifecycle of the container, including after startup.
    - It continuously checks whether the application is ready to receive traffic, and it may succeed or fail multiple times during the lifecycle.

- **Liveness Probe**:
    - Also runs throughout the container’s lifecycle.
    - If the liveness probe fails, Kubernetes assumes the container is broken and will **restart it**, regardless of whether the readiness probe is succeeding or failing.

### 4. **Typical Use Cases**:
- **Readiness Probe**:
    - Applications that may not always be ready to serve requests immediately, such as apps that depend on external services (e.g., databases or APIs).
    - Applications performing background tasks, like scheduled jobs, which need to continue running but may temporarily not want to serve traffic.
    - Scenarios where temporary **unreadiness** (e.g., during rolling updates, scaling operations) does not require a restart but requires withholding traffic until ready.

- **Liveness Probe**:
    - Detects when an application is in a **failed or unrecoverable state**.
    - Typically used for applications that can get stuck in a **deadlock**, run into a memory leak, or stop responding to any requests.
    - Containers that need to be restarted if they encounter unrecoverable conditions.

### 5. **Impact on Container Lifecycle**:
- **Readiness Probe**:
    - Does **not affect** the lifecycle of the container in terms of restarts.
    - A pod can be in a **not ready** state (due to failed readiness probes) but still remain running and alive.
    - Kubernetes will stop sending traffic to the container until it passes the readiness probe again.

- **Liveness Probe**:
    - Directly **affects the lifecycle** of the container.
    - If the liveness probe fails, Kubernetes will restart the container, assuming it’s in an irrecoverable state.

### 6. **Scenarios in which Probes Differ**:
- **Readiness Probe Failure Example**:
    - Suppose your Spring Boot application requires a connection to an external database. If the database temporarily goes down, the **readiness probe** would fail, and Kubernetes/OpenShift would stop routing traffic to that pod. Once the database is back up and the application is connected again, the readiness probe succeeds, and traffic resumes.
    - No restart happens here, as the application itself is still running fine but just not ready to serve traffic due to external dependency failure.

- **Liveness Probe Failure Example**:
    - If your Spring Boot application encounters an internal error, such as a deadlock or memory exhaustion, causing the entire app to hang, the **liveness probe** will fail. Kubernetes will interpret this as the container being in a non-recoverable state and will restart it. This might fix the issue if it’s a temporary internal error or bug.
    - A restart happens because the container itself is considered broken.

### 7. **Configuration Example**:
Here’s an example where both **Readiness Probe** and **Liveness Probe** are configured for a Spring Boot application:

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 5
  failureThreshold: 3
```

- **Liveness Probe**: This probe checks the `/liveness` endpoint every 10 seconds after an initial 30-second delay. If it fails 3 consecutive times, Kubernetes will restart the container.

- **Readiness Probe**: This probe checks the `/readiness` endpoint every 5 seconds after an initial 10-second delay. If it fails 3 consecutive times, Kubernetes stops sending traffic to the container but does not restart it.

### Summary of Key Differences:
| **Aspect**           | **Readiness Probe**                                      | **Liveness Probe**                                      |
|----------------------|---------------------------------------------------------|---------------------------------------------------------|
| **Purpose**           | Checks if the app is ready to serve traffic              | Checks if the app is running and alive                   |
| **Action on Failure** | Marks the pod as **unready** (no traffic)               | Restarts the container                                   |
| **Use Case**          | Temporary unavailability (e.g., during heavy load)      | Unrecoverable failures (e.g., deadlocks, app crashes)    |
| **Impact on Lifecycle**| Does **not** restart the container                     | **Restarts** the container if the probe fails            |
| **Running Period**    | Continues running during the app's lifecycle            | Continues running during the app's lifecycle             |

## What will happen if the start up probe is failing repeatedly

If the **Startup Probe** fails repeatedly in Kubernetes or OpenShift, the container will eventually be killed and restarted based on the **failureThreshold** configuration. The purpose of the **Startup Probe** is to ensure that the application has successfully started before the liveness or readiness probes are applied. It is particularly useful for applications that may take a long time to initialize.

### Key Outcomes When the Startup Probe Fails Repeatedly:
1. **Kubernetes/OpenShift will restart the container**:
    - If the **Startup Probe** fails more than the specified `failureThreshold`, Kubernetes/OpenShift considers the container as failing to start properly.
    - The container will be **killed** and **restarted** according to the container's restart policy.

2. **Impact on Container Lifecycle**:
    - Each time the **Startup Probe** fails and the container is restarted, the probe is **reset** (i.e., it starts the probing process from the beginning).
    - If the application keeps failing the startup probe, the container will continue to **restart repeatedly**, which can lead to a **CrashLoopBackOff** state if the problem persists.

3. **CrashLoopBackOff**:
    - If the container repeatedly fails to start (i.e., fails the startup probe and is restarted multiple times), it may eventually enter the **CrashLoopBackOff** state.
    - This state occurs when Kubernetes/OpenShift keeps restarting the container but it continues to fail.
    - In this state, Kubernetes increases the time between each restart attempt using an exponential backoff (gradually increasing delay between restart attempts).

4. **No Readiness or Liveness Probes During Startup**:
    - While the **Startup Probe** is active and failing, **Readiness** and **Liveness** probes are **not run**.
    - This prevents the container from being killed prematurely or being marked as unready until the startup probe determines that the application has successfully initialized.

### Example Scenario:
Imagine a Spring Boot application that takes a long time to start due to database initialization or complex startup logic. You configure a **Startup Probe** to check whether the application has successfully completed startup.

```yaml
startupProbe:
  httpGet:
    path: /healthz
    port: 8080
  initialDelaySeconds: 5
  periodSeconds: 10
  failureThreshold: 6
```

- The **Startup Probe** will start checking `/healthz` 5 seconds after the container starts, and it will run every 10 seconds.
- If the application fails to respond with a successful status code 6 times in a row (i.e., after 60 seconds of probing), Kubernetes/OpenShift will consider the container as failed to start and will **restart it**.

If the application keeps failing to start, the container will be restarted repeatedly, and if the failures persist, it may lead to a **CrashLoopBackOff** state.

### How to Prevent Repeated Failures:
1. **Ensure Proper Startup Configuration**:
    - Ensure that the application has enough time to initialize properly by adjusting the `initialDelaySeconds`, `periodSeconds`, and `failureThreshold` settings to give the app sufficient time to start.

2. **Investigate the Logs**:
    - Check the application logs using:
      ```bash
      oc logs <pod-name> --previous
      ```
    - This will help identify why the application is failing to start.

3. **Check Dependencies**:
    - Ensure that any external services or dependencies (e.g., databases, APIs) are available and properly configured for the application to start successfully.

### In summary:
- If the **Startup Probe** fails repeatedly, Kubernetes/OpenShift will kill and restart the container.
- Continuous failure can lead to a **CrashLoopBackOff** state, where the container is restarted with increasing delays.
- Proper configuration of the startup probe and addressing underlying issues in the application or its environment can prevent repeated failures.

The CrashLoopBackOff state occurs when a container fails to start repeatedly, and Kubernetes/OpenShift introduces exponential backoff to prevent constant rapid restarts.
During this state, the container is not available, and the underlying issue must be addressed to stop the cycle of failures.
Kubernetes/OpenShift will not delete the pod on its own; it will continue attempting restarts with increasing delays until the issue is resolved or the pod is manually deleted.

## Restart policies

In Kubernetes (and by extension, OpenShift), there are three main types of **restart policies** that dictate how pods behave when they encounter issues like a failing **startup probe**. These restart policies come into play based on the exit status of containers, and the **startup probe** is responsible for determining whether the application is able to start successfully within the specified conditions.

### 1. **Always** (Default)
- **Behavior**: If the startup probe fails, the pod’s container will be restarted **indefinitely**, regardless of the exit status (success or failure).
- **Use Case**: This is the default policy for most pods. It is useful for services that are meant to run continuously, like web servers or long-running applications.
- **In case of failure**:
    - If the container fails the startup probe repeatedly, it will continue restarting indefinitely until the application either starts correctly or the pod enters a **CrashLoopBackOff** state (with increasing delays between restart attempts).
    - **CrashLoopBackOff**: If failures continue, the pod will not be deleted, but the time between restarts will increase exponentially to avoid system overload.

   ```yaml
   spec:
     restartPolicy: Always
   ```

### 2. **OnFailure**
- **Behavior**: The pod’s container will be restarted **only if the container exits with a failure** (i.e., non-zero exit code).
- **Use Case**: This policy is typically used for batch jobs or tasks that should only be restarted if they fail. It’s not typically used with long-running services like web servers.
- **In case of startup probe failure**:
    - If the startup probe fails, the container will be restarted because it didn’t start successfully (assuming a non-zero exit code).
    - If the container exits successfully (exit code `0`), it won’t be restarted, even if the startup probe failed initially but the container eventually started.

   ```yaml
   spec:
     restartPolicy: OnFailure
   ```

### 3. **Never**
- **Behavior**: The container will **not be restarted** after it fails, regardless of whether the failure is due to a startup probe or some other issue.
- **Use Case**: This is typically used for one-off jobs or containers that should only run once. It is not suitable for long-running applications or services that are expected to recover from failure.
- **In case of startup probe failure**:
    - If the startup probe fails, the container will not be restarted.
    - The pod will enter a **Failed** state, and you will need to manually intervene (e.g., by deleting or restarting the pod) to resolve the issue.

   ```yaml
   spec:
     restartPolicy: Never
   ```

### Detailed Behavior with **Startup Probes**:
- **Startup Probe Failing**: The startup probe is designed to check whether an application has started correctly. If the probe fails consistently (within the defined `failureThreshold` and `periodSeconds`), the container is considered to have failed startup.
- **Failure Handling**: When the startup probe fails, the pod will behave according to the set restart policy:
    - **Always**: The container is restarted and continues retrying until it starts successfully or enters CrashLoopBackOff.
    - **OnFailure**: The container is restarted only if it failed (non-zero exit code).
    - **Never**: The container does not restart, and the pod is marked as Failed.

### Key Parameters for Probes:
- **initialDelaySeconds**: The time Kubernetes waits before starting the first probe.
- **periodSeconds**: How often to perform the probe.
- **failureThreshold**: How many times the probe can fail before Kubernetes considers the pod unhealthy.

### Crash Scenarios with Startup Probe:
1. **CrashLoopBackOff**:
    - With the **Always** policy, if the startup probe keeps failing, the pod may enter a **CrashLoopBackOff** state, where Kubernetes will retry to restart the container but with exponentially increasing delays.

2. **Pod Failure**:
    - With the **Never** policy, repeated failure of the startup probe means the container will not be restarted after it fails, and the pod will enter a **Failed** state.

3. **Controlled Restarts**:
    - With **OnFailure**, the container will be restarted if the exit code is non-zero, but if the startup probe passes at some point, the container will continue running and will not be restarted again unless there’s another failure.

### Summary of Restart Policies with Startup Probe Failure:
| **Restart Policy** | **Behavior if Startup Probe Fails** | **Use Case** |
| --- | --- | --- |
| **Always** (default) | Continues restarting indefinitely until the container starts successfully or enters CrashLoopBackOff | Long-running services that must always be running |
| **OnFailure** | Restarts the container only if it exits with a non-zero status code | Batch jobs or applications that should only restart if they fail |
| **Never** | Container will not restart, and the pod enters a Failed state | One-off jobs that should not restart on failure |

Each policy serves a different purpose and is suited for different types of workloads and deployment strategies, based on the expected behavior of your application.

## maxSurege = 1 and maxUnavailable = 0

If **maxSurge** is set to **1** and **maxUnavailable** is set to **0** during a rolling update in Kubernetes or OpenShift, it means that Kubernetes will handle the deployment with a very conservative approach to avoid any downtime or reduction in service capacity. Here's what will happen:

### Breakdown of the Settings:

- **maxSurge: 1**:
    - Kubernetes is allowed to create at most **1 extra pod** (above the desired replica count) during the rolling update. This means that at any given time, there will be at most 1 more pod than the desired number of replicas.

- **maxUnavailable: 0**:
    - No existing pods can be terminated or become unavailable until the new pod is created, passes its health checks (startup, readiness, etc.), and is fully ready. This setting ensures that there is always the full set of functioning pods available to serve traffic, meaning no reduction in capacity during the update.

### What Happens During a Rolling Update with These Settings?

1. **New Pod Creation**:
    - Kubernetes will start by creating **1 additional pod** (due to `maxSurge: 1`).
    - This new pod will go through the startup process, including passing any **startup probes** and **readiness probes**.
    - If the new pod passes all health checks and becomes **ready**, it will be added to the pool of active pods.

2. **No Pods Terminated Until New Pod is Ready**:
    - Since `maxUnavailable` is set to **0**, Kubernetes will not terminate any of the old pods until the new pod is confirmed to be **fully ready**.
    - This ensures that there is **no downtime** and that your application maintains 100% capacity during the deployment.

3. **Rolling Process**:
    - After the new pod becomes ready, Kubernetes will terminate **one of the old pods** and create another new pod to continue the rolling update process.
    - This process continues one pod at a time (because of `maxSurge: 1`), with Kubernetes ensuring that:
        - There is always **1 additional pod** temporarily (during new pod creation).
        - No pod is terminated until its replacement is fully ready (because of `maxUnavailable: 0`).

4. **Impact of Startup/Readiness Probe Failures**:
    - If the new pod fails its **startup probe** or **readiness probe**, it will be restarted or marked as failed, and Kubernetes will **not terminate the old pod** until the new one becomes ready.
    - If the new pod repeatedly fails, the old pod will continue to run, ensuring uninterrupted service, but the rolling update will be delayed or paused until the issue is resolved.

### Example Scenario:

- Suppose your deployment has 5 replicas.
- Kubernetes will:
    - Create **1 additional pod** (for a total of 6 pods) due to `maxSurge: 1`.
    - Wait for the new pod to become ready.
    - Once the new pod is ready, it will terminate **1 old pod**, ensuring that the total number of pods running at any given time is never less than 5 (because `maxUnavailable: 0`).

- This process repeats, with Kubernetes ensuring that:
    - There is no downtime during the update.
    - The application always has **at least 5 active pods** serving traffic throughout the rolling update.

### Key Takeaways:
- **No Service Downtime**: Since `maxUnavailable` is 0, Kubernetes ensures there are always the full number of replicas serving traffic, meaning the application never drops below its required capacity during the update.
- **Slow Deployment**: The deployment may be slower compared to other settings because Kubernetes can only replace one pod at a time, waiting for each new pod to become ready before terminating an old one.
- **High Availability**: This configuration is useful when you want **high availability** during the update, especially in production environments where downtime or reduced capacity is unacceptable.

### Summary:

With **maxSurge: 1** and **maxUnavailable: 0**, the rolling update is performed one pod at a time,
ensuring that at no point does the application lose capacity.
Old pods are not terminated until their replacements are fully ready, 
ensuring no service disruption during the update. If any new pod fails its startup or readiness checks,
the update pauses until the issue is resolved.

## maxSurge=1 and maxUnavailable=1

If **maxSurge** is set to **1** and **maxUnavailable** is set to **1** in a Kubernetes or OpenShift **rolling update** strategy, the deployment will proceed with more flexibility compared to when `maxUnavailable` is set to 0. This combination allows for a faster rollout, but with a small risk of having fewer replicas temporarily available during the deployment.

### Breakdown of the Settings:

- **maxSurge: 1**:
    - Kubernetes can create **1 additional pod** beyond the desired replica count during the rolling update. This temporarily increases the number of pods to expedite the update process.

- **maxUnavailable: 1**:
    - Kubernetes allows **1 old pod** to be unavailable (terminated or not ready) during the update. This means that during the update process, there could be **1 fewer pod** available than the desired number of replicas.

### What Happens During a Rolling Update with These Settings?

1. **New Pod Creation**:
    - Kubernetes will create **1 additional pod** due to `maxSurge: 1`. This new pod will go through the startup process, including health checks like **startup probes** and **readiness probes**.
    - The new pod must pass the health checks before it is considered ready to handle traffic.

2. **Old Pod Termination**:
    - Since `maxUnavailable: 1`, Kubernetes is allowed to terminate **1 old pod** **even if the new pod is not yet fully ready**. This means at any point during the rolling update, you could temporarily have **1 less pod available** than your desired replica count.

    - However, the combination of `maxSurge: 1` and `maxUnavailable: 1` ensures that the total number of pods never falls below **1 less than the desired replica count**. For example, if you have 5 replicas, Kubernetes will ensure that at least 4 pods are available during the update.

3. **Parallelism in the Update**:
    - With `maxSurge: 1` and `maxUnavailable: 1`, Kubernetes can update multiple pods faster because it can:
        - Add **1 extra new pod** (surging).
        - Remove **1 old pod** at the same time (even before the new pod is ready).
    - This increases the speed of the rolling update by allowing the system to operate with a temporarily reduced number of replicas.

4. **Faster Deployment, With Slight Availability Risk**:
    - By allowing **1 old pod** to be unavailable, the rolling update will progress faster compared to when `maxUnavailable` is set to 0. However, this comes at the cost of potentially reducing the number of available pods by 1 during the update.
    - This might be acceptable in non-critical environments or where slight fluctuations in availability are tolerable.

### Example Scenario:

- Suppose your deployment has **5 replicas**.
- With `maxSurge: 1` and `maxUnavailable: 1`, Kubernetes can:
    - Create **1 new pod**, making a total of **6 pods temporarily** (the original 5 plus 1 surge pod).
    - **Simultaneously terminate 1 old pod**, meaning that you might have only **4 available pods** until the new pod becomes ready.
    - Once the new pod is ready, the process repeats until all the old pods are replaced.

This speeds up the update because Kubernetes does not wait for a new pod to become fully ready before terminating an old pod.

### Behavior in Case of Probe Failures:

1. **New Pod Failing Startup or Readiness Probe**:
    - If a new pod fails its **startup probe** or **readiness probe**, it will be restarted based on the restart policy.
    - Since `maxUnavailable` is set to **1**, the system can tolerate 1 failed pod (either a new pod that hasn’t become ready or an old pod being terminated), but only **1 pod can be unavailable** at a time.

2. **Impact of Multiple Failures**:
    - If more than 1 pod fails (e.g., multiple new pods fail their health checks), the rolling update will pause because the `maxUnavailable: 1` limit has been reached. Kubernetes will wait until at least 1 pod becomes ready before proceeding with the update.
    - This ensures that the number of available pods does not fall too low, but also prevents too many failures from causing a complete outage.

### Summary of Key Points:

- **maxSurge: 1**: Kubernetes can create 1 additional pod beyond the desired replica count to speed up the rolling update.
- **maxUnavailable: 1**: Kubernetes allows 1 old pod to be unavailable at any time during the update, meaning that at any given moment, the total number of available pods could be reduced by 1.
- **Faster Deployment**: This configuration allows for faster rolling updates since Kubernetes can remove an old pod before the new pod is fully ready, but it does introduce the possibility of reduced availability during the update.
- **Slight Availability Risk**: If the application is sensitive to availability, this setting may cause temporary reductions in capacity. However, it ensures that the system never falls below the desired replica count minus 1, providing a good balance between speed and availability.

This configuration is useful for environments where you want quicker updates but can tolerate having slightly fewer pods available during the update process.

## Rolling update deployment
A **rolling update** in Kubernetes (and OpenShift) primarily works at the level of the **ReplicaSet**, not directly on individual pods.

Here’s how it works:

### Rolling Update on ReplicaSets

1. **Deployment Creates and Manages ReplicaSets**:
    - When you create or update a **Deployment**, it manages the ReplicaSets for the application.
    - During a rolling update, the Deployment creates a new **ReplicaSet** for the updated version of your application.

2. **ReplicaSet Controls Pods**:
    - The new ReplicaSet is responsible for creating new pods with the updated version of your application.
    - Meanwhile, the old ReplicaSet (managing the previous version of the application) controls the existing pods.

3. **Rolling Update Process**:
    - **Gradual Replacement**:
        - The **rolling update** works by gradually scaling up the new ReplicaSet (creating new pods) while scaling down the old ReplicaSet (terminating old pods).
        - This happens according to the update strategy you set (e.g., `maxSurge` and `maxUnavailable`).

    - **Pod Updates**:
        - New pods are created by the new ReplicaSet, and once they become ready, the old pods (from the old ReplicaSet) are terminated.
        - This ensures that the rolling update gradually replaces the old pods with new ones without downtime, provided the system can handle a temporary surge or reduction in capacity.

4. **Final Outcome**:
    - Once the rolling update completes, the **new ReplicaSet** will have all the desired replicas of the new version of your application.
    - The old ReplicaSet will typically be scaled down to zero pods, but it remains in the cluster history for rollback purposes.

### Why the Focus is on ReplicaSets, Not Pods Directly:

- **Consistency**: Managing the update at the ReplicaSet level ensures that Kubernetes can easily keep track of which version of the application is running, and how many replicas of each version are present at any time.

- **Rollback**: The old ReplicaSet remains in the cluster even after the rolling update completes, so if anything goes wrong, Kubernetes can quickly revert to the previous version by scaling the old ReplicaSet back up.

- **Efficiency**: Kubernetes handles the rolling update process efficiently by dealing with entire sets of pods (via ReplicaSets), rather than managing individual pods directly. This approach makes the system more predictable and easier to control.

### Summary:

- **Rolling update** works by gradually transitioning from one **ReplicaSet** (old version) to another **ReplicaSet** (new version).
- The **ReplicaSets** manage the lifecycle of the individual **pods**.
- The rolling update process ensures that pods are updated incrementally, without causing service downtime, and enables easy rollback if needed.

