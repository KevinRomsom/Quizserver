package nl.fhict.s3.whiskyapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.scheduling.config.Task;
import org.springframework.stereotype.Controller;
import org.springframework.web.client.RestTemplate;

import org.apache.http.impl.client.HttpClients;
import org.springframework.web.util.HtmlUtils;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.*;

@Controller
public class GreetingController {
    ClientHttpRequestFactory requestFactory = new
            HttpComponentsClientHttpRequestFactory(HttpClients.createDefault());
    RestTemplate restTemplate = new RestTemplate(requestFactory);
    String datalayerResourceUrlUser = "http://localhost:10000/users";
    String getDatalayerResourceUrlWhisky = "http://localhost:10000/whiskys";

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private SimpMessagingTemplate template;

    private static int count = -1;

    private static List<Result> playerresults = new ArrayList<Result>();

    private static List<Player> playersanswered = new ArrayList<Player>();

    private static List<Question> questions = new ArrayList<Question>();

    private long starttime;

    private boolean questionsinitialized = false;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);

    private ScheduledFuture<?> resultFutureTimer;
    private ScheduledFuture<?> resultFutureAllAnswered;
    private ScheduledFuture<?> resultFutureTimerForTimer;

    private boolean afterforcepush = false;
    long maxtime = 60000L;
    private boolean lastquestion = false;

    private final Runnable tasktimer = () -> {
        try {
            if(resultFutureTimerForTimer != null){
                resultFutureTimer = null;
            }
            playersanswered.clear();
            forcePushResults();
            afterforcepush = true;
        }catch (InterruptedException e){
        }catch (Exception e){
            e.printStackTrace();
        }
    };

    private final Runnable taskallanswered = () -> {

        try{
            System.out.println("hoi");
            playersanswered.clear();
            System.out.println("playersanswered" + playersanswered);
            questionNext();
            System.out.println("na nextquestion");
        }catch(InterruptedException e){

        }catch (Exception e) {
            e.printStackTrace();
        }
    };

    private final Runnable tasktimerfortimer = () -> {
        try{
            resultFutureTimer = null;
            resultFutureTimer = scheduler.schedule(tasktimer, maxtime-(System.currentTimeMillis() - starttime), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    };

    @MessageMapping("/force")
    @SendTo("/topic/forceresults")
    public JsonNode forcePushResults() throws Exception{

        ArrayNode array = mapper.valueToTree(playerresults);
        ObjectNode objectNode = mapper.createObjectNode();
        objectNode.putArray("playerresultlist").addAll(array);

        return objectNode;

    }

    @MessageMapping("/hello")
    @SendTo("/topic/greetings")
    public JsonNode initResult(JsonNode userid) throws Exception{
        List<Result> initpresentplayer = new ArrayList<Result>();
        JsonNode node = restTemplate.getForObject(datalayerResourceUrlUser +"/" + userid.get("userid").asInt(), JsonNode.class);
        Player ownplayer = new Player(node.path("id").intValue(), node.path("username").textValue());
        Result owninitresult = new Result(ownplayer, 0);
        for ( Result result : playerresults){
            if(result.getPlayer().getId() == ownplayer.getId()){
                ArrayNode array = mapper.valueToTree(playerresults);
                ObjectNode objectNode = mapper.createObjectNode();
                objectNode.putArray("playerresultlist").addAll(array);
                return objectNode;
            }
        }
        initpresentplayer.add(owninitresult);
        if(playerresults.size() > 0){
            initpresentplayer.addAll(playerresults);
        }
        playerresults.add(owninitresult);

        ArrayNode array = mapper.valueToTree(initpresentplayer);
        ObjectNode objectNode = mapper.createObjectNode();
        objectNode.putArray("playerresultlist").addAll(array);
        return objectNode;
    }

    @MessageMapping("/personanswer")
    @SendTo("/topic/resultplayer")
    public JsonNode result(JsonNode playeranswer) throws Exception{
        resultFutureAllAnswered = null;
        //eigenlijk dit tijdelijk wegschrijven in geval dat men ongeveer op dezelfde tijd antwoord
        long timetoanswer = System.currentTimeMillis() - starttime;

        //testen vraag beantwoorden hier gebleven
        Question question = null;
        for(Question item : questions){
            if(item.getQuestionid() == playeranswer.path("id").asInt()){
                 question = item;
                 break;
            }
        }

        //tip: hashmap playerid
        Player player = null;
        for(Result item : playerresults){
            if(item.getPlayer().getId() == playeranswer.path("userid").asInt()){
                player = item.getPlayer();
                playersanswered.add(player);
                break;
            }
        }

        boolean answerresult = question.checkAnswer(playeranswer.path("content").asText());
        //controleren of de calc ook wordt uitgevoerd voor het item verzonden wordt.
        Result result =  new Result(player, timetoanswer, answerresult);


        for (int i =0; i < playerresults.size(); i++){
            if(playerresults.get(i).getPlayer().getId() == playeranswer.path("userid").asInt()){
                playerresults.set(i, result);
            }
        }
        if(playersanswered.size() == playerresults.size()){
            resultFutureTimer.cancel(true);
            if(!lastquestion) {
               resultFutureAllAnswered = scheduler.schedule(taskallanswered, 10, TimeUnit.SECONDS);
            }
        }




        OutputStream outputStream = new ByteArrayOutputStream();
        ObjectNode objectNode = mapper.createObjectNode();
        objectNode.put("timetoanswer", (int) timetoanswer/1000);
        objectNode.put("score", result.getScore());
        ObjectNode objectNode1 = objectNode.putObject("player");
        objectNode1.put("id", result.getPlayer().getId());
        objectNode1.put("name", result.getPlayer().getName());
        mapper.writeValue(outputStream, objectNode1);



        return objectNode;
    }

    //gaat hier een probleem ontstaan met aanroepen van next question. wordt wrs meer over en weer met frontend
    @MessageMapping("newquestion")
    @SendTo("/topic/newquestion")
    public JsonNode newQuestion() throws Exception{
        count = -1;
        ObjectNode node = mapper.createObjectNode();
        if(!questionsinitialized){
            initQuestions();
            questionsinitialized = true;
        }
        else if(afterforcepush){
            afterforcepush = false;
            count++;
            node.put("questionid", questions.get(count).getQuestionid());
            node.put("question", questions.get(count).getQuestion());
            ArrayNode array = mapper.valueToTree(questions.get(count).getAnswers());
            node.putArray("answers").addAll(array);
            starttime = System.currentTimeMillis();
            resultFutureTimerForTimer = scheduler.schedule(tasktimerfortimer, 100, TimeUnit.MILLISECONDS);
            return node;
        }
        count++;
        node.put("questionid", questions.get(count).getQuestionid());
        node.put("question", questions.get(count).getQuestion());
        ArrayNode array = mapper.valueToTree(questions.get(count).getAnswers());
        node.putArray("answers").addAll(array);
        resultFutureTimer = scheduler.schedule(tasktimer, maxtime, TimeUnit.MILLISECONDS);
        starttime = System.currentTimeMillis();
        return node;
    }

    public void questionNext() throws Exception{
        ObjectNode node = mapper.createObjectNode();
        if(afterforcepush){
            afterforcepush = false;
            count++;
            node.put("questionid", questions.get(count).getQuestionid());
            node.put("question", questions.get(count).getQuestion());
            ArrayNode array = mapper.valueToTree(questions.get(count).getAnswers());
            node.putArray("answers").addAll(array);
            starttime = System.currentTimeMillis();
            resultFutureTimerForTimer = scheduler.schedule(tasktimerfortimer, 100, TimeUnit.MILLISECONDS);
            if(questions.size() == count + 1){
                node.put("lastquestion", "yes");
                lastquestion = true;
            }
            resultFutureTimer = scheduler.schedule(tasktimer, 60000, TimeUnit.MILLISECONDS);
            starttime = System.currentTimeMillis();
            template.convertAndSend("/topic/questionnext", node);

        }
        count++;
        node.put("questionid", questions.get(count).getQuestionid());
        node.put("question", questions.get(count).getQuestion());
        ArrayNode array = mapper.valueToTree(questions.get(count).getAnswers());
        node.putArray("answers").addAll(array);
        if(questions.size() == count + 1){
            node.put("lastquestion", "yes");
            lastquestion = true;
        }
        else{
            resultFutureTimer = scheduler.schedule(tasktimer, 60000, TimeUnit.MILLISECONDS);
            starttime = System.currentTimeMillis();
        }


        template.convertAndSend("/topic/questionnext", node);
    }

    private void initQuestions(){
        Question generatedquestion = generateQuestion("Rokerig");
        Question question2 = new Question(2, "Welk van deze whisky heeft een erg aanwezige honingtoon?", "Dalwhinnie 15yrs",
        "Scapa Skiren", "The Ardmore Legacy", "Lagavulin 16yrs", "Dalwhinnie 15yrs");
        Question question3 = new Question(3, "Welk van deze whisky heeft de Mild categorie?", "Ardbeg 10yrs",
                "Glenlivet Founders Reserve", "The Ardmore Legacy", "Lagavulin 16yrs", "Glenlivet Founders Reserve");
        Question question4 = new Question(4, "Welk van deze whisky is niet Schots?", "Dalwhinnie 15yrs",
                "Scapa Skiren", "Tomintoul 12yrs", "Kavalan Blenders Select", "Kavalan Blenders Select");
        Question question5 = new Question(5, "Welk van deze whisky is Nederlands?", "Jack daniels no7",
                "Dimple 15yrs", "Kavalan Blenders Select", "Millstone 5yrs", "Millstone 5yrs");

        questions.add(generatedquestion);
        questions.add(question2);
        questions.add(question3);
        questions.add(question4);
        questions.add(question5);

    }

    private Question generateQuestion(String kind){
        Random random = new Random();

        JsonNode node = restTemplate.getForObject("http://localhost:10000/whiskys" + "kind" + "?kind=" + kind, JsonNode.class);
        String question = "Welk van de volgende whiskies is rokerig?";
        //dit is voor een random rokerige whisky
        JsonNode jnrokerigewhisky = node.path("WhiskyKindList").path(random.nextInt(node.path("WhiskyKindList").size()));
        String rokerigewhisky = jnrokerigewhisky.path("name").asText();

        JsonNode node1 = restTemplate.getForObject(getDatalayerResourceUrlWhisky + "kindreverse" + "?kind=" + kind, JsonNode.class);
        List<String> templist = new ArrayList<String>();
        int a = 0;
        List<Integer> astore = new ArrayList<Integer>();
        for (int i = 0; i < 3; i++){

            a=random.nextInt(node1.path("WhiskyKindList").size());
            for(int j = 0; j < astore.size(); j++){
                if(a == astore.get(j)){
                    a=random.nextInt(node1.path("WhiskyKindList").size());
                    j = 0;
                }
            }
            astore.add(a);
            ObjectNode onode = (ObjectNode) node1.path("WhiskyKindList").path(a);
            String nietrokerigewhisky = onode.path("name").asText();
            templist.add(nietrokerigewhisky);
        }
        templist.add(rokerigewhisky);

        Question questionToSave = new Question(1, question, templist.get(0), templist.get(1), templist.get(2), templist.get(3), rokerigewhisky);
        return questionToSave;
    }
}
