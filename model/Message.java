package model;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

public class Message implements Serializable
{
    //obiekt Connection nie jest serializowany podczas przesyłania
    //przez sieć, klient nie potrzebuje dostępu do tego obiektu
    private transient Connection connection;
    
    private int id;
    private int receiver_id;
    private int sender_id;
    private String message;
    private String created_date; //"YYYY-MM-DD HH:MM:SS.SSS".
    private String read_date; //"YYYY-MM-DD HH:MM:SS.SSS".
    
    private static final String INSERT_MESSAGE = "INSERT INTO message VALUES (NULL, ?, ?, ?, ?, ?);";
    private static final String UPDATE_MESSAGE = "UPDATE message SET receiver_id = ?, sender_id = ?, message = ?, created_date = ?, read_date = ? WHERE message_id = ?;";
    private static final String DELETE_MESSAGE = "DELETE FROM message WHERE message_id = ?;";
    private static final String SELECT_MESSAGE = "SELECT * FROM message WHERE message_id = ?;";
    private static final String DELETE_RECEIVER_MESSAGES = "DELETE FROM message WHERE receiver_id = ?;";
    private static final String SELECT_UNREAD_MESSAGES = "SELECT * FROM message WHERE receiver_id = ? AND (read_date IS NULL OR read_date = '');";
    private static final String DELETE_SENDER_MESSAGES = "DELETE FROM message WHERE sender_id = ?;";
    private static final String SELECT_ARCHIVE_MESSAGES = "SELECT * FROM message WHERE (receiver_id = ? AND sender_id = ?) OR (sender_id = ? AND receiver_id = ?);";
    
    /**
     * Pobiera z bazy danych obiekt Message o wskazanym message_id
     **/
    public static Message getMessageWithId(Connection conn, int message_id)
    {
        Message msg = null;
        
        //1. pobieramy obiekt z message_id używając connection
        PreparedStatement stmt = null;
        
        try {
            stmt = conn.prepareStatement(SELECT_MESSAGE);
            stmt.setInt(1, message_id);
            
            //wykonanie kwerendy
            ResultSet rs = stmt.executeQuery();
            if(rs.next()) {
                //odczytanie zawartości zwróconego rekordu
                int id = rs.getInt("message_id");
                int receiver_id = rs.getInt("receiver_id");
                int sender_id = rs.getInt("sender_id");
                String message = rs.getString("message");
                String created_date = rs.getString("created_date");
                String read_date = rs.getString("read_date");
                //2. konstruujemy obiekt Message
                msg = new Message(conn, id, receiver_id, sender_id, message, created_date, read_date);
            }
        
        } catch(SQLException e)
        {
            System.out.println("Błąd podczas pobierania wiadomości o id: " + message_id + " z bazy danych");
        } finally {
            if(stmt != null) {
                try {
                    stmt.close();
                } catch(SQLException e) {
                }
            }
        }
    
        return msg;
    }
    
    public static List<Message> getArchiveMessagesFor(Connection conn, int client_id, int contact_id)
    {
        List<Message> archiveMessages = new ArrayList<Message>();
        
        //tworzymu kwerende
        PreparedStatement stmt = null;

        try {
            stmt = conn.prepareStatement(SELECT_ARCHIVE_MESSAGES);
            stmt.setInt(1, client_id);
            stmt.setInt(2, contact_id);
            stmt.setInt(3, client_id);
            stmt.setInt(4, contact_id);
            
            //wykonanie kwerendy
            ResultSet rs = stmt.executeQuery();
            while(rs.next()) {
                archiveMessages.add( new Message(conn,
                                                rs.getInt("message_id"),
                                                rs.getInt("receiver_id"),
                                                rs.getInt("sender_id"),
                                                rs.getString("message"),
                                                rs.getString("created_date"),
                                                rs.getString("read_date")
                                                )
                                   );
            }
            
        } catch(SQLException e) {
            System.out.println("Błąd podczas pobierania listy archiwalnych wiadomości dla usera: " + contact_id);
        } finally {
            if(stmt != null) {
                try {
                    stmt.close();
                } catch(SQLException e) {
                }
            }
        }
        
        return archiveMessages;
    }
    
    /**
     * Pobiera liste nieprzeczytanych wiadomości dla określonego odbiorcy
     **/
    public static List<Message> getUnreadMessagesFor(Connection conn, int receiver_id) {
        
        List<Message> unreadMessages = new ArrayList<Message>();
        
        //tworzymu kwerende
        PreparedStatement stmt = null;
        
        try {
            stmt = conn.prepareStatement(SELECT_UNREAD_MESSAGES);
            stmt.setInt(1, receiver_id);
            
            //wykonanie kwerendy
            ResultSet rs = stmt.executeQuery();
            while(rs.next()) {
                unreadMessages.add( new Message(conn,
                                                rs.getInt("message_id"),
                                                rs.getInt("receiver_id"),
                                                rs.getInt("sender_id"),
                                                rs.getString("message"),
                                                rs.getString("created_date"),
                                                rs.getString("read_date")
                                                )
                                   );
            }
            
        } catch(SQLException e) {
            System.out.println("Błąd podczas pobierania listy nieprzeczytanych wiadomości dla usera: " + receiver_id);
            e.printStackTrace();
        } finally {
            if(stmt != null) {
                try {
                    stmt.close();
                } catch(SQLException e) {
                }
            }
        }
        
        return unreadMessages;
    }
    
