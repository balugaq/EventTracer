package com.balugaq.eventtrace.config;

import com.balugaq.eventtrace.SampleEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExemptEventsTest {

    @Test
    void simpleNameMatches() {
        ExemptEvents exempt = new ExemptEvents();
        exempt.load(List.of("SampleEvent"));
        assertTrue(exempt.isExempt(new SampleEvent()));
        assertFalse(exempt.isExempt(new OtherEvent()));
    }

    @Test
    void fullNameResolvesAndMatches() {
        ExemptEvents exempt = new ExemptEvents();
        exempt.load(List.of(SampleEvent.class.getName()));
        assertTrue(exempt.isExempt(new SampleEvent()));
        assertFalse(exempt.isExempt(new OtherEvent()));
    }

    @Test
    void unknownFullNameFallsBackToSimpleSegment() {
        ExemptEvents exempt = new ExemptEvents();
        // 一个不存在的全名 → 取最后一段作为简名
        exempt.load(List.of("com.example.nonexistent.FooEvent"));
        assertFalse(exempt.isExempt(new SampleEvent()));
    }

    @Test
    void unrelatedEventNotExempt() {
        ExemptEvents exempt = new ExemptEvents();
        exempt.load(List.of("PlayerMoveEvent"));
        assertFalse(exempt.isExempt(new SampleEvent()));
    }

    private static final class OtherEvent extends SampleEvent {
    }
}
