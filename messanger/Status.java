package messanger;

import java.io.Serializable;

/**
 * Typ Wyliczeniowy Statusu
 **/
public enum Status {
    OFFLINE(0),
    ONLINE(1),
    IDLE(2),
    INVISIBLE(3);

public static final String ONLINE_IMG = "messanger/online.png";
public static final String OFFLINE_IMG = "messanger/offline.png";
public static final String IDLE_IMG = "messanger/idle.png";
public static final String INVISIBLE_IMG = "messanger/invisible.png";
    private final int value;

    private Status(int value) {
        this.value = value;
    }

    public int getValue()
    {
        return value; 
    }
};
