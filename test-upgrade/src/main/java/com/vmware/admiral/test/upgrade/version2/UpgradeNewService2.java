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

package com.vmware.admiral.test.upgrade.version2;

import java.util.List;

import com.esotericsoftware.kryo.serializers.VersionFieldSerializer.Since;

import com.vmware.admiral.common.serialization.ReleaseConstants;
import com.vmware.admiral.common.serialization.ThreadLocalVersionHolder;
import com.vmware.admiral.common.util.AssertUtil;
import com.vmware.admiral.test.upgrade.common.UpgradeUtil;
import com.vmware.admiral.test.upgrade.version1.UpgradeOldService2;
import com.vmware.xenon.common.Operation;
import com.vmware.xenon.common.ServiceDocument;
import com.vmware.xenon.common.ServiceDocumentDescription.PropertyUsageOption;
import com.vmware.xenon.common.StatefulService;
import com.vmware.xenon.common.Utils;

/**
 * Represents the base service {@link UpgradeOldService2} with new required fields.
 */
public class UpgradeNewService2 extends StatefulService {

    public static final String FACTORY_LINK = UpgradeUtil.UPGRADE_SERVICE2_FACTORY_LINK;

    public static class UpgradeNewService2State extends ServiceDocument {

        public static final String KIND = UpgradeUtil.UPGRADE_SERVICE2_STATE_KIND;

        @PropertyOptions(usage = { PropertyUsageOption.REQUIRED,
                PropertyUsageOption.AUTO_MERGE_IF_NOT_NULL })
        public String field1;

        @PropertyOptions(usage = { PropertyUsageOption.OPTIONAL,
                PropertyUsageOption.AUTO_MERGE_IF_NOT_NULL })
        public String field2;

        @Since(ReleaseConstants.RELEASE_VERSION_0_9_5)
        @PropertyOptions(usage = { PropertyUsageOption.REQUIRED,
                PropertyUsageOption.AUTO_MERGE_IF_NOT_NULL })
        public String field3;

        @Since(ReleaseConstants.RELEASE_VERSION_0_9_5)
        @PropertyOptions(usage = { PropertyUsageOption.REQUIRED,
                PropertyUsageOption.AUTO_MERGE_IF_NOT_NULL })
        public Long field4;

        @Since(ReleaseConstants.RELEASE_VERSION_0_9_5)
        @PropertyOptions(usage = { PropertyUsageOption.REQUIRED,
                PropertyUsageOption.AUTO_MERGE_IF_NOT_NULL })
        public List<String> field5;
    }

    public UpgradeNewService2() {
        super(UpgradeNewService2State.class);
        toggleOption(ServiceOption.IDEMPOTENT_POST, true);
        toggleOption(ServiceOption.PERSISTENCE, true);
        toggleOption(ServiceOption.REPLICATION, true);
        toggleOption(ServiceOption.OWNER_SELECTION, true);
    }

    static {
        UpgradeNewService2StateConverter.INSTANCE.init();
    }

    @Override
    public void handleRequest(Operation request) {
        ThreadLocalVersionHolder.setVersion(request);
        try {
            super.handleRequest(request);
        } finally {
            ThreadLocalVersionHolder.clearVersion();
        }
    }

    @Override
    public void handleStart(Operation post) {
        UpgradeNewService2State body = post.getBody(UpgradeNewService2State.class);
        AssertUtil.assertNotNull(body, "body");
        // validate based on annotations
        Utils.validateState(getStateDescription(), body);
        super.handleStart(post);
    }

    @Override
    public void handlePut(Operation put) {
        UpgradeNewService2State body = put.getBody(UpgradeNewService2State.class);
        AssertUtil.assertNotNull(body, "body");
        // validate based on annotations
        Utils.validateState(getStateDescription(), body);
        super.handlePut(put);
    }

}
