/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package qa.qcri.aidr.predictui.facade.imp;

import qa.qcri.aidr.predictui.dto.TaggersForCodes;
import qa.qcri.aidr.predictui.facade.*;

import java.math.BigInteger;
import java.util.*;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import qa.qcri.aidr.predictui.entities.Crisis;
import qa.qcri.aidr.predictui.entities.ModelFamily;

/**
 *
 * @author Imran
 */
@Stateless
public class ModelFamilyFacadeImp implements ModelFamilyFacade{
    
    @PersistenceContext(unitName = "qa.qcri.aidr.predictui-EJBS")
    private EntityManager em;

    public List<ModelFamily> getAllModelFamilies() {
        Query query = em.createNamedQuery("ModelFamily.findAll", ModelFamily.class);
        List<ModelFamily> modelFamilyList = query.getResultList();
        return modelFamilyList;
        
    }

    public ModelFamily getModelFamilyByID(int id) {
        Query query = em.createNamedQuery("ModelFamily.findByModelFamilyID", ModelFamily.class);
        query.setParameter("modelFamilyID", id);
        ModelFamily modelFamily = (ModelFamily)query.getSingleResult();
        return modelFamily;
    }

    public List<ModelFamily> getAllModelFamiliesByCrisis(int crisisID) {
        Query query = em.createNamedQuery("Crisis.findByCrisisID", Crisis.class);
        query.setParameter("crisisID", crisisID);
        Crisis crisis = (Crisis)query.getSingleResult();
        
        query = em.createNamedQuery("ModelFamily.findByCrisis", ModelFamily.class);
        query.setParameter("crisis", crisis);
        List<ModelFamily> modelFamilyList = query.getResultList();
        
        
        return modelFamilyList;
    }

    public ModelFamily addCrisisAttribute(ModelFamily modelFamily) {
        em.persist(modelFamily);
        return modelFamily;
    }
    
    public void deleteModelFamily(int modelFamilyID){
        ModelFamily mf = em.find(ModelFamily.class, modelFamilyID);
        if (mf != null){
            em.remove(mf);
        }
    }

    public List<TaggersForCodes> getTaggersByCodes(final List<String> codes) {
        List<TaggersForCodes> result = new ArrayList<TaggersForCodes>();

        String sql = "select c.code as code, " +
                " count(mf.modelFamilyID) as modelsCount " +
                " from model_family mf " +
                " right outer join crisis c on c.crisisID = mf.crisisID " +
                " where c.code in :codes " +
                " group by mf.crisisID ";

        Query query = em.createNativeQuery(sql);
        query.setParameter("codes", codes);

        List<Object[]> rows = query.getResultList();
        for (Object[] row : rows) {
            TaggersForCodes taggersForCodes = new TaggersForCodes();
            taggersForCodes.setCode((String) row[0]);
            taggersForCodes.setCount((BigInteger) row[1]);
            result.add(taggersForCodes);
        }

        return result;
    }
    
}
