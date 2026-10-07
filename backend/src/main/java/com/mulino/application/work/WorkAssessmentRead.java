package com.mulino.application.work;
import com.mulino.application.core.DomainContext;
import java.util.*;
/** Read-only module port, scoped by the same authorized Work. */
public interface WorkAssessmentRead {List<Map<String,Object>> snapshots(DomainContext context,String workId);}
