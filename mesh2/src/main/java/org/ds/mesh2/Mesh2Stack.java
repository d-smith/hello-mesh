package org.ds.mesh2;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.appmesh.*;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecr.IRepository;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecs.CloudMapNamespaceOptions;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.FargateService;
import software.amazon.awscdk.services.elasticloadbalancingv2.AddApplicationTargetsProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationListener;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationLoadBalancer;
import software.amazon.awscdk.services.elasticloadbalancingv2.BaseApplicationListenerProps;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.route53.*;
import software.amazon.awscdk.services.servicediscovery.*;
import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;

import java.util.List;
// import software.amazon.awscdk.Duration;
// import software.amazon.awscdk.services.sqs.Queue;

public class Mesh2Stack extends Stack {
    public Mesh2Stack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public Mesh2Stack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        //
        // Create the infrastructure & IAM
        //
        final String serviceDomain = "app.local";

        Vpc vpc = Vpc.Builder.create(this, "hello-vpc")
                .maxAzs(3)
                .build();


        //PrivateHostedZone appzone = PrivateHostedZone.Builder.create(this, "appzone")
        //       .vpc(vpc)
        //        .zoneName(serviceDomain)
        //        .build();




        Cluster cluster = Cluster.Builder.create(this, "hello-cluster")
                .vpc(vpc)
                .defaultCloudMapNamespace(
                        CloudMapNamespaceOptions.builder()
                                .name(serviceDomain)
                                .type(NamespaceType.DNS_PRIVATE)
                                .build()
                )
                .build();



        /*IHostedZone appzone = PrivateHostedZone.fromLookup(this, "appzone",
                HostedZoneProviderProps.builder()
                        .domainName(serviceDomain)
                        .privateZone(true)
                        .build());

        ARecord.Builder.create(this, "fakey")
                .zone(appzone)
                .recordName("nameteller")
                .target(RecordTarget.fromIpAddresses("10.10.10.10"))
                .build();*/

        Role taskRole = IamComponents.createTaskIamRole(this);
        Role executionRole = IamComponents.createTaskExecutionIamRole(this);

        Mesh mesh = Mesh.Builder.create(this, "hello-mesh").build();

        IRepository envoyRepo = Repository.fromRepositoryArn(this, "envoyRepo", "arn:aws:ecr:us-west-2:840364872350:repository/aws-appmesh-envoy");

        MeshContext meshContext = new MeshContext(this,mesh, serviceDomain, taskRole, executionRole, cluster, envoyRepo);

        //
        // Create the service mesh
        //

        // name virtual node
        VirtualNode nameVirtualNode = AppMeshComponentSetup.createVirtualNode(meshContext, "namenode");

        // name service
        VirtualService nameVirtualService = AppMeshComponentSetup.createServiceForNode(meshContext, nameVirtualNode, "namesvc");

        // name 2 virtual node
        VirtualNode name2VirtualNode = AppMeshComponentSetup.createVirtualNode(meshContext, "name2node");

        // name 2 virtual service
        VirtualService name2VirtualService = AppMeshComponentSetup.createServiceForNode(meshContext, name2VirtualNode, "name2svc");

        //greeting virtual node

        // greeting service

        // hello virtual node
        VirtualNode helloNode = AppMeshComponentSetup.createVirtualNode(meshContext, "hellonode");


        // hello service
        VirtualService helloService = AppMeshComponentSetup.createServiceForNode(meshContext, helloNode, "hellosvc");


        //
        // Add fargate services to tie virtual constructs to concrete implementations
        //
        FargateService namesvc = EcsServices.createNameService(meshContext);
        FargateService name2svc = EcsServices.createName2Service(meshContext);
        FargateService hellosvc = EcsServices.createHelloService(meshContext);

       // Fake it till you make it
        VirtualNode nametellerVirtualNode = AppMeshComponentSetup.createVirtualNode(meshContext, "nameteller");
        FargateService fakey = EcsServices.createFakeNameteller(meshContext);



        VirtualRouter nametellerRouter = VirtualRouter.Builder.create(this, "nameteller-vr")
                .mesh(mesh)
                .virtualRouterName("nameteller-vr")
                .listeners(
                        List.of(
                                VirtualRouterListener.http(8080)
                        )
                )
                .build();

        nametellerRouter.addRoute("nameteller-vr-route", RouteBaseProps.builder()
                .routeName("nameteller-route")
                .routeSpec(RouteSpec.http(
                        HttpRouteSpecOptions.builder()
                                .weightedTargets(List.of(
                                        WeightedTarget.builder()
                                                .virtualNode(nameVirtualNode)
                                                .weight(1)
                                                .build(),
                                        WeightedTarget.builder()
                                                .virtualNode(name2VirtualNode)
                                                .weight(1)
                                                .build()
                                ))
                                .build()
                ))
                .build());

        VirtualService nametellterVS = VirtualService.Builder.create(meshContext.getScope(), "nameteller-vs")
                .virtualServiceName("nameteller" + "." + meshContext.getServiceDomain())
                .virtualServiceProvider(VirtualServiceProvider.virtualRouter(nametellerRouter))
                .build();




        helloNode.addBackend(Backend.virtualService(nametellterVS));

        //
        // Hook up alb to front door
        //
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
                        hellosvc
                ))
                .healthCheck(software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck.builder()
                        .path("/health")
                        .build())
                .build());


    }
}
