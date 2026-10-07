package com.mulino.application.work;
import com.mulino.application.core.DomainContext;
import java.util.Map;
/** Transactional runtime due-index projection hook; never changes Work state itself. */
public interface WorkLifecycleObserver {void changed(DomainContext context,Map<String,Object> work);}
