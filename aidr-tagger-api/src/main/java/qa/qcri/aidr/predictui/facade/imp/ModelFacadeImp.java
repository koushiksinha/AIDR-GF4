/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package qa.qcri.aidr.predictui.facade.imp;

import java.util.ArrayList;
import java.util.Collection;

import qa.qcri.aidr.predictui.dto.ModelHistoryWrapper;
import qa.qcri.aidr.predictui.facade.*;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import qa.qcri.aidr.predictui.entities.Crisis;
import qa.qcri.aidr.predictui.entities.Model;
import qa.qcri.aidr.predictui.entities.ModelFamily;
import qa.qcri.aidr.predictui.entities.ModelNominalLabel;
import qa.qcri.aidr.predictui.entities.NominalAttribute;
import qa.qcri.aidr.predictui.dto.ModelWrapper;
import qa.qcri.aidr.predictui.entities.Document;
import qa.qcri.aidr.predictui.entities.NominalLabel;

/**
 *
 * @author Imran
 */
@Stateless
public class ModelFacadeImp implements ModelFacade {

    @PersistenceContext(unitName = "qa.qcri.aidr.predictui-EJBS")
    private EntityManager em;

    public List<Model> getAllModels() {
        Query query = em.createNamedQuery("Model.findAll", Model.class);
        List<Model> modelsList = query.getResultList();
        return modelsList;

    }

