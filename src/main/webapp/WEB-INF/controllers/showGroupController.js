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
(function() {
	'use strict'

	angular.module('cougrApp').controller('ShowGroupCtrl', Controller);

	Controller.$inject = ['$routeParams', 'GroupService', 'UserService', '$log' ];

	function Controller($routeParams, groupService, userService, logger) {
		var vm = this;
		vm.currentGroup = [];
		vm.isAddingMember = false;
		vm.loggedInUser = userService.user.current;
		vm.parenGroup={};
		vm.ismember = false;

		//Functions
		vm.getCurrentGroup = getCurrentGroup;
		vm.removeMember = removeMember;
		vm.addMember = addMember;
		vm.promoteToOwner = promoteToOwner;
		vm.addAdmin = addAdmin;
		vm.removeAdmin = removeAdmin;
		vm.isGroupAdmin = isGroupAdmin;
		vm.addSubGroup = addSubGroup;
		vm.removeParent = removeParent;
		vm.approveMember = approveMember;
		vm.declineMember = declineMember;
		vm.joinGroup = joinGroup;
		vm.leaveGroup = leaveGroup;
		


		loadCurrentGroup();

		// //////////////////////////////////////////////////////////
		function loadCurrentGroup() {
			return getCurrentGroup($routeParams.groupid).then(function(data) {
				vm.currentGroup = data;
				logger.info("Loaded current group: " + vm.currentGroup)
				
				vm.ismember = data.members.some(function(user) {
					return user.id == vm.loggedInUser.id;
				});
				
				return vm.currentGroup;
			});
		}

		function getCurrentGroup(groupid) {
			return groupService.getGroup(groupid);
		}

		function removeMember(username) {
			var deferred = groupService.removeMember(vm.currentGroup.id,
					username).then(function() {
				loadCurrentGroup();
			});
			return deferred;
		}
		
		function promoteToOwner(username) {
			if (confirm("Are you sure you want to promote this user to group owner? The current owner will be removed as owner.")) {
				var deferred = groupService.promoteToOwner(vm.currentGroup.id,
						username).then(function() {
					loadCurrentGroup();
				});
				return deferred;
			}
		}

		function addAdmin(username) {
			var deferred = groupService.addAdmin(vm.currentGroup.id,
					username).then(function() {
				loadCurrentGroup();
			});
			return deferred;
		}
		
		function removeAdmin(username) {
			var deferred = groupService.removeAdmin(vm.currentGroup.id,
					username).then(function() {
				loadCurrentGroup();
			});
			return deferred;
		}
		
		function isGroupAdmin(username){
			var admins = vm.currentGroup.admins;
			for (var i = 0; i < admins.length; i++) {
				if(admins[i].username === username){
					return true;
				}
			}
			return false;
		}
		
		function addMember() {
			vm.isAddingMember = false;
			var deferred = groupService.addMember(vm.currentGroup.id,
					vm.newMember.username).then(function() {
				loadCurrentGroup();
			});
			vm.newMember.username = "";
			return deferred;
		}
		
		function addSubGroup() {
			var deferred = groupService.addSubGroup(vm.parenGroup.id,
					vm.currentGroup.id).then(function() {
				loadCurrentGroup();
			});
			return deferred;
		}
		
		function removeParent() {
			var deferred = groupService.removeParent(vm.currentGroup.id).then(function() {
				loadCurrentGroup();
			});
			return deferred;
		}
		
		function approveMember(username){
			var deferred = groupService.approvePendingMember(vm.currentGroup.id, username).then(function() {
				loadCurrentGroup();
			});
			return deferred;
		}
		
		function declineMember(username){
			var deferred = groupService.declinePendingMember(vm.currentGroup.id, username).then(function() {
				loadCurrentGroup();
			});
			return deferred;
		}
		
		function joinGroup(){
			var deferred = groupService.addMember(vm.currentGroup.id,
					vm.loggedInUser.username).then(function() {
				loadCurrentGroup();
			});
		}
		function leaveGroup(){
			var deferred = groupService.removeMember(vm.currentGroup.id,
					vm.loggedInUser.username).then(function() {
				loadCurrentGroup();
			});
		}

	}
})();
