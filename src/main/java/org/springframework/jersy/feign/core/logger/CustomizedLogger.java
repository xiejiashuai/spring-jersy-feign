package org.springframework.jersy.feign.core.logger;

import feign.Logger;
import feign.Response;
import org.slf4j.LoggerFactory;
import org.springframework.core.NamedInheritableThreadLocal;

import java.io.IOException;

/**
 * Extends {@link Logger}
 *
 * @author jiashuai.xie
 */
public class CustomizedLogger extends Logger {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CustomizedLogger.class);

    private static final NamedInheritableThreadLocal<String> LOGGER_CONTEXT = new NamedInheritableThreadLocal<String>("Feign Logger") {
        @Override
        protected String initialValue() {
            return "\n";
        }
    };

    @Override
    protected void log(String configKey, String format, Object... args) {
        String content = LOGGER_CONTEXT.get();
        content = content + String.format(methodTag(configKey) + format + "\n", args);
        LOGGER_CONTEXT.set(content);
    }

    @Override
    protected Response logAndRebufferResponse(String configKey, Level logLevel, Response response, long elapsedTime) throws IOException {
        Response response1 = super.logAndRebufferResponse(configKey, logLevel, response, elapsedTime);
        LOGGER.info(LOGGER_CONTEXT.get());
        LOGGER_CONTEXT.remove();
        return response1;
    }

    @Override
    protected IOException logIOException(String configKey, Level logLevel, IOException ioe, long elapsedTime) {
        IOException ioException = super.logIOException(configKey, logLevel, ioe, elapsedTime);
        LOGGER.info(LOGGER_CONTEXT.get());
        LOGGER_CONTEXT.remove();
        return ioException;
    }

}