    public Model getModelByID(int id) {
        Model model = null;
        try {
            Query query = em.createNamedQuery("Model.findByModelID", Model.class);
            query.setParameter("modelID", id);
            model = (Model) query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
        return model;
    }

    public Integer getModelCountByModelFamilyID(int modelFamilyID) {
        String sqlCount = " SELECT count(*) "
                + " FROM model m "
                + " WHERE m.modelFamilyID = :modelFamilyID ";

        Query queryCount = em.createNativeQuery(sqlCount);
        queryCount.setParameter("modelFamilyID", modelFamilyID);
        Object res = queryCount.getSingleResult();

        return Integer.parseInt(res.toString());
    }

    public List<ModelHistoryWrapper> getModelByModelFamilyID(int modelFamilyID, Integer start, Integer limit) {
        ModelFamily modelFamily = em.find(ModelFamily.class, modelFamilyID);
        Query query = em.createNamedQuery("Model.findByModelFamilyID", Model.class);
        query.setParameter("modelFamily", modelFamily);
        query.setFirstResult(start);
        query.setMaxResults(limit);
        List<Model> modelList = query.getResultList();

        List<ModelHistoryWrapper> modelWrapperList = new ArrayList<ModelHistoryWrapper>();
        if (modelList.size() > 0){
            for (Model model : modelList) {
                ModelHistoryWrapper wrapper = new ModelHistoryWrapper();

                wrapper.setModelID(model.getModelID());
                wrapper.setAvgPrecision(model.getAvgPrecision());
                wrapper.setAvgRecall(model.getAvgRecall());
                wrapper.setAvgAuc(model.getAvgAuc());
                wrapper.setTrainingCount(model.getTrainingCount());
                wrapper.setTrainingTime(model.getTrainingTime());

                modelWrapperList.add(wrapper);
            }
        }

        return modelWrapperList;
    }

    public List<ModelWrapper> getModelByCrisisID(int crisisID) {
        List<ModelWrapper> modelWrapperList = new ArrayList<ModelWrapper>();
        Crisis crisis = em.find(Crisis.class, crisisID);
        Collection<ModelFamily> modelFamilyList = crisis.getModelFamilyCollection();
        // for each modelFamily get all the models and take avg
        for (ModelFamily modelFamily : modelFamilyList) {
            Collection<Model> modelList = modelFamily.getModelCollection();
            ModelWrapper modelWrapper = new ModelWrapper();
            modelWrapper.setModelFamilyID(modelFamily.getModelFamilyID());
            long classigiedElements = 0;
            double auc = 0.0;
            double aucAverage = 0.0;
            int modelID = 0;
            long trainingExamples = 0;

            // if size 0 we will get NaN for aucAverage
            if (modelList.size() > 0) {
                for (Model model : modelList) {
                    if (model.getIsCurrentModel()) {
                        auc = model.getAvgAuc();
                        modelID = model.getModelID();

                        //for each model get all the labels and sum over classifiedDocumentCount
                        Collection<ModelNominalLabel> modelLabels = model.getModelNominalLabelCollection();
                        int totalClassifiedDocuments = 0;
                        for (ModelNominalLabel label : modelLabels) {
                            totalClassifiedDocuments += label.getClassifiedDocumentCount();
                        }
                        classigiedElements = totalClassifiedDocuments;

                    }

                }
            }

            //getting trainingCount
            trainingExamples = 0;
            NominalAttribute na = modelFamily.getNominalAttribute();
            Collection<NominalLabel> nlc = na.getNominalLabelCollection();
            for (NominalLabel label : nlc) {
                if (!(label.getNominalLabelCode().equalsIgnoreCase("null"))) {
                    Collection<Document> dc = label.getDocumentCollection();
                    for (Document doc : dc) {
                        if (!doc.getIsEvaluationSet() && doc.getHasHumanLabels()) {
                            trainingExamples++;
                        }
                    }
                }
            }

            modelWrapper.setTrainingExamples(trainingExamples);
            modelWrapper.setAttribute(modelFamily.getNominalAttribute().getName());
            modelWrapper.setAttributeID(modelFamily.getNominalAttribute().getNominalAttributeID());
            modelWrapper.setAuc(auc);
            modelWrapper.setClassifiedDocuments(classigiedElements);
            String status = "";
            if (modelFamily.getIsActive()) {
                status = "Active";
            } else {
                status = "Inactive";
            }
            modelWrapper.setStatus(status);
            modelWrapper.setModelID(modelID);

            modelWrapperList.add(modelWrapper);
        }
        return modelWrapperList;
    }

//    public List<ModelWrapper> getModelByCrisisID(int crisisID) {
//        List<ModelWrapper> modelWrapperList = new ArrayList<ModelWrapper>();
//        Crisis crisis = em.find(Crisis.class, crisisID);
//        Collection<ModelFamily> modelFamilyList = crisis.getModelFamilyCollection();
//        // for each modelFamily get all the models and take avg
//        for (ModelFamily modelFamily : modelFamilyList) {
//            Collection<Model> modelList = modelFamily.getModelCollection();
//            ModelWrapper modelWrapper = new ModelWrapper();
//            modelWrapper.setModelFamilyID(modelFamily.getModelFamilyID());
//            long classigiedElements = 0;
//            double auc = 0.0;
//            double aucAverage = 0.0;
//            int modelID = 0;
//             long trainingExamples = 0;
//
//            // if size 0 we will get NaN for aucAverage
//            if (modelList.size() > 0) {
//                for (Model model : modelList) {
//                    //trainingExamples += model.getTrainingCount();
//                    auc += model.getAvgAuc();
//                    modelID = model.getModelID();
//
//                    //for each model get all the labels and sum over classifiedDocumentCount
//                    Collection<ModelNominalLabel> modelLabels = model.getModelNominalLabelCollection();
//                    int totalClassifiedDocuments = 0;
//                    for (ModelNominalLabel label : modelLabels) {
//                        totalClassifiedDocuments += label.getClassifiedDocumentCount();
//                    }
//                    classigiedElements = totalClassifiedDocuments;
//                }
//                aucAverage = auc / modelList.size();
//            }
//
//            //getting trainingCount
//            trainingExamples = 0;
//            NominalAttribute na = modelFamily.getNominalAttribute();
//            Collection<NominalLabel> nlc = na.getNominalLabelCollection();
//            for (NominalLabel label : nlc) {
//                Collection<Document> dc = label.getDocumentCollection();
//                for (Document doc : dc){
//                    if (!doc.getIsEvaluationSet() && doc.getHasHumanLabels())
//                    trainingExamples ++;
//                }
//            }
//            
//            modelWrapper.setTrainingExamples(trainingExamples);
//            modelWrapper.setAttribute(modelFamily.getNominalAttribute().getName());
//            modelWrapper.setAuc(aucAverage);
//            modelWrapper.setClassifiedDocuments(classigiedElements);
//            String status = "";
//            if (modelFamily.getIsActive()) {
//                status = "Active";
//            } else {
//                status = "Inactive";
//            }
//            modelWrapper.setStatus(status);
//            modelWrapper.setModelID(modelID);
//
//            modelWrapperList.add(modelWrapper);
//        }
//        return modelWrapperList;
//    }
}
