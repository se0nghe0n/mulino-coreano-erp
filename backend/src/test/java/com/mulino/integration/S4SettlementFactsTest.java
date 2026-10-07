package com.mulino.integration;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.mulino.application.core.*;
import com.mulino.application.trade.*;
import com.mulino.application.trade.purchase.PurchaseLinePort;
import com.mulino.domain.trade.purchase.PurchaseRepository;
import java.math.BigDecimal;import java.time.Instant;import java.util.*;
import org.junit.jupiter.api.Test;
class S4SettlementFactsTest {
 @Test void physicalInvoiceComparisonDoesNotReplaceGoalRecognitionOrAuthorizePayment(){
  var c=new DomainContext("org","actor","request",Instant.EPOCH,Instant.EPOCH);
  var purchase=mock(PurchaseLinePort.class);var repository=mock(PurchaseRepository.class);var sales=mock(SalesOrderLinePort.class);var credits=mock(SalesDeliveryCreditPort.class);var effects=mock(SalesExecutionPort.class);
  var adapter=new SettlementFactsAdapter(purchase,repository,sales,credits,effects);
  when(purchase.receiptCredit(c,"line","receipt")).thenReturn(Map.of("actualQuantity",new BigDecimal("105"),"contributedQuantity",new BigDecimal("100"),"unit","BOX","occurrenceId","receipt"));
  var received=adapter.contribution(c,"PURCHASE","line","receipt");assertEquals(new BigDecimal("105"),received.get("quantity"));assertEquals(new BigDecimal("100"),received.get("recognizedQuantity"));
  when(credits.deliveryCredit(c,"sale","delivery")).thenReturn(Map.of("actualQuantity",new BigDecimal("20"),"contributedQuantity",BigDecimal.ZERO,"unit","BOX","canonicalOccurrenceId","canonical"));
  var delivered=adapter.contribution(c,"SALE","sale","delivery");assertEquals(new BigDecimal("20"),delivered.get("quantity"));assertEquals(BigDecimal.ZERO,delivered.get("recognizedQuantity"));assertEquals("canonical",delivered.get("occurrenceId"));assertEquals("delivery",delivered.get("referenceId"));
  assertThrows(DomainError.class,()->adapter.contribution(c,"bank","sale","delivery"));verifyNoInteractions(effects);
 }
}
