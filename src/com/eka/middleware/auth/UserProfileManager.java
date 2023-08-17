package com.eka.middleware.auth;

import com.eka.middleware.service.PropertyManager;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.sqlite.entity.Group;
import com.eka.middleware.sqlite.entity.User;
import com.eka.middleware.sqlite.impl.GroupRepoImpl;
import com.eka.middleware.sqlite.impl.UserRepoImpl;
import com.eka.middleware.template.SystemException;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.idm.PasswordCredential;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pac4j.core.profile.UserProfile;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class UserProfileManager implements IdentityManager {
	public static final Map<String, Object> usersMap = new ConcurrentHashMap<String, Object>();
	private static final Set<String> tenants = new HashSet();
	private static final Set<String> groups = new HashSet();
	private static UserProfileManager upm = null;
	public static Logger LOGGER = LogManager.getLogger(UserProfileManager.class);

	public static final Map<String, Object> getUsers() throws SystemException {
		return UserRepoImpl.getUsers();
	}

	public static List<String> getTenants() {
		byte bytes[] = null;
		try {
			bytes = PropertyManager.readConfigurationFile("profiles.json");
		} catch (SystemException e) {
			ServiceUtils.printException("Failed while loading tenant list", e);
		}
		List<String> tenantList = null;
		if (bytes != null) {
			String json = new String(bytes);
			final Map<String, Object> map = ServiceUtils.jsonToMap(json);
			tenantList = (List<String>) map.get("tenants");
			tenants.addAll(tenantList);
		}
		return new ArrayList(tenants);
	}
	
	public static List<String> getGroups() throws SystemException {
		return GroupRepoImpl.getAllGroups();
	}

	public static void newTenant(String name) throws Exception {
		final Map<String, Object> map = new HashMap();
		List<String> tenantList = getTenants();
		tenantList.add(name);
		tenants.add(name);
		map.put("tenants", tenantList);
		map.put("groups", getGroups());
		map.put("users", getUsers());
		String json = ServiceUtils.toPrettyJson(map);
		PropertyManager.writeConfigurationFile("profiles.json", json.getBytes());
	}
	
	public static void newGroup(String name) throws Exception {
		GroupRepoImpl.addGroup(new Group(name));
	}
	public static void removeGroup(String name) throws Exception {
		GroupRepoImpl.deleteGroup(name);
	}

	public static boolean isUserExist(String user) throws SystemException {
		final Map<String, Object> map = new HashMap();
		final Map<String, Object> umap = getUsers();
		Object existingUser = umap.get(user);
		return existingUser != null;
	}

	public static void addUser(AuthAccount account) throws SystemException {
		try {
			final Map<String, Object> map = new HashMap();
			final Map<String, Object> umap = getUsers();
			if (isUserExist(account.getUserId())) {
				throw new Exception("User already exists: " + account.getUserId());
			}
			Map<String, Object> user = new HashMap();
			user.put("profile", account.getAuthProfile());

			if (account.getUserId().equals("admin")) {
				
				Scanner in = new Scanner(System.in);
				String pass = "admin";
				LOGGER.info("Default tenant is created with user: admin & password: admin.");
				while(pass==null) {
					LOGGER.info("Creating Admin account for default tenant.\nPlease enter strong password:");
					pass=in.nextLine();
					LOGGER.info("\nRe-enter your password:");
					String again=in.nextLine();
					if(!pass.equals(again)) {
						pass=null;
						LOGGER.info("Password did not match. Try again.");
					}
				}
				user.put("password", pass);
			}
			UserRepoImpl.addUser(createUserFromAccount(account));

		} catch (Exception e) {
			throw new SystemException("EKA_MWS_1001", e);
		}
	}
	private static User createUserFromAccount(AuthAccount account) {
		Map<String, Object> profile = account.getProfile();
		String name = profile.get("name") != null ? profile.get("name").toString() : "";
		String password = profile.get("password") != null ? profile.get("password").toString() : "";
		String email = profile.get("email") != null ? profile.get("email").toString() : "";
		List<String> groupName = (List<String>) profile.get("groups");
		List<Group> groups = groupName.stream()
				.map(Group::new)
				.collect(Collectors.toList());
		String tenant = profile.get("tenant").toString();
		return new User(email,password, name, groups, email, tenant, "1");
	}

	public static void updateUser(AuthAccount account,final byte[] pass) throws SystemException {
		User userFromAccount = createUserFromAccount(account);
		UserRepoImpl.updateUser(userFromAccount.getEmail(),userFromAccount);
	}

	public static void updateUser(AuthAccount account,final byte[] pass, String status) throws SystemException {
		try {
			final Map<String, Object> map = new HashMap();
			final Map<String, Object> umap = getUsers();
			Object existingUser = umap.get(account.getUserId());
			if (existingUser == null) {
				throw new Exception("User not found: " + account.getUserId());
			}
			String password=(String) ((Map)existingUser).get("password");
			if(pass!=null) {
				password=new String(pass,StandardCharsets.UTF_8);
				password = "[#]" + ServiceUtils.generateUUID(password + account.getUserId());
			}

			Map<String, Object> user = new HashMap();
			user.put("profile", account.getAuthProfile());

			user.put("password", password);
			user.put("status", status);
			umap.put(account.getUserId(), user);
			map.put("users", umap);
			map.put("tenants", getTenants());
			String json = ServiceUtils.toPrettyJson(map);
			PropertyManager.writeConfigurationFile("profiles.json", json.getBytes());
		} catch (Exception e) {
			throw new SystemException("EKA_MWS_1001", e);
		}
	}

	public static void removeUser(String id) throws SystemException {
		UserRepoImpl.deleteUser(id);
	}

	public static UserProfileManager create() throws SystemException {
		if (upm == null)
			upm = new UserProfileManager();
		return upm;
	}

	public static UserProfileManager getUserProfileManager() {
		return upm;
	}

	private UserProfileManager() throws SystemException {
		getUsers();
	}

	@Override
	public Account verify(Account account) {
		// An existing account so for testing assume still valid.
		return account;
	}

	@Override
	public AuthAccount verify(String id, Credential credential) {
		AuthAccount account = getAccount(id, null);
		try {
			if (account != null && verifyCredential(account, credential)) {
				return account;
			}
		} catch (SystemException e) {
			ServiceUtils.printException("Login exception for " + id, e);
		}

		return null;
	}

	@Override
	public Account verify(Credential credential) {
		// TODO Auto-generated method stub
		return null;
	}

	private boolean verifyCredential(Account account, Credential credential) throws SystemException {
		if (credential instanceof PasswordCredential) {
			char[] password = ((PasswordCredential) credential).getPassword();
			Map<String, Object> usersMap = (Map<String, Object>) getUsers();
			String userId = account.getPrincipal().getName();
			Map<String, Object> user = (Map<String, Object>) usersMap.get(userId);
			if (user == null) {
				return false;
			}

			if (user.get("status") != null && user.get("status").equals("0")) {
				return false;
			}


			if (user.get("password") == null)
				return false;
			char[] expectedPassword = user.get("password").toString().toCharArray();
			String pass = new String(password);
			String passHash = "[#]" + ServiceUtils.generateUUID(pass + userId);
			return Arrays.equals(passHash.toCharArray(), expectedPassword);
		}
		return false;
	}

	public AuthAccount getAccount(UserProfile up) {
		if (up == null)
			return null;
		String id = (String) up.getId();
		if (up.getAttribute("email") != null)
			id = (String) up.getAttribute("email");
		return getAccount(id, up);
	}

	public static AuthAccount getAccount(final String id, final UserProfile up) {
		Map<String, Object> usersMap = null;
		try {
			usersMap = (Map<String, Object>) getUsers();
		} catch (SystemException e) {
			ServiceUtils.printException("Could not load users list: " + id, e);
			return null;
		}
		Map<String, Object> user = (Map<String, Object>) usersMap.get(id);

		if (user != null) {
			Map<String, Object> profile = (Map<String, Object>) user.get("profile");
			String tenant = (String) profile.get("tenant");
			if (up != null && up.getAttribute("access_token") != null && profile==null) {
				profile = createDefaultProfile(up,tenant);
				// profile.put("tenant", tenant);
			}

			AuthAccount authAccount = new AuthAccount(id);
			final Map<String, Object> profle = profile;
			authAccount.setProfile(profle);
			return authAccount;
		} else {
			final Map<String, Object> profile = createDefaultProfile(up,null);
			AuthAccount authAccount = new AuthAccount(id);
			authAccount.setProfile(profile);
			// authAccount.getAuthProfile().put("groups", authAccount);
			return authAccount;
		}
	}

	public static Map<String, Object> createDefaultProfile(UserProfile up,String tenant) {
		Map<String, Object> profile = new HashMap<String, Object>();
		Map<String, Object> jwtMap = null;
		//String tenant = null;
		List<String> groups = new ArrayList<String>();
		String UUID = null;
		String name = null;
		String userId = null;
		String email = null;
		Long creationTimestamp = null;
		try {
			BearerAccessToken bat = (BearerAccessToken) up.getAttribute("access_token");
			if (bat != null) {
				String jwt = bat.getValue();
				if (jwt != null) {
					try {
						jwtMap = JWTParser.parse(jwt).getJWTClaimsSet().toJSONObject();
					} catch (Exception ignored) {}
					if(jwtMap!=null) {
						groups = (List<String>) jwtMap.get("groups");
						UUID = (null == up.getAttribute("UUID")) ? null : up.getAttribute("UUID").toString();
						name = (null == up.getAttribute("name")) ? null : up.getAttribute("name").toString();
						userId = (null == up.getAttribute("userId")) ? null : up.getAttribute("userId").toString();
						email = (null == up.getAttribute("email")) ? null : up.getAttribute("email").toString();
						creationTimestamp = (null == up.getAttribute("creationTimestamp")) ? null : Long.parseLong(up.getAttribute("creationTimestamp").toString());
						tenant = (String) jwtMap.get("tenant");
						if (groups == null)
							groups = (List<String>) jwtMap.get("groups");
					}
				}
			}else {
				tenant=(String) up.getAttribute("tenant");
				UUID = (null == up.getAttribute("UUID")) ? null : up.getAttribute("UUID").toString();
				name = (null == up.getAttribute("name")) ? null : up.getAttribute("name").toString();
				userId = (null == up.getAttribute("userId")) ? null : up.getAttribute("userId").toString();
				email = (null == up.getAttribute("email")) ? null : up.getAttribute("email").toString();
				creationTimestamp = (null == up.getAttribute("creationTimestamp")) ? null : Long.parseLong(up.getAttribute("creationTimestamp").toString());
				if(up.getAttribute("groups")!=null)
					groups = (List<String>)up.getAttribute("groups");
			}
		} catch (Exception e) {
			ServiceUtils.printException("Failed while get token from UserProfile", e);
		}
		// String groups[] = { "Guest" };
		if(groups.size()==0) {
			groups.add("guest");
			groups.add("default");
		}
		profile.put("groups", groups);
		profile.put("UUID", UUID);
		profile.put("name", name);
		profile.put("userId", userId);
		profile.put("email", email);
		profile.put("creationTimestamp", creationTimestamp);
		if(tenant!=null)
			profile.put("tenant",tenant);
		return profile;
	}

	public static final UserProfile SYSTEM_PROFILE = new UserProfile() {

		@Override
		public String getId() {
			// TODO Auto-generated method stub
			return "SYSTEM";
		}

		@Override
		public void setId(String id) {
			// TODO Auto-generated method stub

		}

		@Override
		public String getTypedId() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getUsername() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Object getAttribute(String name) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Map<String, Object> getAttributes() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean containsAttribute(String name) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void addAttribute(String key, Object value) {
			// TODO Auto-generated method stub

		}

		@Override
		public void removeAttribute(String key) {
			// TODO Auto-generated method stub

		}

		@Override
		public void addAuthenticationAttribute(String key, Object value) {
			// TODO Auto-generated method stub

		}

		@Override
		public void removeAuthenticationAttribute(String key) {
			// TODO Auto-generated method stub

		}

		@Override
		public void addRole(String role) {
			// TODO Auto-generated method stub

		}

		@Override
		public void addRoles(Collection<String> roles) {
			// TODO Auto-generated method stub

		}

		@Override
		public Set<String> getRoles() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void addPermission(String permission) {
			// TODO Auto-generated method stub

		}

		@Override
		public void addPermissions(Collection<String> permissions) {
			// TODO Auto-generated method stub

		}

		@Override
		public Set<String> getPermissions() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean isRemembered() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void setRemembered(boolean rme) {
			// TODO Auto-generated method stub

		}

		@Override
		public String getClientName() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setClientName(String clientName) {
			// TODO Auto-generated method stub

		}

		@Override
		public String getLinkedId() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setLinkedId(String linkedId) {
			// TODO Auto-generated method stub

		}

		@Override
		public boolean isExpired() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public Principal asPrincipal() {
			// TODO Auto-generated method stub
			return null;
		}
	};

}
