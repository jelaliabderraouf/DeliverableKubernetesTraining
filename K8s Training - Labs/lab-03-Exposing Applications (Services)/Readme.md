# Lab 03- Exposing applications using Services
---

### Lab Steps

- [Step 1 - Labels and Selectors](#step-1---labels-and-selectors)
- [Step 2 - ClusterIP Services](#step-2---clusterip-services)
- [Step 3- Nodeport Services](#step-3--nodeport-services)
- [Step 4- LoadBalancer Services](#step-4--loadbalancer-services)
- [Step 5- Exercice: Choosing the right service when connecting applications](#step-5--exercice-choosing-the-right-service-when-connecting-applications)
- [Step 6- Using Ingress](#step-6--using-ingress)
- [Step 7 - Horizontal Pod Autoscaler Walkthrough](#step-7---horizontal-pod-autoscaler-walkthrough)











Now that you have a continuously running, replicated application you can expose it on a network. Services are an abstract way to expose an application running on a set of Pods as a network service. With Kubernetes you don't need to modify your application to use an unfamiliar service discovery mechanism. Kubernetes gives Pods their own IP addresses and a single DNS name for a set of Pods, and can load-balance across them.

Here are general attributes of a Kubernetes service:
   - A label selector can find pods that are targeted by a service.
   - A service is assigned an IP address ("cluster IP"), which the service proxies use.
   - A service can map an incoming port to any targetPort. (The targetPort is set, by default, to the port field’s same value. The targetPort can be defined as a string.)

There are four different service types, each with different behaviors:
  - **ClusterIP** exposes the service on an internal IP only. This makes the service reachable only from within the cluster. This is the default type.
  - **NodePort** exposes the service on each node’s IP at a specific port. This gives the developers the freedom to set up their own load balancers, for example, or configure environments not fully supported by Kubernetes.
  - **LoadBalancer** exposes the service externally using a cloud provider’s load balancer. This is often used when the cloud provider’s load balancer is supported by Kubernetes, as it automates their configuration.
  - **ExternalName** will just map a CNAME record in DNS. No proxying of any kind is established. This is commonly used to create a service within Kubernetes to represent an external datastore like a database that runs externally to Kubernetes. 

# Step 1 - Labels and Selectors
In this step, you will be able to use labels and selectors when addressing Pods.
Let's go through a Deployment and a Service.
- Create the following Yaml file defining a Deployment and a Service for MySQL application. Name The file `unit3-01-labels-and-selectors.yaml`.
  ```yaml
  apiVersion: apps/v1
  kind: Deployment
  metadata:
    name: mysql
  spec:
    replicas: 2
    selector:
      matchLabels:
        app: mysql
        tier: db
    template:
      metadata:
        labels:
          app: mysql
          tier: db
      spec:
        containers:
        - image: quay.io/mromdhani/mysql:5.6
          name: mysql
          env:          
          - name: MYSQL_ROOT_PASSWORD # Use secret in real usage
            value: password
          ports:
          - containerPort: 3306
            name: mysql
  ---
  apiVersion: v1
  kind: Service
  metadata:
    name: mysql
  spec:
    selector:
      app: mysql
      tier: db
    ports:
    - port: 3306

  ```
In the Deployment specification, we defined the following labels in the ReplicaSet section for the deployment.
```yaml
 matchLabels:
        app: mysql
        tier: db 
```
In the Service specification, we defined the following selector for Pods.
```yaml
 selector:
    app: mysql
    tier: db
```
The defined selector expression matches the label definitions within the Pod template.  This way the service will balance the traffic for the pods. Let's verify that the Service manages access for the Pods.
- Apply the Yaml file using the following command.
```shell
  kubectl apply -f unit3-01-labels-and-selectors.yaml
```
- View the Endpoints managed by the service. You will see two endpoints, each one is dedicated to MySQL Pod.
```shell
  kubectl get ep mysql 
  NAME       ENDPOINTS                      AGE
  my-nginx   10.1.1.93:3306,10.1.1.94:3306  1m       
```
- Let's comment out the `tier: db` label from the Replicaset definition (Line `10`) and from the Pod template (Line `15`) in the deployment specification and apply the Yaml file again. You shoudl remove the deployment completely and re-apply it because we have touched its selector ! 
```shell
  kubectl delete -f unit3-01-labels-and-selectors.yaml
  kubectl apply -f unit3-01-labels-and-selectors.yaml
```
- View the Endpoints managed by the service. You will see no endpoints due to the missmatching between the labels and selector expression.
```shell
  kubectl describe  svc mysql | findstr Endpoints
  Endpoints:         <none>  
```
- Clean up. Remove the deployment and the service.
```shell
  kubectl delete -f unit3-01-labels-and-selectors.yaml
```

# Step 2 - ClusterIP Services

The ClusterIP is the default service type. Kubernetes will assign an internal IP address to your service. This IP address is reachable only from inside the cluster. You can - optionally - set this IP in the service definition file. Think of the case when you have a DNS record that you don't want to change and you want the name to resolve to the same IP address. You can do this by defining the clusterIP part of the service definition as follows:
```shell
kubectl cluster-info dump | findstr service-cluster-ip-range
       --service-cluster-ip-range=10.96.0.0/12
```
- Let's continue with the Yaml used in the previous Step. The service was of type ClusterIP since this is the default service which assigned by Kubernetes API Server whenever you don't specify any type.
Here is a copy of the same Yaml renamed `unit3-02-services-cluster-ip.yaml`. The service type has been added explicity just for better lisibility.
Create the `unit3-02-services-cluster-ip.yaml` file and initialize it as follows.
  ```yaml
  apiVersion: apps/v1
  kind: Deployment
  metadata:
    name: mysql-local
  spec:
    replicas: 2
    selector:
      matchLabels:
        app: mysql-local
        tier: db  
    template:
      metadata:
        labels:
          app: mysql-local
          tier: db  
      spec:
        containers:
        - image: quay.io/mromdhani/mysql:5.6
          name: mysql
          env:          
          - name: MYSQL_ROOT_PASSWORD # Use secret in real usage
            value: password
          ports:
          - containerPort: 3306
            name: mysql
  ---
  apiVersion: v1
  kind: Service
  metadata:
    name: mysql
  spec:
    selector:
      app: mysql-local
      tier: db
    type: ClusterIP   # this is the default type
    ports:
    - port: 3306
  ```
- Apply the Yaml file using the following command.
```shell
  kubectl apply -f unit3-02-services-cluster-ip.yaml
```
- View the Endpoints managed by the service. You will see two endpoints, each one is dedicated to MySQL Pod.
```shell
  kubectl get ep mysql 
  NAME       ENDPOINTS                      AGE
  my-nginx   10.1.1.97:3306,10.1.1.98:3306   1m       
```
- View the hostnames and the IPs of the replica Pods:
```shell
kubectl get pods -o wide
NAME                     READY   STATUS    RESTARTS   AGE     IP           NODE             NOMINATED NODE   READINESS GATES
mysql-8474c4447c-d2pww   1/1     Running   0          35m     10.1.1.97    docker-desktop   <none>           <none>
mysql-8474c4447c-qzlqx   1/1     Running   0          35m     10.1.1.98    docker-desktop   <none>           <none>
```
- The question now how to access the MySQL Pods (`mysql-8474c4447c-d2pww:10.1.1.97` , `mysql-8474c4447c-qzlqx:10.1.1.98`) ? CluserIP does not enable the access from the outside of the cluster. We should get into the cluster to access the the Pods. 

 An easy solution, is to spin up a new Pod within the cluster. This Pod will run  a MySQL Client image in order to connect to one of the MySQL Pods. Run the following command to implement this solution.

  ```
  kubectl run -it --rm --image=quay.io/mromdhani/mysql:5.6 --restart=Never mysql-client -- mysql -h mysql -ppassword -h 10.1.1.97
  If you don't see the prompot press Enter.
  mysql>
  ```
  From within the mysql> prompt run the following commands to verify the running hostname and the remote client.
  ```
  mysql> show variables where Variable_name='hostname';
  +---------------+------------------------+
  | Variable_name | Value                  |
  +---------------+------------------------+
  | hostname      | mysql-8474c4447c-d2pww |
  +---------------+------------------------+
  1 row in set (0.01 sec)
  ```
  To display the IP of the Client which is the new pod, run the following command:
  ```shell
  mysql> select host from information_schema.processlist;
  +-----------------+
  | host            |
  +-----------------+
  | 10.1.1.99:36196 |
  +-----------------+
  1 row in set (0.00 sec)
```
  > NOTE: ClusterIP loadbalances the requests between all the Pods[[ref](https://kubernetes.io/docs/concepts/services-networking/service/#defining-a-service)]. In order to get a correct database behaviour, We should share the data between the two pods. A correct solution should make use of a shared Persistent Volume between the two pods. We will see this solution in the next Lab.  
- Clean Up. Remove the application and the service 
```shell
  kubectl delete -f unit3-02-services-cluster-ip.yaml
```

# Step 3- Nodeport Services

The NodePort services makes the Pods accessible on a static port on each Node in the cluster. This means that the service can handle requests that originate from outside the cluster.

You can use the IP address of any node, the service will receive the request and route it to one of the pods. Manually allocating a port to the service is optional. If left undefined, Kubernetes will automatically assign one. It must be in the range of `30000-32767`. If you are going to choose it, ensure that the port was not already used by another service. Otherwise, Kubernetes will report that the API transaction has failed.

Notice that you must always anticipate the event of a node going down and its IP address becomes no longer reachable. The best practice here is to place a load balancer above your nodes.

- Apply the Yaml file using the following command.
```shell
  kubectl apply -f unit3-03-services-nodeport.yaml
```
This is the content of `unit3-03-services-nodeport.yaml`
  ```yaml
  apiVersion: apps/v1
  kind: Deployment
  metadata:
    name: app-nodeport
  spec:
    selector:
      matchLabels:
        run: dockercloud-nodeport
    replicas: 3
    template:
      metadata:
        labels:
          run: dockercloud-nodeport
      spec:
        containers:
        - name: dockercloud
          image: quay.io/mromdhani/hello-world
          ports:
          - containerPort: 80
  ---
  apiVersion: v1
  kind: Service
  metadata:
    name: svc-nodeport
  spec:
    # Exposes the service on a static port on each node
    # so that we can access the service from outside the cluster 
    type: NodePort
    # When the node receives a request on the static port (30166) "select pods with
    #  the label 'run' set to 'dockercloud-nodeport'" and forward the request to one of them
    selector:
      run: dockercloud-nodeport
    ports:
    - nodePort: 30166   # A static port assigned to each node for external access
      port: 8080        # Port  exposed internally in the cluster
      targetPort: 80    # port that containers are listening to
      protocol: TCP
  ```
- View the Endpoints managed by the service. You will see two endpoints, each one is dedicated to CodeCloud Pod.
```shell
  kubectl get ep svc-nodeport 
  NAME       ENDPOINTS                      AGE
  svc-nodeport   10.1.1.106:80,10.1.1.107:80,10.1.1.108:80   2m56s     
```
- View the description of the service:
```shell
  kubectl get svc svc-nodeport 
  NAME           TYPE       CLUSTER-IP     EXTERNAL-IP   PORT(S)          AGE
 svc-nodeport   NodePort   10.99.219.38   <none>        8080:30166/TCP   3m56s    
```
- Curl the application or browse it at `http://localhost:30166`. The Node address in the case of Docker Desktop for Windows is `localhost`  and the NodePort is `30166` which we have specified in the Yaml manifest. 
When using a web browser, refresh several times the URL and you will notice that the service load balances the requests to the 3 pods. 
- Clean Up. Remove the application and the service application  
```shell
  kubectl delete -f unit3-03-services-nodeport.yaml
```

# Step 4- LoadBalancer Services

This service type works when you are using a cloud provider to host your Kubernetes cluster. When you choose LoadBalancer as the service type, the cluster will contact the cloud provider and create a load balancer. Traffic arriving at this load balancer will be forwarded to the backend pods. The specifics of this process is dependent on how each provider implements its load balancing technology. 

Docker Desktop for Windows creates an external load balancer which is able to automatically handle the workload in case we have multiple instances of our web application running.

- Apply the Yaml file using the following command.
```shell
  kubectl apply -f unit3-04-services-loadbalancer.yaml
```
This is the content of `unit3-04-services-loadbalancer.yaml`
  ```yaml
  apiVersion: apps/v1
  kind: Deployment
  metadata:
    name: app-loadbalancer
  spec:
    selector:
      matchLabels:
        run: dockercloud-lb
    replicas: 3
    template:
      metadata:
        labels:
          run: dockercloud-lb
      spec:
        containers:
        - name: dockercloud
          image: quay.io/mromdhani/hello-world
          ports:
          - containerPort: 80
  ---
  apiVersion: v1
  kind: Service
  metadata:
    name: svc-loadbalancer
  spec:
    # Exposes the service using an external loadbalancer
    type: LoadBalancer
    # When the loadbalancer receives a request on the  port (8080) it forward the request to one 
    # of the pods on their port 80.
    selector:
      run: dockercloud-lb
    ports:
    - port: 8080        # Port exposed by the loadbalancer
      targetPort: 80    # port that containers are listening to
      protocol: TCP
  ```
- View the description of the service:
```shell
  kubectl get svc svc-loadbalancer 
  NAME               TYPE           CLUSTER-IP      EXTERNAL-IP   PORT(S)          AGE
 svc-loadbalancer   LoadBalancer   10.101.77.108   localhost     8080:30672/TCP   22s   
```
- Curl the application or browse it at `http://localhost:8080`. The external IP  is `localhost`  and the loadbalancer port is `8080` which we have specified in the Yaml manifest. 
When using a web browser, refresh several times the URL and you will notice that the service load balances the requests to the 3 pods. 
- Clean Up. Remove the application and the service application  
```shell
  kubectl delete -funit3-04-services-loadbalancer.yaml
```

# Step 5- Exercice: Choosing the right service when connecting applications

- Requirements :
   - We are building a Spring Boot Microservice that provides REST endpoint and uses a Relational MySQL database in its backend. The microservice and database should be loosely coupled.
   - We want access the REST endpoind from outside the cluster. The access to the database should be restricted to from within the cluster.
- Starter Resources
   - The YAML manifests for the REST endpoint and the MySQL database are provided in these starter files :  `unit3-05-microservice-mysql.yaml` and `unit3-05-microservice-rest-endpoint-starter.yaml`. The specification of service types are missing !
- Complete the specification the service types. Perform the deployment and test it.  To test the REST endpoint, use its relative path `/products`.

- The solution, if you need it,  in given in folder `step5-solution`.        


# Step 6- Using Ingress

An ingress is really just a set of rules to pass to a controller that is listening for them. You can deploy a bunch of ingress rules, but nothing will happen unless you have a controller that can process them. A LoadBalancer service could listen for ingress rules, if it is configured to do so.

Ingress sits between the public network (Internet) and the Kubernetes services that publicly expose our Api's implementation. Ingress is capable to provide Load Balancing, SSL termination, and name-based virtual hosting.
Ingress capabilities allows to securely expose multiple API's or Applications from a single domain name.

To set up an ingress, we need to configure a **Ingress Controller** is simply a pod that is configured to interpret ingress rules. One of the most popular ingress controllers supported by Kubernetes is nginx. There are other controller implementations like [Traefik](https://docs.traefik.io/providers/kubernetes-ingress/), ...

- Install the Nginx Ingress Controller
  - Create the Ingress Controller using this command:
    ```shell
    kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-0.32.0/deploy/static/provider/cloud/deploy.yaml
    ```
  - Verification: 
    ```shell
    kubectl get pods --all-namespaces -l app.kubernetes.io/name=ingress-nginx
    ```
- Test sample application
  - Deploy test application:
    ```shell
    kubectl apply -f unit3-06-ingress-apple.yaml
    kubectl apply -f unit3-06-ingress-banana.yaml
    kubectl apply -f unit3-06-ingress-main.yaml
    ```
  - Test sample application
    ```shell
    $ curl -kL http://localhost/apple
    apple
    $ curl -kL http://localhost/banana
    banana
    ```
- Delete the sample app
    ```shell
    kubectl delete -f unit3-06-ingress-apple.yaml
    kubectl delete -f unit3-06-ingress-banana.yaml
    kubectl delete -f unit3-06-ingress-main.yaml
    ```
- Remove Ingress
  ```shell
  kubectl delete -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-0.32.0/deploy/static/provider/cloud/deploy.yaml
  ```

# Step 7 - Horizontal Pod Autoscaler Walkthrough

Horizontal Pod Autoscaler automatically scales the number of pods in a replication controller, deployment, replica set or stateful set based on observed CPU utilization (or, with beta support, on some other, application-provided metrics).

This step walks you through an example of enabling Horizontal Pod Autoscaler for an application php-apache server. _metrics-server monitoring needs to be deployed in the cluster_ to provide metrics via the resource metrics API, as Horizontal Pod Autoscaler uses this API to collect metrics. 

- Installing Metrics Server

    [Metrics Server](https://github.com/kubernetes-sigs/metrics-server) collects resource metrics from Kubelets and exposes them in Kubernetes apiserver through Metrics API for autoscaling puposes.  Metrics Server isn't installed by default with Docker Desktop. Follow these steps to install it and configure it.

    1. Clone or download the release branch of Metrics Server project. Here is the link : <https://codeload.github.com/kubernetes-sigs/metrics-server/zip/release-0.3>. You can also use the provided version for metrics version. 

    2. Open the `deploy/1.8+/metrics-server-deployment.yaml` file in an editor and perform the following changes.
      If you are using the provided version, the configuration required here is already done. Skip to 3.    

        - Change the image as follows:
        ```shell
          image: k8s.gcr.io/metrics-server-amd64:v0.3.1
        ```
        - Add the `–kubelet-insecure-tls` argument into the existing `args` section. That section will look like the following once you're done:
        ```shell
        args:
          - --cert-dir=/tmp
          - --secure-port=4443
          - --kubelet-insecure-tls
        ```
    3. Apply all the yaml files from the installation folder of metrics server, to create the deployment, services, etc.
        ```shell
        kubectl create -f deploy/1.8+/
        ```
    4. You should now be able to run kubectl `top` commands!
        ```shell
        kubectl get pods -n kube-system

- Run & expose php-apache server
  First, we will start a deployment running the image and expose it as a service using the following configuration: (`unit3-07-horizontal-pod-autoscaler.yaml`)
  ```yaml
  apiVersion: apps/v1
  kind: Deployment
  metadata:
    name: php-apache
  spec:
    selector:
      matchLabels:
        run: php-apache
    replicas: 1
    template:
      metadata:
        labels:
          run: php-apache
      spec:
        containers:
        - name: php-apache
          image: k8s.gcr.io/hpa-example
          ports:
          - containerPort: 80
          resources:
            limits:
              cpu: 500m
            requests:
              cpu: 200m
  ---
  apiVersion: v1
  kind: Service
  metadata:
    name: php-apache
    labels:
      run: php-apache
  spec:
    ports:
    - port: 80
    selector:
      run: php-apache
  ```
  - Apply the application and service.
    ```shell
    kubectl apply -f unit3-07-horizontal-pod-autoscaler.yaml
    ```
- Create Horizontal Pod Autoscaler    
   Now that the server is running, we will create the autoscaler using kubectl autoscale. The following command will create a Horizontal Pod Autoscaler that maintains between 1 and 10 replicas of the Pods controlled by the php-apache deployment we created in the first step of these instructions. Roughly speaking, HPA will increase and decrease the number of replicas (via the deployment) to maintain an average CPU utilization across all Pods of 50% (since each pod requests 200 milli-cores by kubectl run), this means average CPU usage of 100 milli-cores).
  ```shell
  kubectl autoscale deployment php-apache --cpu-percent=50 --min=1 --max=10
  ```
  We may check the current status of autoscaler by running:
  ```shell
  kubectl get hpa
  NAME         REFERENCE               TARGETS      MINPODS   MAXPODS   REPLICAS   AGE  
  php-apache   Deployment/php-apache    0%/50%      1         10        0          9s 
  ```
  Please note that the current CPU consumption is 0% as we are not sending any requests to the server (the TARGET column shows the average across all the pods controlled by the corresponding deployment).
- Increase load
   
   Now, we will see how the autoscaler reacts to increased load. We will start a container, and send an infinite loop of queries to the php-apache service (please run it in a different terminal):
  ```shell
  kubectl run -it --rm load-generator --image=k8s.gcr.io/busybox
  ```
  Hit enter for command prompt:
  ```shell
  while true; do wget -q -O- http://php-apache; done
  ```
  Within a minute or so, we should see the higher CPU load by executing:

  ```shell
  kubectl get hpa  
  NAME         REFERENCE               TARGETS    MINPODS   MAXPODS   REPLICAS   AGE
  php-apache   Deployment/php-apache   248%/50%   1         10        1          6m24s
  ```
  Here, CPU consumption has increased to 248% of the request. As a result, the deployment was resized to 5 replicas:
  ```shell
  kubectl get deployment php-apache
  NAME         READY   UP-TO-DATE   AVAILABLE   AGE
  php-apache   5/5     5            5           8m7s
  ```
    > Note: It may take a few minutes to stabilize the number of replicas. Since the amount of load is not controlled in any way it may happen that the final number of replicas will differ from this example.
- Stop load
     
  We will finish our example by stopping the user load.
  In the terminal where we created the container with busybox image, terminate the load generation by typing <Ctrl> + C.
  Then we will verify the result state (after a minute or so):
  ```shell
  kubectl get hpa
  NAME         REFERENCE                     TARGET       MINPODS   MAXPODS   REPLICAS   AGE
  php-apache   Deployment/php-apache/scale   0% / 50%     1         10        1          11m
  ```
  ```shell
  kubectl get deployment php-apache
  NAME         READY   UP-TO-DATE   AVAILABLE   AGE
  php-apache   1/1     1            1           27m
  ```
  Here CPU utilization dropped to 0, and so HPA autoscaled the number of replicas back down to 1.

  > Note: Autoscaling the replicas may take a few minutes
- Clean Up
     
  Remove the deployment and the service
  ```shell 
  kubectl delete -f unit3-07-horizontal-pod-autoscaler.yaml
  ```  
  Remove the HPA autoscaler
  ```shell 
  kubectl delete hpa php-apache
  ```  