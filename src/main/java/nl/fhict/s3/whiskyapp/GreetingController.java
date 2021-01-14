package nl.fhict.s3.whiskyapp;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@Controller
public class GreetingController {
    private static int count = -1;

    private static List<Player> players = new ArrayList<Player>();

    private static List<Question> questions = new ArrayList<Question>();

    private long starttime;

    TimerTask task = new TimerTask() {
        @Override
        public void run() {
            //todo: door de lijst van players lopen en vergelijken met temp lijst die hebben
            //geantwoord. Iedereen die niet heeft geantwoord 0 punten en force push results en nieuwe vraag.
            //reset temp dingen
        }
    };

    Timer timer = new Timer("Timer");
    long maxtime = 60000L;

    @MessageMapping("/hello")
    @SendTo("/topic/greetings")
    public Greeting greeting(HelloMessage message) throws Exception{
        Thread.sleep(1000); //simulated delay
        return new Greeting("Hello, " + HtmlUtils.htmlEscape(message.getName()), "pieter " + count);
    }

    @MessageMapping("/personanswer")
    @SendTo("/topic/resultplayer") //todo: nog playerid toevoegen in frontend en zorgen dat de players overal goed worden ingezet
    public Result result(String answer, String questionid, String playerid) throws Exception{
        long timetoanswer = System.currentTimeMillis() - starttime;
        timer.cancel();
        int idquestion = Integer.parseInt(questionid);
        int idplayer = Integer.parseInt(playerid);

        Question question = null;
        for(Question item : questions){
            if(item.getQuestionid() == idquestion){
                 question = item;
            }
        }

        //tip: hashmap playerid
        Player player = null;
        for(Player item : players){
            if(item.getId() == idplayer){
                player = item;
            }
        }

        boolean answerresult = question.checkAnswer(answer);
        //controleren of de calc ook wordt uitgevoerd voor het item verzonden wordt.
        return new Result(player, timetoanswer, answerresult);
    }

    //gaat hier een probleem ontstaan met aanroepen van next question. wordt wrs meer over en weer met frontend
    @MessageMapping("newquestion")
    @SendTo("/topic/nextquestion")
    public Question nextQuestion(){
        count++;
        //todo: if schrijven voor wanneer alle players geantwoord hebben. Iets met templist wrs.
        timer.schedule(task, maxtime);
        starttime = System.currentTimeMillis();
        return questions.get(count);
    }

    private void initQuestions(){

    }
}
