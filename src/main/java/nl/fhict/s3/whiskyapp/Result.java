package nl.fhict.s3.whiskyapp;

public class Result {
    private int score;
    private Player player;

    private int basepoints = 10;

    public Result(Player player, long time, boolean answer){
        this.player = player;
        this.score = calcScore(time, answer);
    }

    public Result(Player player, int score){
        this.player = player;
        this.score = score;
    }

    //kan dit uibreiden voor mogelijk algoritmiek met een streak, bijv. kan nog wel meer ideeën verzinnen.
    public int calcScore(long time, boolean answer){
        if(answer){
            int percent = (int) (1 - (time/60000));
            int points = basepoints + (100 * percent);

            return points;
        }
        return 0;
    }


}
