package core.path_handler;

import java.util.Map;

public class HandlerWithParam {
    private final Handler handler;
    private final Map<String, String> params;

    public HandlerWithParam(Handler handler, Map<String, String> params) {
        this.handler = handler;
        this.params = params;
    }

    public Handler getHandler() {
        return handler;
    }

    public Map<String, String> getParams() {
        return params;
    }
}
