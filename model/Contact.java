package model;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.math.BigInteger;
import java.io.UnsupportedEncodingException; 


public class Contact implements Serializable
{
    //obiek Connection nie jest serializowany podczas przesylania
    //przez sieć, klient nie potrzebuje dostepu do tego obiektu
    private transient Connection connection;
    
    private int id;
    private String nickname;
    private String sha1password;
    private String firstName;
    private String lastName;
    
    private static final String INSERT_CONTACT = "INSERT INTO contact VALUES (NULL, ?, ?, ?, ?);";
    private static final String UPDATE_CONTACT = "UPDATE contact SET nickname = ?, sha1password = ?, first_name = ?, last_name = ? WHERE contact_id = ?;";
    private static final String SELECT_CONTACT = "SELECT * FROM contact WHERE contact_id = ?";
    private static final String DELETE_CONTACT = "DELETE FROM contact WHERE contact_id = ?;";
    
    private static final String SELECT_CONTACT_WITH_NICKNAME = "SELECT * FROM contact WHERE nickname = ?;";
    private static final String AUTHORIZE_CLIENT_SQL = "SELECT * FROM contact WHERE nickname = ? AND sha1password = ?;";
    private static final String SELECT_MATCHED_CONTACTS = "SELECT * FROM contact WHERE nickname LIKE ? AND first_name LIKE ? AND last_name LIKE ? LIMIT 10;";
    
    /**
     * Wczytuje z bazy danych obiekt Contact o wskazanym contact_id
     **/
    public static Contact getContactWithId(Connection conn, int contact_id) {
        
        Contact contact = null;
        
        //1. pobieramy obiekt z contact_id używając connection
        PreparedStatement stmt = null;
        
        try {
            stmt = conn.prepareStatement(SELECT_CONTACT);
            stmt.setInt(1, contact_id);
            
            //wykonanie kwerendy
            ResultSet rs = stmt.executeQuery();
            if(rs.next()) {
                //odczytanie zawartości zwróconego rekordu
                int id = rs.getInt("contact_id");
                String nickname = rs.getString("nickname");
                String sha1password = rs.getString("sha1password");
                String firstName = rs.getString("first_name");
                String lastName = rs.getString("last_name");
                
                //2. konstruujemy obiekt Contact
                contact = new Contact(conn, id, nickname, sha1password, firstName, lastName);
            }
        } catch(SQLException e) {
            System.out.println("Błąd podczas pobierania kontaktu o id: " + contact_id + " z bazy danych");
        } finally {
            if(stmt != null) {
                try {
                    stmt.close();
                } catch(SQLException e) {
                }
            }
        }
        
        return contact;
    }
    
    /**
     * Pobranie z bazy danych obiektu Contact o wzkazanym nicku... Nicki powinny być unikalne
     **/
    public static Contact getContactWithNickname(Connection conn, String nick)
    {
        Contact contact = null;
        
        //1. pobieramy obiekt o nickname jak przekazany w parametrze
        PreparedStatement stmt = null;
        
        try {
            stmt = conn.prepareStatement(SELECT_CONTACT_WITH_NICKNAME);
            stmt.setString(1, nick);
            
            //wykonanie kwerendy
            ResultSet rs = stmt.executeQuery();
            if(rs.next()) {
                //odczytanie zawartości zwróconego rekordu
                int id = rs.getInt("contact_id");
                String nickname = rs.getString("nickname");
                String sha1password = rs.getString("sha1password");
                String firstName = rs.getString("first_name");
                String lastName = rs.getString("last_name");
                
                //2. kontruujemy obiekt Contact
                contact = new Contact(conn, id, nickname, sha1password, firstName, lastName);
            }
        } catch(SQLException e) {
            System.out.println("Błąd podczas próby pobrania kontaktu o nicku: " + nick);
        } finally {
            if(stmt != null) {
                try {
                    stmt.close();
                } catch(SQLException e) {
                }
            }
        }
        
        return contact;
    }
    
