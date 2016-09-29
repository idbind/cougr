Cross OpenID User Group Management Service (COUGr)
==================================================

The Cross OpenID User Group Management Service (COUGr) lets OpenID Connect (OIDC) users to manage their groups at this centralized service. Other applications could then query for and update this group information, allowing for groups to be synced across multiple applications.

There are two main parts to this service:

1. Web front-end interface for users to perform group management actions such as creating groups and joining/leaving groups. Users login to the COUGr front-end web application using OIDC.
2. OAuth-protected RESTful Web API endpoints for service to directly query for group information and update group information at COUGr.

To get started, you will need to have an existing OIDC server and set up the appropriate OIDC/OAuth configuration in src/main/resources/websecurity.properties. The current Spring configuration assumes that the OAuth server is the same as the OIDC server, but you can use separate ones by rewiring clientConfigurationService and serverConfigurationService in OAuthProtectedResourceConfiguration.java.

Currently, OIDC users are mapped to their local COUGr user entity via the hardcoded information in org.mitre.cougr.service.FakeDataService. You will need to update these accordingly, or set up your own persistence for local COUGr user entities.

Description of web API endpoints:

- GET: /COUGr/rest/users: List of all users
- GET: /COUGr/rest/users/{id}: User with id=:id
- GET: /COUGr/rest/users/current_user: The user object for the logged in user
- GET: /COUGr/rest/users/cougr_admin/{id}: Elevate given user to role Admin
- GET: /COUGr/rest/groups: All visible groups
- POST: /COUGr/rest/groups/create
	- name (String): Group name
	- owner (String): owner username
	- description (String): text description of group
	- isPublic (Boolean): Can users see this group
	- isOpen (Boolean): Can users freely join this group
- GET: /COUGr/rest/groups/{id}: Group with given id
- GET: /COUGr/rest/groups/owner/{id}: List of groups owners by the user with the given id
- GET: /COUGr/rest/groups/member/{id}: List of groups the user with the given id is a member of
- POST: /COUGr/rest/groups/member/add
	- group_id: group to add user to
	- username: username of user to add to group
- POST: /COUGr/rest/groups/member/delete
	- group_id: group to remove user to
	- username: username of user to remove from group
- POST: /COUGr/rest/groups/admin/add
	- group_id: is of group that user will become an admin of
	- username: username of user to become an admin of group
- POST: /COUGr/rest/groups/admin/delete
	- group_id: group to revoke admin privileges from
	- username: user to be removed as admin
- POST: /COUGr/rest/groups/subgroup/add
	- parent_id: id of group that will become parent of subgroup
	- child_id: id of group to become sub-group
- POST: /COUGr/rest/groups/subgroup/remove
	- child_id: id of group to become a parent group
	
	
Copyright ©2016, The MITRE Corporation. Licensed under the Apache 2.0 license, for details see LICENSE.txt. 
