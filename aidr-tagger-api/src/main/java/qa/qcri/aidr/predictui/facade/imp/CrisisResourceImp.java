/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package qa.qcri.aidr.predictui.facade.imp;

import java.util.ArrayList;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import qa.qcri.aidr.predictui.entities.Crisis;
import qa.qcri.aidr.predictui.entities.ModelFamily;
import qa.qcri.aidr.predictui.entities.NominalLabel;
import qa.qcri.aidr.predictui.entities.Users;
import qa.qcri.aidr.predictui.facade.CrisisResourceFacade;

/**
 *
 * @author Imran
 */
@Stateless
public class CrisisResourceImp implements CrisisResourceFacade {

    @PersistenceContext(unitName = "qa.qcri.aidr.predictui-EJBS")
    private EntityManager em;

    public Crisis addCrisis(Crisis crisis) {
        em.persist(crisis);
        return crisis;
    }

    public Crisis getCrisisByID(int id) {
        Crisis crisis = null;
        Query query = em.createNamedQuery("Crisis.findByCrisisID", Crisis.class);
        query.setParameter("crisisID", id);
        if (query.getSingleResult() != null) {
            crisis = (Crisis) query.getSingleResult();
        }
        return crisis;
    }

    public Crisis getCrisisByCode(String code) {
        Query query = em.createNamedQuery("Crisis.findByCode", Crisis.class);
        query.setParameter("code", code);
        return query.getSingleResult() != null ? (Crisis) query.getSingleResult() : null;
    }

    public Crisis editCrisis(Crisis crisis) {
        Crisis cr = em.find(Crisis.class, crisis.getCrisisID());
        if (cr != null) {
            cr = em.merge(cr);
            if (crisis.getCrisisType() != null) {
                cr.setCrisisType(crisis.getCrisisType());
            }
            if (crisis.getCode() != null && crisis.getCode().length() > 0) {
                cr.setCode(crisis.getCode());
            }
            if (crisis.getName() != null && crisis.getName().length() > 0) {
                cr.setName(crisis.getName());
            }
            return cr;
        } else {
            throw new RuntimeException("Not found");
        }
    }

    public List<Crisis> getAllCrisis() {
        List<Crisis> crisisList = new ArrayList<Crisis>();
        Query q = em.createNamedQuery("Crisis.findAll", Crisis.class);
        crisisList = q.getResultList();
        //for getting attributes for individual crisis
            for (Crisis crisis: crisisList){
                Query attributeQuery = em.createNamedQuery("ModelFamily.findByCrisis", ModelFamily.class );
                attributeQuery.setParameter("crisis", crisis);
                crisis.setModelFamilyCollection(attributeQuery.getResultList());
            }
        return crisisList;
    }

    public List<Crisis> getAllCrisisByUserID(int userID){
        List<Crisis> crisisList = null;
        try {
        Query userQuery = em.createNamedQuery("Users.findByUserID", Users.class);
        userQuery.setParameter("userID", userID);
        
        if (!(userQuery.getResultList().isEmpty())) {
            Users user = (Users) userQuery.getSingleResult();
            Query crisisQuery = em.createNamedQuery("Crisis.findByUserID", Crisis.class);
            crisisQuery.setParameter("user", user);
            crisisList = crisisQuery.getResultList();
            //for getting attributes for individual crisis
            for (Crisis crisis: crisisList){
                Query attributeQuery = em.createNamedQuery("ModelFamily.findByCrisis", ModelFamily.class );
                attributeQuery.setParameter("crisis", crisis);
                List<ModelFamily> mfList = attributeQuery.getResultList();
                //getting labels for individual attribute
                    for (ModelFamily mf: mfList){
                        Query labelQuery = em.createNamedQuery("NominalLabel.findByNominalAttribute", NominalLabel.class);
                        labelQuery.setParameter("nominalAttribute", mf.getNominalAttribute());
                        mf.getNominalAttribute().setNominalLabelCollection(labelQuery.getResultList());
                    }
                crisis.setModelFamilyCollection(attributeQuery.getResultList());
           }
        }
        }catch (NoResultException e){
            return null;
        }
        return crisisList;
        
    }

    public Integer isCrisisExists(String crisisCode) {
        try{
        Query query = em.createNamedQuery("Crisis.findByCode", Crisis.class);
        query.setParameter("code", crisisCode);
        
        if (query.getSingleResult() != null) {
            Crisis crisis = (Crisis) query.getSingleResult();
            return crisis.getCrisisID();
        }
        }catch(NoResultException e){
            return null;
        }
        return null;
    }

}
