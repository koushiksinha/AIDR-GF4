package qa.qcri.aidr.predict.classification;

import java.util.ArrayList;
import java.util.LinkedList;

import qa.qcri.aidr.predict.DataStore;
import qa.qcri.aidr.predict.common.Config;
import qa.qcri.aidr.predict.common.PipelineProcess;
import qa.qcri.aidr.predict.common.RateLimiter;
import qa.qcri.aidr.predict.data.Document;
import qa.qcri.aidr.predict.featureextraction.WordSet;

/**
 * LabelingTaskWriter consumes fully classified items and writes them to the
 * task buffer in the database for human annotation. If more documents are
 * available than what can reasonably be processed by humans, the documents are
 * discarded.
 * 
 * @author jrogstadius
 */
public class LabelingTaskWriter extends PipelineProcess {

    class DocumentHistory {
        LinkedList<WordSet> recentWordVectors = new LinkedList<WordSet>();
        int bufferSize = 50;

        public boolean addItemIfNovel(Document doc) {
            WordSet w1 = doc.getFeatures(WordSet.class).get(0);

            double maxSim = 0;
            for (WordSet w2 : recentWordVectors) {
                double sim = w2.getSimilarity(w1);
                if (sim > maxSim) {
                    if (sim > 0.5) // TODO: This threshold needs some tuning,
                                   // probably
                        return false;
                    maxSim = sim;
                }
            }

            recentWordVectors.add(w1);
            if (recentWordVectors.size() > bufferSize)
                recentWordVectors.remove();

            return true;
        }
    }

    long lastDBWrite = 0;
    public long writeCount = 0;
    ArrayList<Document> writeBuffer = new ArrayList<Document>();
    RateLimiter taskRateLimiter = new RateLimiter(
            Config.MAX_NEW_TASKS_PER_MINUTE);
    DocumentHistory history = new DocumentHistory();

    protected void processItem(Document item) {
        // Write novel DocumentSets to the database at a maximum rate of up to N
        // items per minute
        if (item.hasHumanLabels()
                || (!taskRateLimiter.isLimited() && item.isNovel() && history
                        .addItemIfNovel(item))) {
            // log(LogLevel.INFO, "LabelingTaskWriter recieved an item");
            save(item);
            taskRateLimiter.logEvent();
        }
    }

    void save(Document item) {
        writeBuffer.add(item);

        if (!isWriteRateLimited()) {
            writeToDB();
        }
    }

    @Override
    protected void idle() {
        if (writeBuffer.size() > 0)
            writeToDB();
    }

    void writeToDB() {
        // log(LogLevel.INFO, "Writing " + writeBuffer.size() +
        // " tasks/labeled items from pipeline to DB");
        DataStore.saveDocumentsToDatabase(writeBuffer);
        writeCount += writeBuffer.size(); 
        writeBuffer.clear();
        DataStore
                .truncateLabelingTaskBuffer(Config.LABELING_TASK_BUFFER_MAX_LENGTH);
        lastDBWrite = System.currentTimeMillis();
    }

    boolean isWriteRateLimited() {
        return (System.currentTimeMillis() - lastDBWrite) < Config.MAX_TASK_WRITE_FQ_MS;
    }
}
