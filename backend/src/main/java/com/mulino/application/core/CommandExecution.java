package com.mulino.application.core;
import org.springframework.transaction.support.TransactionSynchronizationManager;
/** Transaction-bound command ID for same-transaction outbox/effect references. */
public final class CommandExecution {
 private static final ThreadLocal<String> CURRENT=new ThreadLocal<>();
 private CommandExecution(){}
 public static String commandId(){String id=CURRENT.get();if(id==null||!TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("No active command transaction");return id;}
 static String enter(String id){String previous=CURRENT.get();CURRENT.set(id);return previous;}
 static void exit(String previous){if(previous==null)CURRENT.remove();else CURRENT.set(previous);}
}
