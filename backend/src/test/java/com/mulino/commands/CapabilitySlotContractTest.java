package com.mulino.commands;

import static org.junit.jupiter.api.Assertions.*;

import com.mulino.application.core.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * User decision 2026-10-11 (3): contracts/capability-slots.json is packaged unchanged as the product slot registry, its
 * forms stay inside the published names, and the pipeline check rejects any other name with TYPE_INVALID. That the
 * registry names exactly the registered product handlers is checked when ApplicationCommands starts (every Spring test).
 */
class CapabilitySlotContractTest {
  @Test void packagedRegistryIsThePublishedContractFile() throws Exception {
    byte[] published=Files.readAllBytes(Path.of("../contracts/capability-slots.json"));
    try(var in=CapabilitySlots.class.getClassLoader().getResourceAsStream(CapabilitySlots.RESOURCE)){assertNotNull(in);assertArrayEquals(published,in.readAllBytes());}
    assertEquals(97,CapabilitySlots.capabilities().size());
    assertTrue(CapabilitySlots.allowed("splitQuantity").containsAll(Set.of("segmentId","quantities","unit")));
    assertTrue(CapabilitySlots.allowed("createWork").containsAll(CapabilitySlots.form("createWork","public")));
    assertEquals(Set.of("observationId","canonicalOccurrenceId"),CapabilitySlots.form("recordDelivery","confirmation"));
  }
  @Test void unpublishedSlotNameIsATypeErrorBeforeTheHandler() {
    assertDoesNotThrow(()->CapabilitySlots.require("moveQuantity",Map.of("segmentId","a","destinationId","b")));
    var e=assertThrows(DomainError.class,()->CapabilitySlots.require("moveQuantity",Map.of("segmentId","a","quantity","20")));
    assertEquals("TYPE_INVALID",e.code());assertTrue(e.getMessage().contains("quantity"));
    assertEquals("VERSION_UNSUPPORTED",assertThrows(DomainError.class,()->CapabilitySlots.allowed("noSuchCapability")).code());
    assertThrows(IllegalStateException.class,()->CapabilitySlots.form("moveQuantity","noSuchForm"));
  }
}
