package org.apache.coyote.http11;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import nextstep.jwp.config.ControllerConfig;
import nextstep.jwp.exception.UncheckedServletException;
import nextstep.jwp.presentation.FrontController;
import org.apache.coyote.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Http11Processor implements Runnable, Processor {

    private static final Logger log = LoggerFactory.getLogger(Http11Processor.class);

    private final Socket connection;
    private final FrontController frontController;

    public Http11Processor(Socket connection) {
        this.connection = connection;
        frontController = ControllerConfig.frontController;
    }

    @Override
    public void run() {
        log.info("connect host: {}, port: {}", connection.getInetAddress(), connection.getPort());
        process(connection);
    }

    @Override
    public void process(final Socket connection) {
        try (InputStream inputStream = connection.getInputStream();
             OutputStream outputStream = connection.getOutputStream()) {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            outputStream.write(process(bufferedReader).getBytes());
            outputStream.flush();
        } catch (IOException | UncheckedServletException e) {
            log.error(e.getMessage(), e);
        }
    }

    private HttpResponse process(BufferedReader bufferedReader) {
        HttpExceptionHandler httpExceptionHandler = new HttpExceptionHandler();
        try {
            HttpRequest httpRequest = HttpRequest.from(bufferedReader);
            return frontController.service(httpRequest);
        } catch (HttpException e) {
            return httpExceptionHandler.handleException(e);
        }
    }
}
