package org.ds.mesh2;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.services.appmesh.Mesh;
import software.amazon.awscdk.services.ecr.IRepository;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.servicediscovery.DnsRecordType;

import java.awt.*;
import java.util.List;
import java.util.Map;

public class EcsServices {

    public static HealthCheck createContainerHealthCheck() {
        return  HealthCheck.builder()
                .command(List.of("curl localhost:8080/health"))
                .startPeriod(Duration.seconds(10))
                .interval(Duration.seconds(5))
                .timeout(Duration.seconds(2))
                .retries(3)
                .build();
    }

    public static ContainerDefinitionOptions createEnvoyContainerDef(MeshContext meshContext, String serviceName, String nodename) {

        IRepository envoyRepo = Repository.fromRepositoryArn(meshContext.getScope(), "envoyRepo", "arn:aws:ecr:us-west-2:840364872350:repository/aws-appmesh-envoy");

        return ContainerDefinitionOptions.builder()
                .containerName("envoy")
                .image(ContainerImage.fromEcrRepository(envoyRepo, "v1.20.0.1-prod"))
                .essential(true)
                .environment(Map.of(
                        "APPMESH_VIRTUAL_NODE_NAME", "mesh/" + meshContext.getMesh().getMeshName() + "/virtualNode/" + nodename,
                        "AWS_REGION", Stack.of(meshContext.getScope()).getRegion()
                ))
                .healthCheck(
                        HealthCheck.builder()
                                .command(List.of(
                                        "CMD-SHELL",
                                        "curl -s http://localhost:9901/server_info | grep state | grep -q LIVE"
                                ))
                                .startPeriod(Duration.seconds(10))
                                .interval(Duration.seconds(5))
                                .timeout(Duration.seconds(2))
                                .retries(3)
                                .build()
                )
                .memoryLimitMiB(512)
                .user("1337")
                .logging(AwsLogDriver.Builder.create()
                        .streamPrefix(serviceName + "-envoy").build()
                )
                .build();
    }

    public static TaskDefinition createTaskDefinition(MeshContext meshContext, String serviceName) {
        return TaskDefinition.Builder.create(meshContext.getScope(), serviceName + "-task-definition")
                .compatibility(Compatibility.EC2_AND_FARGATE)
                .networkMode(NetworkMode.AWS_VPC)
                .proxyConfiguration(
                        AppMeshProxyConfiguration.Builder.create()
                                .containerName("envoy")
                                .properties(
                                        AppMeshProxyConfigurationProps.builder()
                                                .appPorts(List.of(8080))
                                                .proxyEgressPort(15001)
                                                .proxyIngressPort(15000)
                                                .ignoredUid(1337)
                                                .egressIgnoredIPs(List.of("169.254.170.2", "169.254.169.254"))
                                                .build()
                                )
                                .build()
                )
                .taskRole(meshContext.getTaskRole())
                .executionRole(meshContext.getExecutionRole())
                .memoryMiB("1024")
                .cpu("512")
                .build();
    }

    public static FargateService createNameService(MeshContext meshContext) {
        String serviceName = "namesvc";
        String nodeName = "namenode";
        TaskDefinition taskDefinition = createTaskDefinition(meshContext, serviceName);

        ContainerDefinitionOptions nameContainerDefinitionOpts = ContainerDefinitionOptions.builder()
                .image(ContainerImage.fromRegistry("dasmith/name"))
                .healthCheck(createContainerHealthCheck())
                .memoryLimitMiB(512)
                .logging(AwsLogDriver.Builder.create().streamPrefix("app-mesh-name").build())
                .build();

        ContainerDefinition nameContainer = taskDefinition.addContainer("name", nameContainerDefinitionOpts);
        nameContainer.addPortMappings(PortMapping.builder()
                .containerPort(8080)
                .hostPort(8080)
                .build());

        ContainerDefinition envoyContainer = taskDefinition.addContainer("envoy",
                createEnvoyContainerDef(meshContext, serviceName, nodeName));
        nameContainer.addContainerDependencies(ContainerDependency.builder()
                .container(envoyContainer)
                .condition(ContainerDependencyCondition.HEALTHY)
                .build());

        return FargateService.Builder.create(meshContext.getScope(), serviceName)
                .cluster(meshContext.getCluster())
                .desiredCount(1)
                .taskDefinition(taskDefinition)
                .cloudMapOptions(
                        CloudMapOptions.builder()
                                .dnsRecordType(DnsRecordType.A)
                                .dnsTtl(Duration.seconds(10))
                                .failureThreshold(2)
                                .name(serviceName + "." + meshContext.getServiceDomain())
                                .build()
                )
                .build();

    }

}
