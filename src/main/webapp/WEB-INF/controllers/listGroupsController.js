/* 
* Copyright 2016 The MITRE Corporation
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
(function () {
    'use strict'

    angular.module('cougrApp').controller('ListGroupsCtrl', Controller);

    Controller.$inject = ['GroupService','UserService', '$log'];

    function Controller(groupService, userService, logger) {
		var vm = this;
        vm.groups = [];
		vm.owned_groups = [];
		vm.newGroup ={
			isopen : false,
			ispublic : false
		};
		vm.isSubGroup = false;
		vm.parenGroup = {};

        vm.getGroups = getGroups;
		vm.createNewGroup = createNewGroup;

        loadGroups();
		
        ///////////////////////////////////////////////
        function loadGroups(){
			var promise = getGroups();
            promise.then(loadSuccess, loadFailed);
            return promise;
        }

        function getGroups(){
            return groupService.getGroups();
        }
		
		function loadSuccess(results){
			var username = userService.user.current.username;
			vm.groups = results.data;
			
			vm.owned_groups = vm.groups.filter(function(group) {
				return group.owner.username == username;
			});
			
			return vm.groups;
        }
		
        function loadFailed(err){
            logger.info("listGroupsController::loadGroups: Load Groups failed. "+err);
        }
		
		function createNewGroup(){
			var currentUser = userService.getLoggedInUser();
			
			if(vm.parentGroup){
				var deferred = groupService.createGroup(vm.newGroup.name,
					vm.newGroup.description, currentUser, vm.newGroup.ispublic, vm.newGroup.isopen, vm.parentGroup.id).then(function() {
					loadGroups();
				});
			}else{
				var deferred = groupService.createGroup(vm.newGroup.name,
					vm.newGroup.description, currentUser, vm.newGroup.ispublic, vm.newGroup.isopen).then(function() {
					loadGroups();
				});
			}
			vm.newGroup.name = "";
			vm.newGroup.description = ""
			vm.newGroup.ispublic = false;
			vm.newGroup.isopen = false;
			return deferred;
			
		}
		
    }
})();