    /**
     * Konstruktor pustego obiektu 
     **/
    public Contact() {}
    
    /**
     * Konstruktor tworzy lokalnie nowy obiekt Contact, 
     * wymaga zapisania do bazy danych metoda save()
     **/
    public Contact(Connection conn, int id, String nickname, String sha1password, String firstName, String lastName)
    {
        //ustawienie obiektu połączenia z SQLite
        this.connection = conn;
        
        //ustawienie atrybutów obiektu Contact
        this.id = id;
        this.nickname = nickname;
        this.sha1password = sha1password;
        this.firstName = firstName;
        this.lastName = lastName;
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
    
    public String getNickname() {
        return nickname;
    }
    
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
    
    public String getSha1Password() {
        return sha1password;
    }
    
    public void setSha1Password(String sha1password) {
        this.sha1password = sha1password;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    /***/
    
    @Override
    public String toString() {
        return firstName + " " + lastName;
    }
    
    /**
     * Metoda zapisuje bieżący obiekt Contact do tablicy 
     * contact w bazie danych SQLite 
     **/
    public boolean save()
    {
        if(connection == null) {
            System.out.println("Brak obiektu Connection, nie udało się zapisać kontaktu Contact do bazy SQLite!");
            return false;
        }
        
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            
            if(id > 0) {
                //UPDATE Contact Object
                stmt = connection.prepareStatement(UPDATE_CONTACT);
                stmt.setInt(5, id);
            } else {
                //INSERT Contact Object
                stmt = connection.prepareStatement(INSERT_CONTACT,
                                         Statement.RETURN_GENERATED_KEYS);
            }
            
            stmt.setString(1, nickname);
            stmt.setString(2, sha1password);
            stmt.setString(3, firstName);
            stmt.setString(4, lastName);
            
            int affectedRows = stmt.executeUpdate();
            //jeżeli zapytanie do bazy danych zakończyło się niepowodzeniem
            if(affectedRows == 0) {
                throw new SQLException("Próba utworzenia/aktualizacji kontaktu zakończona niepowodzeniem.");
            }
            
            //jeżeli wykonano INSERT to musimy odczytać zwrócony w wyniku zapytania
            //autogenerowany identyfikator contact_id nowo dodanego kontaktu
            if(id < 1) {
                rs = stmt.getGeneratedKeys();
                if(rs.next()) {
                    setId( rs.getInt(1) );
                } else {
                    throw new SQLException("Błąd podczas odczytywania identyfikatora nowo wstawionego do bazy danych kontaktu.");
                }
            }
            
        } catch(SQLException e) {
            System.out.println("Błąd podczas zapisywania kontaktu do bazy danych.");
            return false;
        } finally {
            if(stmt != null) {
                try {
                    stmt.close();
                } catch(SQLException e) {
                }
            }
            if(rs != null) {
                try {
                    rs.close();
                } catch(SQLException e) {
                }
            }
        }
        
        return true;
    }
    
    /**
     * Metoda usuwa Contact z bazy danych
     **/
    public boolean remove()
    {
        if(connection == null) {
            System.out.println("Brak obiektu Connection, nie udało się usunąć kontaktu Contact z bazy SQLite!");
            return false;
        }
        
        PreparedStatement stmt = null;
        
        try {
            //DELETE Contact Object
            stmt = connection.prepareStatement(DELETE_CONTACT);
            stmt.setInt(1, id);
            
            stmt.executeUpdate();
            
            System.out.println("Usunięto kontakt o id: " + id + " z bazy danych.");
        } catch(SQLException e) {
            System.out.println("Błąd podczas usuwania Contact z bazy danych.");
            return false;
        } finally {
            if(stmt != null) {
                try {
                    stmt.close();
                } catch(SQLException e) {
                }
            }
        }
        
        //wymaga usuniecia również:
        //- wiadomości związanych z tym kontaktem
        Message.removeMessagesReceivedBy(connection, id);
        Message.removeMessagesSentBy(connection, id);
        //- wpisów na listy kontaktów innych użytkowników
        ContactList.removeEntriesWithContactId(connection, id);
        //- usunięcie listy kontaktów bieżącego usera
        ContactList.removeForOwnerId(connection, id);
        
        return true;
    }
    
    /**
     * Pomocnicza metoda konwertująca niezakodowane Stringowe hasło
     * na hasło Stringowe encodowane algorytmem SHA-1
     **/
    public static String SHA1(String password) throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        String sha1password;
    
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.reset();
        md.update(password.getBytes("UTF-8"));
    
        sha1password = new BigInteger(1, md.digest()).toString(16);
        
        return sha1password;
    }
    
