package qa.qcri.aidr.trainer.api.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: jilucas
 * Date: 9/20/13
 * Time: 2:35 PM
 * To change this template use File | Settings | File Templates.
 */
@Entity
@Table(catalog = "aidr_predict",name = "users")
public class Users implements Serializable {

    private static final long serialVersionUID = -5527566248002296042L;

    public Long getUserID() {
        return userID;
    }

    public void setUserID(Long userID) {
        this.userID = userID;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Id
    @Column(name = "userID")
    private Long userID;

    @Column (name = "name", nullable = false)
    private String name;

    @Column (name = "role", nullable = false)
    private String role;

}
