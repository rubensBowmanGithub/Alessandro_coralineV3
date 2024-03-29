/*
 * Copyright 2017 StreamSets Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * SDC Configuration module.
 */

angular
  .module('commonUI.coralineConfiguration')
  .config(['$routeProvider', function ($routeProvider) {
    $routeProvider.when('/collector/configuration',
      {
        templateUrl: 'common/administration/coralineConfiguration/coralineConfiguration.tpl.html',
        controller: 'coralineConfigurationController',
        resolve: {
          myVar: function (authService) {
            return authService.init();
          }
        },
        data: {
          authorizedRoles: ['admin', 'creator', 'manager']
        }
      }
    );
  }])
  .controller('coralineConfigurationController', function ($scope, $rootScope, $q, Analytics, configuration, _) {
    $rootScope.common.title = "Data Collector Configuration";
    angular.extend($scope, {
      initDefer: undefined,
      configKeys: [],
      coralineConfiguration: {}
    });

    $scope.initDefer = $q.all([configuration.init()]).then(function () {
      if (configuration.isAnalyticsEnabled()) {
        Analytics.trackPage('/collector/configuration');
      }
      $scope.coralineConfiguration = configuration.getConfiguration();
      $scope.configKeys = _.keys($scope.coralineConfiguration);
      $scope.configKeys.sort();
    });
  });
