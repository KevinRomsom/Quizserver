package nl.fhict.s3.whiskyapp;

public class Result {
    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    private int score;
    private Player player;



    public Result(Player player, long time, boolean answer){
        this.player = player;
        this.score = this.score + calcScore(time, answer);
    }

    public Result(Player player, int score){
        this.player = player;
        this.score = score;
    }


    public int calcScore(long time, boolean answer){
        int basepoints = 10;
        if(answer){
            int points = 0;
            if(0 > time){
                points = 0;
            }
            else if(time > 60000){
                time = 60000;
                int percent = (int) (1 - (time/60000));
                points = basepoints + (100 * percent);
            }
            else if(time <= 60000){
                double percent = ((double)1 - ((double) time / (double) 60000));
                points = (int) (basepoints + (100 * percent));
            }

            return points;
        }
        return 0;
    }


}
