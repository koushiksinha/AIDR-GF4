package qa.qcri.aidr.manager.social.security;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.socialsignin.springsocial.security.api.SpringSocialProfile;
import org.socialsignin.springsocial.security.signin.SpringSocialSecurityAuthenticationFactory;
import org.socialsignin.springsocial.security.signup.SignUpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.UsersConnectionRepository;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.MultiValueMap;

import qa.qcri.aidr.manager.hibernateEntities.Role;
import qa.qcri.aidr.manager.hibernateEntities.UserEntity;
import qa.qcri.aidr.manager.service.UserService;

@Repository
@Service
@Qualifier("springSocialUserDetailsService")
public class SpringSocialUserDetailService implements UserDetailsService {

	@Autowired
	private UsersConnectionRepository usersConnectionRepository;

	@Autowired
	private SpringSocialSecurityAuthenticationFactory authenticationFactory;
	
	@Autowired
	private SignUpService<?> signUpService;
	
	@Resource(name="userService")
	private UserService userService;
	
	@Override
	public UserDetails loadUserByUsername(String userName)throws UsernameNotFoundException {
		ConnectionRepository connectionRepository = usersConnectionRepository.createConnectionRepository(userName);
		SpringSocialProfile springSocialProfile = signUpService.getUserProfile(userName);
		List<Connection<?>> allConnections = getConnections(connectionRepository,userName);
		if (allConnections.size() > 0) {
				Authentication authentication = authenticationFactory.createAuthenticationForAllConnections(userName,
								springSocialProfile.getPassword(),allConnections);
				UserEntity userEntity =  userService.fetchByUserName(userName);
				List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
				authorities.addAll(authentication.getAuthorities());
				if(userEntity.getRoles()!=null && !userEntity.getRoles().isEmpty() ){
					for(Role role : userEntity.getRoles()){
					   GrantedAuthority authority = new SimpleGrantedAuthority(role.getName());
					   authorities.add(authority);
					}
				}
				return new User(userName, authentication.getCredentials().toString(), true, true, true, true,authorities);
		
		} else {
			throw new UsernameNotFoundException(userName);
		}
	}
	
	private List<Connection<?>> getConnections(ConnectionRepository connectionRepository,String userName)
	{
		MultiValueMap<String, Connection<?>> connections = connectionRepository.findAllConnections();
		List<Connection<?>> allConnections = new ArrayList<Connection<?>>();
		if (connections.size() > 0) {
			for (List<Connection<?>> connectionList : connections.values()) {
				for (Connection<?> connection : connectionList) {
					allConnections.add(connection);
				}
			}
		}
		return allConnections;
	}

}
