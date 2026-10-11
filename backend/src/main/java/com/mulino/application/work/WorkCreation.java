package com.mulino.application.work;
import com.mulino.application.core.DomainContext;
import java.util.Map;
/** Creates a DRAFT Work with a draft GoalVersion inside the caller's command transaction (plan §5.2 createDraft).
 * The draft uses the createDraft goal form: itemId, kind, ownerId, supervisorId, goal, originalText, conversationRequestId;
 * definitionVersion is the intent's published definition version. Returns the new Work ID. */
public interface WorkCreation {String createDraftWork(DomainContext context,String definitionVersion,Map<String,Object> draft);}
