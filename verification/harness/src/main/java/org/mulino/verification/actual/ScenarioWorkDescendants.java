package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.node.*;
import org.mulino.verification.Json;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.*;

/** Descendants are directed persisted FOLLOWUP links, never all organization Works. */
final class ScenarioWorkDescendants {
    record Expansion(List<String> ids,ObjectNode query) {}
    static Expansion expand(Connection connection,List<String> organizations,List<String> roots,OffsetDateTime knownAt) throws SQLException {
        // A flat union of IDs from several organizations could widen another organization's scope.
        if(organizations.size()!=1)throw new UnsupportedOperationException("Descendant observation requires one explicit organization");
        if(roots.isEmpty())throw new IllegalArgumentException("Descendant observation requires explicit Work roots");
        String in=String.join(",",Collections.nCopies(roots.size(),"?"));
        String sql="WITH RECURSIVE selected(ID) AS ("
            +" SELECT w.ID FROM mulino_work_read_Works w WHERE w.organizationId=? AND w.ID IN ("+in+") AND w.createdAt<=? AND w.recordedAt<=?"
            +" UNION SELECT child.ID FROM selected parent"
            +" JOIN mulino_work_WorkLinks link ON link.organizationId=? AND link.sourceWorkId=parent.ID AND link.kind='FOLLOWUP' AND link.createdAt<=?"
            +" JOIN mulino_work_read_Works original ON original.organizationId=link.organizationId AND original.ID=parent.ID"
            +" JOIN mulino_work_read_Works child ON child.organizationId=link.organizationId AND child.ID=link.targetWorkId"
            +" AND child.itemId=original.itemId AND child.createdAt<=? AND child.recordedAt<=?"
            +") SELECT ID FROM selected ORDER BY ID LIMIT 10001";
        var parameters=new ArrayList<Object>();parameters.add(organizations.getFirst());parameters.addAll(roots);
        parameters.add(knownAt);parameters.add(knownAt);parameters.add(organizations.getFirst());
        parameters.add(knownAt);parameters.add(knownAt);parameters.add(knownAt);
        var ids=new ArrayList<String>();
        try(var statement=connection.prepareStatement(sql)) {
            for(int n=0;n<parameters.size();n++)statement.setObject(n+1,parameters.get(n));
            try(var rows=statement.executeQuery()){while(rows.next())ids.add(rows.getString(1));}
        }
        if(ids.size()>10000)throw new UnsupportedOperationException("Descendant scope exceeds bounded complete observation");
        // Empty/unknown roots are unavailable evidence, not an empty complete scope.
        if(!ids.containsAll(roots))throw new UnsupportedOperationException("Descendant root Work unavailable at knownAt");
        var proof=Json.object().put("table","mulino_work_WorkLinks").put("recipe","FOLLOWUP_DESCENDANT_SCOPE_V1").put("sql",sql);
        proof.set("boundValues",Json.MAPPER.valueToTree(parameters.stream().map(Object::toString).toList()));
        proof.set("rootWorkIds",Json.MAPPER.valueToTree(roots));proof.set("selectedWorkIds",Json.MAPPER.valueToTree(ids));
        return new Expansion(List.copyOf(ids),proof);
    }
    private ScenarioWorkDescendants() {}
}