    /**
     * Usuwa z bazy danych wiadomości otrzymane przez receiver_id
     **/
    public static boolean removeMessagesReceivedBy(Connection conn, int receiver_id)
    {
        PreparedStatement stmt = null;
        
        try {
            
            stmt = conn.prepareStatement(DELETE_RECEIVER_MESSAGES);
            stmt.setInt(1, receiver_id);
            
            stmt.executeUpdate();
            
        } catch(SQLException e) {
            System.out.println("Błąd podczas usuwania wiadmości odebranych przez użytkownika: " + receiver_id);
            return false;
        } finally {
            if(stmt != null) {
                try {
                    stmt.close();
                } catch(SQLException e) {
                }
            }
        }
        
        return true;
    }
    
    /**
     * Usuwanie z bady danych wiadomości wysłanych przez sender_id
     **/
    public static boolean removeMessagesSentBy(Connection conn, int sender_id)
    {
        PreparedStatement stmt = null;
        
        try {
            stmt = conn.prepareStatement(DELETE_SENDER_MESSAGES);
            stmt.setInt(1, sender_id);
            
            stmt.executeUpdate();
            
        } catch(SQLException e) {
            System.out.println("Błąd podczas usuwania wiadomości wysłanych przez użytkownika: " + sender_id);
            return false;
        } finally {
            if(stmt != null) {
                try {
                    stmt.close();
                } catch(SQLException e) {
                }
            }
        }
        
        return true;
    }
    
    /**
     * Konstruktor pustego obiektu
     **/
    public Message() { }
    /**
     * Konsruktor tworzy nowy obiekt Message wymaga
     * zapisania do bazy danych metoda save();
     **/
    public Message(Connection conn, int id, int from_id, int to_id, String m, String created_date, String read_date)
    {
        //ustawienie obiektu połączenia z SQLite
        this.connection = conn;
        
        //ustawienie atrybutów obiektu Message
        this.id = id;
        sender_id = from_id;
        receiver_id = to_id;
        message = m;
        this.created_date = created_date;
        this.read_date = read_date;
    }
    
    public void setConnection(Connection conn)
    {
        this.connection = conn;
    }
    
    /**
     * Grupa metod do pobierania i ustawiania atrybutów obiektu
     **/
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getReceiverId() {
        return receiver_id;
    }
    
    public void setReceiverId(int to_id) {
        receiver_id = to_id;
    }
    
    public int getSenderId() {
        return sender_id;
    }
    
    public void setSenderId(int from_id) {
        sender_id = from_id;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String m) {
        message = m;
    }
    
    public String getCreatedDate() {
        return created_date;
    }
    
    public void setCreatedDate(String created_date) {
        this.created_date = created_date;
    }
    
    public String getReadDate() {
        return read_date;
    }
    
    public void setReadDate(String read_date) {
        this.read_date = read_date; 
    }
    
    /***/
    
    @Override
    public String toString() {
        return message;
    }
    
    /**
     * Metoda zapisuje bieżący obiekt Message do tablicy
     * message w bazie danych SQLite
     **/
    public boolean save()
    {
        if(connection == null) {
            System.out.println("Brak obiektu Connection, nie udało się zapisać wiadomości Message do bazy SQLite!");
            return false;
        }
        
        PreparedStatement stmt = null;
        
        try {
            
            if(id > 0) {
                //UPDATE Message Object
                stmt = connection.prepareStatement(UPDATE_MESSAGE);
                stmt.setInt(6, id);
            } else {
                //INSERT Message Object
                stmt = connection.prepareStatement(INSERT_MESSAGE);
            }
            
            stmt.setInt(1, receiver_id);
            stmt.setInt(2, sender_id);
            stmt.setString(3, message);
            stmt.setString(4, created_date);
            stmt.setString(5, read_date);
            
            stmt.executeUpdate();
            
        } catch(SQLException e) {
            System.out.println("Błąd podczas zapisywania Message do bazy danych.");
            return false;
        } finally {
            if(stmt != null) {
                try {
                    stmt.close();
                } catch(SQLException e) {
                }
            }
        }
        
        return true;
    }
    
    public boolean remove() {
        if(connection == null) {
            System.out.println("Brak obiektu Connection, nie udało się usunąć wiadomości Message z bazy SQLite!");
            return false;
        }
        
        PreparedStatement stmt = null;
        
        try {
            //DELETE Message Object
            stmt = connection.prepareStatement(DELETE_MESSAGE);
            stmt.setInt(1, id);
            
            stmt.executeUpdate();
            
            System.out.println("Usunięto wiadomość o id: " + id + " z bazy danych");
            
        } catch(SQLException e) {
            System.out.println("Błąd podczas usuwania Message z bazy danych.");
            return false;
        } finally {
            if(stmt != null) {
                try {
                    stmt.close();
                } catch(SQLException e) {
                }
            }
        }
        
        return true;
    }
}

/**
 * Obiekty mozna wysyłać do strumienia -> ObjectOutputStream
 * przesyłanie obiektu przez sieć:
 * ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream()); 
 * oos.writeObject(m); 
 * Odczytywanie przesyłanego obiektu ze strumienia -> ObjectInputStream
 * ObjectInputStream ois = new ObjectInputStream(sock.getInputStream()); 
 * m = (Message) ois.readObject(); 
 **/