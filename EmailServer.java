import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class EmailServer{
    private static final int PORT=9090;
    private static final BlockingQueue<String> emailQueue=new ArrayBlockingQueue<>(50);

    public static void main(String[] args){
        System.out.println("Email server started. Waiting for client connections...");
        new Thread(new EmailConsumer(emailQueue)).start();

        try(ServerSocket serverSocket = new ServerSocket(PORT)){
            while(true){
                Socket clientSocket = serverSocket.accept();
                System.out.println("Connected to client: " +clientSocket.getInetAddress().getHostAddress());
                new Thread(new EmailProducer(clientSocket, emailQueue)).start();
            }
        }catch(IOException e){
            System.out.println("Server error: " + e.getMessage());
        }
    }

    static class EmailConsumer implements Runnable{
        private final BlockingQueue<String> emailQueue;
        public EmailConsumer(BlockingQueue<String> emailQueue){
            this.emailQueue=emailQueue;
        }

        @Override
        public void run(){
            while(true){
                try{
                    String emailData=emailQueue.take();
                    processEmail(emailData);
                }catch(InterruptedException e){
                    System.out.println("Consumer interrupted: "+e.getMessage());
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        private void processEmail(String emailData){
            String[] parts=emailData.split(";");
            if(parts.length==4){
                String sender=parts[0];
                String recipient=parts[1];
                String subject=parts[2];
                String content=parts[3];

                System.out.println("\nProcessing email:");
                System.out.println("From: "+sender);
                System.out.println("To: "+recipient);
                System.out.println("Subject: "+subject);
                System.out.println("Content: "+content);
                System.out.println("Email sent successfully.\n");
            }else{
                System.out.println("Invalid email format received.");
            }
        }
    }

    static class EmailProducer implements Runnable{
        private final Socket clientSocket;
        private final BlockingQueue<String> emailQueue;
        public EmailProducer(Socket clientSocket, BlockingQueue<String> emailQueue){
            this.clientSocket=clientSocket;
            this.emailQueue=emailQueue;
        }

        @Override
        public void run(){
            try(BufferedReader reader=new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))){
                String emailData=reader.readLine();
                if(emailData!=null){
                    emailQueue.put(emailData);
                    System.out.println("Email queued for processing: "+emailData);
                }else{
                    System.out.println("Received null data from client.");
                }
            }catch(Exception e){
                System.out.println("Client error: "+e.getMessage());
            }finally{
                try{
                    clientSocket.close();
                }catch(IOException e){
                    System.out.println("Error closing client socket: " + e.getMessage());
                }
            }
        }
    }
}
