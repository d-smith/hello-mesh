package org.ds.appmesh.mesh;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.appmesh.Mesh;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.elasticloadbalancingv2.AddApplicationTargetsProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationListener;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationLoadBalancer;
import software.amazon.awscdk.services.elasticloadbalancingv2.BaseApplicationListenerProps;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.servicediscovery.NamespaceType;
import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;

import java.util.List;
import java.util.Map;
// import software.amazon.awscdk.Duration;
// import software.amazon.awscdk.services.sqs.Queue;

public class MeshStack extends Stack {
    public MeshStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public MeshStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        Vpc vpc = Vpc.Builder.create(this, "hello-vpc")
                .maxAzs(2)
                .build();

        Cluster cluster = Cluster.Builder.create(this, "hello-cluster")
                .vpc(vpc)
                .defaultCloudMapNamespace(
                        CloudMapNamespaceOptions.builder()
                                .name("internal")
                                .type(NamespaceType.DNS_PRIVATE)
                                .build()
                )
                .build();

        Mesh mesh = Mesh.Builder.create(this, "hello-mesh").build();

        Role taskRole = IamComponents.createTaskIamRole(this);
        Role executionRole = IamComponents.createTaskExecutionIamRole(this);

        HealthCheck healthCheck = HealthCheck.builder()
                .command(List.of("curl localhost:8080/actuator/health"))
                .startPeriod(Duration.seconds(10))
                .interval(Duration.seconds(5))
                .timeout(Duration.seconds(2))
                .retries(3)
                .build();

        ContainerDefinitionOptions nameContainerDefinitionOpts = ContainerDefinitionOptions.builder()
                .image(ContainerImage.fromRegistry("dasmith/name"))
                .healthCheck(healthCheck)
                .memoryLimitMiB(512)
                .logging(AwsLogDriver.Builder.create().streamPrefix("app-mesh-name").build())
                .build();


        FargateAppMeshService nameService = new FargateAppMeshService(this, "name",
                cluster, mesh, taskRole, executionRole, nameContainerDefinitionOpts, 8080);

        ContainerDefinitionOptions greetingContainerDefinitionOpts = ContainerDefinitionOptions.builder()
                .image(ContainerImage.fromRegistry("dasmith/greeting"))
                .healthCheck(healthCheck)
                .memoryLimitMiB(512)
                .logging(AwsLogDriver.Builder.create().streamPrefix("app-mesh-greeting").build())
                .build();

        FargateAppMeshService greetingService = new FargateAppMeshService(this, "greeting",
                cluster, mesh, taskRole, executionRole, greetingContainerDefinitionOpts, 8080);

        ContainerDefinitionOptions helloContainerDefinitionsOpt = ContainerDefinitionOptions.builder()
                .image(ContainerImage.fromRegistry("dasmith/hello"))
                .healthCheck(healthCheck)
                .memoryLimitMiB(512)
                .logging(AwsLogDriver.Builder.create().streamPrefix("app-mesh-greeting").build())
                .environment(Map.of(
                        "NAME_ENDPOINT", "http://name.internal:8080",
                        "GREETING_ENDPOINT", "http://greeting.internal:8080"
                ))
                .build();
        FargateAppMeshService helloService = new FargateAppMeshService(this,"hello",
                cluster, mesh, taskRole, executionRole, helloContainerDefinitionsOpt, 8080);

        helloService.connectToMeshService(nameService);
        helloService.connectToMeshService(greetingService);

        ApplicationLoadBalancer alb = ApplicationLoadBalancer.Builder.create(this, "alb")
                .vpc(vpc)
                .internetFacing(true)
                .build();

        ApplicationListener applicationListener = alb.addListener("public-listener", BaseApplicationListenerProps.builder()
                .port(80)
                .open(true)
                .build());

        applicationListener.addTargets("hello", AddApplicationTargetsProps.builder()
                .port(80)
                .targets(List.of(
                        helloService.fargateService
                ))
                .build());



    }
}
