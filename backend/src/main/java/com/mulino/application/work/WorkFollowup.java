package com.mulino.application.work;
import com.mulino.application.core.DomainContext;
import java.time.Instant;
import java.util.Map;
/** A stable followup for a new source revision; the original Work stays closed. */
public interface WorkFollowup {Map<String,Object> ensure(DomainContext context,String originalWorkId,String sourceId,String reason,String nextAction,Instant nextCheckAt);}
