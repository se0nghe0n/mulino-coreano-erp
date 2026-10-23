package com.mulino.application.work;
import com.mulino.application.core.DomainContext;
import java.util.*;
/** Each row is one disjoint actual occurrence credit range, never a copied child result. */
/** One immutable allocation ID, viewed from its pinned SOURCE or TARGET goal.
 * Raw goalId/targetWorkId always retain their stored target meaning. A legacy
 * source without a pin is UNKNOWN and cannot establish a fulfilled quantity. */
public interface WorkContributionRead {List<Map<String,Object>> contributions(DomainContext context,String workId,String goalId);}
