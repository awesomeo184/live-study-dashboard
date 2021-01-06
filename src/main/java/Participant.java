import java.util.HashMap;
import java.util.Map;

public class Participant {
    private final String userName;
    private final Map<Integer, Boolean> rollBook;

    public Participant(String name) {
        userName = name;
        rollBook = new HashMap<>();
    }

    public String getUserName() {
        return userName;
    }

    public void checkAttendance(int weekNumber) {
        rollBook.put(weekNumber, true);
    }
}
