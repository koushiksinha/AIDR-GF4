package qa.qcri.aidr.manager.hibernateEntities;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "AIDR_USER")
public class UserEntity implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 3485170416057248803L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(name="user_name")
	private String userName;

	private String provider;

	@OneToMany(fetch=FetchType.EAGER)
    @JoinTable(
	      name="USER_ROLE",
	      joinColumns={ @JoinColumn(name="user") },
	      inverseJoinColumns={ @JoinColumn(name="role") }
	)
	private List<Role> roles;
	
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public List<Role> getRoles() {
		return roles;
	}

	public void setRoles(List<Role> roles) {
		this.roles = roles;
	}

}
