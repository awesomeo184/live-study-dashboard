import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;


@TestInstance(Lifecycle.PER_CLASS)
class GithubAPITest {

    GitHub gitHub;
    GHRepository repository;
    List<GHIssue> firstSeasonIssues;

    @BeforeAll
    void setUp() throws IOException {
        gitHub = GitHubBuilder.fromPropertyFile(".github.properties").build();
        repository = gitHub.getRepository("awesomeo184/live-study").getSource();
        List<GHIssue> issues = repository.getIssues(GHIssueState.ALL);

        firstSeasonIssues = issues.stream()
            .filter(i -> i.getLabels().stream()
                .anyMatch(l -> l.getName().equals("시즌1")))
            .sorted(Comparator.comparing(GHIssue::getNumber))
            .collect(Collectors.toList());
    }

    @Test
    @DisplayName("시즌 1의 이슈를 잘 가져오는가")
    void getFirstSeasonIssues() throws IOException {
        assertThat(firstSeasonIssues.size()).isEqualTo(15);
    }

    @Test
    @DisplayName("이슈에 달린 댓글들을 잘 가져오는가")
    void getComments() throws IOException {
        GHIssue firstWeekIssue = firstSeasonIssues.get(0);
        List<GHIssueComment> comments = firstWeekIssue.getComments();

        assertThat(comments.size()).isEqualTo(293);
    }


}
