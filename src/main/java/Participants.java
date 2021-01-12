import java.util.ArrayList;
import java.util.List;

public class Participants {

    private static final List<Participant> participants = new ArrayList<>();

    public static List<Participant> getList() {
        return participants;
    }

    public static Participant findParticipant(String name) {
        if (isNewParticipant(name)) {
            Participant participant = new Participant(name);
            Participants.addParticipant(participant);
            return participant;
        }

        return participants.stream()
            .filter(p -> p.getUserName().equals(name))
            .findFirst()
            .orElseThrow();
    }

    private static boolean isNewParticipant(String name) {
        return participants.stream().noneMatch(p -> p.getUserName().equals(name));
    }

    public static void addParticipant(Participant participant) {
        participants.add(participant);
    }

}
