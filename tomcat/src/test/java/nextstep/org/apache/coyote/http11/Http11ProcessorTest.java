package nextstep.org.apache.coyote.http11;

import support.StubSocket;
import org.apache.coyote.http11.Http11Processor;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

class Http11ProcessorTest {

    @Test
    void process() {
        // given
        final var socket = new StubSocket();
        final var processor = new Http11Processor(socket);

        // when
        processor.process(socket);

        // then
        var expected = String.join("\r\n",
                "HTTP/1.1 200 OK ",
                "Content-Type: text/html;charset=utf-8 ",
                "Content-Length: 12 ",
                "",
                "Hello world!");

        assertThat(socket.output()).isEqualTo(expected);
    }

    @Test
    void index() throws IOException {
        // given
        final String httpRequest= String.join("\r\n",
                "GET /index.html HTTP/1.1 ",
                "Host: localhost:8080 ",
                "Connection: keep-alive ",
                "",
                "");

        final var socket = new StubSocket(httpRequest);
        final Http11Processor processor = new Http11Processor(socket);

        // when
        processor.process(socket);

        // then
        final URL resource = getClass().getClassLoader().getResource("static/index.html");
        var expected = "HTTP/1.1 200 OK \r\n" +
                "Content-Type: text/html;charset=utf-8 \r\n" +
                "Content-Length: 5564 \r\n" +
                "\r\n"+
                new String(Files.readAllBytes(new File(resource.getFile()).toPath()));

        assertThat(socket.output()).isEqualTo(expected);
    }

    @Test
    void css() {
        // given
        String httpRequest = String.join("\r\n",
                "GET /css/styles.css HTTP/1.1 ",
                "Host: localhost:8080 ",
                "Accept: text/css,*/*;q=0.1 ",
                "Connection: keep-alive ",
                "",
                "");
        StubSocket socket = new StubSocket(httpRequest);
        Http11Processor processor = new Http11Processor(socket);


        // when
        processor.process(socket);

        // then
        assertThat(socket.output()).contains("Content-Type: text/css");
    }

    @Test
    void parseQueryParameter() {
        // given
        String requestBody = "account=gugu&password=password";
        int contentLength = requestBody.getBytes().length;
        String httpRequest = String.join("\r\n",
                "POST /login HTTP/1.1 ",
                "Host: localhost:8080 ",
                "Accept: text/html,*/*;q=0.1 ",
                "Connection: keep-alive ",
                "Content-Length: " + contentLength,
                "",
                requestBody);
        StubSocket socket = new StubSocket(httpRequest);
        Http11Processor processor = new Http11Processor(socket);


        // when
        processor.process(socket);

        // then
        assertThat(socket.output()).contains("HTTP/1.1 302 FOUND", "Location: /index.html");
    }

    @Test
    void loginWithInvalidValue() {
        // given
        String requestBody = "account=abc&password=abc";
        int contentLength = requestBody.getBytes().length;
        String httpRequest = String.join("\r\n",
                "POST /login HTTP/1.1 ",
                "Host: localhost:8080 ",
                "Accept: text/html,*/*;q=0.1 ",
                "Connection: keep-alive ",
                "Content-Length: " + contentLength,
                "",
                requestBody);
        StubSocket socket = new StubSocket(httpRequest);
        Http11Processor processor = new Http11Processor(socket);


        // when
        processor.process(socket);

        // then
        assertThat(socket.output()).contains("HTTP/1.1 401 UNAUTHORIZED");
    }
}
