package org.ps5jb.test;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests using Mockito mocks should extend this class
 * to automatically register the extension.
 */
public class MockitoTestCase {
    /**
     * Cannot use {@link org.junit.jupiter.api.extension.ExtendWith}
     * with {@link Class} from Personal Basis Profile because it lacks
     * generic parameters. So instantiate the Mockito extension
     * programmatically.
     */
    @RegisterExtension
    protected final MockitoExtension mockito = new MockitoExtension();
}
