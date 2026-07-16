package server;

import dataaccess.MemoryDataAccess;
import io.javalin.Javalin;
import service.ClearService;

public class Server {

    private final Javalin javalin;
    private final MemoryDataAccess dataAccess = new MemoryDataAccess();

    public Server() {
        javalin = Javalin.create(config -> config.staticFiles.add("web"));

        javalin.delete("/db", ctx -> {
            ClearService service = new ClearService(dataAccess);
            service.clear();

            ctx.status(200);
            ctx.result("{}");
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