package auctionclient.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

/**
 * Lớp phụ để các test chờ FX load xong rồi mới bắt đầu
 */
public abstract class FxControllerTestBase extends ApplicationTest {

    private static final int FX_STABILIZE_MS = 1000;

    @BeforeEach
    void stabilizeFxBeforeTest() {
        WaitForAsyncUtils.waitForFxEvents();
        try {
            Thread.sleep(FX_STABILIZE_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        WaitForAsyncUtils.waitForFxEvents();
    }
}
