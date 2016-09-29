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

    angular.module('cougrApp').factory('UserService', UserService);

    UserService.$inject = ['$rootScope', '$http', '$log'];

    function UserService($rootScope, $http, logger){
        var service = {};
        service.user = {};
        service.user.current = null;
		
		//Functions
        service.getLoggedInUser = getLoggedInUser;
        service.getUsers = getUsers;
        service.getUser = getUser;
		service.addCougrAdmin = addCougrAdmin;
		service.removeCougrAdmin = removeCougrAdmin;

        return service;
		
		
        ////////////////////////////////////////////////////////////
        function getLoggedInUser(){
            if(service.user.current === null){
                return $http.get('/cougr/local/users/current_user')
                    .then(function(result){
                        service.user.current = result.data;
						return service.user.current;
                    })
                    .catch(getFailed)
            }else{
                return service.user.current;
            }
        }
        function getUsers() {
            return $http.get('/cougr/local/users/')
                .then(getComplete)
                .catch(getFailed);
        }

        function getUser(userid) {
            return $http.get('/cougr/local/users/'+userid)
                .then(getComplete)
                .catch(getFailed);
        }

		function addCougrAdmin(userid){
			return $http({
				method: 'POST',
				url: '/cougr/local/users/cougr_admin',
				data: JSON.stringify({
        			userid: userid,
        		}),
				contentType: 'application/json',
				dataType: 'json',
			}).then(function(response) {
				getCompleteWithFeedback(response, "User added as COUGr admin");
			})
            .catch(getFailed);
		}
		
		function removeCougrAdmin(userid){
			return $http({
				method: 'POST',
				url: '/cougr/local/users/cougr_admin/remove',
				data: JSON.stringify({
        			userid: userid,
        		}),
				contentType: 'application/json',
				dataType: 'json',
			}).then(function(response) {
				getCompleteWithFeedback(response, "User removed as COUGr admin");
			})
            .catch(getFailed);
		
		}

        function getCompleteWithFeedback(results, msg){
			$rootScope.$broadcast('addNewAlert', {'type': 'success', 'msg': msg});
            return results.data;
        }

        function getComplete(results){
            return results.data
        }

        function getFailed(error){
            logger.warn('XHR Failed for UserService: '+error.data)
            return [];
        }
    }

})();
