package org.apache.coyote.http11;

import static org.apache.coyote.http11.ContentType.TEXT_CSS;
import static org.apache.coyote.http11.ContentType.TEXT_HTML;
import static org.apache.coyote.http11.HttpMethod.GET;
import static org.apache.coyote.http11.HttpMethod.POST;
import static org.apache.coyote.http11.HttpStatus.BAD_REQUEST;
import static org.apache.coyote.http11.HttpStatus.FOUND;
import static org.apache.coyote.http11.HttpStatus.OK;
import static org.apache.coyote.http11.HttpStatus.UNAUTHORIZED;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Optional;
import java.util.UUID;
import nextstep.jwp.db.InMemoryUserRepository;
import nextstep.jwp.exception.UncheckedServletException;
import nextstep.jwp.model.User;
import org.apache.catalina.Session;
import org.apache.catalina.SessionManager;
import org.apache.coyote.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Http11Processor implements Runnable, Processor {

    private static final String INDEX_HTML = "/index.html";
    private static final String JSESSIONID = "JSESSIONID";
    private static final Logger log = LoggerFactory.getLogger(Http11Processor.class);

    private final Socket connection;
    private final SessionManager sessionManager;

    public Http11Processor(Socket connection) {
        this.connection = connection;
        this.sessionManager = new SessionManager();
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

    private HttpResponse process(BufferedReader bufferedReader) throws IOException {
        ExceptionHandler exceptionHandler = new ExceptionHandler();
        try {
            HttpRequest httpRequest = HttpRequest.from(bufferedReader);
            return handleRequest(httpRequest);
        } catch (HttpException e) {
            return exceptionHandler.handleException(e);
        }
    }

    private HttpResponse handleRequest(HttpRequest httpRequest) throws IOException {
        ResourceLoader resourceLoader = new ResourceLoader();

        if (httpRequest.method() == POST) {
            if (httpRequest.uri().equals("/register")) {
                return register(httpRequest);
            }
            if (httpRequest.uri().equals("/login")) {
                Optional<String> jsessionid = httpRequest.getCookie(JSESSIONID);
                if (jsessionid.isPresent()) {
                    return loginWithSession(jsessionid.get());
                }
                return login(httpRequest);
            }
            throw new HttpException(BAD_REQUEST, "지원되지 않는 uri 입니다");
        }
        if (httpRequest.method() == GET) {
            if (httpRequest.uri().equals("/")) {
                return HttpResponse.status(OK)
                        .body("Hello world!")
                        .contentType(TEXT_HTML)
                        .build();
            }
            if (httpRequest.uri().equals("/login")) {
                Optional<String> jsessionid = httpRequest.getCookie(JSESSIONID);
                if (jsessionid.isPresent()) {
                    return loginWithSession(jsessionid.get());
                }
                return HttpResponse.status(OK)
                        .body(resourceLoader.load("static/login.html"))
                        .contentType(TEXT_HTML)
                        .build();
            }
            if (httpRequest.uri().equals("/register")) {
                return HttpResponse.status(OK)
                        .body(resourceLoader.load("static/register.html"))
                        .contentType(TEXT_HTML)
                        .build();
            }
            Optional<String> acceptHeader = httpRequest.getHeader("Accept");
            if (acceptHeader.isPresent() && acceptHeader.get().contains("text/css")) {
                return HttpResponse.status(OK)
                        .body(resourceLoader.load("static" + httpRequest.uri()))
                        .contentType(TEXT_CSS)
                        .build();
            }
            return HttpResponse.status(OK)
                    .body(resourceLoader.load("static" + httpRequest.uri()))
                    .contentType(TEXT_HTML)
                    .build();
        }
        throw new HttpException(BAD_REQUEST, "지원되지 않는 요청입니다");
    }

    private HttpResponse register(HttpRequest httpRequest) {
        FormData formData = FormData.from(httpRequest.getBody());
        String account = formData.get("account");
        String password = formData.get("password");
        String email = formData.get("email");
        if (InMemoryUserRepository.findByAccount(account).isPresent()) {
            throw new HttpException(BAD_REQUEST, "이미 존재하는 아이디입니다. 다른 아이디로 가입해주세요");
        }
        User user = new User(account, password, email);
        InMemoryUserRepository.save(user);

        return HttpResponse.status(FOUND)
                .redirectUri(INDEX_HTML)
                .build();
    }

    private HttpResponse loginWithSession(String jsessionid) {
        Session session = sessionManager.findSession(jsessionid)
                .orElseThrow(() -> new HttpException(UNAUTHORIZED, "잘못된 세션 아이디입니다"));
        if (session.getAttribute("user").isEmpty()) {
            throw new HttpException(UNAUTHORIZED, "세션에 회원정보가 존재하지 않습니다");
        }
        return HttpResponse.status(FOUND)
                .redirectUri(INDEX_HTML)
                .build();
    }

    private HttpResponse login(HttpRequest httpRequest) {
        FormData formData = FormData.from(httpRequest.getBody());
        String account = formData.get("account");
        String password = formData.get("password");
        Optional<User> user = InMemoryUserRepository.findByAccount(account);
        if (user.isPresent() && user.get().checkPassword(password)) {
            String jsessionid = UUID.randomUUID().toString();
            String userInfo = user.get().toString();
            log.info(userInfo);
            Session session = new Session(jsessionid);
            session.setAttribute("user", user.get());
            sessionManager.add(session);
            return HttpResponse.status(FOUND)
                    .redirectUri(INDEX_HTML)
                    .cookie(JSESSIONID, jsessionid)
                    .build();
        }
        throw new HttpException(UNAUTHORIZED, "아이디나 비밀번호를 확인해주세요");
    }
}
