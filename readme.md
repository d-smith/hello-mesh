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



```
{
  "meshName": "AwsAppmeshWeightedRoutingStackMeshDC98B208",
  "virtualServiceName": "servicea.appmesh.local",
  "spec": {
    "provider": {
      "virtualRouter": {
        "virtualRouterName": "AwsAppmeshWeightedRoutingStackAppMeshServiceAVirtualRouterserviceAD60612D4"
      }
    }
  },
  "meshOwner": "230586709464"
}

{
  "meshName": "AwsAppmeshWeightedRoutingStackMeshDC98B208",
  "virtualServiceName": "serviceb.appmesh.local",
  "spec": {
    "provider": {
      "virtualRouter": {
        "virtualRouterName": "AwsAppmeshWeightedRoutingStackAppMeshServiceBVirtualRouterserviceB76606811"
      }
    }
  },
  "meshOwner": "230586709464"
}

{
  "meshName": "AwsAppmeshWeightedRoutingStackMeshDC98B208",
  "virtualRouterName": "AwsAppmeshWeightedRoutingStackAppMeshServiceAVirtualRouterserviceAD60612D4",
  "spec": {
    "listeners": [
      {
        "portMapping": {
          "port": 3000,
          "protocol": "http"
        }
      }
    ]
  },
  "meshOwner": "230586709464"
}

{
  "meshName": "AwsAppmeshWeightedRoutingStackMeshDC98B208",
  "virtualRouterName": "AwsAppmeshWeightedRoutingStackAppMeshServiceBVirtualRouterserviceB76606811",
  "spec": {
    "listeners": [
      {
        "portMapping": {
          "port": 3000,
          "protocol": "http"
        }
      }
    ]
  },
  "meshOwner": "230586709464"
}

{
  "meshName": "AwsAppmeshWeightedRoutingStackMeshDC98B208",
  "virtualNodeName": "serviceA",
  "spec": {
    "backends": [
      {
        "virtualService": {
          "virtualServiceName": "serviceb.appmesh.local"
        }
      }
    ],
    "listeners": [
      {
        "healthCheck": {
          "healthyThreshold": 3,
          "intervalMillis": 5000,
          "path": "/health",
          "port": 3000,
          "protocol": "http",
          "timeoutMillis": 2000,
          "unhealthyThreshold": 2
        },
        "portMapping": {
          "port": 3000,
          "protocol": "http"
        }
      }
    ],
    "logging": {
      "accessLog": {
        "file": {
          "path": "/dev/stdout"
        }
      }
    },
    "serviceDiscovery": {
      "awsCloudMap": {
        "namespaceName": "cloudmap.local",
        "serviceName": "serviceA"
      }
    }
  },
  "meshOwner": "230586709464"
}

{
  "meshName": "AwsAppmeshWeightedRoutingStackMeshDC98B208",
  "virtualNodeName": "serviceB_v1",
  "spec": {
    "backends": [],
    "listeners": [
      {
        "healthCheck": {
          "healthyThreshold": 3,
          "intervalMillis": 5000,
          "path": "/health",
          "port": 3000,
          "protocol": "http",
          "timeoutMillis": 2000,
          "unhealthyThreshold": 2
        },
        "portMapping": {
          "port": 3000,
          "protocol": "http"
        }
      }
    ],
    "logging": {
      "accessLog": {
        "file": {
          "path": "/dev/stdout"
        }
      }
    },
    "serviceDiscovery": {
      "awsCloudMap": {
        "namespaceName": "cloudmap.local",
        "serviceName": "serviceB_v1"
      }
    }
  },
  "meshOwner": "230586709464"
}

{
  "meshName": "AwsAppmeshWeightedRoutingStackMeshDC98B208",
  "routeName": "VirtualRoute",
  "virtualRouterName": "AwsAppmeshWeightedRoutingStackAppMeshServiceBVirtualRouterserviceB76606811",
  "spec": {
    "httpRoute": {
      "action": {
        "weightedTargets": [
          {
            "virtualNode": "serviceB_v1",
            "weight": 1
          },
          {
            "virtualNode": "serviceB_v2",
            "weight": 1
          }
        ]
      },
      "match": {
        "prefix": "/"
      }
    }
  },
  "meshOwner": "230586709464"
}

{
  "meshName": "AwsAppmeshWeightedRoutingStackMeshDC98B208",
  "routeName": "VirtualRoute",
  "virtualRouterName": "AwsAppmeshWeightedRoutingStackAppMeshServiceAVirtualRouterserviceAD60612D4",
  "spec": {
    "httpRoute": {
      "action": {
        "weightedTargets": [
          {
            "virtualNode": "serviceA",
            "weight": 1
          }
        ]
      },
      "match": {
        "prefix": "/"
      }
    }
  },
  "meshOwner": "230586709464"
}
