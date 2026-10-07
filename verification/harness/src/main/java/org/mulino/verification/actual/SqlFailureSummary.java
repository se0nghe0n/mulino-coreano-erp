package org.mulino.verification.actual;

import java.sql.SQLException;
import org.postgresql.util.PSQLException;

/** Error identity only: never serialize server messages, SQL, detail, hints, or parameters. */
public final class SqlFailureSummary {
    public static String safe(Throwable failure) {
        Throwable cause=failure;
        for(int i=0;cause!=null&&i<20;i++,cause=cause.getCause())if(cause instanceof SQLException sql) {
            String summary=sql.getClass().getSimpleName()+" SQLSTATE="+identifier(sql.getSQLState());
            if(sql instanceof PSQLException pg && pg.getServerErrorMessage()!=null) {
                var server=pg.getServerErrorMessage();
                summary+=" table="+identifier(server.getTable())+" constraint="+identifier(server.getConstraint())+" column="+identifier(server.getColumn());
            }
            return summary;
        }
        return failure.getClass().getSimpleName();
    }
    private static String identifier(String value){return value!=null&&value.matches("[A-Za-z0-9_]{1,100}")?value:"UNKNOWN";}
    private SqlFailureSummary() {}
}
