package server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Server {

    private final int TAMANHO_MAXIMO_PACOTE = 20;
    private final String dadoParaTransferncia = "este é um teste de como o algoritmo se comporta com diversos tipos de dados selecionados";
    private int ultimoPacoteEnviado = -1;
    private final int PORTA;
    private Scanner entradaDeDados;
    private final int chanceDeEnviarOPacote;
    private final Timer timer = new Timer();
    private boolean run = true;

    public Server(int PORTA, int chanceDeEnviarOPacote) {
        this.PORTA = PORTA;
        this.chanceDeEnviarOPacote = chanceDeEnviarOPacote;
    }

    private PrintWriter out;
    private List<String> pacotesProntosParaTranferir = new ArrayList<>();

    public void iniciaServidor() {

        try {
            ServerSocket serverSocket = new ServerSocket(PORTA);
            Socket incomingSocket = serverSocket.accept();
            try {
                InputStream inStream = incomingSocket.getInputStream();
                OutputStream outStream = incomingSocket.getOutputStream();

                entradaDeDados = new Scanner(inStream);
                out = new PrintWriter(outStream, true /* autoFlush */);


                while (run) {
                    mostrahome();
                    if (entradaDeDados.hasNextLine()) {
                        String line = entradaDeDados.nextLine();
                        switch (line.trim()) {
                            case "end":
                                run = false;
                                out.println("usuario encerrou sessao");
                                break;
                            case "help":
                                mostraHelp();
                                break;
                            case "begin":
                                iniciaTransmissao();
                                break;
                            default:
                                out.println("\nComando Desconhecido\n");
                        }
                    }
                }
            } finally {
                incomingSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void iniciaTransmissao() {
        preparaPacotes();
        adicionaCabecalhos();
        transmite();
    }

    private void transmite() {

        boolean acabou = false;
        while (!acabou) {
            if (entradaDeDados.hasNextLine()) {

                String entrada = entradaDeDados.nextLine();

                if (entrada.trim().equals("end")){
                    acabou = true;
                    run = false;
                    out.println("usuario encerrou sessao");
                    return;
                }

                int valor = verificaValorInseridoPeloUsuario(entrada);

                if(valor == -10) return;

                if (valor == 0 && ultimoPacoteEnviado < 0) {
                    ultimoPacoteEnviado = 0;
                    enviaPacote(pacotesProntosParaTranferir.get(0));

                } else if (valor == ultimoPacoteEnviado && valor >= 0) {
                    enviaPacote(pacotesProntosParaTranferir.get(valor));

                } else if (valor == ultimoPacoteEnviado + 1) {
                    ultimoPacoteEnviado = valor;
                    enviaPacote(pacotesProntosParaTranferir.get(valor));

                    if (valor == pacotesProntosParaTranferir.size() - 1) {
                        ultimoPacoteEnviado = -1;
                        acabou = true;
                    }

                } else {
                    out.println("dados invalidos");
                }
            }
        }
    }

    private int verificaValorInseridoPeloUsuario(String entrada) {

        try{
            return Integer.parseInt(entrada);
        }catch (Exception e){
            out.println("numero invalido");
        }
        return -10;
    }

    /**
     * tem uma chance de enviar o pacote ao destinatrio
     *
     * @param pacote
     */
    private void enviaPacote(String pacote) {

        Random random = new Random();
        int i = random.nextInt(100);
        if (i <= chanceDeEnviarOPacote) {
            out.println(pacote);
        }
            espera();
    }

    private void espera() {

        MeuTimer meuTimer = new MeuTimer(out, pacotesProntosParaTranferir, ultimoPacoteEnviado);
        timer.schedule(meuTimer, 10000);

    }

    public void adicionaCabecalhos() {
        List<String> dadosComCabecalho = new ArrayList<>();
        String fimDoArquivoMsg = "fim do arquivo";

        int i;
        for (i = 0; i < pacotesProntosParaTranferir.size(); i++) {

            String pacote = pacotesProntosParaTranferir.get(i);
            dadosComCabecalho.add("[" + i + "]" + "[" + pacote.length() + "]" + "[*]" + pacote);
        }
        dadosComCabecalho.add("[" + (i) + "]" + "[" + fimDoArquivoMsg.length() + "]" + "[*]" + fimDoArquivoMsg);
        pacotesProntosParaTranferir = dadosComCabecalho;
    }

    public void preparaPacotes() {

        char[] dadosEmChar = dadoParaTransferncia.toCharArray();
        StringBuilder pacotinho = new StringBuilder();
        List<String> meusPacotinhos = new ArrayList<>();
        int indiceUltimoChar = 0;
        boolean pronto = false;

        while (!pronto) {

            if (pacotinho.length() >= TAMANHO_MAXIMO_PACOTE) {
                meusPacotinhos.add(pacotinho.toString());
                pacotinho = new StringBuilder();
            }
            if (indiceUltimoChar >= dadosEmChar.length) {
                meusPacotinhos.add(pacotinho.toString());
                pacotinho = new StringBuilder();
                pronto = true;
            } else {
                pacotinho.append(dadosEmChar[indiceUltimoChar]);
                indiceUltimoChar++;
            }
        }
        pacotesProntosParaTranferir = meusPacotinhos;
    }

    private void mostraHelp() {
        out.println("");
        out.println("--------------------------------------------------------------------------------------------------");
        out.println("------------------------------------ Welcome to STTP 1.0 -----------------------------------------");
        out.println("|                      To start the download enter \"begin\"                                       |");
        out.println("|   The data will be received with a header who is inform the correct receiving sequence         |");
        out.println("|             something like this: [0][255][*][this is the data received]                        |");
        out.println("|    the first field indicates the sequence of the data and you must enter the next natural      |");
        out.println("|    number before 10 sec timeout the second field indicates the size of the data received       |");
        out.println("|                   if you enter \"end\" the connection will be killed                             |");
        out.println("--------------------------------------------------------------------------------------------------");
        out.println("");
    }

    private void mostrahome() {
        out.println("\n\n");
        out.println("\tWelcome to STTP");
        out.println("\tEnter \"begin\" to start download.");
        out.println("\tEnter \"help\" to figure out how it works.");
        out.println("\tEnter \"end\" to finish the connection.");
        out.println("");

    }

    private static class MeuTimer extends TimerTask {

        private final PrintWriter out;
        private final List<String> lista;
        private final int ultimoPacoteEnviado;

        public MeuTimer(PrintWriter out, List<String> lista, int ultimoPacoteEnviado) {
            this.out = out;
            this.lista = lista;
            this.ultimoPacoteEnviado = ultimoPacoteEnviado;
        }

        @Override
        public void run() {
            if(ultimoPacoteEnviado == lista.size() - 1){
                return;
            }

            out.println(lista.get(ultimoPacoteEnviado));
        }
    }
}
