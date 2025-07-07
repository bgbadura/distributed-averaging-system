import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Scanner;

public class DAS {
    public static String PREFIX;
    public static int PORT;
    public static int NUMBER;
    public static DatagramSocket SOCKET;
    public static DatagramPacket PACKET;

    public static void main(String[] args) {

        { // Zdefiniowanie portu i numeru
            try {

                if (args.length != 2) {
                    System.out.println("usage: java DAS <port> <number>");
                    System.exit(-1);
                }

                PORT = Integer.parseInt(args[0]);
                NUMBER = Integer.parseInt(args[1]);
                System.out.println("[DAS start] port = " + PORT + "; number = " + NUMBER);
            } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
                System.out.println("usage: java DAS <port> <number>");
                System.exit(-1);
            }
        }

        {   // Wybór między MASTER, a SLAVE
            // Ustawienie loggera i otwarcie gniazda UDP
            try {
                PREFIX = PORT + "";
                log("Starting DAS");
                log("Opening UDP Socket");
                SOCKET = new DatagramSocket(PORT);
                SOCKET.setSoTimeout(80000);
            } catch (SocketException e) {
                // SocketException znaczy, że już wcześniej był Socket na tym porcie -> tworzy się Slave
                log("Port " + PORT + " is already taken, launching as Slave.");
                try {   // TUTAJ JAKO SLAVE
                    PREFIX = "Slave " + PORT;
                    SOCKET = new DatagramSocket();

                    // wysyłanie
                    byte[] slaveMsg = ByteBuffer.allocate(4).putInt(NUMBER).array();
                    byte[] buf = slaveMsg;

                    InetAddress receiverAddress = InetAddress.getByName("localhost");

                    PACKET = new DatagramPacket(buf, buf.length, receiverAddress, PORT);
                    log("Sending Slave message: " + NUMBER);
                    SOCKET.send(PACKET);
                    log("Slave message sent. Exiting...");
                    stopBeforeExit();
                    SOCKET.close();     // Po wysłaniu liczby do Master'a Slave kończy działanie
                    System.exit(0);
                } catch (IOException ioe1) {
                    log("Slave IOException:");
                    ioe1.printStackTrace();
                }
            }
        }

        {   // JAKO MASTER
            try {
                PREFIX = "Master " + PORT;
                LinkedList<Integer> masterReceivedNums = new LinkedList<>();
                if (NUMBER != 0 && NUMBER != -1) masterReceivedNums.add(NUMBER);

                byte[] buf = new byte[1400];
                PACKET = new DatagramPacket(buf, buf.length);
                log("Listening for packets...");

                // Nasłuchiwanie na nadchodzące pakiety i printowanie ich
                while (true) {
                    SOCKET.receive(PACKET);
                    int receivedNumber = ByteBuffer.wrap(PACKET.getData(), 0, PACKET.getLength()).getInt();
                    log("Packet received from: " + PACKET.getAddress().getHostAddress() + ":"
                            + PACKET.getPort() + "\tmessage = " + receivedNumber);
                    if (receivedNumber == 0) {
                        double sum = 0.0;
                        for (Integer num : masterReceivedNums) {
                            sum += num;
                        }

                        double avg = sum / masterReceivedNums.size();
                        int roundedAvg = (int)Math.floor(avg);
                    // 1) Wylicza średnią wszystkich niezerowych liczb, razem z <number>
                    // 2) i wypisuje na konsolę
                        log("RESULT AVERAGE OF " + masterReceivedNums + " ROUNDED DOWN = " + roundedAvg);

                    // 3) Wysyła ze swojego gniazda komunikat
                        // na port o numerze 60000 zawierający wyliczoną wartość średnią.
                        // Dodatkowo program ma wyliczyć i wyświetlić na konsoli
                        // wartość adresu rozgłoszeniowego dla sieci lokalnej.

                        // adres rozgłoszeniowy = negacja binarna maski + adres sieciowy
                        // Da się łatwiej: metoda na wyznaczenie adresu rozgłoszeniowego i wyprintowanie go na ekran
                        log("Broadcast address = " + showBroadcastAdr());

                        // Wysyłanie wyliczonej średniej na port 60000
                        byte[] caseZeroMsg = ByteBuffer.allocate(4).putInt(roundedAvg).array();
                        byte[] caseZeroBuf = caseZeroMsg;

                        InetAddress receiverAddress = InetAddress.getByName("localhost");
                        PACKET = new DatagramPacket(caseZeroBuf, caseZeroBuf.length, receiverAddress, 60000);
                        SOCKET.send(PACKET);
                        log("Sent rounded average = " + roundedAvg + " to port 60000");
                    } else if (receivedNumber == -1) {
                        // 1) Wypisuje -1 na konsolę
                        log(-1 + "");

                        // 2) Wysyła komunikat rozgłoszeniowy z tą wartością "-1" do wszystkich komputerów
                        // w swojej sieci lokalnej na port o numerze <port>
                        byte[] caseMinusOneMsg = ByteBuffer.allocate(4).putInt(-1).array();
                        byte[] caseMinusOneBuf = caseMinusOneMsg;

                        InetAddress receiverAddress = InetAddress.getByName("localhost");
                        PACKET = new DatagramPacket(caseMinusOneBuf, caseMinusOneBuf.length, receiverAddress, 60000);
                        SOCKET.send(PACKET);
                        log("Broadcasting message -1 to port 60000");
                        log("Closing UDP socket");

                        // 3) Proces zamyka używane gniazdo i kończy pracę
                        stopBeforeExit();
                        SOCKET.close();
                    }
                    // Dodanie odebranej wiadomości do listy
                    if (receivedNumber != 0 && receivedNumber != -1) masterReceivedNums.add(receivedNumber);
                    log("All collected packets: " + masterReceivedNums + "... Waiting for new packets...");
                }
            } catch (IOException ioe1) {
                ioe1.printStackTrace();
            }
        }
    }

    public static void log(String message) {
        System.out.println("[" + PREFIX + "] " + message);
        System.out.flush();
    }

    public static String showBroadcastAdr() throws UnknownHostException, SocketException {
        NetworkInterface networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
        for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
            InetAddress broadcastAddress = interfaceAddress.getBroadcast();
            if (broadcastAddress != null)
                return broadcastAddress.getHostAddress();
        }
        return null;
    }

    public static void stopBeforeExit() {
//        Scanner scan = new Scanner(System.in);
//        System.out.println("Press ENTER to exit the program");
//        String line = scan.nextLine();
    }
}