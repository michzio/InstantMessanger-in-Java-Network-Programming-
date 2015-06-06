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
import java.util.*;
import javax.swing.BorderFactory;
import javax.swing.border.Border;

import model.Contact;
import model.Message;


public class ArchiveDialog extends JDialog implements ListSelectionListener
{
    private Contact contact;
    private Socket socket;
    private ObjectOutputStream oos;
    
    //kontrolki zawierające listę zarchiwizowanych wiadomości
    private JList<Message> messageList;
    private DefaultListModel<Message> listModel;
    
    ArchiveListener archiveListener;
    
    /**
     * Klasa renderująca komórki w JList z wiadomościami
     **/
     public class MessageCellRenderer extends JPanel implements ListCellRenderer<Object>
    {
        public MessageCellRenderer()
        {
            //setOpaque(true);
        }
        
        /**
         * Przeciążona metoda, która customizuje renderowane komórki listy
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
            
            if(msg.getReceiverId() == contact.getId()) {
                panel.setBackground(Color.WHITE);
            } else {
                panel.setBackground(new Color(247,247,247));
            }
            
            panel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.DARK_GRAY));
            
            return panel;

            
        }
    }
    
    
    
    public ArchiveDialog(Contact c, ObjectOutputStream oos) throws UnknownHostException, IOException
    {
        super();
        
        //przypisanie obiektowych strumieni wyjściowych
        //oraz obiektu kontaktu dla którego pobieramy wiadomości
        this.oos = oos;
        this.contact = c;
       
        createView();
        
        //pobranie zarchiwizowanych wiadomości dla klienta c
        getMessagesFor(c);
        
        //dodanie listenera zdarzenia zamknięcia okna
        addWindowListener( new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                archiveListener.archiveDialogClosed();
            }
        } );
    }
    
    /**
     * Metoda ustawiająca na obiekcie okna dialogowego obiekt nasłuchujący 
     * momentu zamknięcia okna
     **/
    public void addArchiveListener( ArchiveListener listener)
    {
        archiveListener = listener;
    }
    
    /**
     * Tworzenie widoku okna dialogowego wyszukiwania kontaktów
     **/
    private void createView()
    {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setTitle("Archiwum Wiadomości...");
        setSize(500, 400);
        setLocationRelativeTo(null);
        
        setLayout(new GridBagLayout());
        
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        
        //dodanie listy wiadomości
        listModel = new DefaultListModel<Message>();
        messageList = new JList<Message>(listModel);
        messageList.setCellRenderer( new MessageCellRenderer());
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 2;
        add( new JScrollPane(messageList), constraints);
        
        //dodanie listenera selekcji kontaktu na liście sugerowanych kontaktów
        messageList.addListSelectionListener(this);
        
        //wyswietlenie komponentów
        pack();
        setSize(500,400);
        setVisible(true);

        
    }
    
    
    /**
     * ListSelectionListener - zdarzenia listy sugerowanych kontaktów
     **/
    @Override
    public void valueChanged(ListSelectionEvent event) {
        //to do...
    }
    
    /**
     * Metoda wykonywana w wyniku kliknięcia przycisku "Szukaj Kontaktu"
     * lub zaplanowanego TimerTaska który podczas dłuższej bezczynności
     * uaktualnia listę sugerowanych kontaktów na podstawie danych 
     * wprowadzonych w pola inputowe wyszukiwarki
     **/
    private synchronized void getMessagesFor(Contact c)
    {
        System.out.println("Pobieranie archiwum wiadomości dla: " + c.getNickname());
        
        //opakowanie obiektu Contact w pakiet Packet<Contact> zapytania
        //o listę kontaktów zgodnych z atrybutami opakowywanego kontaktu
        Packet<Contact> request = new Packet<Contact>(
           Packet.PacketCommand.GET_ARCHIVE_MESSAGES, c);
        
        try {
            
            //przesłanie pakietu z danymi poszukiwanego kontaktu
            oos.writeObject(request);
            
        } catch(IOException e) {
            System.out.println("Błąd I/O podczas połączenia sieciowego z zapytaniem o archiwum wiadomości.");
        }

    }
    
    public synchronized void updateArchiveMessagesList(
                                Packet< java.util.List<Message> >packet)
    {
        java.util.List<Message> archiveMessages =  packet.object();
        
        for(Message msg : archiveMessages)
            listModel.addElement(msg);
    }
    
}

