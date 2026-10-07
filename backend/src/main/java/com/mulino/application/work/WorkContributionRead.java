package com.mulino.application.work;
import com.mulino.application.core.DomainContext;
import java.util.*;
/** Each row is one disjoint actual occurrence credit range, never a copied child result. */
public interface WorkContributionRead {List<Map<String,Object>> contributions(DomainContext context,String workId,String goalId);}
