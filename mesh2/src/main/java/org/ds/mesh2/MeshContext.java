package org.ds.mesh2;

import software.amazon.awscdk.services.appmesh.Mesh;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.iam.Role;
import software.constructs.Construct;

public class MeshContext {
    private Construct scope;
    private Mesh mesh;
    private String serviceDomain;
    private Role taskRole;
    private Role executionRole;
    private Cluster cluster;

    public MeshContext(Construct scope, Mesh mesh, String serviceDomain, Role taskRole, Role executionRole, Cluster cluster) {
        this.scope = scope;
        this.mesh = mesh;
        this.serviceDomain = serviceDomain;
        this.taskRole = taskRole;
        this.executionRole = executionRole;
        this.cluster = cluster;
    }

    public Construct getScope() {
        return scope;
    }

    public Mesh getMesh() {
        return mesh;
    }

    public String getServiceDomain() {
        return serviceDomain;
    }

    public Role getTaskRole() {
        return taskRole;
    }

    public Role getExecutionRole() {
        return executionRole;
    }

    public Cluster getCluster() {
        return cluster;
    }
}
