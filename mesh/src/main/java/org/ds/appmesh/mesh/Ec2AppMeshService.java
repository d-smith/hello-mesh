package org.ds.appmesh.mesh;

import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.regioninfo.RegionInfo;
import software.amazon.awscdk.services.appmesh.*;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.Protocol;
import software.amazon.awscdk.services.ecr.IRepository;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.HealthCheck;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.servicediscovery.DnsRecordType;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

public class Ec2AppMeshService extends Construct {
    String serviceName;
    Integer portNumber;
    TaskDefinition taskDefinition;
    ContainerDefinition applicationContainer;
    FargateService fargateService;
    VirtualNode virtualNode;
    VirtualService virtualService;

    public Ec2AppMeshService(@NotNull Construct scope, @NotNull String id,
                             Cluster cluster, Mesh mesh,
                             Role taskRole, Role executionRole,
                             ContainerDefinitionOptions appContainerOpts, Integer port) {
        super(scope, id);

        IRepository envoyRepo = Repository.fromRepositoryArn(this, "envoyRepo", "arn:aws:ecr:us-west-2:840364872350:repository/aws-appmesh-envoy");
        this.serviceName = id;
        this.portNumber = port;

        this.taskDefinition = TaskDefinition.Builder.create(scope, serviceName + "-task-definition")
                .compatibility(Compatibility.EC2_AND_FARGATE)
                .networkMode(NetworkMode.AWS_VPC)
                .proxyConfiguration(
                        AppMeshProxyConfiguration.Builder.create()
                                .containerName("envoy")
                                .properties(
                                        AppMeshProxyConfigurationProps.builder()
                                                .appPorts(List.of(portNumber))
                                                .proxyEgressPort(15001)
                                                .proxyIngressPort(15000)
                                                .ignoredUid(1337)
                                                .egressIgnoredIPs(List.of("169.254.170.2", "169.254.169.254"))
                                                .build()
                                )
                                .build()
                )
                .taskRole(taskRole)
                .executionRole(executionRole)
                .memoryMiB("1024")
                .cpu("512")
                .build();


        this.applicationContainer = this.taskDefinition.addContainer("app", appContainerOpts);
        this.applicationContainer.addPortMappings(
                PortMapping.builder()
                        .containerPort(port)
                        .hostPort(port)
                        .build()
        );

        ContainerDefinition envoy = this.taskDefinition.addContainer("envoy",
                ContainerDefinitionOptions.builder()
                        .containerName("envoy")
                        .image(ContainerImage.fromEcrRepository(envoyRepo, "v1.20.0.1-prod"))
                        .essential(true)
                        .environment(Map.of(
                                "APPMESH_VIRTUAL_NODE_NAME", "mesh/" + mesh.getMeshName() + "/virtualNode/" + serviceName,
                                "AWS_REGION", Stack.of(this).getRegion()
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
                        .build());

        this.applicationContainer.addContainerDependencies(
                ContainerDependency.builder()
                        .container(envoy)
                        .condition(ContainerDependencyCondition.HEALTHY)
                        .build()
        );

        this.fargateService = FargateService.Builder.create(this, serviceName + "-service")
                .cluster(cluster)
                .desiredCount(2)
                .taskDefinition(this.taskDefinition)
                .cloudMapOptions(
                        CloudMapOptions.builder()
                                .dnsRecordType(DnsRecordType.A)
                                .dnsTtl(Duration.seconds(10))
                                .failureThreshold(2)
                                .name(this.serviceName)
                                .build()
                )
                .build();

        this.virtualNode = VirtualNode.Builder.create(this, serviceName + "-virtual-node")
                .mesh(mesh)
                .virtualNodeName(serviceName)
                .serviceDiscovery(ServiceDiscovery.cloudMap(fargateService.getCloudMapService()))
                .listeners(List.of(
                        VirtualNodeListener.http(HttpVirtualNodeListenerOptions.builder()
                                .port(port)
                                .healthCheck(software.amazon.awscdk.services.appmesh.HealthCheck.http(
                                        HttpHealthCheckOptions.builder()
                                                .healthyThreshold(2)
                                                .interval(Duration.seconds(5))
                                                .path("/")
                                                .timeout(Duration.seconds(2))
                                                .unhealthyThreshold(2)
                                                .build()
                                ))
                                .build())
                ))
                .build();

        this.virtualService = VirtualService.Builder.create(this, serviceName + "-virtual-service")
                .virtualServiceName(serviceName + "." + cluster.getDefaultCloudMapNamespace().getNamespaceName())
                .virtualServiceProvider(VirtualServiceProvider.virtualNode(virtualNode))
                .build();

    }

    public void connectToMeshService(Ec2AppMeshService appMeshService) {

        this.fargateService.getConnections().allowTo(
                appMeshService.fargateService,
                Port.tcp(8080),
                "Inbound traffic from the app mesh enabled " + serviceName
        );

        this.virtualNode.addBackend(Backend.virtualService(appMeshService.virtualService));

    }
}
