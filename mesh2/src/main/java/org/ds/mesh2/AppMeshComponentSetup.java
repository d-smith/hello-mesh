package org.ds.mesh2;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.appmesh.*;
import software.constructs.Construct;

import java.util.List;

public class AppMeshComponentSetup {

    public static HealthCheck createVirtualNodeHealthCheck() {
        return HealthCheck.http(
                HttpHealthCheckOptions.builder()
                        .healthyThreshold(2)
                        .interval(Duration.seconds(5))
                        .path("/health")
                        .timeout(Duration.seconds(2))
                        .unhealthyThreshold(2)
                        .build()
        );
    }

    public static VirtualNode createVirtualNode(MeshContext meshContext, String serviceName) {

        return VirtualNode.Builder.create(meshContext.getScope(), serviceName + "-vn")
                .mesh(meshContext.getMesh())
                .virtualNodeName(serviceName)
                .serviceDiscovery(ServiceDiscovery.dns(serviceName + "." + meshContext.getServiceDomain()))
                .listeners(List.of(
                        VirtualNodeListener.http(HttpVirtualNodeListenerOptions.builder()
                                .port(8080)
                                .healthCheck(createVirtualNodeHealthCheck())
                                .build())
                ))
                .build();
    }

    public static VirtualService createServiceForNode(MeshContext meshContext, VirtualNode node, String serviceName) {
        return VirtualService.Builder.create(meshContext.getScope(), serviceName + "-vs")
                .virtualServiceName(serviceName + "." + meshContext.getServiceDomain())
                .virtualServiceProvider(VirtualServiceProvider.virtualNode(node))
                .build();
    }
}
