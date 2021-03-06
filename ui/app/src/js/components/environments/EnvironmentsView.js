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

import EnvironmentsViewVue from 'components/environments/EnvironmentsViewVue.html';
import EnvironmentItem from 'components/environments/EnvironmentItem'; //eslint-disable-line
import EnvironmentEditView from 'components/environments/EnvironmentEditView'; //eslint-disable-line
import GridHolderMixin from 'components/common/GridHolderMixin';
import constants from 'core/constants';
import { EnvironmentsActions, NavigationActions } from 'actions/Actions';

var EnvironmentsView = Vue.extend({
  template: EnvironmentsViewVue,

  props: {
    model: {
      required: true,
      type: Object
    }
  },

  data: function() {
    return {
      constants: constants,
      // this view behaves better if the target width is set before the width transition
      requiresPreTransitionWidth: true
    };
  },

  mixins: [GridHolderMixin],

  attached: function() {
    var $mainPanel = $(this.$el).find('.list-holder > .main-panel');
    $mainPanel.on('transitionend MSTransitionEnd webkitTransitionEnd oTransitionEnd',
      (e) => {
        if (e.target === $mainPanel[0]) {
          this.unsetPostTransitionGridTargetWidth();
        }
      }
    );
  },

  detached: function() {
    var $mainPanel = $(this.$el).find('.list-holder > .main-panel');
    $mainPanel.off('transitionend MSTransitionEnd webkitTransitionEnd oTransitionEnd');
  },

  computed: {
    searchSuggestions: function() {
      return constants.ENVIRONMENTS.SEARCH_SUGGESTIONS;
    }
  },

  methods: {
    goBack: function() {
      NavigationActions.openEnvironments(this.model.listView && this.model.listView.queryOptions);
    },
    search: function(queryOptions) {
      NavigationActions.openEnvironments(queryOptions);
    },
    refresh: function() {
      EnvironmentsActions.openEnvironments(this.model.listView.queryOptions, true);
    },
    loadMore: function() {
      if (this.model.listView.nextPageLink) {
        EnvironmentsActions.openEnvironmentsNext(this.model.listView.queryOptions,
          this.model.listView.nextPageLink);
      }
    },
    addEnvironment: function() {
      NavigationActions.openAddEnvironment();
    },
    editEnvironment: function(environment) {
      NavigationActions.editEnvironment(environment.documentSelfLink);
    }
  }
});

Vue.component('environments-view', EnvironmentsView);

export default EnvironmentsView;
