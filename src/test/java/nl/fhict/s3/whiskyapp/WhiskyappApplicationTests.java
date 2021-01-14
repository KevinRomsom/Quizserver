package nl.fhict.s3.whiskyapp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class WhiskyappApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    public void scoreShouldSucceedMax(){
        Player player = new Player(1, "Kevin");
        Result result = new Result(player, 0);

        int calcresult = result.calcScore(1, true);

        assertEquals(110, calcresult);
    }

    @Test
    public void scoreShouldBeZeroWrongAnswer(){
        Player player = new Player(1, "Kevin");
        Result result = new Result(player, 0);

        int calcresult = result.calcScore(1, false);

        assertEquals(0, calcresult);
    }

    @Test
    public void scoreShouldBeZeroWrongTimeValueNegative(){
        Player player = new Player(1, "Kevin");
        Result result = new Result(player, 0);

        int calcresult = result.calcScore(-100, true);

        assertEquals(0, calcresult);
    }

    @Test
    public void scoreShouldBeMinimalScoreWrongTimeValueMoreThanMax(){
        Player player = new Player(1, "Kevin");
        Result result = new Result(player, 0);

        int calcresult = result.calcScore(80000, true);

        assertEquals(10, calcresult);
    }
}
