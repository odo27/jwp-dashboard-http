package org.apache.coyote.http11;

public enum ContentType {

    TEXT_HTML("text/html;charset=utf-8"),
    TEXT_CSS("text/css;charset=utf-8"),
    ;

    private final String value;

    ContentType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
