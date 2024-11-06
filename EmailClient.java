import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class EmailClient extends JFrame{
    private final JTextField senderField=new JTextField(20);
    private final JTextField recipientField=new JTextField(20);
    private final JTextField subjectField=new JTextField(20);
    private final JTextArea contentArea=new JTextArea(5, 20);
    private final JTextArea logArea=new JTextArea(10, 40);

    private final BlockingQueue<String> emailQueue=new LinkedBlockingQueue<>();
    private final AtomicBoolean serverAvailable=new AtomicBoolean(false);

    public EmailClient(){
        setTitle("Email Client");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel inputPanel=new JPanel(new GridLayout(5, 2, 5, 5));
        inputPanel.add(new JLabel("Sender:"));
        inputPanel.add(senderField);
        inputPanel.add(new JLabel("Recipient:"));
        inputPanel.add(recipientField);
        inputPanel.add(new JLabel("Subject:"));
        inputPanel.add(subjectField);
        inputPanel.add(new JLabel("Content:"));
        contentArea.setLineWrap(true);
        inputPanel.add(new JScrollPane(contentArea));

        JButton sendButton=new JButton("Send Email");
        sendButton.addActionListener(e -> sendEmail());
        inputPanel.add(sendButton);
        add(inputPanel, BorderLayout.NORTH);

        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        new Thread(this::processQueue).start();
    }

    private void sendEmail(){
        String sender=senderField.getText().trim();
        String recipient=recipientField.getText().trim();
        String subject=subjectField.getText().trim();
        String content=contentArea.getText().trim();

        if(sender.isEmpty()|| recipient.isEmpty()|| subject.isEmpty()|| content.isEmpty()){
            logArea.append("All fields must be filled.\n");
            return;
        }

        String emailData=sender + ";" + recipient + ";" + subject + ";" + content;

        if(serverAvailable.get()){
            if(!sendToServer(emailData)){
                emailQueue.offer(emailData);
            }
        }else{
            emailQueue.offer(emailData);
            logArea.append("Server unavailable. Email queued: "+emailData);
        }
        senderField.setText("");
        recipientField.setText("");
        subjectField.setText("");
        contentArea.setText("");
    }

    private boolean sendToServer(String emailData){
        try (Socket socket=new Socket("localhost", 9090);
            PrintWriter writer=new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)){
    
            if(emailData != null&& !emailData.isEmpty()){
                writer.println(emailData);
                writer.flush();  // Ensure data is sent immediately
                logArea.append("Email sent to server: "+emailData );
                serverAvailable.set(true);
                return true;
            }else{
                logArea.append("Attempted to send empty email data.\n");
                return false;
            }
    
        }catch(IOException e){
            logArea.append("Error connecting to server: "+e.getMessage());
            serverAvailable.set(false);
            return false;
        }
    }
    

    private void processQueue(){
        while(true){
            try{
                if(!emailQueue.isEmpty() && serverAvailable.get()){
                    String emailData = emailQueue.take();
                    if (!sendToServer(emailData)) {
                        emailQueue.offer(emailData);
                        serverAvailable.set(false);
                    }
                }else if(!serverAvailable.get() && !emailQueue.isEmpty()){
                    checkServerConnection();
                    Thread.sleep(5000);
                }else{
                    Thread.sleep(2000);
                }
            }catch(InterruptedException e){
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    

    private void checkServerConnection(){
        try(Socket socket=new Socket("localhost", 9090)){
            serverAvailable.set(true);
            logArea.append("Server reconnected. Processing queued emails...\n");
        }catch(IOException e){
            serverAvailable.set(false);
        }
    }

    public static void main(String[] args){
        EmailClient client = new EmailClient();
        client.setVisible(true);
    }
}
