package messanger;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import model.Message;

public class ClientMessageFrame extends JFrame implements ActionListener
{
    //lista wiadomosci
    private JList<Message> msgList;
    private DefaultListModel<Message> listModel;
    
    //pole tekstowe do wprowadzania wiadomosci
    private JTextField textField;
    
    //obiekt Client przekazany do konstruktora
    private Client client;
    private int receiverId;
    
    public class MessageCellRenderer extends JPanel implements ListCellRenderer<Object>
    {
        public MessageCellRenderer()
        {
            //setOpaque(true);
        }
        
        /**
         * Przeciążona metoda customizująca renderowanie komórek 
         * listy wiadomości
         **/
        @Override
        public Component getListCellRendererComponent(JList<?> list,
                                                      Object value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus)
        {
            JPanel panel = new JPanel(new BorderLayout());
            
            //pobranie obiektu wiadomości
            Message msg = (Message) value;
            
            JLabel timeLabel = new JLabel(msg.getCreatedDate());
            //timeLabel.setFont(new Font("Serif", Font.PLAIN, 14));
            panel.add(timeLabel, BorderLayout.NORTH);
            panel.add(new JLabel(msg.toString()), BorderLayout.CENTER);
                                      
            if(msg.getReceiverId() == receiverId) {
                 panel.setBackground(Color.WHITE);
            } else {
                 panel.setBackground(new Color(247,247,247));
            }
                                      
            panel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.DARK_GRAY));
            
            return panel;
        }
    };
    
    
    public ClientMessageFrame(Client client, int receiverId)
    {
        super();
        
        this.client = client;
        this.receiverId = receiverId;
        
        createView();
    }
    
    private void createView()
    {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        addWindowListener( new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                client.closeFrameFor(receiverId);
            }
        } );
        setTitle("Rozmowa z użytkownikiem: " + receiverId);
        setSize(500,500);
        setLocationRelativeTo(null);
        //ustawienie GridBagLayout jako menadżera layoutu
        setLayout(new GridBagLayout());
        
        GridBagConstraints constraints = new GridBagConstraints();
       
        //tworzymy domyślny listModel umożliwia późniejsze dodawanie elementów
        //do listy to jest tresci wiadomosci
        listModel = new DefaultListModel<Message>();
        msgList = new JList<Message>(listModel);
        msgList.setCellRenderer( new MessageCellRenderer() );
        
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        getContentPane().add( new JScrollPane(msgList), constraints);
        
        textField = new JTextField();
        constraints.gridy = 2;
        constraints.weighty = (double)1/3;
        getContentPane().add(textField, constraints);
        
        //dodanie obsługi zdarzenia klikniecia w kontrolkę JTextField
        textField.addActionListener(this);
        
        //wyświetlenie komponentów
        pack();
        setSize(500,500);
        setVisible(true);
        
    }
    
    @Override
    public void actionPerformed(ActionEvent e)
    {
        //obsluga zdarzenia
        System.out.println("Kilknięto ENTER w TextField.");
        
        //pobranie wpisanej wiadomości
        String message = ((JTextField)e.getSource()).getText();
        System.out.println("Wpisano: " + message);
        
        //wyczyszczenie pola do wpisywania wiadomości
        ((JTextField)e.getSource()).setText("");
        
        //utworzenie obiektu wiadomości Message który zostanie wysłany
        //na serwer w celu przekazania do odpowiedniego odbiorcy
        
        //pobranie daty/czasu wysłania wiadomości w formie timestampu
        String createdTimeStamp =
                new SimpleDateFormat("YYYY-MM-dd HH:MM:ss.SSS").format(
                                        Calendar.getInstance().getTime());
        
        Message msg = new Message(null, 0,
                                  client.getClientId(),
                                  receiverId,
                                  message,
                                  createdTimeStamp,
                                  null);
                                  
        listModel.addElement(msg);
        
        //opakowanie obiektu Message w pakiet Packet<Message>
        Packet<Message> packet = new Packet<Message>(
                            Packet.PacketCommand.MESSAGE, msg);
       
        try {
            
            client.getObjectOutputStream().writeObject(packet);
            System.out.println("Wysłano wiadomość do " + receiverId);
            
        } catch(IOException ex) {
            System.out.println("Przechwycono IOException podczas wysyłania wiadomosci w ClientMessageFrame.");
        } catch(Exception ex) {}
    
    }
    
    public void addMessage(Message m) {
        //wypisanie wiadomosci metoda .toString()
        listModel.addElement(m);
    }

}