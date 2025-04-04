package io.quarkus.test.junit.mockito.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.mockito.Mockito;

final class MockitoMocksTracker {

    final static Map<Object, Set<Mocked>> TEST_TO_USED_MOCKS = new ConcurrentHashMap<>();

    private MockitoMocksTracker() {
    }

    static void track(Object testInstance, Object mock, Object beanInstance) {
        TEST_TO_USED_MOCKS.computeIfAbsent(testInstance, k -> new HashSet<>()).add(new Mocked(mock, beanInstance));
    }

    static Set<Mocked> getMocks(Object testInstance) {
        return TEST_TO_USED_MOCKS.getOrDefault(testInstance, Collections.emptySet());
    }

    static void reset(Object testInstance) {
        Mockito.framework().clearInlineMock(testInstance);
    }

    static Optional<Object> currentMock(Object testInstance, Object beanInstance) {
        Set<Mocked> mocks = getMocks(testInstance);
        for (Mocked mocked : mocks) {
            if (mocked.beanInstance == beanInstance) {
                return Optional.of(mocked.mock);
            }
        }
        return Optional.empty();
    }

    static class Mocked {
        final Object mock;
        final Object beanInstance;

        public Mocked(Object mock, Object beanInstance) {
            this.mock = mock;
            this.beanInstance = beanInstance;
        }
    }
}
