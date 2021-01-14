package nl.fhict.s3.whiskyapp;

import java.util.ArrayList;
import java.util.List;

public class Question {
    private int questionid;
    private String question;
    private List<String> answers = new ArrayList<String>();
    private String correctanswer;

    public Question(int questionid, String question, String answer1, String answer2, String answer3, String answer4, String correctanswer){
        this.question = question;
        this.correctanswer = correctanswer;
        this.questionid = questionid;
        answers.add(answer1);
        answers.add(answer2);
        answers.add(answer3);
        answers.add(answer4);
    }

    public int getQuestionid() {
        return questionid;
    }


    public boolean checkAnswer(String answer){
        if(correctanswer.equals(answer)){
            return true;
        }
        return false;
    }
}
