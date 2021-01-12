

# Github API로 스터디 출석률 계산기 만들기



백기선 님이 진행하는 [자바 스터디](https://github.com/whiteship/live-study) 첫 프로그래밍 과제인 스터디 출석률 계산기 만들기 문제입니다.

스터디원들은 매주 깃헙 이슈에 올라오는 스터디 이슈에 댓글을 답니다.

댓글에는 각 주차마다 공부한 내용을 정리한 사이트의 링크를 첨부합니다.

이를 바탕으로 출석률 계산기를 만듭니다.

[최종 결과물](https://github.com/awesomeo184/live-study-dashboard/blob/master/result.md)은 이런 형태가 될 겁니다.

![img](https://blog.kakaocdn.net/dn/KiIEW/btqSIZuBo5E/nKlqHxQSpAC3QGk13uP5UK/img.jpg)



### 구현 전략

------

구현 전략은 다음과 같습니다.

\1. 스터디 시즌 1의 출석률을 계산할 것이므로 모든 이슈들 중에서 "시즌 1"이라는 라벨이 붙은 이슈만 순회합니다.

\2. 이슈를 순회하면서 각 이슈에 달린 댓글들을 확인합니다.

\3. 댓글을 단 아이디를 참여자 정보에 추가합니다. 이때 한 참여자가 하나의 이슈에 여러 댓글을 작성할 수 있으므로, 예외 로직을 구현해야 합니다.

\4. 참여자의 댓글에 **공부한 사이트의 링크가 있다면** 해당 주차에 출석 체크를 합니다.

\5. 각 참여자에 대해 총 스터디 주차에 대한 참여 주차의 비율을 계산합니다.

\6. 모든 정보를 마크다운 테이블 형태로 출력합니다.



![img](https://blog.kakaocdn.net/dn/p4I8U/btqSASX2bL7/K0C7kx8CWY2GnKMZkukvH0/img.png)

객체들의 역할은 이렇게 구성해볼 수 있을 것 같습니다.

### 구현

------

#### 깃헙 서버 연결

우선 그래들 프로젝트를 하나 만들고, [**깃헙 자바 API**](https://github-api.kohsuke.org/githubappflow.html)를 당겨옵니다. 

```java
// https://mvnrepository.com/artifact/org.kohsuke/github-api
compile group: 'org.kohsuke', name: 'github-api', version: '1.118'
```





![img](https://blog.kakaocdn.net/dn/cUVmvK/btqSDtQ4Q0s/cRaSAnprYpK94LGnG0DlK1/img.jpg)

테스트 프레임워크는 JUnit 5를 이용하겠습니다. 그리고 편리한 테스트를 위해서 [assertj](https://assertj.github.io/doc/)도 추가했습니다.

우선 깃헙에 연결하는 방법에는 아이디와 비밀번호를 이용하는 방법, Personal Access Token을 이용하는 방법, JWT Token을 이용하는 방법 등이 있습니다. 저는 Personal Access Token을 이용하겠습니다. 토큰은 깃헙 프로필 설정에서 생성할 수 있습니다.

토큰은 외부로 노출되면 안 되기 때문에 프로퍼티 파일을 이용해 토큰을 연결해줍니다. 다행히 해당 라이브러리에서 이를 편리하게 할 수 있도록 기능을 제공하고 있다고 합니다. 자세한 내용은 [문서](https://github-api.kohsuke.org/index.html)를 참조해주세요. (문서에는 디폴트로 루트 디렉토리에서 .github 파일을 찾도록 되어있다는데 제가 이해를 잘못했는지 파일을 계속 못 찾길래 그냥 설정 파일의 위치를 지정해줬습니다.)

![img](https://blog.kakaocdn.net/dn/lvy4D/btqSNmCQCWT/4Z7cA6Ha1WRzRjlnPDsKy1/img.jpg)



프로퍼티 파일을 이용해 깃헙 서버가 잘 연결이 되었습니다. 이때 getSource()를 이용하면, fork한 저장소에서 타겟 저장소의 정보를 가져올 수 있습니다. "awesomeo184/live-study"는 스터디 저장소인 "whiteship/live-study"를 fork한 저장소인데, 이 저장소로 스터디 저장소의 정보를 가져오는 것입니다.

![img](https://blog.kakaocdn.net/dn/dx7zl4/btqSQ5ncnQw/n0oEofrOCHrBly2PKSVXrK/img.jpg)



이제 본격적으로 프로그램을 구현해보도록 하겠습니다.



#### 시즌 1 이슈 가져오기

우선 출석 체크는 스터디 과제 중 시즌 1에 해당하는 과제만 할 것입니다.

![img](https://blog.kakaocdn.net/dn/dg02My/btqSGiuTjc8/Jb4bJPsHjfjGT3nZtAaTBK/img.jpg)https://github.com/whiteship/live-study/issues?q=is%3Aopen+is%3Aissue

API의 GHIssue 객체로부터 이슈에 붙어 있는 라벨을 모아놓은 Collection을 가져올 수 있습니다.

```java
GHIssue firstWeekIssue = repository.getIssue(1);

Collection<GHLabel> labelsInFirstWeek = issue.getLabels();
```

예를 들어 1주 차 과제 이슈에 draft, 과제, 시즌1이라는 라벨이 붙어있으면, labelsInFirstWeek 안에는 각각 이름이 draft, 과제, 시즌1인 GHLabel 객체가 존재합니다.

우리는 이슈들 중에서 "시즌1"이라는 라벨이 붙은 이슈만 가져오고 싶으므로 이를 필터링해서 가져옵니다.

```java
List<GHIssue> issues = repository.getIssues(GHIssueState.ALL);  // Closed 여부 상관없이 모든 이슈를 가져온다.

List<GHIssue> firstSeasonIssues = issues.stream()    //이름이 시즌1인 라벨을 가진 이슈만 필터링한다. 
		.filter(i -> i.getLabels().stream()
        	.anyMatch(l -> l.getName().equals("시즌1")))
        .collect(Collectors.toList());
```



현재 시즌1 스터디는 15주 차까지 있습니다. 그러므로 firstSeasonIssue의 크기는 15가 되어야 합니다. 이를 검증하는 코드를 작성합니다.

```java
class GithubAPITest {

    GitHub gitHub;
    GHRepository repository;

    GithubAPITest() throws IOException {
        gitHub = GitHubBuilder.fromPropertyFile(".github.properties").build();
        repository = gitHub.getRepository("awesomeo184/live-study").getSource();
    }

    @Test
    @DisplayName("시즌 1의 이슈를 잘 가져오는가")
    void getFirstSeasonIssues() throws IOException {
        List<GHIssue> issues = repository.getIssues(GHIssueState.ALL);
        
        List<GHIssue> firstSeasonIssues = issues.stream()
            .filter(i -> i.getLabels().stream()
                .anyMatch(l -> l.getName().equals("시즌1")))
            .collect(Collectors.toList());
        
        assertThat(firstSeasonIssues.size()).isEqualTo(15);
    }

}
```



#### 댓글 가져오기

시즌 1의 이슈를 성공적으로 가져왔으니, 이제 각 이슈마다 달린 댓글들을 가져올 차례입니다.

댓글을 가져오는 테스트를 진행하다가 **firstSeasonIssues**를 가져왔을 때, 리스트 내 순서가 예상과 반대로 뒤집혀있는 것을 확인했습니다.

즉 0번 인덱스에 15주차 이슈가, 14번 인덱스에 1주 차 과제가 들어있었습니다. 스트림을 필터링 할때 순서 정보가 바뀌는 건지 애초에 API에서 가져올 때 뒤에서부터 가져오는지는 잘 모르겠습니다. 추후에 알아봐야 할 것 같습니다. 일단은 리스트를 이슈 순서대로 정렬하는 코드를 추가해줍니다.

```java
firstSeasonIssues = issues.stream()
	.filter(i -> i.getLabels().stream()
		.anyMatch(l -> l.getName().equals("시즌1")))
	.sorted(Comparator.comparing(GHIssue::getNumber))
	.collect(Collectors.toList());
```

getNumber 메서드는 해당 이슈의 번호를 가져오는 메서드입니다. 이슈 번호대로 스트림을 정렬해줍니다.

이제 1주 차 이슈에 달린 댓글을 잘 가져오는지 테스트 해보겠습니다. 현재 시간을 기준으로 1주차 과제에는 293개의 댓글이 달려있습니다.

![img](https://blog.kakaocdn.net/dn/bAgK0H/btqSKM3mWdr/h73gK4ebXGRjmNThKKky1k/img.jpg)

```java
    @Test
    @DisplayName("이슈에 달린 댓글들을 잘 가져오는가")
    void getComments() throws IOException {
        List<GHIssue> issues = repository.getIssues(GHIssueState.ALL);

        firstSeasonIssues = issues.stream()
            .filter(i -> i.getLabels().stream()
                .anyMatch(l -> l.getName().equals("시즌1")))
            .sorted(Comparator.comparing(GHIssue::getNumber))
            .collect(Collectors.toList());
        
        GHIssue firstWeekIssue = firstSeasonIssues.get(0);
        List<GHIssueComment> comments = firstWeekIssue.getComments();

        assertThat(comments.size()).isEqualTo(293);
    }
```

테스트는 항상 통과해야 하는데, 이 경우에는 1주 차 댓글의 개수가 계속해서 바뀔 수 있으므로 다른 방식으로 테스트를 해야 할 것 같다는 생각이 듭니다만, 현재로서는 어떻게 해야 할지 잘 모르겠네요.

댓글을 확인하려면 이전 테스트에서 사용했던 fisrtSeasonIssues를 다시 생성해야 하는 문제가 있습니다. 뭔가 방법이 있을 것 같아 찾아보던 도중 테스트 내 인스턴스를 공유하는 방법을 알게 되었습니다. JUnit 5에 대해 아직 잘 몰라서 이렇게 사용하는 것이 맞는지는 모르겠는데, 암튼 코드를 다음과 같이 고쳐봤습니다. 

```java
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
```



깃헙 API에 대해서는 이제 어느정도 알 것 같으니 본격적으로 로직을 짜보도록 하겠습니다.

시즌 1 이슈를 순회하면서 댓글들을 확인하고 댓글에 URL 정보가 있으면 해당 댓글의 작성자를 참석자 정보에 담습니다.

```java
    public static void main(String[] args) throws IOException {
        GitHub gitHub = GitHubBuilder.fromPropertyFile(".github.properties").build();
        GHRepository repository = gitHub.getRepository("awesomeo184/live-study").getSource();
        List<GHIssue> allIssues = repository.getIssues(GHIssueState.ALL);


        List<String> participants = new ArrayList<>();

        List<GHIssue> firstSeasonIssues = getFirstSeasonIssues(allIssues);
        for (GHIssue issue : firstSeasonIssues) {
            List<GHIssueComment> comments = issue.getComments();
            for (GHIssueComment comment : comments) {
                if (comment.getUrl() != null) {
                    String userName = comment.getUser().getLogin();
                    participants.add(userName);
                }
            }
        }
        
    }
```



참석자 이름을 넣으면서 출석 체크를 해야하는데, 어떤 방식으로 해야할까요?

일단, 매주 이슈를 순회하면서 participants에 참석자 이름을 넣습니다. 이때 participants 안에 참석자의 이름이 없다면 (ex. 해당 주차에 처음 스터디에 참여한 경우), 새로 이름을 넣어주고 출석을 체크합니다. 만약 이름이 이미 존재한다면? 기존에 스터디에 참여하고 있던 사람이므로 바로 출석 체크를 해줍니다.

출석 체크는 어떻게 해야할까요. 참석자는 15 개의 스터디에 대해 출석을 했는지 안했는지에 대한 정보를 가지고 있어야합니다. 그러므로 참여자 객체를 새로 만들고 참여자가 스터디 번호를 Key로, 참석 여부를 Value로 갖는 테이블을 들고 있도록 만들어 줍니다.

```java
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
```



```java
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
```



이슈를 도는 부분이 인덴트가 너무 깊고 전체적으로 산만해서 리팩토링을 했습니다.

```java
public class App {

    public static void main(String[] args) throws IOException {
        GitHub gitHub = GitHubBuilder.fromPropertyFile(".github.properties").build();
        GHRepository repository = gitHub.getRepository("awesomeo184/live-study").getSource();
        List<GHIssue> allIssues = repository.getIssues(GHIssueState.ALL);


        List<GHIssue> firstSeasonIssues = getFirstSeasonIssues(allIssues);
        for (GHIssue issue : firstSeasonIssues) {
            rotateCommentsWithCheckAttendance(issue);
        }

    }

    private static void rotateCommentsWithCheckAttendance(GHIssue issue) throws IOException {
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
public class Participants {

    private static final List<Participant> participants = new ArrayList<>();

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
```



#### 출석률 계산

이제 만들어진 rollbook을 바탕으로 출석률을 계산합니다. 매개변수를 double로 받아 소수점으로 계산이 가능하도록 형변환을 해줍니다.

```java
public double getRate(double totalWeeks) {
    long count = rollBook.values().stream().filter(value -> value).count();

    return count * 100 / totalWeeks;
}
```



그리고 각 참석자의 출석 현황을 마크다운 테이블 형식으로 반환해줍니다. 만약 해당 주차에 출석을 했다면 체크 표시를 해주고 아니라면 그냥 넘어갑니다.

```java
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
```



#### 마크다운 파일로 결과 반환

PrintWriter 객체를 만들어서 결과 파일을 생성해주고, 결과를 마크다운 테이블 형식으로 반환할 수 있도록 처리해줬습니다.

```java
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
```



