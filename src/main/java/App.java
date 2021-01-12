import java.io.IOException;
import java.io.PrintWriter;
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


        List<GHIssue> firstSeasonIssues = getFirstSeasonIssues(allIssues);
        for (GHIssue issue : firstSeasonIssues) {
            rotateCommentsWithCheckingAttendance(issue);
        }

        int totalWeeks = firstSeasonIssues.size();
        PrintWriter pw = new PrintWriter("result.md");

        StringBuilder title = new StringBuilder();
        StringBuilder tableMarkDown = new StringBuilder();
        title.append("| 참여자 ");
        for (int i = 1; i <= totalWeeks; i++) {
            String result = "| " + i + "주차 ";
            title.append(result);
            tableMarkDown.append("| --- ");
        }
        title.append("| 출석률 |");
        tableMarkDown.append("| --- | --- |");

        pw.println(title.toString());
        pw.println(tableMarkDown.toString());


        List<Participant> participants = Participants.getList();
        participants.forEach(p -> {
            String row = String.format("| %s %s | %.2f%% |",
                p.getUserName(), p.makeRow(totalWeeks), p.getRate(totalWeeks));
            pw.println(row);
        });

        pw.close();
    }

    private static void rotateCommentsWithCheckingAttendance(GHIssue issue) throws IOException {
        int weekNumber = issue.getNumber();
        List<GHIssueComment> comments = issue.getComments();
        for (GHIssueComment comment : comments) {
            checkAttendance(weekNumber, comment);
        }
    }

    private static void checkAttendance(int weekNumber, GHIssueComment comment) throws IOException {
        if (hasUrl(comment)) {
            String userName = comment.getUser().getLogin();
            Participant participant = Participants.findParticipant(userName);
            participant.checkAttendance(weekNumber);
        }
    }

    private static boolean hasUrl(GHIssueComment comment) {
        return comment.getUrl() != null;
    }

    private static List<GHIssue> getFirstSeasonIssues(List<GHIssue> issues) {
        return issues.stream()
            .filter(i -> i.getLabels().stream()
                .anyMatch(l -> l.getName().equals("시즌1")))
            .sorted(Comparator.comparing(GHIssue::getNumber))
            .collect(Collectors.toList());
    }

}
