package si.showdown.owainbot;

import java.util.concurrent.ThreadLocalRandom;

public class BotUtils {
	public static int randomNumber(int min, int max) {
		return ThreadLocalRandom.current().nextInt(min, max+1);
	}
}
