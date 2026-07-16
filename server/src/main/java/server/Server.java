package server;

import dataaccess.MemoryDataAccess;
import io.javalin.Javalin;
import service.ClearService;
import com.google.gson.Gson;
import service.RegisterRequest;
import service.UserService;

public class Server {

    private final Javalin javalin;
    private final MemoryDataAccess dataAccess = new MemoryDataAccess();
    private final UserService userService = new UserService(dataAccess);
    private final Gson gson = new Gson();

    public Server() {
        javalin = Javalin.create(config -> config.staticFiles.add("web"));

        javalin.delete("/db", ctx -> {
            ClearService service = new ClearService(dataAccess);
            service.clear();

            ctx.status(200);
            ctx.result("{}");
        });

        javalin.post("/user", ctx -> {
            RegisterRequest request =
                    gson.fromJson(ctx.body(), RegisterRequest.class);

            var result = userService.register(request);

            ctx.status(200);
            ctx.contentType("application/json");
            ctx.result(gson.toJson(result));
        });
    }

    public int run(int desiredPort) {
        javalin.start(desiredPort);
        return javalin.port();
    }

    public void stop() {
        javalin.stop();
    }
}