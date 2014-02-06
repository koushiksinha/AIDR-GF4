/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package qa.qcri.aidr.predictui.facade;

import java.util.List;
import javax.ejb.Local;
import qa.qcri.aidr.predictui.dto.ModelNominalLabelDTO;
import qa.qcri.aidr.predictui.entities.ModelNominalLabel;

/**
 *
 * @author Imran
 */
@Local
public interface ModelNominalLabelFacade {
    
    public List<ModelNominalLabel> getAllModelNominalLabels();
    public List<ModelNominalLabelDTO> getAllModelNominalLabelsByModelID(int modelID);
    
}
