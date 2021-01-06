import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

public class App {

    public static void main(String[] args) throws IOException {
        GitHub gitHub = GitHubBuilder.fromPropertyFile(".github.properties").build();
        GHRepository repository = gitHub.getRepository("awesomeo184/live-study").getSource();
        List<GHIssue> allIssues = repository.getIssues(GHIssueState.ALL);

        List<Participant> participants = new ArrayList<>();

        List<GHIssue> firstSeasonIssues = getFirstSeasonIssues(allIssues);
        for (GHIssue issue : firstSeasonIssues) {
            int weekNumber = issue.getNumber();
            List<GHIssueComment> comments = issue.getComments();
            for (GHIssueComment comment : comments) {
                if (comment.getUrl() != null) {
                    String userName = comment.getUser().getLogin();
                    Participant participant = findParticipant(participants, userName);
                    participant.checkAttendance(weekNumber);
                }
            }
        }
    }

    private static Participant findParticipant(List<Participant> participants, String userName) {
        if (isNewUser(participants, userName)) {
            Participant participant = new Participant(userName);
            participants.add(participant);
            return participant;
        }
        return participants.stream()
            .filter(p -> p.getUserName().equals(userName))
            .findFirst()
            .orElseThrow();
    }

    private static boolean isNewUser(List<Participant> participants, String userName) {
        return participants.stream().noneMatch(p -> p.getUserName().equals(userName));
    }

    private static List<GHIssue> getFirstSeasonIssues(List<GHIssue> issues) {
        return issues.stream()
            .filter(i -> i.getLabels().stream()
                .anyMatch(l -> l.getName().equals("시즌1")))
            .sorted(Comparator.comparing(GHIssue::getNumber))
            .collect(Collectors.toList());
    }

}
