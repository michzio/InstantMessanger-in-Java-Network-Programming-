package model;

import java.io.Serializable;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;
import java.util.ArrayList;


public class ContactList implements Serializable
{
    private Connection connection;
    
    private int list_owner_id;
    private int contact_id;
    //private transient int something; - nie jest serializowane
    private static final String INSERT_LIST_ENTRY = "INSERT INTO contact_list VALUES (?,?);";
    private static final String DELETE_LIST_ENTRY = "DELETE FROM contact_list WHERE list_owner_id = ? AND contact_id = ?;";
    private static final String SELECT_CONTACT_LIST = "SELECT c.* FROM contact c INNER JOIN contact_list cl ON (c.contact_id = cl.contact_id) WHERE cl.list_owner_id = ?;";
    private static final String DELETE_CONTACT_LIST = "DELETE FROM contact_list WHERE list_owner_id = ?";
    private static final String DELETE_ENTRIES_WITH_CONTACT = "DELETE FROM contact_list WHERE contact_id = ?";

    /**
     * Metoda pobierająca listę kontaktów Contact dla danego 
     * użytkownika owner_id
     **/
    public static List<Contact> getForOwnerId(Connection conn, int owner_id)
    {
        List<Contact> contactList = new ArrayList<Contact>();
        
        //1. pobieranie obiektów kontaktu dla owner_id używając
        //   connection
        
        PreparedStatement stmt = null;
        
        try {
            stmt = conn.prepareStatement(SELECT_CONTACT_LIST);
            stmt.setInt(1,owner_id);
            
            //wykonanie kwerendy
            ResultSet rs = stmt.executeQuery();
            
            //2. konstrukcja listy obiektów Contact
            while(rs.next()) {
                //dodanie kontaktu na liste kontaktów
                contactList.add( new Contact(conn,
                                             rs.getInt("contact_id"),
                                             rs.getString("nickname"),
                                             rs.getString("sha1password"),
                                             rs.getString("first_name"),
                                             rs.getString("last_name")
                                             )
                );
            }
            
            
        } catch(SQLException e) {
            System.out.println("Błąd podczas pobierania listy kontaktów dla użytkownika " + owner_id + " z bazy danych");
        } finally {
            if(stmt != null) {
                try {
                   stmt.close();
                } catch(SQLException e) {
                }
            }
        }
        
        return contactList;
    }
    
    /**
     * Metoda usuwa liste kontaktów dla wybraneco uzytkownika onwer_id
     **/
    public static boolean removeForOwnerId(Connection conn, int owner_id)
    {
        PreparedStatement stmt = null;
        
        try {
            
            stmt = conn.prepareStatement(DELETE_CONTACT_LIST);
            stmt.setInt(1, owner_id);
            
            stmt.executeUpdate();
            
            System.out.println("Usunięto listę kontaktu dla usera o id: " + owner_id + " z bazy danych");
            
        } catch(SQLException e) {
            System.out.println("Błąd podczas usuwania listy kontaktów dla usera: " + owner_id + " z bazy danych");
            return false;
        } finally {
            if(stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                }
            }
        }
        
        return true;
    }
    
    /**
     * Metoda usuwa wpisy na listach kontaktów z wybranym contact_id
     **/
    public static boolean removeEntriesWithContactId(Connection conn, int contact_id) {
        
        
        
        PreparedStatement stmt = null;
        
        try {
            stmt = conn.prepareStatement(DELETE_ENTRIES_WITH_CONTACT);
            stmt.setInt(1, contact_id);
        
            stmt.executeUpdate();
            
            System.out.println("Usunięto wpisy kontaktów z id kontaktu: " + contact_id + " z bazy danych");
            
        } catch(SQLException e) {
            System.out.println("Błąd podczas usuwania wpisów kontaktów z id kontaktu: " + contact_id);
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
     * Konstruktor pustego wpisu na liste kontaktów
     **/
    public ContactList() { }
    
    /**
     * Konstruktor tworzący nowy wpis na liście kontaktów 
     * Aby zapisac do bazy danych nalezy uzyc metody save()
     **/
    public ContactList(Connection conn, int owner_id, int contact_id)
    {
        //ustawienie obiektu ustanawiania połączenia z SQLite
        this.connection = conn;
        
        //ustawianie atrybutów
        this.list_owner_id = owner_id;
        this.contact_id = contact_id;
    }
    
    /**
     * Ustawianie i pobieranie atrybutów wpisu na listę kontaktów
     **/
    public int getListOwnerId() {
        return list_owner_id;
    }
    
    public void setListOwnerId(int owner_id) {
        this.list_owner_id = owner_id;
    }
    
    public int getContactId() {
        return contact_id;
    }
  
    public void setContactId(int contact_id) {
        this.contact_id = contact_id;
    }
    
    /***/
    
    /**
     * Metoda zapisuje wpisa listy kontaktów do bazy danych SQLite
     * w tablicy contact_list
     **/
    public boolean save() {
        if(connection == null) {
            System.out.println("Brak obiektu Connection, nie udało się zapisać wpisu na listę kontaktów ContactList do bazy SQLite!");
            return false;
        }
        
        PreparedStatement stmt = null;
        
        try {
            stmt = connection.prepareStatement(INSERT_LIST_ENTRY);
            stmt.setInt(1,list_owner_id);
            stmt.setInt(2,contact_id);
            
            stmt.executeUpdate();
            
        } catch(SQLException e)
        {
            System.out.println("Błąd podczas zapisywania kontaktu na liste kontaktów do bazy danych.");
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
     * Metoda usuwa bieżący wpis z listy kontaktów ContactList 
     * w bazie danych SQLite z tablicy contact_list
     **/
    public boolean remove() {
        if(connection == null) {
            System.out.println("Brak obiektu Connection, nie udało się usunąć wpisu na listę kontaktów ContactList z bazy SQLite!");
            return false;
        }
        
        PreparedStatement stmt = null;
        
        try {
            stmt = connection.prepareStatement(DELETE_LIST_ENTRY);
            stmt.setInt(1, list_owner_id);
            stmt.setInt(2, contact_id);
            
            stmt.executeUpdate();
            
            System.out.println("Usunięto wpis: (" + list_owner_id + "," + contact_id + ") z listy kontaktów w bazie danych.");
            
        } catch (SQLException e)
        {
            System.out.println("Błąd podczas usuwania wpisu kontaktu z listy kontaktów z bazy danych!");
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