    /**
     * Metoda dokonująca uwierzytelnienia użytkownika na podstawie bieżących pól
     * nickname oraz sha1password w bazie danych do której połączenie stanowi obiekt Connection
     * Wyniku zapytania pobierane są dane użytkownika z bazy danych które wypełniają bieżący obiekt
     * Jeżeli udało się poprawnie uwierzytelnić usera to zwracamy true, else zwracamy false
     **/
    public boolean authorizeClient()
    {
        boolean result = false;
        
        //1. Utworzenie kwerendy SQL
        PreparedStatement stmt = null;
        
        try {
            
            stmt = connection.prepareStatement(AUTHORIZE_CLIENT_SQL);
            stmt.setString(1, nickname);
            stmt.setString(2, sha1password);
            
            //wykonanie kwerendy i pobranie zbioru wyników
            ResultSet rs = stmt.executeQuery();
            if(rs.next()) {
                
                id = rs.getInt("contact_id");
                firstName = rs.getString("first_name");
                lastName = rs.getString("last_name");
                //nickname i sha1password nie wymaga aktualizacji
                
                result = true;
                
            } else {
                //nie znalaziono użytkownika o podam loginie lub haśle
                id = 0;
                result = false;
            }
            
        } catch(SQLException e) {
            System.out.println("Błąd podczas próby uwierzytelnienia użytkownika: " + nickname + " w bazie danych SQL");
            result = false;
            
        } finally {
            if(stmt != null) {
                try {
                    stmt.close();
                } catch(SQLException e) {
                }
            }
        }
        
        return result;
    }
    
    /**
     * Metoda dopasowująca na podstawie bieżących wzorcowych atrybutów:
     * nickname, firstName, lastName 
     * kontakty znajdujące się w bazie danych. W ten sposób realizuje wyszukiwanie 
     * kontaktów na podstawie bieżącego wzrocowego obietku 
     * Atrybuty wzorca są uzupełnianane do 'nickname%', 'firstName%', 'lastName%'
     **/
    public List<Contact> getMatchedContacts()
    {
        List<Contact> matchedContacts = new ArrayList<Contact>();
        
        //1. Utworzenie kwerendy SQL
        PreparedStatement stmt = null;
        
        try {
            
            stmt = connection.prepareStatement(SELECT_MATCHED_CONTACTS);
            stmt.setString(1, nickname + "%");
            stmt.setString(2, firstName + "%");
            stmt.setString(3, lastName + "%");
            
            //wykonanie kwerendy i pobranie zbioru wyników
            ResultSet rs = stmt.executeQuery();
            
            //dodanie kontaktów do listy kontaktów dopasowanych do wzorca
            while(rs.next()) {
                
                matchedContacts.add( new Contact(connection,
                                                 rs.getInt("contact_id"),
                                                 rs.getString("nickname"),
                                                 rs.getString("sha1password"),
                                                 rs.getString("first_name"),
                                                 rs.getString("last_name")
                                                )
                                    );
            }
            
        } catch(SQLException e) {
            System.out.println("Błąd podczas próby pobrania kontaktów dopasowanych do przekazanego wzroca z bazy danych SQL");
        } finally {
            if(stmt != null) {
                try {
                    stmt.close();
                } catch(SQLException e) {
                }
            }
        }
        
        return matchedContacts;
    }
    
}