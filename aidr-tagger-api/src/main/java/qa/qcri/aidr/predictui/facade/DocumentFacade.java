package qa.qcri.aidr.predictui.facade;

import java.util.List;
import javax.ejb.Local;
import qa.qcri.aidr.predictui.entities.Document;

/**
 *
 * @author Imran
 */
@Local
public interface DocumentFacade {
    
    public List<Document> getAllDocuments();
    public Document getDocumentByID(long id);
    public List<Document> getAllLabeledDocumentbyCrisisID(long crisisID, long attributeID);
    public void deleteDocument(Long documentID);
    public void removeTrainingExample(Long documentID);

}
