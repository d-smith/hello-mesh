# hello-mesh

Shameless rip-off of Nathan Peck's [Greeter App Mesh CDK](project), writting in Java.


## Run containers locally

```
docker network create hello-net
docker run --name name --network hello-net -p 8080:8080 name
docker run --name greeting --network hello-net -p 8081:8080 greeting
docker run --network hello-net  --env NAME_ENDPOINT=http://name:8080 --env GREETING_ENDPOINT=http://greeting:8080 -p 3000:8080 hello
```

