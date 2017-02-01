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

import VueDropdownSearchInput from 'components/common/VueDropdownSearchInput'; //eslint-disable-line

const PROVIDER_TYPES = [

  {
    id: 'aws',
    name: 'AWS',
    iconSrc: 'image-assets/endpoints/aws.png'
  },
  {
    id: 'azure',
    name: 'Azure',
    iconSrc: 'image-assets/endpoints/azure.png'
  },
  {
    id: 'vsphere',
    name: 'vSphere',
    iconSrc: 'image-assets/endpoints/vsphere.png'
  }
];

var getSupportedProviders = function() {
  let supportedProviders = PROVIDER_TYPES.slice();
  return supportedProviders;
};

export default Vue.component('vro-endpoint-editor', {
    template: `
      <div>
        <text-input
          :disabled="model.documentSelfLink"
          :label="i18n('app.endpoint.edit.vro.hostNameLabel')"
          :required="true"
          :value="hostName"
          @change="onHostNameChange">
        </text-input>
        <text-input
          :label="i18n('app.endpoint.edit.vro.privateKeyIdLabel')"
          :required="true"
          :value="privateKeyId"
          @change="onPrivateKeyIdChange">
        </text-input>
        <password-input
          :label="i18n('app.endpoint.edit.vro.privateKeyLabel')"
          :required="true"
          :value="privateKey"
          @change="onPrivateKeyChange">
        </password-input>
        <dropdown-input
          :entity="i18n('app.endpoint.providerTypeEntity')"
          :label="i18n('app.endpoint.edit.providerTypeLabel')"
          :options="supportedProviders"
          :required="true"
          :value="convertToObject(providerType)"
          @change="onProviderTypeChange">
        </dropdown-input>
        <text-input
          :label="i18n('app.endpoint.edit.vro.providerHostNameLabel')"
          :value="providerHostName"
          :required="true"
          v-if="providerType === 'vsphere'"
          @change="onProviderHostNameChange">
        </text-input>
        <text-input
          :label="i18n('app.endpoint.edit.vro.providerPrivateKeyIdLabel')"
          :value="providerPrivateKeyId"
          :required="true"
          v-if="providerType === 'vsphere'"
          @change="onProviderPrivateKeyIdChange">
        </text-input>
        <password-input
          :label="i18n('app.endpoint.edit.vro.providerPrivateKeyLabel')"
          :value="providerPrivateKey"
          :required="true"
          v-if="providerType === 'vsphere'"
          @change="onProviderPrivateKeyChange">
        </password-input>
      </div>
    `,
    props: {
      model: {
        required: true,
        type: Object
      }
    },
    attached: function() {
    let supportedProviders = getSupportedProviders();
    this.supportedProviders = supportedProviders;
    },
    data() {
      let properties = this.model.endpointProperties || {};
      let customProperties = this.model.customProperties || {};
      return {
        hostName: properties.hostName,
        privateKeyId: properties.privateKeyId,
        privateKey: properties.privateKey,
        providerType: customProperties.providerType,
        providerHostName: customProperties.providerHostName,
        providerPrivateKeyId: customProperties.providerPrivateKeyId,
        providerPrivateKey: customProperties.providerPrivateKey,
        supportedProviders: []
      };
    },
    methods: {
      onHostNameChange(hostName) {
        this.hostName = hostName;
        this.dispatchChangeIfNeeded();
      },
      onPrivateKeyIdChange(privateKeyId) {
        this.privateKeyId = privateKeyId;
        this.dispatchChangeIfNeeded();
      },
      onPrivateKeyChange(privateKey) {
        this.privateKey = privateKey;
        this.dispatchChangeIfNeeded();
      },
      onProviderTypeChange(providerType) {
        this.providerType = providerType.id;
        this.dispatchChangeIfNeeded();
      },
      onProviderHostNameChange(providerHostName) {
      this.providerHostName = providerHostName;
      this.dispatchChangeIfNeeded();
      },
      onProviderPrivateKeyIdChange(providerPrivateKeyId) {
      this.providerPrivateKeyId = providerPrivateKeyId;
      this.dispatchChangeIfNeeded();
      },
      onProviderPrivateKeyChange(providerPrivateKey) {
      this.providerPrivateKey = providerPrivateKey;
      this.dispatchChangeIfNeeded();
      },
      dispatchChange() {
        this.$dispatch('change', {
          hostName: this.hostName,
          privateKeyId: this.privateKeyId,
          privateKey: this.privateKey,
          providerType: this.providerType,
          providerHostName: this.providerHostName,
          providerPrivateKeyId: this.providerPrivateKeyId,
          providerPrivateKey: this.providerPrivateKey
        });
      },
      dispatchChangeIfNeeded() {
        if (this.hostName && this.privateKeyId && this.privateKey && this.providerType && this
        .providerHostName && this.providerPrivateKeyId && this.providerPrivateKey) {
          this.dispatchChange();
        }
      },
      convertToObject(value) {
        if (value) {
          return {
            id: value,
            name: value
          };
        }
      }
    }
});
