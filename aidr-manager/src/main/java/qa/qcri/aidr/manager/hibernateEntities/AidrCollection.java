package qa.qcri.aidr.manager.hibernateEntities;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import qa.qcri.aidr.manager.util.CollectionStatus;
import qa.qcri.aidr.manager.util.JsonDateDeSerializer;
import qa.qcri.aidr.manager.util.JsonDateSerializer;

@Entity
@Table(name = "AIDR_COLLECTION")
public class AidrCollection implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4720813686204397970L;
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
    
	@Column(length=64, name="code", unique=true)
	private String code;
	private String name;
	private String target;
	@OneToOne(targetEntity=UserEntity.class,cascade=CascadeType.ALL)
	@JoinColumn(name="user_id",referencedColumnName="id")
	private UserEntity user;
    private Integer count;
    private CollectionStatus status;
    @Column(length=1000,name="track")
    private String track;
    private String follow;
    private String geo;
    private String langFilters;
    private Date startDate;
    private Date endDate;
    private Date createdDate;
    @Column(length=1000,name="last_document")
    private String lastDocument;
	
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public UserEntity getUser() {
		return user;
	}

	public void setUser(UserEntity user) {
		this.user = user;
	}

	public Integer getCount() {
		return count;
	}

	public void setCount(Integer count) {
		this.count = count;
	}

	public CollectionStatus getStatus() {
		return status;
	}

	public void setStatus(CollectionStatus status) {
		this.status = status;
	}

	public String getTrack() {
		return track;
	}

	public void setTrack(String track) {
		this.track = track;
	}

	public String getFollow() {
		return follow;
	}

	public void setFollow(String follow) {
		this.follow = follow;
	}

	public String getGeo() {
		return geo;
	}

	public void setGeo(String geo) {
		this.geo = geo;
	}

	@JsonSerialize(using=JsonDateSerializer.class)
	public Date getStartDate() {
		return startDate;
	}

	@JsonDeserialize(using=JsonDateDeSerializer.class)
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	@JsonSerialize(using=JsonDateSerializer.class)
	public Date getEndDate() {
		return endDate;
	}

	@JsonDeserialize(using=JsonDateDeSerializer.class)
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	@JsonSerialize(using=JsonDateSerializer.class)
	public Date getCreatedDate() {
		return createdDate;
	}

	@JsonDeserialize(using=JsonDateDeSerializer.class)
	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}

	public String getLastDocument() {
		return lastDocument;
	}

	public void setLastDocument(String lastDocument) {
		this.lastDocument = lastDocument;
	}

    /**
     * @return the langFilter
     */
    public String getLangFilters() {
        return langFilters;
    }

    /**
     * @param langFilter the langFilter to set
     */
    public void setLangFilters(String langFilter) {
        this.langFilters = langFilter;
    }

}
