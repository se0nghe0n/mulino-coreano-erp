package com.mulino.trade;
import com.mulino.application.core.*;
import com.mulino.application.trade.*;
import com.mulino.application.responsibility.ResponsibilityService;
import com.mulino.domain.responsibility.ResponsibilityRepository;
import com.mulino.domain.trade.purchase.PurchaseRepository;
import com.mulino.domain.trade.receipt.ReceiptRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TradeResidualRemedyTest {
 final PurchaseRepository purchase=mock(PurchaseRepository.class);final ReceiptRepository receipts=mock(ReceiptRepository.class);
 final ResponsibilityRepository duties=mock(ResponsibilityRepository.class);final TradeImpact impact=mock(TradeImpact.class);final TradeEvidence evidence=mock(TradeEvidence.class);
 final ResponsibilityService service=new ResponsibilityService(duties,null,null,null,null,null);
 final TradeResidualRemedy remedy=new TradeResidualRemedy(purchase,receipts,duties,service,impact,evidence);
 final Instant now=Instant.parse("2026-10-08T00:00:00Z");final DomainContext c=new DomainContext("org","actor","fixture",now,now);
 final List<Map<String,Object>> roots=new ArrayList<>(),assignments=new ArrayList<>(),scopes=new ArrayList<>(),credits=new ArrayList<>();
 TradeResidualRemedyTest(){
  when(duties.rows("ReceiptResidualRoots","org")).thenAnswer(x->List.copyOf(roots));when(duties.rows("Assignments","org")).thenAnswer(x->List.copyOf(assignments));
  when(duties.require(eq("Roots"),eq("org"),eq("root"))).thenReturn(Map.of("ID","root","kind","RECEIPT_SHORTFALL","quantity",q(40),"unit","EA"));
  when(duties.require(eq("Scopes"),eq("org"),anyString())).thenAnswer(x->scopes.stream().filter(s->x.getArgument(2).equals(s.get("ID"))).findFirst().orElseThrow());
  doAnswer(x->{String table=x.getArgument(0);Map<String,Object> row=new LinkedHashMap<>(x.getArgument(1));switch(table){case "ReceiptResidualRoots"->roots.add(row);case "Scopes"->scopes.add(row);case "Assignments"->assignments.add(row);case "ReceiptResidualCredits"->credits.add(row);}return null;}).when(duties).insert(anyString(),anyMap());
  doAnswer(x->{String table=x.getArgument(0),id=x.getArgument(2);var list=table.equals("Assignments")?assignments:scopes;list.stream().filter(s->id.equals(s.get("ID"))).findFirst().orElseThrow().putAll(x.getArgument(3));return null;}).when(duties).update(anyString(),anyString(),anyString(),anyMap());
  when(purchase.require(c,"OrderLines","line")).thenReturn(Map.of("workId","work","itemId","item","destinationId","place","quantity",q(100),"unit","EA"));
 }
 static BigDecimal q(int n){return BigDecimal.valueOf(n);}
 Map<String,Object> receipt(String id,int quantity){return Map.ofEntries(Map.entry("ID",id),Map.entry("purchaseLineId","line"),Map.entry("workId","work"),Map.entry("itemId","item"),Map.entry("unit","EA"),Map.entry("placeId","place"),Map.entry("rangeRootId","range"+id),Map.entry("canonicalOccurrenceId","canonical"+id),Map.entry("quantity",q(quantity)),Map.entry("contributedQuantity",q(quantity)));}
 Map<String,Object> contribution(String id,int quantity){return Map.of("lineId","line","occurrenceId","canonical"+id,"actualQuantity",q(quantity),"contributedQuantity",q(quantity),"unit","EA");}
 void bind(){roots.add(new LinkedHashMap<>(Map.of("rootId","root","lineId","line","initialContribution",q(60),"orderedQuantity",q(100),"unit","EA")));}
 void leaf(String id,int start,int quantity,String owner){scopes.add(new LinkedHashMap<>(Map.of("ID",id,"rootId","root","startQuantity",q(start),"quantity",q(quantity),"unit","EA","scopeJson","{}","leaf",true)));var a=new LinkedHashMap<String,Object>();a.putAll(Map.of("ID","a"+id,"rootId","root","scopeId",id,"status","OPEN","valid",true,"revision",0,"quantity",q(quantity),"ownerId",owner,"supervisorId",owner,"workId",owner+"work"));a.put("nextAction","receive");a.put("nextCheckAt",now.plusSeconds(100));assignments.add(a);}
 BigDecimal open(){return assignments.stream().filter(a->"OPEN".equals(a.get("status"))&&Boolean.TRUE.equals(a.get("valid"))).map(a->(BigDecimal)a.get("quantity")).reduce(BigDecimal.ZERO,BigDecimal::add);}
 @Test void sixtyTwentyTwentyKeepsOneRootAndExactFortyTwentyZero(){
  bind();leaf("s",0,40,"owner");when(receipts.require(c,"Receipts","one")).thenReturn(receipt("one",60));when(purchase.rows(c,"ReceiptCredits")).thenReturn(List.of(contribution("one",60)));remedy.reconcileReceiptShortfall(c,"one",now);assertEquals(q(40),open());
  when(receipts.require(c,"Receipts","two")).thenReturn(receipt("two",20));when(purchase.rows(c,"ReceiptCredits")).thenReturn(List.of(contribution("one",60),contribution("two",20)));remedy.reconcileReceiptShortfall(c,"two",now);assertEquals(q(20),open());assertEquals(q(20),credits.getFirst().get("quantity"));
  when(receipts.require(c,"Receipts","three")).thenReturn(receipt("three",20));when(purchase.rows(c,"ReceiptCredits")).thenReturn(List.of(contribution("one",60),contribution("two",20),contribution("three",20)));remedy.reconcileReceiptShortfall(c,"three",now);assertEquals(q(0),open());assertEquals(1,roots.size());assertEquals(2,credits.size());assertEquals(q(40),credits.stream().map(x->(BigDecimal)x.get("quantity")).reduce(q(0),BigDecimal::add));
  remedy.reconcileReceiptShortfall(c,"three",now);assertEquals(2,credits.size());verifyNoInteractions(impact);
 }
 @Test void partialTransferKeepsRecipientRemainingUntilItsExactRangeArrives(){bind();leaf("parent",0,30,"parent");leaf("recipient",30,10,"recipient");when(receipts.require(c,"Receipts","two")).thenReturn(receipt("two",20));when(purchase.rows(c,"ReceiptCredits")).thenReturn(List.of(contribution("one",60),contribution("two",20)));remedy.reconcileReceiptShortfall(c,"two",now);assertEquals(q(20),open());assertEquals("OPEN",assignments.stream().filter(a->"arecipient".equals(a.get("ID"))).findFirst().orElseThrow().get("status"));assertTrue(assignments.stream().anyMatch(a->"parent".equals(a.get("ownerId"))&&"OPEN".equals(a.get("status"))&&q(10).equals(a.get("quantity"))));}
 @Test void authorizedResolutionIsNeverReopened(){bind();leaf("s",0,40,"owner");assignments.getFirst().put("status","RESOLVED");when(receipts.require(c,"Receipts","two")).thenReturn(receipt("two",20));when(purchase.rows(c,"ReceiptCredits")).thenReturn(List.of(contribution("one",60),contribution("two",20)));remedy.reconcileReceiptShortfall(c,"two",now);assertEquals("RESOLVED",assignments.getFirst().get("status"));assertTrue(credits.isEmpty());verify(duties,never()).update(any(),any(),any(),any());}
 @Test void crossWorkReceiptDoesNotResolveAnything(){bind();leaf("s",0,40,"owner");var bad=new LinkedHashMap<>(receipt("two",20));bad.put("workId","other");when(receipts.require(c,"Receipts","two")).thenReturn(bad);assertThrows(DomainError.class,()->remedy.reconcileReceiptShortfall(c,"two",now));verify(duties,never()).update(any(),any(),any(),any());}
 @Test void missingCanonicalContributionFailsClosed(){bind();leaf("s",0,40,"owner");when(receipts.require(c,"Receipts","two")).thenReturn(receipt("two",20));when(purchase.rows(c,"ReceiptCredits")).thenReturn(List.of(contribution("one",60)));assertThrows(DomainError.class,()->remedy.reconcileReceiptShortfall(c,"two",now));assertTrue(credits.isEmpty());}
}
