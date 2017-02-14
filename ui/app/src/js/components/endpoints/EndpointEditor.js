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

import VueDropdownInput from 'components/common/VueDropdownInput'; //eslint-disable-line
import VuePasswordInput from 'components/common/VuePasswordInput'; //eslint-disable-line
import VueTextInput from 'components/common/VueTextInput'; //eslint-disable-line
import AwsEndpointEditor from 'components/endpoints/aws/EndpointEditor'; //eslint-disable-line
import AzureEndpointEditor from 'components/endpoints/azure/EndpointEditor'; //eslint-disable-line
import NimbusEndpointEditor from 'components/endpoints/nimbus/EndpointEditor'; //eslint-disable-line
import VsphereEndpointEditor from 'components/endpoints/vsphere/EndpointEditor'; //eslint-disable-line
import VroEndpointEditor from 'components/endpoints/vro/EndpointEditor'; //eslint-disable-line
import EndpointEditorVue from 'components/endpoints/EndpointEditorVue.html';
import { EndpointsActions } from 'actions/Actions';
import utils from 'core/utils';
import services from 'core/services';

const OOTB_TYPES = [
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
  },
  {
    id: 'vro',
    name: 'vRO',
    iconSrc: 'image-assets/endpoints/vro.png'
  }
];

var externalAdapters = null;

var loadExternalAdapters = function() {
  return services.loadAdapters().then((adapters) => {
    externalAdapters = [];
    if (adapters) {
      for (var k in adapters) {
        if (!adapters.hasOwnProperty(k)) {
          continue;
        }
        var doc = adapters[k];

        var icon = doc.customProperties && doc.customProperties.icon;
        if (icon && icon[0] === '/') {
          // Remove slash, as UI is not always served at /, e.g. in CAFE embedded.
          // So instead use relative path.
          icon = icon.substring(1);
        }

        var uiLink = doc.customProperties && doc.customProperties.uiLink;
        if (uiLink && uiLink[0] === '/') {
          // Remove slash, as UI is not always served at /, e.g. in CAFE embedded.
          // So instead use relative path.
          uiLink = uiLink.substring(1);
        }

        var endpointEditor = doc.customProperties && doc.customProperties.endpointEditor;

        externalAdapters.push({
          id: doc.id,
          name: doc.name,
          iconSrc: icon,
          uiLink: uiLink,
          endpointEditor: endpointEditor
        });
      }
    }

    return Promise.all(externalAdapters.map(({uiLink}) =>
        services.loadScript(uiLink)));
  });
};

var getSupportedEditors = function() {
  let supportedEditors = [
    'aws-endpoint-editor',
    'azure-endpoint-editor',
    'vsphere-endpoint-editor',
    'vro-endpoint-editor'
  ];
  if (utils.isNimbusEnabled()) {
    supportedEditors.push('nimbus-endpoint-editor');
  }
  if (externalAdapters) {
    supportedEditors = supportedEditors.concat(
        externalAdapters.map(({endpointEditor}) => endpointEditor));
  }
  return supportedEditors;
};

var getSupportedTypes = function() {
  let supportedTypes = OOTB_TYPES.slice();
  if (utils.isNimbusEnabled()) {
    supportedTypes.push({
      id: 'nimbus',
      name: 'Nimbus',
      iconSrc: 'image-assets/endpoints/nimbus.png'
    });
  }
  if (externalAdapters) {
    supportedTypes = supportedTypes.concat(externalAdapters);
  }
  return supportedTypes;
};

export default Vue.component('endpoint-editor', {
  template: EndpointEditorVue,
  props: {
    model: {
      required: true,
      type: Object
    }
  },
  computed: {
    validationErrors() {
      return (this.model.validationErrors && this.model.validationErrors._generic) ||
          (this.propertiesErrors && this.propertiesErrors._generic);
    }
  },
  attached() {
    let supportedEditors = getSupportedEditors();
    let supportedTypes = getSupportedTypes();

    if (!externalAdapters) {
      if (utils.isExternalPhotonAdaptersEnabled()) {
        var loading = {
          id: 'loading',
          name: 'Loading',
          isBusy: true
        };

        supportedTypes.push(loading);
        loadExternalAdapters().then(() => {
          this.supportedEditors = getSupportedEditors();
          this.supportedTypes = getSupportedTypes();
        }).catch(() => {
          this.supportedEditors = getSupportedEditors();
          this.supportedTypes = getSupportedTypes();
        });
      } else {
        externalAdapters = [];
      }
    }

    this.supportedEditors = supportedEditors;
    this.supportedTypes = supportedTypes;
  },
  data() {
    return {
      endpointType: this.model.item.endpointType,
      name: this.model.item.name,
      properties: this.model.item.endpointProperties || {},
      customProperties: this.model.item.customProperties || {},
      propertiesErrors: null,
      saveDisabled: !this.model.item.documentSelfLink,
      supportedEditors: [],
      supportedTypes: []
    };
  },
  methods: {
    cancel($event) {
      $event.stopImmediatePropagation();
      $event.preventDefault();
      EndpointsActions.cancelEditEndpoint();
    },
    save($event) {
      $event.stopImmediatePropagation();
      $event.preventDefault();
      let toSave = this.getModel();
      if (toSave.documentSelfLink) {
        EndpointsActions.updateEndpoint(toSave);
      } else {
        EndpointsActions.createEndpoint(toSave);
      }
    },
    onNameChange(name) {
      this.name = name;
      this.saveDisabled = this.isSaveDisabled();
    },
    onEndpointTypeChange(endpointType) {
      this.endpointType = endpointType && endpointType.id;
      this.saveDisabled = this.isSaveDisabled();
    },
    onPropertiesChange(properties) {
      this.properties = properties;
      if (this.endpointType === 'vro') {
        this.onCustomPropertiesChange(properties);
      }
      this.saveDisabled = this.isSaveDisabled();
    },
    onCustomPropertiesChange(properties) {
      this.customProperties.provider = properties.providerType;
      this.customProperties.providerHostname = properties.providerHostName;
      this.customProperties.providerPrivateKeyId = properties.providerPrivateKeyId;
      this.customProperties.providerPrivateKey = properties.providerPrivateKey;
    },
    onPropertiesError(errors) {
      this.propertiesErrors = errors;
    },
    isSaveDisabled() {
      let model = this.getModel();
      if (!model.name || !model.endpointType) {
        return true;
      }
      let properties = model.endpointProperties;
      //removed regionId
      if (!properties.privateKeyId || !properties.privateKey) {
        return true;
      }
      if (model.endpointType === 'azure' && (!properties.userLink || !properties.azureTenantId)) {
          return true;
      }
      if (model.endpointType === 'vsphere' && !properties.hostName) {
          return true;
      }
      if (model.endpointType === 'vro' && !properties.providerHostName && !properties
      .providerPrivateKeyId && !properties.providerPrivateKey) {
          return true;
      }
      return false;
    },
    getModel() {
      return $.extend({}, this.model.item, {
        endpointProperties: $.extend(this.model.item.endpointProperties || {}, this.properties),
        customProperties: $.extend(this.model.item.customProperties || {}, this.customProperties),
        endpointType: this.endpointType,
        name: this.name
      });
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
