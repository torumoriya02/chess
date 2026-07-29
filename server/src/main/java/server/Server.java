package server;

import com.google.gson.Gson;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MySqlDataAccess;
import io.javalin.Javalin;
import service.ClearService;
import service.CreateGameRequest;
import service.ErrorResult;
import service.GameService;
import service.JoinGameRequest;
import service.LoginRequest;
import service.RegisterRequest;
import service.UserService;

public class Server {

    private final Javalin javalin;
    private final DataAccess dataAccess;
    private final UserService userService;
    private final GameService gameService;
    private final Gson gson = new Gson();

    public Server() {
        dataAccess = createDataAccess();
        userService = new UserService(dataAccess);
        gameService = new GameService(dataAccess);

        javalin = Javalin.create(
                config -> config.staticFiles.add("web")
        );

        registerRoutes();
        registerExceptionHandlers();
    }

    private DataAccess createDataAccess() {
        try {
            return new MySqlDataAccess();
        } catch (DataAccessException ex) {
            throw new RuntimeException(
                    "Unable to initialize database",
                    ex
            );
        }
    }

    private void registerRoutes() {
        registerClearRoute();
        registerUserRoutes();
        registerGameRoutes();
    }

    private void registerClearRoute() {
        javalin.delete("/db", ctx -> {
            ClearService service = new ClearService(dataAccess);
            service.clear();

            ctx.status(200);
            ctx.result("{}");
        });
    }

    private void registerUserRoutes() {
        javalin.post("/user", ctx -> {
            RegisterRequest request = gson.fromJson(
                    ctx.body(),
                    RegisterRequest.class
            );

            var result = userService.register(request);

            ctx.status(200);
            ctx.contentType("application/json");
            ctx.result(gson.toJson(result));
        });

        javalin.post("/session", ctx -> {
            LoginRequest request = gson.fromJson(
                    ctx.body(),
                    LoginRequest.class
            );

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
    }

    private void registerGameRoutes() {
        javalin.get("/game", ctx -> {
            String authToken = ctx.header("authorization");

            var result = gameService.listGames(authToken);

            ctx.status(200);
            ctx.contentType("application/json");
            ctx.result(gson.toJson(result));
        });

        javalin.post("/game", ctx -> {
            String authToken = ctx.header("authorization");

            CreateGameRequest request = gson.fromJson(
                    ctx.body(),
                    CreateGameRequest.class
            );

            var result = gameService.createGame(
                    authToken,
                    request
            );

            ctx.status(200);
            ctx.contentType("application/json");
            ctx.result(gson.toJson(result));
        });

        javalin.put("/game", ctx -> {
            String authToken = ctx.header("authorization");

            JoinGameRequest request = gson.fromJson(
                    ctx.body(),
                    JoinGameRequest.class
            );

            gameService.joinGame(authToken, request);

            ctx.status(200);
            ctx.result("{}");
        });
    }

    private void registerExceptionHandlers() {
        javalin.exception(
                IllegalArgumentException.class,
                (exception, ctx) -> sendError(
                        ctx,
                        400,
                        "Error: bad request"
                )
        );

        javalin.exception(
                SecurityException.class,
                (exception, ctx) -> sendError(
                        ctx,
                        401,
                        "Error: unauthorized"
                )
        );

        javalin.exception(
                IllegalStateException.class,
                (exception, ctx) -> sendError(
                        ctx,
                        403,
                        "Error: already taken"
                )
        );

        javalin.exception(
                Exception.class,
                (exception, ctx) -> sendError(
                        ctx,
                        500,
                        "Error: " + exception.getMessage()
                )
        );
    }

    private void sendError(
            io.javalin.http.Context ctx,
            int status,
            String message
    ) {
        ctx.status(status);
        ctx.contentType("application/json");
        ctx.result(gson.toJson(new ErrorResult(message)));
    }

    public int run(int desiredPort) {
        javalin.start(desiredPort);
        return javalin.port();
    }

    public void stop() {
        javalin.stop();
    }
}