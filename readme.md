# hello-mesh

Shameless rip-off of Nathan Peck's [Greeter App Mesh CDK](project), written in Java.

## Build and Deploy

To deploy using the containers I built and pushed to docker hub, in the mesh directory simply

```
cdk deploy
```

This assumes you have the CDK installed and have bootstrapped your account.

***Note - solution hardcodes the envoy ecr repository for us-west-2***

If you are reluctant to deploy a stranger's containers into your account, first of all good for you, and second of all you can build the containers yourself. The code for the containers is provided in the hello, greeter, and name subdirectories. They use the jib plugin to create the containers.

To build and push to dockerhub, docker login using your credentials, update the image spec in the pom jib config, then

```
mvn compile jib:build
```


## Run containers locally

Note - you can build local containers only via `mvn compile jib:dockerBuild`

```
docker network create hello-net
docker run --name name --network hello-net -p 8080:8080 name
docker run --name greeting --network hello-net -p 8081:8080 greeting
docker run --network hello-net  --env NAME_ENDPOINT=http://name:8080 --env GREETING_ENDPOINT=http://greeting:8080 -p 3000:8080 hello
```

## Next steps

* Determine if the task and execution roles were needed
* See about using less memory for the app containers. Note 128 was not enough
for the springboot app containers (well, two of them...)
* Add another implementation of name, do some envoy routing, etc.
