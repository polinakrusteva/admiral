/*
 * Copyright (c) 2016 VMware, Inc. All Rights Reserved.
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with separate copyright notices
 * and license terms. Your use of these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package com.vmware.admiral.request;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.vmware.admiral.common.DeploymentProfileConfig;
import com.vmware.admiral.compute.ResourceType;
import com.vmware.admiral.compute.container.ContainerFactoryService;
import com.vmware.admiral.compute.container.ContainerService.ContainerState;
import com.vmware.admiral.compute.container.HostPortProfileService;
import com.vmware.admiral.compute.container.network.ContainerNetworkDescriptionService;
import com.vmware.admiral.compute.container.network.ContainerNetworkDescriptionService.ContainerNetworkDescription;
import com.vmware.admiral.compute.container.network.ContainerNetworkService;
import com.vmware.admiral.compute.container.network.ContainerNetworkService.ContainerNetworkState;
import com.vmware.admiral.compute.container.volume.ContainerVolumeDescriptionService;
import com.vmware.admiral.compute.container.volume.ContainerVolumeDescriptionService.ContainerVolumeDescription;
import com.vmware.admiral.compute.container.volume.ContainerVolumeService;
import com.vmware.admiral.compute.container.volume.ContainerVolumeService.ContainerVolumeState;
import com.vmware.admiral.request.ContainerHostRemovalTaskService.ContainerHostRemovalTaskState;
import com.vmware.admiral.request.RequestBrokerService.RequestBrokerState;
import com.vmware.admiral.request.util.TestRequestStateFactory;
import com.vmware.admiral.service.test.MockDockerAdapterService;
import com.vmware.photon.controller.model.resources.ComputeService.ComputeState;

public class ContainerHostRemovalTaskServiceTest extends RequestBaseTest {
    private RequestBrokerState request;

    @Before
    @Override
    public void setUp() throws Throwable {
        DeploymentProfileConfig.getInstance().setTest(true);
        super.setUp();

        request = TestRequestStateFactory.createRequestState();
        request.resourceDescriptionLink = containerDesc.documentSelfLink;
        request.tenantLinks = groupPlacementState.tenantLinks;
        request.resourceCount = 2;
    }

    @Test
    public void testContainerHostRemovalResourceOperationCycle() throws Throwable {
        request = startRequest(request);
        waitForRequestToComplete(request);

        request = getDocument(RequestBrokerState.class, request.documentSelfLink);
        assertNotNull(request);

        // verify the resources are created as expected:
        assertEquals(request.resourceCount, request.resourceLinks.size());
        List<String> containerStateLinks = findResourceLinks(ContainerState.class,
                request.resourceLinks);

        // create a host removal task
        ContainerHostRemovalTaskState state = new ContainerHostRemovalTaskState();
        state.resourceLinks = new HashSet<>(Collections.singletonList(
                computeHost.documentSelfLink));
        state = doPost(state, ContainerHostRemovalTaskFactoryService.SELF_LINK);

        assertNotNull("task is null", state);
        waitForTaskSuccess(state.documentSelfLink, ContainerHostRemovalTaskState.class);

        validateHostRemoved(containerStateLinks);
    }

    @Test
    public void testRequestBrokerContainerHostRemovalResourceOperationCycle() throws Throwable {
        request = startRequest(request);
        waitForRequestToComplete(request);

        request = getDocument(RequestBrokerState.class, request.documentSelfLink);
        assertNotNull(request);

        // verify the resources are created as expected:
        assertEquals(request.resourceCount, request.resourceLinks.size());
        List<String> containerStateLinks = findResourceLinks(ContainerState.class,
                request.resourceLinks);

        // create a host removal task - RequestBroker
        RequestBrokerState request = new RequestBrokerState();
        request.resourceType = ResourceType.CONTAINER_HOST_TYPE.getName();
        request.resourceLinks = new HashSet<>(Collections.singletonList(
                computeHost.documentSelfLink));
        request.operation = RequestBrokerState.REMOVE_RESOURCE_OPERATION;

        request = startRequest(request);
        waitForRequestToComplete(request);

        validateHostRemoved(containerStateLinks);
    }

    @Test
    public void testRequestBrokerContainerHostRemovalWithSystemContainer() throws Throwable {
        request = startRequest(request);
        waitForRequestToComplete(request);

        request = getDocument(RequestBrokerState.class, request.documentSelfLink);
        assertNotNull(request);

        // create a system container
        ContainerState container = TestRequestStateFactory.createContainer();
        container.descriptionLink = containerDesc.documentSelfLink;
        container.adapterManagementReference = containerDesc.instanceAdapterReference;
        container.groupResourcePlacementLink = groupPlacementState.documentSelfLink;
        container.parentLink = computeHost.documentSelfLink;
        container.system = Boolean.TRUE;
        container = doPost(container, ContainerFactoryService.SELF_LINK);

        // verify the resources are created as expected:
        assertEquals(request.resourceCount, request.resourceLinks.size());
        List<String> containerStateLinks = findResourceLinks(ContainerState.class,
                request.resourceLinks);

        // create a host removal task - RequestBroker
        RequestBrokerState request = new RequestBrokerState();
        request.resourceType = ResourceType.CONTAINER_HOST_TYPE.getName();
        request.resourceLinks = new HashSet<>(Collections.singletonList(
                computeHost.documentSelfLink));
        request.operation = RequestBrokerState.REMOVE_RESOURCE_OPERATION;

        request = startRequest(request);
        waitForRequestToComplete(request);

        validateHostRemoved(containerStateLinks);
    }

    @Test
    public void testRequestBrokerContainerHostRemovalWithSystemContainerAndNetworks()
            throws Throwable {
        request = startRequest(request);
        waitForRequestToComplete(request);

        request = getDocument(RequestBrokerState.class, request.documentSelfLink);
        assertNotNull(request);

        // create a system container
        ContainerState container = TestRequestStateFactory.createContainer();
        container.descriptionLink = containerDesc.documentSelfLink;
        container.adapterManagementReference = containerDesc.instanceAdapterReference;
        container.groupResourcePlacementLink = groupPlacementState.documentSelfLink;
        container.parentLink = computeHost.documentSelfLink;
        container.system = Boolean.TRUE;
        container = doPost(container, ContainerFactoryService.SELF_LINK);

        // verify the resources are created as expected:
        assertEquals(request.resourceCount, request.resourceLinks.size());
        List<String> containerStateLinks = findResourceLinks(ContainerState.class,
                request.resourceLinks);

        // create a network
        ContainerNetworkDescription networkDesc = TestRequestStateFactory
                .createContainerNetworkDescription("test-net");
        networkDesc = doPost(networkDesc, ContainerNetworkDescriptionService.FACTORY_LINK);
        addForDeletion(networkDesc);

        ContainerNetworkState network = TestRequestStateFactory.createNetwork("test-net-003");
        network.adapterManagementReference = networkDesc.instanceAdapterReference;
        network.descriptionLink = networkDesc.documentSelfLink;
        network = doPost(network, ContainerNetworkService.FACTORY_LINK);

        // verify the network is created as expected
        List<String> containerNetworkStateLinks = findResourceLinks(ContainerNetworkState.class,
                Arrays.asList(network.documentSelfLink));
        assertEquals(1, containerNetworkStateLinks.size());

        // create a host removal task - RequestBroker
        RequestBrokerState request = new RequestBrokerState();
        request.resourceType = ResourceType.CONTAINER_HOST_TYPE.getName();
        request.resourceLinks = new HashSet<>(Collections.singletonList(
                computeHost.documentSelfLink));
        request.operation = RequestBrokerState.REMOVE_RESOURCE_OPERATION;

        request = startRequest(request);
        waitForRequestToComplete(request);

        // verify that the network state was removed
        containerNetworkStateLinks = findResourceLinks(ContainerNetworkState.class,
                Arrays.asList(network.documentSelfLink));
        assertTrue("ContainerNetworkState not removed: " + containerNetworkStateLinks,
                containerNetworkStateLinks.isEmpty());

        validateHostRemoved(containerStateLinks);
    }

    private void validateHostRemoved(List<String> containerStateLinks) throws Throwable {
        // verify that the container states were removed
        containerStateLinks = findResourceLinks(ContainerState.class, containerStateLinks);
        assertTrue("ContainerState not removed: " + containerStateLinks,
                containerStateLinks.isEmpty());

        // verify that the host was removed
        Collection<String> computeSelfLinks = findResourceLinks(ComputeState.class,
                Collections.singletonList(computeHost.documentSelfLink));

        assertTrue("ComputeState was not deleted: " + computeSelfLinks, computeSelfLinks.isEmpty());

        // verify that the containers where removed from the docker mock
        Map<String, String> containerRefsByIds = MockDockerAdapterService
                .getContainerIdsWithContainerReferences();
        for (String containerRef : containerRefsByIds.values()) {
            for (String containerLink : containerStateLinks) {
                if (containerRef.endsWith(containerLink)) {
                    fail("Container State not removed with link: " + containerLink);
                }
            }
        }

        List<String> portProfileStates = findResourceLinks(
                HostPortProfileService.HostPortProfileState.class,
                Arrays.asList(hostPortProfileState.documentSelfLink));

        assertTrue("HostPortProfileState was not deleted: " + portProfileStates,
                portProfileStates.isEmpty());
    }

    @Test
    public void testRequestBrokerContainerHostRemovalWithSystemContainerAndVolumes()
            throws Throwable {
        request = startRequest(request);
        waitForRequestToComplete(request);

        request = getDocument(RequestBrokerState.class, request.documentSelfLink);
        assertNotNull(request);

        // create a system container
        ContainerState container = TestRequestStateFactory.createContainer();
        container.descriptionLink = containerDesc.documentSelfLink;
        container.adapterManagementReference = containerDesc.instanceAdapterReference;
        container.groupResourcePlacementLink = groupPlacementState.documentSelfLink;
        container.parentLink = computeHost.documentSelfLink;
        container.system = Boolean.TRUE;
        container = doPost(container, ContainerFactoryService.SELF_LINK);

        // verify the resources are created as expected:
        assertEquals(request.resourceCount, request.resourceLinks.size());
        List<String> containerStateLinks = findResourceLinks(ContainerState.class,
                request.resourceLinks);

        // create a volume
        ContainerVolumeDescription volumeDesc = TestRequestStateFactory
                .createContainerVolumeDescription("test-volume");
        volumeDesc = doPost(volumeDesc, ContainerVolumeDescriptionService.FACTORY_LINK);
        addForDeletion(volumeDesc);

        ContainerVolumeState volume = TestRequestStateFactory.createVolume("test-volume-003");
        volume.adapterManagementReference = volumeDesc.instanceAdapterReference;
        volume.descriptionLink = volumeDesc.documentSelfLink;
        volume = doPost(volume, ContainerVolumeService.FACTORY_LINK);

        // verify the volume is created as expected
        List<String> containerVolumeStateLinks = findResourceLinks(ContainerVolumeState.class,
                Arrays.asList(volume.documentSelfLink));
        assertEquals(1, containerVolumeStateLinks.size());

        // create a host removal task - RequestBroker
        RequestBrokerState request = new RequestBrokerState();
        request.resourceType = ResourceType.CONTAINER_HOST_TYPE.getName();
        request.resourceLinks = new HashSet<>(Collections.singletonList(
                computeHost.documentSelfLink));
        request.operation = RequestBrokerState.REMOVE_RESOURCE_OPERATION;

        request = startRequest(request);
        waitForRequestToComplete(request);

        // verify that the volume state was removed
        containerVolumeStateLinks = findResourceLinks(ContainerVolumeState.class,
                Arrays.asList(volume.documentSelfLink));
        assertTrue("ContainerVolumeState not removed: " + containerVolumeStateLinks,
                containerVolumeStateLinks.isEmpty());

        // verify that the container states were removed
        containerStateLinks = findResourceLinks(ContainerState.class, containerStateLinks);
        assertTrue("ContainerState not removed: " + containerStateLinks,
                containerStateLinks.isEmpty());

        // verify that the host was removed
        Collection<String> computeSelfLinks = findResourceLinks(ComputeState.class,
                Collections.singletonList(computeHost.documentSelfLink));

        assertTrue("ComputeState was not deleted: " + computeSelfLinks, computeSelfLinks.isEmpty());

        // verify that the containers where removed from the docker mock
        Map<String, String> containerRefsByIds = MockDockerAdapterService
                .getContainerIdsWithContainerReferences();
        for (String containerRef : containerRefsByIds.values()) {
            for (String containerLink : containerStateLinks) {
                if (containerRef.endsWith(containerLink)) {
                    fail("Container State not removed with link: " + containerLink);
                }
            }
        }
    }
}
