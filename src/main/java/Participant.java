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

    public double getRate(double totalWeeks) {
        long count = rollBook.values().stream().filter(value -> value).count();

        return count * 100 / totalWeeks;
    }

    public String makeRow(int totalWeeks) {
        StringBuilder result = new StringBuilder();
        for (int i = 1 ; i <= totalWeeks; i++) {
            if(rollBook.containsKey(i) && rollBook.get(i)) {
                result.append("|:white_check_mark:");
            } else {
                result.append("|");
            }
        }
        return result.toString();
    }
}
