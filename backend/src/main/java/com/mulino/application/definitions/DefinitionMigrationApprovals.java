package com.mulino.application.definitions;
import com.mulino.application.core.DomainContext;
import java.util.Map;
/** Plan §8 explicit Work migration: the definitions module owns the WORK_MIGRATION review/approval; the work module applies it. */
public interface DefinitionMigrationApprovals {
  /** The current, unconsumed APPROVED WORK_MIGRATION approval of the target package naming this Work and migration; its goalChanges (may be empty). */
  Map<String,Object> requireMigrationApproval(DomainContext context,String approvalId,String targetDefinitionId,String workId,String migrationId);
  /** Consumes the approval (ONE_MIGRATION) and records the Work's old and new definition/goal in the same transaction. */
  void recordMigration(DomainContext context,String approvalId,String workId,String migrationId,String previousDefinitionId,String definitionId,String previousGoalVersionId,String goalVersionId,String reason);
}
