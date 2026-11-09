package com.obraia.backend.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutorService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.obraia.backend.ai.AiServiceClient;
import com.obraia.backend.ai.AiServiceException;

class AiWakeServiceTest {

    private AiServiceClient ai;
    private ExecutorService executor;
    private AiWakeService service;

    @BeforeEach
    void setUp() {
        ai = mock(AiServiceClient.class);
        executor = mock(ExecutorService.class);
        // The executor runs the task right away, so the test does not wait for a thread.
        doAnswer(call -> {
            call.<Runnable>getArgument(0).run();
            return null;
        }).when(executor).execute(any());
        service = new AiWakeService(ai, executor);
    }

    @Test
    void wakesTheAiInTheBackground() {
        when(ai.available()).thenReturn(true);

        assertTrue(service.wake().enabled());

        verify(executor).execute(any());
        verify(ai).wake();
    }

    @Test
    void aFailedWakeUpIsIgnored() {
        when(ai.available()).thenReturn(true);
        doThrow(new AiServiceException("asleep", null)).when(ai).wake();

        assertTrue(service.wake().enabled());
    }

    @Test
    void withoutTheAiNothingIsWoken() {
        when(ai.available()).thenReturn(false);

        assertFalse(service.wake().enabled());

        verify(ai, never()).wake();
    }
}
