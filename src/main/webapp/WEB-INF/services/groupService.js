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
(function(){
    'use strict'

    angular.module('cougrApp').factory('GroupService', GroupService);

    GroupService.$inject = ['$rootScope', '$http', '$log'];

    function GroupService($rootScope, $http, logger) {

        return{
            getGroups: getGroups,
			getOwnedGroups: getOwnedGroups,
			getMemberGroups: getMemberGroups,
            getGroup: getGroup,
            createGroup: createGroup,
            addMember: addMember,
            removeMember: removeMember,
			promoteToOwner: promoteToOwner,
			addAdmin: addAdmin,
			removeAdmin: removeAdmin,
			addSubGroup: addSubGroup,
			removeParent: removeParent,
			approvePendingMember: approvePendingMember,
			declinePendingMember: declinePendingMember
        };


        // //////////////////////////////////////////////////////////
        function getGroups() {
            var promise = $http.get('/cougr/local/groups');
            promise.then(getComplete, getFailed);
            return promise;
        }
		
		function getOwnedGroups(userid) {
			var promise = $http.get('/cougr/local/groups/owner/' + userid);
            promise.then(getComplete, getFailed);
            return promise;
			
        }

		function getMemberGroups(userid) {
			var promise = $http.get('/cougr/local/groups/member/' + userid);
            promise.then(getComplete, getFailed);
            return promise;
			
        }


        function getGroup(groupid) {
            if (groupid === undefined) {
                logger.warn("GroupFactory::getGroup: groupid is undefined");
                return [];
            } else {
                return $http.get('/cougr/local/groups/' + groupid)
                    .then(getComplete)
                    .catch(getFailed);
            }
        }
        
        function createGroup(groupName, description, owner, ispublic, isopen, parent) {
			//alert(parent);
			return $http({
				method: 'POST',
				url: '/cougr/local/groups/create',
				data: JSON.stringify({
						name: groupName,
						owner: owner.username,
						description: description,
						isPublic: ispublic,
						isOpen: isopen,
						parent: parent
        		}),
				contentType: 'application/json',
				dataType: 'json',
			}).then(function(response) {
				getCompleteWithFeedback(response, "Group successfully created");
			})
            .catch(getFailed);
        }
        		
        function addMember(groupID, username) {
			return $http({
				method: 'POST',
				url: '/cougr/local/groups/member/add',
				data: JSON.stringify({
        			group_id: groupID,
        			username: username
        		}),
				contentType: 'application/json',
				dataType: 'json',
			}).then(function(response) {
				getCompleteWithFeedback(response, "Group join request sent");
			})
            .catch(getFailed);
        }
        
        function removeMember(groupID, username) {
			return $http({
				method: 'POST',
				url: '/cougr/local/groups/member/delete',
				data: JSON.stringify({
        			group_id: groupID,
        			username: username
        		}),
				contentType: 'application/json',
				dataType: 'json',
			}).then(function(response) {
				getCompleteWithFeedback(response, "User successfully removed from group");
			})
            .catch(getFailed);
        }
		
		function promoteToOwner(groupID, username){
			return $http({
				method: 'POST',
				url: '/cougr/local/groups/owner/promote',
				data: JSON.stringify({
        			group_id: groupID,
        			username: username
        		}),
				contentType: 'application/json',
				dataType: 'json',
			}).then(function(response) {
				getCompleteWithFeedback(response, "User successfully promoted to group owner");
			})
            .catch(getFailed);
		}
		
		function addAdmin(groupID, username){
			return $http({
				method: 'POST',
				url: '/cougr/local/groups/admin/add',
				data: JSON.stringify({
        			group_id: groupID,
        			username: username
        		}),
				contentType: 'application/json',
				dataType: 'json',
			}).then(function(response) {
				getCompleteWithFeedback(response, "User successfully added as admin");
			})
            .catch(getFailed);
		}
		
		function removeAdmin(groupID, username){
			return $http({
				method: 'POST',
				url: '/cougr/local/groups/admin/delete',
				data: JSON.stringify({
        			group_id: groupID,
        			username: username
        		}),
				contentType: 'application/json',
				dataType: 'json',
			}).then(function(response) {
				getCompleteWithFeedback(response, "User's admin privileges successfully revoked");
			})
            .catch(getFailed);
		}
		
		function addSubGroup(parentid, childid){
			return $http({
				method: 'POST',
				url: '/cougr/local/groups/subgroup/add',
				data: JSON.stringify({
        			parent_id: parentid,
        			child_id: childid
        		}),
				contentType: 'application/json',
				dataType: 'json',
			}).then(function(response) {
				getCompleteWithFeedback(response, "Subgroup successful added");
			})
            .catch(getFailed);
		}
		
		function removeParent(groupID){
			return $http({
				method: 'POST',
				url: '/cougr/local/groups/subgroup/remove',
				data: JSON.stringify({
        			group_id: groupID
        		}),
				contentType: 'application/json',
				dataType: 'json',
			}).then(function(response) {
				getCompleteWithFeedback(response, "Group is no longer a subgroup");
			})
            .catch(getFailed);
		}
		
		function approvePendingMember(groupID, username){
			return $http({
				method: 'POST',
				url: '/cougr/local/groups/member/pending/approve',
				data: JSON.stringify({
        			group_id: groupID,
        			username: username
        		}),
				contentType: 'application/json',
				dataType: 'json',
			}).then(function(response) {
				getCompleteWithFeedback(response, "User successfully added to group");
			})
            .catch(getFailed);
		}
		
		function declinePendingMember(groupID, username){
			return $http({
				method: 'POST',
				url: '/cougr/local/groups/member/pending/decline',
				data: JSON.stringify({
        			group_id: groupID,
        			username: username
        		}),
				contentType: 'application/json',
				dataType: 'json',
			}).then(function(response) {
				getCompleteWithFeedback(response, "User group request declined");
			})
            .catch(getFailed);
		}
		
        function getCompleteWithFeedback(results, msg){
			$rootScope.$broadcast('addNewAlert', {'type': 'success', 'msg': msg});
            return results.data;
        }

        function getComplete(results){
            return results.data;
        }
		
        function getFailed(error){
			$rootScope.$broadcast('addNewAlert', {'type': 'danger', 'msg': 'An error has occured'});
            logger.warn('XHR Failed for GroupService: '+error.data)
            return [];
        }
    }
})();


