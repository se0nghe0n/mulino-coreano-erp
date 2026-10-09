package org.mulino.verification.actual;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RESULT_REVISION support for ./verify scenarios --actual without re-issuing the product query.
 *
 * The product snapshotRevision is an opaque SHA-256 over its own projection; recomputing it would mean re-implementing
 * the projection in the harness, which is neither independent nor feasible. Instead the observer proves, from rows it
 * reads itself, that the database state behind the issued revision is the state it observes:
 *
 * 1. Around every api/mcp query the driver computes an independent row digest of every business table of the installed
 *    organizations (all columns, all rows, ordered) immediately before and after the call. Only when both digests are
 *    equal (no concurrent change) is the product revision bound to that digest.
 * 2. The observer computes the same digest inside its own REPEATABLE READ snapshot, the transaction that reads the raw
 *    rows. When it equals the bound digest, the rows it reports are exactly the rows the product read, and it reports the
 *    bound revision; otherwise it reports "row-digest:&lt;digest&gt;", which the harness sees as a mismatch.
 *
 * The observer never receives the revision from the request and never calls the product API. What is not proven: that
 * the product's projection of those rows is correct (that is what the case assertions check), and runtime tables written
 * by background workers (mulino_runtime_*) are outside the digest.
 */
final class ScenarioRevisionBinding {
    record Binding(String digest,String revision,String organizations) {}
    private static final Map<String,Binding> BINDINGS=new ConcurrentHashMap<>();
    private static volatile List<String> organizations=List.of();
    private static volatile String digestSql;
    private ScenarioRevisionBinding() {}

    /** A new installed fixture is an isolation boundary. */
    static void reset(Collection<String> installedOrganizations) {BINDINGS.clear();organizations=List.copyOf(new TreeSet<>(installedOrganizations));}
    static List<String> organizations() {return organizations;}

    static String digest(ActualConfiguration config) throws SQLException {
        try(var c=DriverManager.getConnection(config.jdbcUrl(),config.username(),config.password())) {
            c.setAutoCommit(false);c.setReadOnly(true);c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            String d=digest(c);c.commit();return d;
        }
    }
    static void bind(ActualConfiguration config,String actionId,String before,String revision) throws SQLException {
        String after=digest(config);
        if(before.equals(after))BINDINGS.put(actionId,new Binding(after,revision,String.join(",",organizations)));
        else BINDINGS.remove(actionId);
    }
    static Binding binding(String actionId) {return BINDINGS.get(actionId);}

    /** Digest of every row of every organization-scoped business table, computed in the caller's transaction. */
    static String digest(Connection c) throws SQLException {
        if(organizations.isEmpty())return "no-organization";
        String sql=digestSql;
        if(sql==null) {
            List<String> tables=new ArrayList<>();
            try(var s=c.prepareStatement("SELECT table_name FROM information_schema.columns WHERE table_schema=current_schema() AND column_name='organizationid' AND table_name LIKE 'mulino\\_%' AND table_name NOT LIKE 'mulino\\_runtime\\_%' ORDER BY table_name");var r=s.executeQuery()){while(r.next())tables.add(r.getString(1));}
            try(var s=c.prepareStatement("SELECT table_name FROM information_schema.tables WHERE table_schema=current_schema() AND table_type='BASE TABLE'");var r=s.executeQuery()){Set<String> base=new HashSet<>();while(r.next())base.add(r.getString(1));tables.retainAll(base);}
            var parts=new ArrayList<String>();
            for(String t:tables){if(!t.matches("[a-z0-9_]+"))continue;parts.add("SELECT '"+t+"'||':'||md5(coalesce(string_agg(x::text,'|' ORDER BY x::text),'')) d FROM "+t+" x WHERE x.organizationid::text = ANY(?)");}
            sql="SELECT md5(string_agg(d,'|' ORDER BY d)) FROM ("+String.join(" UNION ALL ",parts)+") t";
            digestSql=sql;
        }
        int n=sql.split("ANY\\(\\?\\)",-1).length-1;
        try(var s=c.prepareStatement(sql)) {
            var array=c.createArrayOf("text",organizations.toArray());
            for(int i=1;i<=n;i++)s.setArray(i,array);
            try(var r=s.executeQuery()){r.next();return r.getString(1);}
        }
    }
}
