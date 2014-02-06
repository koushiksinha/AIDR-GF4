package qa.qcri.aidr.trainer.api.template;

import qa.qcri.aidr.trainer.api.entity.Crisis;
import qa.qcri.aidr.trainer.api.entity.ModelFamily;
import qa.qcri.aidr.trainer.api.entity.NominalAttribute;
import qa.qcri.aidr.trainer.api.entity.NominalLabel;

import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: jilucas
 * Date: 9/23/13
 * Time: 8:55 AM
 * To change this template use File | Settings | File Templates.
 */
public class CrisisJsonOutput {

    public CrisisJsonModel crisisJsonModelGenerator(Crisis crisis){
        CrisisJsonModel crisisJsonModel = new CrisisJsonModel();
        crisisJsonModel.setCrisisID(crisis.getCrisisID());
        crisisJsonModel.setCode(crisis.getCode());
        crisisJsonModel.setName(crisis.getName());

        Set<ModelFamily> modelFamilySet =  crisis.getModelFamilySet();
        Set<NominalAttributeJsonModel> nominalAttributeJsonModelSetTemp = new HashSet <NominalAttributeJsonModel>();
        for(ModelFamily obj : modelFamilySet){
            if(obj.isActive()){
                NominalAttributeJsonModel nominalAttributeJsonModel = new NominalAttributeJsonModel();

                NominalAttribute nominalAttribute= obj.getNominalAttribute();
                Set<NominalLabel> nominalLabelSet = nominalAttribute.getNominalLabelSet();

                nominalAttributeJsonModel.setCode(nominalAttribute.getCode());
                nominalAttributeJsonModel.setName(nominalAttribute.getName());
                nominalAttributeJsonModel.setDescription(nominalAttribute.getDescription());
                nominalAttributeJsonModel.setNominalAttributeID(nominalAttribute.getNominalAttributeID());

                Set<NominalLabelJsonModel> nominalLabelJsonModelSetTemp = new HashSet <NominalLabelJsonModel>();
                for(NominalLabel nominalLabel : nominalLabelSet){

                    NominalLabelJsonModel nominalLabelJsonModel = new NominalLabelJsonModel();
                    nominalLabelJsonModel.setName(nominalLabel.getName());
                    nominalLabelJsonModel.setNorminalLabelCode(nominalLabel.getNorminalLabelCode());
                    nominalLabelJsonModel.setNorminalLabelID(nominalLabel.getNorminalLabelID());
                    nominalLabelJsonModel.setDescription(nominalLabel.getDescription());
                    nominalLabelJsonModelSetTemp.add(nominalLabelJsonModel) ;

                }
                nominalAttributeJsonModel.setNominalLabelJsonModelSet(nominalLabelJsonModelSetTemp);
                nominalAttributeJsonModelSetTemp.add(nominalAttributeJsonModel);
            }
        }
        crisisJsonModel.setNominalAttributeJsonModelSet(nominalAttributeJsonModelSetTemp);

        return crisisJsonModel;
    }
}
