/*******************************************************************************
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
 ******************************************************************************/
package org.mitre.cougr.controller;

import org.mitre.cougr.dao.model.User;
import org.mitre.cougr.exception.CougrUnknownUser;
import org.mitre.cougr.service.GroupService;
import org.mitre.cougr.service.UserService;
import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import org.springframework.security.access.prepost.PreAuthorize;

@Controller
@PreAuthorize("hasRole('ROLE_USER')")
public class IndexController {
    private static final Logger LOG = LoggerFactory.getLogger(IndexController.class);

    @Autowired
    private GroupService groupService;

    @Autowired
    private UserService userService;

    @RequestMapping({"/"}) //map all endpioints to index and let angular routeprovider manage the view
    public String index(Principal p) {
        LOG.info("IN INDEX CONTROLLER");
        User user = User.EMPTY_USER;
        try {
            user = userService.getLoggedInUser((OIDCAuthenticationToken) p);
        }catch(CougrUnknownUser ex){
            LOG.error("New User: "+((OIDCAuthenticationToken) p).getSub());
            //return "forward:/user/new";
        }
		
        return "forward:/templates/home.html";


    }

//    @RequestMapping({"/profile"})
//    public String profile() {
//        return "profile";
//    }
//
//    @RequestMapping({"/users"})
//    public String users() {
//        return "users";
//    }
//
//    @RequestMapping({"/groups"})
//    public String groups() {
//        return "groups";
//    }
//
//    @RequestMapping({"/group/{id}"})
//    public String group(@PathVariable Long groupId) {
////        ModelAndView mav = new ModelAndView();
////        mav.setViewName("group");
////        mav.addObject("id", groupId);
////
////        Group group = groupService.getGroupById(groupId);
////        mav.addObject("group", group);
////        return mav;
//        return "group";
//    }
}
