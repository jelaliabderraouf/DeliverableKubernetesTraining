# Lab 04- Stateful Applications
---


- [Step 1 - ConfigMaps and Secrets](#step-1---configmaps-and-secrets)
    - [Creating ConfigMaps](#creating-configmaps)
    - [Accessing ConfigMaps](#accessing-configmaps)
    - [Creating Secrets](#creating-secrets)
    - [Configuring and Deploying MySQL](#configuring-and-deploying-mysql)
- [Step 2 - Local Volumes : EmptyDir and HostPath](#step-2---local-volumes--emptydir-and-hostpath)
    - [emptyDir](#emptydir)
    - [hostPath](#hostpath)
- [Step 3- Persistent Volumes (PVs) and Persistent Volume Claims (PVCs)](#step-3--persistent-volumes-pvs-and-persistent-volume-claims-pvcs)
    - [Creating a PersistentVolume](#creating-a-persistentvolume)
    - [Creating a PersistentVolumeClaim](#creating-a-persistentvolumeclaim)
    - [Adding Storage to Pods](#adding-storage-to-pods)
- [Step 4- Statefull Sets](#step-4--statefull-sets)
    - [Pods in a StatefulSet](#pods-in-a-statefulset)
      




# Step 1 - ConfigMaps and Secrets
The 3rd factor (Configuration) of the [Twelve-Factor App principles](https://12factor.net/) states:

  > Configuration that varies between deployments should be stored in the environment.

This is where Kubernetes ConfigMaps and Secrets can help by supplying your deployment containers with the contextual and secretive information they require. Secrets and ConfigMaps behave similarly in Kubernetes, both in how they are created and because they can be exposed inside a container as mounted files or volumes or environment variables.

A core component of the Kubernetes management plane is **etcd**. Etcd is a high-available, distributed key/value store ideal for contextual environment settings and data. The ConfigMaps and Secrets are simply interfaces for managing this information in etcd.

In the following steps you will learn:

  - how to create configuration data in the form of ConfigMaps and Secrets,
  - how Pods make configuration accessible for applications in containers,
  - how secrets should remain secrets.

### Creating ConfigMaps
A ConfigMap is simple data associated with a unique key. They can be created and shared in the containers in the same ways as secrets. ConfigMaps are intended for non-sensitive data—configuration data—like config files and environment variables and are a great way to create customized running services from generic container images.
- **Create ConfigMaps from litteral values**
  
  You can write a `YAML` representation of the ConfigMap manually and load it into Kubernetes, or you can use the CLI  `kubectl create configmap` command to create it from the command line. 
  - **Create ConfigMap from CLI**
  
   The following example creates a ConfigMap using the CLI.
    ```shell
    kubectl create configmap mysql-config --from-literal=DB_NAME="ProductsDB" --from-literal=USER_NAME="kubernetes"
    ```
     Key/value configs are passed using the `--from-literal` option. It is possible to declare more than one `--from-literal` in order to pass multiple configuration entries in the same command. 
     
    - Check Kubernetes using `kubectl get configmap `after the create. 
    ```shell
      kubectl get configmap mysql-config 
    ```
    - To see the actual data, get it in YAML form.
    ```shell
      kubectl get configmap mysql-config -o yaml
    ```
    - Or, in description form
    ```shell
      kubectl describe configmap mysql-config 
    ```
    - Finally, to clean up delete the configmap.
    ```shell
      kubectl delete configmap mysql-config
    ```
  - **Create ConfigMap from YAML**
   A better way to define ConfigMaps is with a resource YAML file in this form.

   - Edit a yaml file named `unit3-01-configmaps.yaml` and initialize it as follows.
      ```yaml
      apiVersion: v1
      kind: ConfigMap
      metadata:
        name: mysql-config-yaml
        namespace: default
      data:
        DB_NAME: ConfluenceDB
        USER_NAME: kubernetes
        confluence.cnf: |-
          [mysqld]
            collation-server=utf8_bin
            default-storage-engine=INNODB 
            max_allowed_packet=256M           
      ``` 
    - Create the ConfigMap YAML resource using the following command
      ```shell
      kubectl create -f unit3-01-configmaps.yaml
      ```
    - Then, view it.

      ```shell
      kubectl describe configmap mysql-config-yaml
      ```
    The same ConfigMaps can also be explored in the Kubernetes Dashboard.
    - Clean up. Remove the configMap using `kubectl delete configmap command`.

      ```shell
      kubectl delete configmap mysql-config-yaml
      ```

- **Create ConfigMaps from files**
 
 You can also create ConfigMaps from from a file. To do this use the `--from-file` option of the `kubectl create configmap` command.  Your file should have a set of `key=value` pairs, one per line. If a value spans over more than one line, rely on backslash + end-of-line to escape the end of line. 
  - View the content of the given properties file which is located in th `config` folder `unit3-01-mysql.properties`
    ```yaml
    DB_NAME=ConfluenceDB
    USER_NAME=kubernetes 
    ```
  - Execute the following to create a ConfigMap from that configuration file:
    ```shell
    kubectl create configmap mysql-config-from-file --from-file=configs/unit3-01-mysql.properties
    ```
  - Now that we've created the ConfigMap, we can view it with:
    ```shell
    kubectl get configmaps
    ```
    We can get an even better view with:
    ```shell
    kubectl get configmap mysql-config-from-file -o yaml
    ```
  - Clean Up. Remove the ConfigMap using the following command:
    ```shell
    kubectl delete configmap mysql-config-from-file
    ```
  > **Note**: Kubernetes does not have know-how of how to ignore the **comments** and **blank lines** if we use `--from-file`. The `--from-env-file` option fixes that automatically. 
  It is a good practice to have your ConfigMaps in environment file format and use `--from-env-file` to create your ConfigMaps. For more information about the Docker environment file format, visit this [link](https://docs.docker.com/compose/env-file).  

- **Create ConfigMaps from directories**
 
  You can also create a ConfigMap from a directory. This is very similar to creating them from files. It can be very useful, as it allows you to separate out configuration into multiple directories, and then you can create an individual ConfigMp for each directory, and then quickly swap out configuration.
  - Let's create a new one from the `configs` directory.
  ```shell
  kubectl create configmap mysql-config-from-dir --from-file=configs
  ```
  At this point, Kubernetes will create a ConfigMap and populate it with all of the configuration from all files in the directory.
 - Let's view its content:
  ```shell
  kubectl get configmap mysql-config-from-dir  -o yaml
  ```
  - Clean Up. Remove the ConfigMap using the following command:
  ```shell
  kubectl delete configmap mysql-config-from-dir
  ```

### Accessing ConfigMaps
Once the configuration data is stored in ConfigMaps, the containers can access the data. Pods grant their containers access to the ConfigMaps through these three techniques:
   1. through the application command-line arguments,
   2. through the system environment variables accessible by the application,
   3. through a specific read-only file accessible by the application.

Let's explore these access techniques.

- **Accessing ConfigMaps through Command Line Arguments**
   
   This example shows how a Pod accesses configuration data from the ConfigMap by passing in the data through the command-line arguments when running the container. Upon startup, the application would reference these parameters from the program's command-line arguments.

  - Let's create a ConfigMap resource definition
   ```shell
    kubectl create configmap mysql-config-cli --from-env-file=configs/unit3-01-mysql.properties
   ```
  - Create the following Pod Definition. Name the Yaml file `unit3-01-configmaps-inject-cli.yaml` and set it as follows.
    ```yaml
    apiVersion: v1
    kind: Pod
    metadata:
      name: inject-config-via-cli
    spec:
      containers:
        - name: consuming-container
          image: k8s.gcr.io/busybox
          command: [ "/bin/sh", "-c", "echo $(PROPERTY_DB_NAME); echo $(PROPERTY_DB_USER); env" ]
          env:        
            - name: PROPERTY_DB_NAME
              valueFrom:
                configMapKeyRef:
                  name: mysql-config-cli
                  key: DB_NAME
            - name: PROPERTY_DB_USER
              valueFrom:
                configMapKeyRef:
                  name: mysql-config-cli
                  key: USER_NAME
          restartPolicy: Never
      ```  
  Run the following command to create the Pod
  ```shell
  kubectl apply -f unit3-01-configmaps-inject-cli.yaml
  ```
  - Inspect the log of the Pod to verify that the configuration has been applied.
  ```shell
  kubectl logs inject-config-via-cli
  ```
  - Clean Up. Remove the ConfigMap and the Pod.
  ```shell
  kubectl delete configmap mysql-config-cli 
  kubectl delete pod inject-config-via-cli
  ```
- **Accessing ConfigMaps Through Environment variables**
   
   This example shows how a Pod accesses configuration data from the ConfigMap by passing in the data as environmental parameters of the container. Upon startup, the application would reference these parameters as system environment variables.

  - Let's create a ConfigMap resource definition
   ```shell
    kubectl create configmap mysql-config-env --from-env-file=configs/unit3-01-mysql.properties
   ```
  - Create the following Pod Definition. Name the Yaml file `unit3-01-configmaps-inject-env.yaml` and set it as follows.
    ```yaml
    apiVersion: v1
    kind: Pod
    metadata:
      name: inject-config-via-env
    spec:
      containers:
        - name: consuming-container
          image: k8s.gcr.io/busybox      
          command: [ "/bin/sh", "-c", "env" ]
          envFrom:      
          - configMapRef:
              name: mysql-config-env
      restartPolicy: Never
    ```  
  Run the following command to create the Pod
  ```shell
  kubectl apply -f unit3-01-configmaps-inject-env.yaml
  ```
  - Inspect the log of the Pod to verify that the configuration has been applied.
  ```shell
  kubectl logs inject-config-via-env
  ```
  - Clean Up. Remove the ConfigMap and the Pod.
  ```shell
  kubectl delete configmap mysql-config-env 
  kubectl delete pod inject-config-via-env
  ```
- **Accessing ConfigMaps Through Volume Mounts**
   
   This example shows how a Pod accesses configuration data from the ConfigMap by reading from a file in a directory of the container. Upon startup, the application would reference these parameters by referencing the named files in the known directory.

  - Let's create a ConfigMap resource definition
   ```shell
    kubectl create configmap mysql-config-volume --from-env-file=configs/unit3-01-mysql.properties
   ```
  - Create the following Pod Definition. Name the Yaml file `unit3-01-configmaps-inject-volume.yaml` and set it as follows.
    ```yaml
    apiVersion: v1
    kind: Pod
    metadata:
      name: inject-config-via-volume
    spec:
      containers:
        - name: consuming-container
          image: k8s.gcr.io/busybox      
          command: [ "/bin/sh","-c","cat /etc/config/keys" ]
          volumeMounts:      
          - name: config-volume
            mountPath: /etc/config
      volumes:
        - name: config-volume
          configMap:
            name: mysql-config-volume
            items:
            - key: DB_NAME
              path: keys
      restartPolicy: Never
     ```  
  Run the following command to create the Pod
  ```shell
  kubectl apply -f unit3-01-configmaps-inject-volume.yaml
  ```
  - Inspect the log of the Pod to verify that the configuration has been applied.
  ```shell
  kubectl logs inject-config-via-volume
  ```
  - Clean Up. Remove the ConfigMap and the Pod.
  ```shell
  kubectl delete configmap mysql-config-volume 
  kubectl delete pod inject-config-via-volume
  ```

### Creating Secrets

Secrets are Kubernetes objects intended for storing a small amount of sensitive data. It is worth noting that Secrets are stored base64-encoded within Kubernetes, so they are not wildly secure. Make sure to have appropriate role-based access controls (RBAC) to protect access to Secrets. Even so, extremely sensitive Secrets data should probably be stored using something like HashiCorp Vault.
Both ConfigMaps and Secrets are stored in etcd, but the way you submit secrets is slightly different than ConfigMaps.

- **Create Secrets from CLI**
  
  To create secrets you can use the CLI  `kubectl create secret` command or you can write a `YAML` representation of the Secret manually and load it into Kubernetes.

 To create the Secret you should convert it to base64. To do this, there are many possiblities like the `base64` Unix Command, the `System.Convert` Utility in Pwershell, on you can simply use one of the free online base64 encoders.

    _Encoding/Decoding into base64 in Linux Shell_
    ```shell
    $ echo -n 'KubernetesRocks!' | base64    # For Encoding
      S3ViZXJuZXRlc1JvY2tzIQ==
    $ echo "TXlEYlBhc3N3MHJkCg==" | base64 --decode   # For Decoding
      KubernetesRocks!
    ```
    _Encoding/Decoding into base64 in Windows PowerShell_
    ```shell
    PS> [System.Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes("KubernetesRocks!"))
        S3ViZXJuZXRlc1JvY2tzIQ==
    PS> [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String("S3ViZXJuZXRlc1JvY2tzIQ=="))
       KubernetesRocks!
    ```  
  - Let's create a secret using the encoded `base64` value of '`KubernetesRocks!`';
     ```shell
      kubectl create secret generic db-password --from-literal=password=S3ViZXJuZXRlc1JvY2tzIQ==
     ```
  - Check Kubernetes using `kubectl get secret `after the create. 
      ```shell
        kubectl get secret db-password 
      ```
  - To see the actual data, get it in YAML form.
  ```shell
    kubectl get secret db-password -o yaml
  ```
  - To decode the secret in Bash Shell
  ```shell
    kubectl get secret db-password -o 'go-template={{index .data "password"}}' | base64 --decode
  ```    
  - Finally, to clean up delete the secret.
  ```shell
    kubectl delete secret db-password
  ```
    > Note : Never confuse encoding with encryption, as they are two very different concepts. Values encoded are not encrypted. The encoding is to allow a wider variety of values for secrets. You can easily decode the text with a simple base64 command to reveal the original password text.

- **Create Secrets from YAML**
  
 A better way to define Secrets is with a resource YAML file in this form.
 - Create the following Secret YAML definition file. Name it `unit3-01-secrets.yaml` and set it as follows:
    ```yaml
    apiVersion: v1
    kind: Secret
    metadata:
      name: mysql-secrets
    type: Opaque
    data:
      user-password: a3ViZXJuZXRlcw==       #kubernetes
      root-password: S3ViZXJuZXRlc1JvY2tzIQ==   # KubernetesRocks!
    ```
  Run the following command to create the Secret resource.
  ```shell
  kubectl apply -f unit3-01-secrets.yaml
  ```
 - Check Kubernetes using `kubectl get secret `after the create. 
    ```shell
      kubectl get secret mysql-secrets 
    ```
 - To decode the secret in Bash Shell
    ```shell
      kubectl get secret mysql-secrets -o 'go-template={{index .data "password"}}' | base64 --decode
    ```    
 - Finally, to clean up delete the secret.
  ```shell
    kubectl delete secret mysql-secrets
  ```
   **Hint** :  When first creating the YAML file you can skip using the base64 command and instead use the kubectl `--dry-run` feature which will generate the YAML file for you with the encoding.
    ```
    kubectl create secret generic db-password --from-literal=password=MyDbPassw0rd --dry-run -o yaml > my-secret.yaml
    ```
    If you view the content of `my-secret.yaml` you will see the base64 encoded value of the password.

### Configuring and Deploying MySQL

In order to deploy MySQL, you will need to mount the Secrets as environment variables and the ConfigMap as a file. First, though, you need to write a Deployment for MySQL so that you have something to work with. Create a file named `unit3-01-mysql-deployment-starter.yaml` with the following content.
  ```yaml
  apiVersion: apps/v1
  kind: Deployment
  metadata:
    name: mysql-deployment
    labels:
      app: mysql  
  spec:
    replicas: 1
    selector:
      matchLabels:
        app: mysql
    template:
      metadata:
        labels:
          app: mysql
      spec:
        containers:
        - name: mysql
          image: quay.io/mromdhani/mysql:8.0
          ports:
          - containerPort: 3306
            protocol: TCP
          volumeMounts:
          - mountPath: /var/lib/mysql
            name: mysql-volume
        volumes:
        - emptyDir: {}
          name: mysql-volume
  ```
This is a bare-bones Kubernetes Deployment of the official MySQL 8.2 image from Docker Hub. If you'll try to deploy it, it fails and logs this message: `You need to specify one of MYSQL_ROOT_PASSWORD, MYSQL_ALLOW_EMPTY_PASSWORD and MYSQL_RANDOM_ROOT_PASSWORD"`.
Let' add  the Secrets and ConfigMap.

- **Deploy the ConfigMap and the Secret resources**

 Let's first deploy the ConfigMap and Secret Resources and Check that they have been deployed successfully.
  - Deploy the ConfigMap and Verify its deployment.
    ```shell
    kubectl create -f unit3-01-configmaps.yaml
    kubectl get configmap mysql-config
    ```
  - Deploy the Secrets and Verify its deployment.
    ```shell
    kubectl apply -f unit3-01-secrets.yaml
    kubectl get secret mysql-secrets
    ```
- **Add the Secrets to the Deployment as environment variables** 
   
   We have two Secrets that need to be added to the Deployment:
   - The MYSQL_ROOT_PASSWORD (The root password, to add as an environment variable)
   - The MYSQL_PASSWORD (The default user password, to add as an environment variable)
  Let's specify the Secret and the key you want by adding an **env list/array** to **the container spec** in the Deployment and setting the environment variable value to the value of the key in your Secret. In this case, the list contains two entries for for the passwords.
    ```yaml
    env:
      - name: MYSQL_ROOT_PASSWORD
        valueFrom:
          secretKeyRef:
            name: mysql-secrets
            key: root-password
      - name: MYSQL_PASSWORD
        valueFrom:
          secretKeyRef:
            name: mysql-secrets
            key: user-password
    ```
- **Add the ConfigMaps to the Deployment** 
   
  We have to add these configutra:
   - The MYSQL_DATABASE (The default database to create, to add as an environment varaiable)
   - The MYSQL_USER (The default user to create, to add as an environment variable)
   - The cnf configuration entry (To mount as a volume under `/etc/mysql/conf.d/` within the container)
   
     - Let's Specify the MYSQL_DATABASE and MYSQL_USER variables by adding them to the existing **env** list/array of **the container spec**.  
      ```yaml
      - name: MYSQL_DATABASE
        valueFrom:
          configMapKeyRef:
            name: mysql-config
            key: DB_NAME
      - name: MYSQL_USER
        valueFrom:
          configMapKeyRef:
            name: mysql-config
            key: USER_NAME
      ```
    - Let's add a new volume definition under **volumes** section. This volume will be initialized from the ConfigMap (the .cnf key) and will be used to define a new mount path under the  **mountPath** the existing **volumeMounts** section. This mountPath will point to `/etc/mysql/conf.d`.  
        ```yaml
           volumeMounts:
            - mountPath: /var/lib/mysql
              name: mysql-volume
            - mountPath: /etc/mysql/conf.d
              name: mysql-config-volume
        volumes:
          - emptyDir: {}
            name: mysql-volume
          - configMap:
              name: mysql-config
              items:
              - key: confluence.cnf
                path: confluence.cnf
            name: mysql-config-volume
        ```
- **Apply the deploymenet and check the configs and the secrets** 
  - Apply the deployment
    ```shell
    kubectl apply -f unit3-01-mysql-deployment-starter.yaml
    ```
  - Check that the deployment has succeeded and view the MySQLPod state
    ```shell
    kubectl get deploy
    kubectl get pods
    ```
  - Hop into the Pod and check that secrets and the configuration has been applied
    ```shell
    kubectl exec -it [PodName] sh  
    ```
    From within the Pod shell run
    ```shell
    # env
    # cat /etc/mysql/conf.d/confluence.cnf  
    ```
  - Clean Up. Remove the Secret, the ConfigMap and the Deployment
    ```shell
    kubectl delete secret mysql-secrets 
    kubectl delete cm  mysql-config
    kubectl delete deploy mysql-deployment 
    ```

# Step 2 - Local Volumes : EmptyDir and HostPath

There are more than 20 volume types Kubernetes supports: [Kubernetes Volume Types](https://kubernetes.io/docs/concepts/storage/volumes/). In this step you will learn  different usages of **EmptyDir** and **HostPath** volume types. Each of these volume has its own use case and should be used only in those specific cases.

### emptyDir

An `emptyDir` volume is first created when a Pod is assigned to a Node, and **exists as long as that Pod is running on that node**.
As the name says, it is initially empty. All Containers in the same Pod can read and write in the same emptyDir volume.
When a Pod is restarted or removed, the data in the emptyDir is lost forever.

Some use cases for an `emptyDir` are:

- scratch space, such as for a disk-based merge sort
- checkpointing a long computation for recovery from crashes
- holding files (caching) that a content-manager container fetches while a webserver container serves the data

The storage media (Disk, SSD, etc.) of an emptyDir volume is determined by the medium of the filesystem holding the kubelet root dir (typically `/var/lib/kubelet`). You can set the `emptyDir.medium` field to `"Memory"` to tell Kubernetes to mount a tmpfs (RAM-backed filesystem) for you instead.  

- View the YAML file named `unit3-02-emptydir.yaml`. It describes a Pod using an `emptyDir` for Caching.
  ```yaml
  apiVersion: v1
  kind: Pod
  metadata:
    name: test-pd
  spec:
    containers:
    - image: gcr.io/google_containers/test-webserver
      name: test-container
      volumeMounts:
      - mountPath: /cache
        name: cache-volume
    volumes:
    - name: cache-volume
      emptyDir: {}
  ```

### hostPath

A `hostPath` volume mounts a file or directory from **the host node's filesystem** into your pod. This is not something that most Pods will need, but it offers a powerful escape hatch for some applications.

For example, some uses for a `hostPath` are:

- running a container that needs access to Docker internals; use a hostPath of `/var/lib/docker`
- running `cAdvisor` in a container; use a `hostPath` of `/dev/cgroups`

Disadvantages of using this type of volume:

- Pods with identical configuration (such as created from a podTemplate) may behave differently on different nodes due to different files on the nodes
- the directories created on the underlying hosts are only writable by root. Which means, you either need to run your container process as root or modify the file permissions on the host to be writable by non-root user, which may lead to security issues.
- You should NOT use hostPath volume type for StatefulSets

_Task:_

  - Consider the Pod definition given in the YAML file `unit3-02-hostpath-starter.yaml`.The Pod uses an `emptyDir`. You are asked to change this volume into a `hostPath` volume named `hp-volume`. The Path in the host should be `/data` and the mount path within the container should be `/data/test`. 

# Step 3- Persistent Volumes (PVs) and Persistent Volume Claims (PVCs)

Kubernetes persistent volumes (PVs) are user-provisioned storage volumes assigned to a Kubernetes cluster. Persistent volumes' life-cycle is independent from any pod using it. Thus, persistent volumes are perfect for use cases in which you need to retain data regardless of the unpredictable life process of Kubernetes pods.

Persistent Volume Claims (PVCs) are objects that connect to back-end storage volumes through a series of abstractions. A PersistentVolumeClaim is a request for a resource with specific attributes, such as storage size. In between the two is a process that matches a claim to an available volume and binds them together. This allows the claim to be used as a volume in a pod.

### Creating a PersistentVolume

PersistentVolumes abstract the low-level details of a storage device, and provide a high-level API to provide such storage to Pods.

PersistentVolumes are storage inside of your cluster that has been provisioned by your administrator. Their lifecycle is external to your Pods or other objects.

There are many different types of PersistentVolumes that can be used with Kubernetes. As an example, you can use a **local filesystem**, **NFS**, and there are plugins for **cloud vendor storage solutions** like EBS.

- Let's specify PersistentVolumes via a Manifest file (`unit3-03-persistentvolume.yaml`):

  ```yaml
  apiVersion: v1
  kind: PersistentVolume
  metadata:
    name: local-pv
  spec:
    capacity:
      storage: 500Mi
    volumeMode: Filesystem
    accessModes:
    - ReadWriteOnce
    persistentVolumeReclaimPolicy: Delete
    storageClassName: local-storage
    hostPath:
      path: "/mnt/data"
  ```
  This describes a single PersistentVolume. It is mounted to `/mnt/data` on a node. It is of type `Filesystem`, with `500 MB` of storage. (`hostPath` are only appropriate for testing in single node environments)

  - We can create this PersistentVolume:
    ```shell
    kubectl apply -f unit3-03-persistentvolume.yaml
    ```
  - We can then view it with:
  ```shell
  kubectl get pv
  ```
  - We can get even more information with:
  ```shell
  kubectl describe pv local-pv
  ```

### Creating a PersistentVolumeClaim

Now that we have a PersistentVolume, let's make a PersistentVolumeClaim to provide storage to a Pod. PersistentVolumeClaims enable you to request a certain amount of storage from a PersistentVolume, and reserve it for your Pod.

The following is a YAML manifest for a PersistentVolumeClaim (`unit3-03-persistentvolumeclaim.yaml`):
  ```yaml
  apiVersion: v1
  kind: PersistentVolumeClaim
  metadata:
    name: nginx-pvc
  spec:
    # Notice the storage-class name matches the storage class in the PV we made in the previous step.
    storageClassName: local-storage
    accessModes:
      - ReadWriteOnce
    resources:
      requests:
        storage: 20Mi
  ```      
  This PersistentVolumeClaim is requesting `20 MB` of storage from a local Filesystem PersistentVolume. When a Pod uses this Claim, Kubernetes will attempt to satisfy the claim by enumerating all PersistentVolumes, and matching the requirements in this Claim to what is present in the cluster.

  If we were to match this Claim to PersistentVolume, it would succeed, because we have a PersistentVolume of type Filesystem with 100 GB of storage.

  - Let's create the PersistentVolumeClaim:
  ```shell
  kubectl apply -f unit3-03-persistentvolumeclaim.yaml
  ```
  - and wait until the resource is available:
  ```shell
  kubectl get pvc --watch
  ```
  - We can also use label selectors to aid in matching Claims with PersistentVolumes.
  ```yaml
  apiVersion: v1
  kind: PersistentVolumeClaim
  metadata:
    name: nginx-pvc
  spec:
    accessModes:
      - ReadWriteOnce
    resources:
      requests:
        storage: 20Mi
    selector:
      matchLabels:
        env: dev
  ```
  This Claim is identical to the previous one, but it will only be matched with PersistentVolumes that have the label `env: dev`. You can use this to have more control over which Claims bind to a particular PersistentVolume.

### Adding Storage to Pods

Now that we have PersistentVolumes and a PersistentVolumeClaim, we can provide the Claim to a Pod, and Kubernetes will provision storage.
The following YAMLmanifest (`unit3-03-pod-with-pvc.yaml`) describes a Pod using a PVC.
  ```yaml
  apiVersion: v1
  kind: Pod
  metadata:
    name: nginx
  spec:
    containers:
    - name: nginx
      image: quay.io/mromdhani/nginx:latest
      volumeMounts:
      - name: nginx-data
        mountPath: /data/nginx
    volumes:
    - name: nginx-data
      persistentVolumeClaim:
        claimName: nginx-pvc
  ```      
  This is very similar to a Pod that uses a local storage. Now, we have basically changed what provides the Storage with the addition of those bottom two lines. 
  - To deploy our pod, execute the following:
  ```shell
  kubectl apply -f unit3-03-pod-with-pvc.yaml
  ```
  - We can see that the Pod was created, and that the Claim was fulfilled:
  ```shell
  kubectl get pods --watch
  kubectl get pvc
  ```
  - Clean Up. Remove the PV, the PVC and the Pod
   ```shell
  kubectl delete pv/local-pv, pvc/nginx-pvc, po/nginx 
  ```
 
# Step 4- Statefull Sets

 When using Kubernetes, most of the time you don't care how your pods are scheduled, but sometimes you care that pods are deployed in order, that they have a persistent storage volume, or that they have a unique, stable network identifier across restarts and reschedules. In those cases, StatefulSets can help you accomplish your objective.

Some examples of reasons you'd use a StatefulSet include:

- A NoSQL datastore like Redis accessing volumes, but you want it to ensure that each Pod keeps the same id and accesses to the same volume even if it is redeployed or restarted.
- A replicated relational database having Pods for master and agents. Pod replicas are not interchangeable; they should each have each a unique identier and a unique state. 
  
The primary feature that enables StatefulSets to run a replicated database within Kubernetes is providing each pod a unique ID that persists, even as the pod is rescheduled to other machines. The persistence of this ID then lets you attach a particular volume to the pod, retaining its state even as Kubernetes shifts it around your datacenter.

- Create the following YAML for a Statefulset example (`unit3-04-satefulset-nginx.yaml`).
  ```yaml
  apiVersion: apps/v1
  kind: StatefulSet
  metadata:
    name: web
  spec:
    selector:
      matchLabels:
        app: nginx # Label selector that determines which Pods belong to the StatefulSet
                  # Must match spec: template: metadata: labels
    serviceName: "nginx"
    replicas: 3
    template:
      metadata:
        labels:
          app: nginx # Pod template's label selector
      spec:
        terminationGracePeriodSeconds: 10
        containers:
        - name: nginx
          image: gcr.io/google_containers/nginx-slim:0.8
          ports:
          - containerPort: 80
            name: web
          volumeMounts:
          - name: www
            mountPath: /usr/share/nginx/html
    volumeClaimTemplates:
    - metadata:
        name: www
      spec:
        accessModes: [ "ReadWriteOnce" ]
        resources:
          requests:
            storage: 1Gi
  ---
  apiVersion: v1
  kind: Service
  metadata:
    name: nginx
    labels:
      app: nginx
  spec:
    ports:
    - port: 80
      name: web
    clusterIP: None
    selector:
      app: nginx
  ```
  In the above example: 
  - The **StatefulSet**, named `web`, has a Spec that indicates that `3` replicas of the nginx container will be launched in unique Pods.
  - The **volumeClaimTemplates** will provide stable storage using PersistentVolumes provisioned by a PersistentVolume Provisioner.
  - A **Headless Service**, named nginx, is used to control the network domain.

  You will need to use two terminal windows. In the first terminal, use `kubectl get` to watch the creation of the StatefulSet’s Pods.

  ```shell
  kubectl get pods -w -l app=nginx
  ```  
  In the second terminal, use `kubectl apply` to create the Headless Service and StatefulSet defined in web.yaml.

  ```shell
  kubectl apply -f unit3-04-satefulset-nginx.yaml
  statefulset.apps/web configured
  service/nginx created
  ```
  The command above creates three Pods, each running an NGINX webserver. Get the nginx Service and the web StatefulSet to verify that they were created successfully.

  ```shell
  kubectl get service nginx
  NAME      TYPE         CLUSTER-IP   EXTERNAL-IP   PORT(S)   AGE
  nginx     ClusterIP    None         <none>        80/TCP    12s
  ```
  ```
  kubectl get statefulset web
  NAME   READY   AGE
  web    3/3     3m4s
  ```   
  **Ordered Pod Creation**

  For a StatefulSet with `N` replicas, when Pods are being deployed, they are created sequentially, in order from `{0..N-1}`. Examine the output of the kubectl get command in the first terminal. Eventually, the output will look like the example below.
  ```shell
  kubectl get pods -w -l app=nginx
  NAME    READY   STATUS    RESTARTS   AGE
  web-0   0/1     Pending   0          0s
  web-0   0/1     Pending   0          1s
  web-0   0/1     ContainerCreating   0          1s
  web-0   1/1     Running             0          2s
  web-1   0/1     Pending             0          0s
  web-1   0/1     Pending             0          0s
  web-1   0/1     ContainerCreating   0          0s
  web-1   1/1     Running             0          1s
  web-2   0/1     Pending             0          0s
  web-2   0/1     Pending             0          0s
  web-2   0/1     ContainerCreating   0          0s
  web-2   1/1     Running             0          1s
  ```
  Notice that the web-1 Pod is not launched until the web-0 Pod is Running and Ready.

### Pods in a StatefulSet

Pods in a StatefulSet have a unique ordinal index and a stable network identity.

 - Examining the Pod's Ordinal Index
  Get the StatefulSet's Pods.

 ```shell
  kubectl get pods -l app=nginx
  NAME      READY     STATUS    RESTARTS   AGE
  web-0     1/1       Running   0          1m
  web-1     1/1       Running   0          1m
  web-2     1/1       Running   0          1m
 ```
  As mentioned above, the Pods in a StatefulSet have a sticky, unique identity. This identity is based on a unique ordinal index that is assigned to each Pod by the StatefulSet controller. The Pods' names take the form `<statefulset name>-<ordinal index>`. Since the web StatefulSet has two replicas, it creates three Pods, web-0, web-1,  and web-2.

- Using Stable Network Identities
  Each Pod has a stable hostname based on its ordinal index. Use kubectl exec to execute the hostname command in each Pod.
  ```shell
  for i in 0 1 2; do kubectl exec web-$i -- sh -c 'hostname'; done
  web-0
  web-1
  web-2
  ```  
  Use `kubectl run` to execute a container that provides the `nslookup` command from the `dnsutils` package. Using `nslookup` on the Pods' hostnames, you can examine their in-cluster DNS addresses.

  ```shell
  kubectl run -i --tty --image quay.io/mromdhani/busybox:1.28 dns-test --restart=Never --rm  nslookup web-0.nginx
  Server:    10.96.0.10
  Address 1: 10.96.0.10 kube-dns.kube-system.svc.cluster.local

  Name:      web-0.nginx
  Address 1: 10.1.0.8 web-0.nginx.default.svc.cluster.local
  ```

  The CNAME of the headless service points to SRV records (one for each Pod that is Running and Ready). The SRV records point to A record entries that contain the Pods’ IP addresses.
  ```shell
  kubectl run -i --tty --image quay.io/mromdhani/busybox:1.28 dns-test --restart=Never --rm  nslookup nginx
  Server:    10.96.0.10
  Address 1: 10.96.0.10 kube-dns.kube-system.svc.cluster.local

  Name:      nginx
  Address 1: 10.1.0.9 web-1.nginx.default.svc.cluster.local
  Address 2: 10.1.0.8 web-0.nginx.default.svc.cluster.local
  Address 3: 10.1.0.10 web-2.nginx.default.svc.cluster.local
  ```
- Let's recreate the three Pods and compare the new one wuth the previous ones
  
  - In one terminal, watch the StatefulSet's Pods.
    ```shell
    kubectl get pod -w -l app=nginx
    ```
  - In a second terminal, use `kubectl delete` to delete all the Pods in the StatefulSet.
    ```shell
    kubectl delete pod -l app=nginx
    pod "web-0" deleted
    pod "web-1" deleted
    pod "web-2" deleted
    ```
    Wait for the StatefulSet to restart them, and for both Pods to transition to Running and Ready.
    ```shell
    kubectl get pod -w -l app=nginx
    ```
  - Use kubectl exec and kubectl run to view the Pods hostnames and in-cluster DNS entries.
    ```shell
    for i in 0 1 2; do kubectl exec web-$i -- sh -c 'hostname'; done
    web-0
    web-1
    web-2
  ```
  ```shell
  kubectl run -i --tty --image quay.io/mromdhani/busybox:1.28 dns-test --restart=Never --rm sh 
  / # nslookup web-0.nginx
  Server:    10.96.0.10
  Address 1: 10.96.0.10 kube-dns.kube-system.svc.cluster.local
  Name:      web-0.nginx
  Address 1: 10.1.0.8 web-0.nginx.default.svc.cluster.local
  ```
  The Pods' ordinals, hostnames, SRV records, and A record names have not changed, but the IP addresses associated with the Pods may have changed. In the cluster used for this tutorial, they have. This is why it is important not to configure other applications to connect to Pods in a StatefulSet by IP address.
 
   If you need to find and connect to the active members of a StatefulSet, you should query the CNAME of the Headless Service (`nginx.default.svc.cluster.local`). The SRV records associated with the CNAME will contain only the Pods in the StatefulSet that are Running and Ready.

   If your application already implements connection logic that tests for liveness and readiness, you can use the SRV records of the Pods ( `web-0.nginx.default.svc.cluster.local`, `web-1.nginx.default.svc.cluster.local`, `web-2.nginx.default.svc.cluster.local`), as they are stable, and your application will be able to discover the Pods' addresses when they transition to Running and Ready.

- Clean Up. Remove The Statefulset and the service.
  ```
  kubectl delete statefulset/web svc/nginx  
  kubectl delete pvc -l app=nginx    # This will clean up the attached persistent volumes (having DELETE reclaim policy)  
  ```