
import server.Server;

public class App {
    public static void main(String[] args) {

        Server server = new Server(8189,85);
        server.iniciaServidor();

    }
}
