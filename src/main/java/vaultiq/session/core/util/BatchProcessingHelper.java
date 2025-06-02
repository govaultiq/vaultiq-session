package vaultiq.session.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;

/**
 * Helper class for batch processing database operations.
 * Provides utilities for paginated deletion and other batch operations.
 */
public class BatchProcessingHelper {
    private static final Logger log = LoggerFactory.getLogger(BatchProcessingHelper.class);
    
    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final int DEFAULT_MAX_BATCHES = 100; // Safety limit
    
    private BatchProcessingHelper() {
        // Utility class, no instantiation
    }
    
    /**
     * Performs a batch delete operation using pagination to avoid memory issues.
     *
     * @param <T> the entity type
     * @param <ID> the entity ID type
     * @param findFunction a function that returns entities to delete for a given page
     * @param repository the JPA repository to use for deletion
     * @param entityType a description of the entity type (for logging)
     * @param batchSize the number of records to process in each batch
     * @param maxBatches maximum number of batches to process (safety limit)
     * @return the total number of records deleted
     */
    @Transactional
    public static <T, ID> int batchDelete(
            Function<Pageable, List<T>> findFunction,
            JpaRepository<T, ID> repository,
            String entityType,
            int batchSize,
            int maxBatches) {
        
        int totalDeleted = 0;
        int batchCount = 0;
        List<T> batch;
        
        try {
            do {
                // Get a page of entities to delete
                Pageable pageable = PageRequest.of(0, batchSize);
                batch = findFunction.apply(pageable);
                
                if (!batch.isEmpty()) {
                    // Delete the batch
                    repository.deleteAll(batch);
                    totalDeleted += batch.size();
                    batchCount++;
                    
                    log.debug("Deleted batch {} with {} {}", batchCount, batch.size(), entityType);
                }
                
                // Safety check to prevent infinite loops
                if (batchCount >= maxBatches && !batch.isEmpty()) {
                    log.warn("Reached maximum batch count ({}). Some records may not have been processed.", maxBatches);
                    break;
                }
            } while (!batch.isEmpty());
            
            log.info("Completed batch deletion of {}. Deleted {} records in {} batches", 
                    entityType, totalDeleted, batchCount);
            
            return totalDeleted;
        } catch (Exception e) {
            log.error("Error during batch deletion of {}", entityType, e);
            throw e;
        }
    }
    
    /**
     * Performs a batch delete operation with default batch size and max batches.
     *
     * @param <T> the entity type
     * @param <ID> the entity ID type
     * @param findFunction a function that returns entities to delete for a given page
     * @param repository the JPA repository to use for deletion
     * @param entityType a description of the entity type (for logging)
     * @return the total number of records deleted
     */
    @Transactional
    public static <T, ID> int batchDelete(
            Function<Pageable, List<T>> findFunction,
            JpaRepository<T, ID> repository,
            String entityType) {
        return batchDelete(findFunction, repository, entityType, DEFAULT_BATCH_SIZE, DEFAULT_MAX_BATCHES);
    }
    
    /**
     * Calculates a cutoff time based on the current time minus the retention period.
     *
     * @param retentionPeriod the retention period
     * @return the cutoff time
     */
    public static Instant calculateCutoffTime(Duration retentionPeriod) {
        return Instant.now().minus(retentionPeriod);
    }
}