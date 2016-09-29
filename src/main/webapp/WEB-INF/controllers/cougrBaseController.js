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
    'use strict';

    angular.module('cougrApp').controller('CougrBaseCtrl', Controller);
						
    Controller.$inject = ['$location', '$log', 'UserService'];

    function Controller($location, logger, userService){
        var vm = this;
        vm.isActive = isActive;
		vm.isAdmin = isAdmin;
        vm.user = {};
        vm.user.current = {};
        vm.dataLoaded = false;
        loadLoggedInUserInfo();
		

        /////////////////////////////////////////////////
        function isActive(path){
            return path === $location.path();
        }
		
		function isAdmin(){
			alert(vm.user.current.cougrAdmin);
			return(vm.user.current.cougrAdmin);
		}

        function loadLoggedInUserInfo(){
            return userService.getLoggedInUser().then(function(){
                vm.user.current = userService.user.current;
                vm.dataLoaded = true;

                if(isEmptyUser(vm.user.current)){
                    $location.path("/user/new");
                }
            });
        }

        function isEmptyUser(user){
            if(user.subIssuer === "None@None"){
                return true
            }else{
                return false;
            }
        }
    }	
})();
