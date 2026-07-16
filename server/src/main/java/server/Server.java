package server;

import dataaccess.MemoryDataAccess;
import io.javalin.Javalin;
import service.ClearService;
import com.google.gson.Gson;
import service.RegisterRequest;
import service.UserService;
import service.LoginRequest;
import service.GameService;
import service.CreateGameRequest;
import service.JoinGameRequest;
import service.ErrorResult;

public class Server {

    private final Javalin javalin;
    private final MemoryDataAccess dataAccess = new MemoryDataAccess();
    private final UserService userService = new UserService(dataAccess);
    private final Gson gson = new Gson();
    private final GameService gameService = new GameService(dataAccess);

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

        javalin.post("/session", ctx -> {
            LoginRequest request =
                    gson.fromJson(ctx.body(), LoginRequest.class);

            var result = userService.login(request);

            ctx.status(200);
            ctx.contentType("application/json");
            ctx.result(gson.toJson(result));
        });

        javalin.delete("/session", ctx -> {
            String authToken = ctx.header("authorization");

            userService.logout(authToken);

            ctx.status(200);
            ctx.result("{}");
        });

        javalin.get("/game", ctx -> {
            String authToken = ctx.header("authorization");

            System.out.println("GET /game token: [" + authToken + "]");
            System.out.println("Stored auth: " + dataAccess.getAuth(authToken));

            var result = gameService.listGames(authToken);

            ctx.status(200);
            ctx.contentType("application/json");
            ctx.result(gson.toJson(result));
        });

        javalin.post("/game", ctx -> {
            String authToken = ctx.header("authorization");

            CreateGameRequest request =
                    gson.fromJson(ctx.body(), CreateGameRequest.class);

            var result = gameService.createGame(authToken, request);

            ctx.status(200);
            ctx.contentType("application/json");
            ctx.result(gson.toJson(result));
        });

        javalin.put("/game", ctx -> {
            String authToken = ctx.header("authorization");

            JoinGameRequest request =
                    gson.fromJson(ctx.body(), JoinGameRequest.class);

            gameService.joinGame(authToken, request);

            ctx.status(200);
            ctx.result("{}");
        });

        javalin.exception(IllegalArgumentException.class, (exception, ctx) -> {
            ctx.status(400);
            ctx.contentType("application/json");
            ctx.result(gson.toJson(
                    new ErrorResult("Error: bad request")
            ));
        });

        javalin.exception(SecurityException.class, (exception, ctx) -> {
            ctx.status(401);
            ctx.contentType("application/json");
            ctx.result(gson.toJson(
                    new ErrorResult("Error: unauthorized")
            ));
        });

        javalin.exception(IllegalStateException.class, (exception, ctx) -> {
            ctx.status(403);
            ctx.contentType("application/json");
            ctx.result(gson.toJson(
                    new ErrorResult("Error: already taken")
            ));
        });

        javalin.exception(Exception.class, (exception, ctx) -> {
            ctx.status(500);
            ctx.contentType("application/json");
            ctx.result(gson.toJson(
                    new ErrorResult("Error: " + exception.getMessage())
            ));
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