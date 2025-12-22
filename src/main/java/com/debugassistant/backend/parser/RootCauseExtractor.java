package com.debugassistant.backend.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Finds the deepest cause inside a stack trace
 */
@Component
@Slf4j
public class RootCauseExtractor {

    // Python error line
    private static final Pattern PYTHON_EXCEPTION_LINE =
            Pattern.compile("^[A-Za-z0-9_]+(?:Error|Exception):.*");

    public String extractRootCauseLine(String stackTrace) {
        if (stackTrace == null || stackTrace.isBlank()) {
            return null;
        }

        // was: split("\n")
        String[] lines = stackTrace.split("\\R");

        String lastCause = null;
        boolean expectPythonRootNext = false;

        for (String raw : lines) {
            String line = raw.trim();

            // Java/Spring deeper cause
            if (line.startsWith("Caused by:")) {
                lastCause = line.substring("Caused by:".length()).trim();
                continue;
            }

            // Spring nested cause
            if (line.contains("nested exception is")) {
                int index = line.indexOf("nested exception is");
                lastCause = line.substring(index + "nested exception is".length()).trim();
                continue;
            }

            // next line is the real cause in Python
            if (line.startsWith("During handling of the above exception")) {
                expectPythonRootNext = true;
                continue;
            }

            //  actual cause
            if (expectPythonRootNext && PYTHON_EXCEPTION_LINE.matcher(line).matches()) {
                lastCause = line;
                expectPythonRootNext = false;
            }
        }

        if (lastCause != null) {
            log.debug("Root cause: {}", lastCause);
        }

        return lastCause;
    }
}