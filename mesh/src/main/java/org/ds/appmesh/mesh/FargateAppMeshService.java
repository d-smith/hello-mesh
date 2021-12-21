package org.ds.appmesh.mesh;

import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.services.appmesh.*;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.iam.Role;
import software.constructs.Construct;


public class FargateAppMeshService extends FargateVirtualNode {

    VirtualService virtualService;

    public FargateAppMeshService(@NotNull Construct scope, @NotNull String id,
                                 Cluster cluster, Mesh mesh,
                                 Role taskRole, Role executionRole,
                                 ContainerDefinitionOptions appContainerOpts, Integer port) {
        super(scope, id, cluster, mesh, taskRole, executionRole, appContainerOpts, port);



        this.virtualService = VirtualService.Builder.create(this, serviceName + "-virtual-service")
                .virtualServiceName(serviceName + "." + cluster.getDefaultCloudMapNamespace().getNamespaceName())
                .virtualServiceProvider(VirtualServiceProvider.virtualNode(virtualNode))
                .build();

    }

    public void connectToMeshService(FargateAppMeshService appMeshService) {

        this.fargateService.getConnections().allowTo(
                appMeshService.fargateService,
                Port.tcp(8080),
                "Inbound traffic from the app mesh enabled " + serviceName
        );

        this.virtualNode.addBackend(Backend.virtualService(appMeshService.virtualService));

    }



    public void connectVirtualRouterService(VirtualService vrService) {
        this.virtualNode.addBackend(Backend.virtualService(vrService));
    }
}
