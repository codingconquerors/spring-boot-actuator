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

If the **Startup Probe** fails after reaching the defined `failureThreshold` and the container still has not started, Kubernetes/OpenShift will **consider the container unhealthy** and will take corrective actions based on the pod's restart policy. Here's what happens:

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
