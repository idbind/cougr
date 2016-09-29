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

	angular.module('cougrApp').controller('ShowUserCtrl', Controller);

	Controller.$inject = ['$routeParams', 'UserService', 'GroupService', '$log'];

	function Controller($routeParams, userService, groupService, logger) {
		var vm = this;
		vm.user = [];
		vm.my_groups =[];
		vm.pending_groups = [];
		vm.other_groups =[];
		vm.owned_groups = [];
		vm.loggedInUser = [];
		vm.isAddingGroup = false;
		vm.isSubGroup = false;
		vm.parenGroup = {};
		vm.newGroup ={
			isopen : false,
			ispublic : false
		};
		
		//Functions
		vm.getUser = getUser;
		vm.getLoggedInUser = getLoggedInUser;
		vm.createGroup = createGroup;
		vm.joinGroup = joinGroup;
		vm.leaveGroup = leaveGroup;
		vm.addCougrAdmin = addCougrAdmin;
		vm.removeCougrAdmin = removeCougrAdmin;
		vm.approveMember = approveMember;
		vm.declineMember = declineMember;
		
		

		loadUser();

		// /////////////////////////////////////////////
		function loadUser() {
			if($routeParams.userid){
				return getUser($routeParams.userid).then(function(data) {
					vm.user = data;
					vm.loggedInUser = getLoggedInUser();
					getGroupsForUser();
				});
			}else{
				if(userService.user.current){
					vm.user = userService.user.current;
					vm.loggedInUser = getLoggedInUser();
					getGroupsForUser();
				}
				else{
					return getLoggedInUser().then(function(data){
						vm.user = data;
						vm.loggedInUser = data;
						getGroupsForUser();
					});
				}
				
			}
			return vm.user;

			
		}
		
		function getLoggedInUser() {
			if(userService.user.current){
				return userService.user.current;
			}else{
				return userService.getLoggedInUser();
			}
			
		}

		function getUser(userid) {
			return userService.getUser(userid);
		}
		
		function getGroupsForUser() {
			var userid;
			if($routeParams.userid){
				userid = $routeParams.userid;
			}else{
				userid = vm.user.id; 
			}
			groupService.getGroups().then(function(data) {
				vm.groups = data.data;
				vm.my_groups = vm.groups.filter(function(group) {
					return group.members.some(function(user) {
						return user.id == userid;
					});
				});
				
				vm.owned_groups = vm.my_groups.filter(function(group) {
					return group.owner.username == vm.user.username;
				});

				vm.pending_groups = vm.groups.filter(function(group) {
					for(var i=0; i<group.pendingMembers.length; i++){
						//sadly the user objects contained within the pending member
						//list are less detailed then the active user object so we have to
						//maunally loop here
						if(group.pendingMembers[i].username == vm.user.username){
							return true;
						}
					}
				})

				vm.other_groups = vm.groups.filter(function(group) {
					return ((vm.my_groups.indexOf(group) === -1) && (vm.pending_groups.indexOf(group) === -1));
				})
				return vm.groups;
			});
		}

		function createGroup() {
			vm.isAddingGroup = false;
			if(vm.parentGroup){
				var deferred = groupService.createGroup(vm.newGroup.name,
						vm.newGroup.description, vm.loggedInUser, vm.newGroup.ispublic, vm.newGroup.isopen, vm.parentGroup.id).then(function() {
					getGroupsForUser(userService.user.current.id);
				});
			}else{
				var deferred = groupService.createGroup(vm.newGroup.name,
					vm.newGroup.description, vm.loggedInUser, vm.newGroup.ispublic, vm.newGroup.isopen).then(function() {
				getGroupsForUser(userService.user.current.id);
			});
			}
			vm.newGroup.name = "";
			vm.newGroup.description = ""
			vm.newGroup.ispublic = false;
			vm.newGroup.isopen = false;
			return deferred;
			
		}

		function joinGroup(groupID) {
			var deferred = groupService.addMember(groupID, vm.user.username).then(function() {
				getGroupsForUser(userService.user.current.id);
			});
			return deferred;
			
		}
		
		function leaveGroup(groupID) {
			var deferred = groupService.removeMember(groupID, vm.user.username).then(function() {
				getGroupsForUser(userService.user.current.id);
			});
			return deferred;
			
		}
		
		function addCougrAdmin(userID) {
			var deferred = userService.addCougrAdmin(userID).then(function() {
				loadUser();
			});
			return deferred;
		}
		
		function removeCougrAdmin(userID){
			var deferred = userService.removeCougrAdmin(userID).then(function() {
				loadUser();
			});
			return deferred;
		}
		
		function approveMember(username, groupid){
			var deferred = groupService.approvePendingMember(groupid, username).then(function() {
				getGroupsForUser(userService.user.current.id);
			});
			return deferred;
		}
		
		function declineMember(username, groupid){
			var deferred = groupService.declinePendingMember(groupid, username).then(function() {
				getGroupsForUser(userService.user.current.id);
			});
			return deferred;
		}
		
	}
})();
