package nextstep.jwp.service;

import static nextstep.jwp.exception.AuthExceptionType.INVALID_ID_OR_PASSWORD;
import static nextstep.jwp.exception.AuthExceptionType.INVALID_SESSION_ID;
import static nextstep.jwp.exception.AuthExceptionType.USER_NO_EXIST_IN_SESSION;
import static org.apache.coyote.http11.HttpStatus.FOUND;

import java.util.Optional;
import java.util.UUID;
import nextstep.jwp.db.InMemoryUserRepository;
import nextstep.jwp.exception.AuthException;
import nextstep.jwp.model.User;
import org.apache.catalina.Session;
import org.apache.catalina.SessionManager;
import org.apache.coyote.http11.HttpResponse;

public class LoginService {

    private static final String JSESSIONID = "JSESSIONID";
    private static final String INDEX_HTML = "/index.html";

    public void loginWithSession(String jsessionid, HttpResponse response) {
        Session session = SessionManager.findSession(jsessionid)
                .orElseThrow(() -> new AuthException(INVALID_SESSION_ID));
        if (session.getAttribute("user").isEmpty()) {
            throw new AuthException(USER_NO_EXIST_IN_SESSION);
        }
        response.setStatus(FOUND);
        response.sendRedirect(INDEX_HTML);
    }

    public void login(String account, String password, HttpResponse response) {
        Optional<User> user = InMemoryUserRepository.findByAccount(account);
        if (user.isPresent() && user.get().checkPassword(password)) {
            String jsessionid = UUID.randomUUID().toString();
            Session session = new Session(jsessionid);
            session.setAttribute("user", user.get());
            SessionManager.add(session);
            response.setStatus(FOUND);
            response.sendRedirect(INDEX_HTML);
            response.addCookie(JSESSIONID, jsessionid);
            return;
        }
        throw new AuthException(INVALID_ID_OR_PASSWORD);
    }
}
