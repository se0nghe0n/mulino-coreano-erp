package com.mulino.application.evidence;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Reuses only the real receipt fixture, without inheriting its test methods. */
@SpringBootTest
@ActiveProfiles("local")
class ReceiptResponseIdentityPostgresTest {
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        ReceiptGatewayPostgresTest.properties(r);
    }
    @Autowired AutowireCapableBeanFactory beans;
    @MockitoBean com.mulino.application.policy.PolicyCommandGuard localPolicy;
    ReceiptGatewayPostgresTest fixture;

    @BeforeEach void setup() throws Exception {
        fixture = new ReceiptGatewayPostgresTest();
        beans.autowireBean(fixture);
        fixture.receiptFixture();
    }
    @AfterEach void clear() throws Exception {
        fixture.clear();
        String output=System.getProperty("receipt.identity.evidence");
        if(output!=null){
            var path=java.nio.file.Path.of(output);java.nio.file.Files.createDirectories(path);
            var proof=Map.of("blobRoot",ReceiptGatewayPostgresTest.BLOBS.toString(),
                "receipts",fixture.jdbc.queryForList("SELECT ID,observationId,canonicalOccurrenceId,contributedQuantity,excessQuantity FROM mulino_trade_receipt_Receipts"),
                "canonicalCount",fixture.count("mulino_evidence_CanonicalOccurrences"),
                "movementCount",fixture.count("mulino_inventory_QuantityMovements"));
            new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules().writeValue(path.resolve("identity-"+fixture.count("mulino_trade_receipt_Receipts")+".json").toFile(),proof);
        }
    }

    @Test void confirmedReplayAndIndependentDuplicateExposeOnlyPersistedCanonicalIdentity() {
        var input=fixture.provisional(ReceiptGatewayPostgresTest.uuid(),"60",true,null);
        var provisional=fixture.jdbc.queryForMap("SELECT ID,state FROM mulino_trade_receipt_Observations WHERE ID=?",input.provisional());
        assertEquals("PROVISIONAL",provisional.get("state"));
        String canonical=fixture.canonical(input,null),key=ReceiptGatewayPostgresTest.uuid();
        var first=fixture.confirm(input,canonical,key);
        assertEquals("APPLIED",first.get("outcome"),first.toString());
        var persisted=fixture.jdbc.queryForMap("SELECT ID,observationId,canonicalOccurrenceId FROM mulino_trade_receipt_Receipts");
        assertEquals(canonical,persisted.get("canonicaloccurrenceid"));
        assertEquals(persisted.get("canonicaloccurrenceid"),first.get("occurrenceId"));
        assertEquals(canonical,((Map<?,?>)first.get("effects")).get("occurrenceId"));
        assertNotEquals(persisted.get("id"),first.get("occurrenceId"));
        assertNotEquals(persisted.get("observationid"),first.get("occurrenceId"));
        assertEquals(first,fixture.confirm(input,canonical,key));
        var secondSource=fixture.provisional(input.root(),"60",true,null);
        String linked=fixture.canonical(secondSource,canonical);
        var duplicate=fixture.confirm(secondSource,linked,ReceiptGatewayPostgresTest.uuid());
        assertEquals("APPLIED",duplicate.get("outcome"),duplicate.toString());
        assertEquals(canonical,duplicate.get("occurrenceId"));
        assertEquals(true,((Map<?,?>)duplicate.get("effects")).get("duplicate"));
        assertEquals("0",((Map<?,?>)first.get("effects")).get("contributedQuantity"));
        assertEquals("60",((Map<?,?>)first.get("effects")).get("excessQuantity"));
        for(String table:java.util.List.of("mulino_trade_receipt_Receipts","mulino_inventory_QuantityMovements","mulino_evidence_CanonicalOccurrences"))assertEquals(1,fixture.count(table),table);
        assertEquals(1,fixture.jdbc.queryForObject("SELECT count(*) FROM mulino_work_read_ObligationReferences WHERE kind='RECEIPT_UNALLOCATED' AND status='OPEN' AND valid",Integer.class));
    }

    @Test void distinctActualReceiptsExposeTwoDistinctCanonicalIds() {
        var first=fixture.provisional(ReceiptGatewayPostgresTest.uuid(),"60",true,null);
        var second=fixture.provisional(ReceiptGatewayPostgresTest.uuid(),"40",true,null);
        String one=fixture.canonical(first,null),two=fixture.canonical(second,null);
        var a=fixture.confirm(first,one,ReceiptGatewayPostgresTest.uuid());
        var b=fixture.confirm(second,two,ReceiptGatewayPostgresTest.uuid());
        assertEquals("APPLIED",a.get("outcome"),a.toString());assertEquals("APPLIED",b.get("outcome"),b.toString());
        assertNotEquals(one,two);assertEquals(one,a.get("occurrenceId"));assertEquals(two,b.get("occurrenceId"));
        assertEquals(java.util.Set.of(one,two),new java.util.HashSet<>(fixture.jdbc.queryForList("SELECT canonicalOccurrenceId FROM mulino_trade_receipt_Receipts",String.class)));
        assertEquals(2,fixture.count("mulino_inventory_QuantityMovements"));
        assertEquals(0,fixture.count("mulino_trade_purchase_ReceiptCredits"));
    }

    @Test void provisionalResponseNeverInventsCanonicalIdentity() {
        var input=fixture.provisional(ReceiptGatewayPostgresTest.uuid(),"30",true,null);
        String raw=fixture.jdbc.queryForObject("SELECT resultJson FROM mulino_commands_CommandRecords WHERE capabilityId='receiveProvisional'",String.class);
        assertFalse(raw.contains("occurrenceId"),raw);
        assertFalse(raw.contains("canonicalOccurrenceId"),raw);
        assertEquals(0,fixture.count("mulino_evidence_CanonicalOccurrences"));
        assertEquals(0,fixture.count("mulino_trade_receipt_Receipts"));
        assertEquals(0,fixture.count("mulino_inventory_QuantityMovements"));
        assertNotNull(input.provisional());
    }
}
