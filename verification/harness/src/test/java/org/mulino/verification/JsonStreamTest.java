package org.mulino.verification;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** S3-11: the streamed report writer and hash keep the former single-String bytes exactly. */
class JsonStreamTest {
    @TempDir Path dir;

    @Test
    void streamedWriteMatchesFormerStringBytesAndHash() throws Exception {
        ObjectNode report=Json.object();report.put("status","FAIL").put("count",3);
        var cases=report.putArray("cases");for(int i=0;i<200;i++)cases.addObject().put("id","case-"+i).put("text","값 "+i);
        Path file=dir.resolve("nested/report.json");
        Json.write(file,report);
        String expected=Json.MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(report)+"\n";
        assertEquals(expected,Files.readString(file));
        assertEquals(Json.sha256Text(expected),Json.sha256(file));
        var out=new java.io.ByteArrayOutputStream();
        Json.print(out,report);
        assertEquals(expected,out.toString(java.nio.charset.StandardCharsets.UTF_8));
    }
}
