package nextstep.jwp.presentation;

import static org.apache.coyote.http11.ContentType.TEXT_HTML;
import static org.apache.coyote.http11.HttpStatus.OK;

import org.apache.coyote.http11.HttpRequest;
import org.apache.coyote.http11.HttpResponse;

public class HomeController extends AbstractController {

    @Override
    protected void doGet(HttpRequest request, HttpResponse response) {
        response.setStatus(OK);
        response.setContentType(TEXT_HTML);
        response.setBody("Hello world!");
    }
}
