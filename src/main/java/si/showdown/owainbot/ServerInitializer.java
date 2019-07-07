package si.showdown.owainbot;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.vdurmont.emoji.EmojiParser;

@SpringBootApplication
public class ServerInitializer implements ApplicationRunner {
	public void run(ApplicationArguments args) throws FileNotFoundException, IOException {
		boolean prod = true;

		String token = "";
		if (prod) {
			Properties properties = new Properties();
			properties.load(new FileInputStream("webapps/owainbot/WEB-INF/classes/config.properties"));

			token = properties.getProperty("prod");
		} else {
			Properties properties = new Properties();
			properties.load(new FileInputStream("src/main/resources/config.properties"));

			token = properties.getProperty("test");
		}

		DiscordApi api = new DiscordApiBuilder().setToken(token).login().join();

		api.updateActivity("!help for commands");

		api.addMessageCreateListener(event -> {
			if (event.getMessageContent().equalsIgnoreCase("!help")) {
				event.getMessage().getUserAuthor().get().sendMessage(Constants.HELP);
			} else if (event.getMessageContent().equalsIgnoreCase("!bard")) {
				event.getChannel().sendMessage(Constants.BARD_LINK);
			} else if (event.getMessageContent().equalsIgnoreCase("!eb")) {
				event.getChannel().sendMessage(Constants.EB_LINK);
			} else if (event.getMessageContent().equalsIgnoreCase("!source")) {
				event.getChannel().sendMessage(Constants.SOURCE);
			} else if (event.getMessageContent().equalsIgnoreCase("!countdown")) {
				event.getChannel().sendMessage(getCountDown());
			} else if (event.getMessageContent().startsWith("!poll ")) {
				createPoll(event);
			} else if (event.getMessageContent().startsWith("!roll ")) {
				rollDice(event);
			} else if (event.getMessageContent().startsWith("!play ")) {
				play(event);
			}
		});

		api.addServerMemberJoinListener(join -> {
			String message = Constants.GREETINGS[BotUtils.randomNumber(0, Constants.GREETINGS.length - 1)].replaceAll("JOIN_USER", join.getUser().getDisplayName(join.getServer()));
			List<ServerTextChannel> channels = join.getServer().getTextChannels();
			for (ServerTextChannel channel : channels) {
				if (channel.getName().equalsIgnoreCase("general")) {
					channel.sendMessage(message);
				}
			}
		});
	}

	private static void createPoll(MessageCreateEvent event) {
		String pollContents = event.getMessageContent().substring("!poll ".length());
		String[] options = pollContents.split(";");
		String[] reacts = { ":one:", ":two:", ":three:", ":four:", ":five:", ":six:", ":seven:", ":eight:", ":nine:", ":ten:" };

		String pollMessage = Constants.POLL_INTRO.replace("POLL_USER", event.getMessageAuthor().getDisplayName());
		for (int i = 0; i < options.length; i++) {
			if (i == 0) {
				pollMessage = pollMessage + "\n" + options[i];
			} else {
				pollMessage = pollMessage + "\n" + i + ": " + options[i];
			}
		}
		try {
			Message poll = event.getChannel().sendMessage(pollMessage).get();
			for (int i = 0; i < options.length - 1; i++) {
				poll.addReaction(EmojiParser.parseToUnicode(reacts[i]));
			}
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}

	private static void rollDice(MessageCreateEvent event) {
		String message = "```";
		int total = 0;

		String dice = event.getMessageContent().substring("!roll ".length());
		String[] rolls = dice.replaceAll("\\-", "\\+-").split("\\+");

		for (String roll : rolls) {
			if(!roll.trim().equals("")) {
				boolean positive = true;
				String[] rollParts = roll.trim().replace(" ", "").split("d");
				
				if(rollParts[0].startsWith("-") && rollParts.length != 1) {
					message = message + " - ";
					positive = false;
				} else if (!message.equals("```")) {
					message = message + " + ";
				}

				if (rollParts.length == 1) {
					message = message + rollParts[0];
					total = total + Integer.parseInt(rollParts[0]);
				} else {
					message = message + "( ";
					for (int i = 0; i < Math.abs(Integer.parseInt(rollParts[0])); i++) {
						int rollValue = BotUtils.randomNumber(1, Integer.parseInt(rollParts[1]));
						message = message + rollValue + " + ";
						if(positive) {
							total = total + rollValue;
						} else {
							total = total - rollValue;
						}
					}
					if(!message.equals("( ")) {
						message = message.substring(0, message.length() - 3);
					}
					message = message + " )";
				}
			}
		}

		message = message + " = " + total + "```";

		event.getChannel().sendMessage(message);
	}

	private static String getCountDown() {
		String message = Constants.COUNTDOWN;

		Calendar target = Calendar.getInstance();
		target.set(Calendar.DAY_OF_MONTH, 26);
		target.set(Calendar.MONTH, 6);
		target.set(Calendar.HOUR_OF_DAY, 0);
		target.set(Calendar.MINUTE, 0);
		target.set(Calendar.SECOND, 0);
		target.set(Calendar.MILLISECOND, 0);

		Calendar today = Calendar.getInstance();
		today.set(Calendar.HOUR_OF_DAY, 0);
		today.set(Calendar.MINUTE, 0);
		today.set(Calendar.SECOND, 0);
		today.set(Calendar.MILLISECOND, 0);

		long difference = target.getTime().getTime() - today.getTime().getTime();
		int dayCount = Math.toIntExact(difference / (1000 * 60 * 60 * 24));

		return message.replace("DAYCOUNT", dayCount + "");
	}

	private static void play(MessageCreateEvent event) {
		String link = event.getMessageContent().substring("!play ".length());
		System.out.println(link);

		User user = event.getMessageAuthor().asUser().get();
		Server server = event.getMessage().getServer().get();

		if (server.getConnectedVoiceChannel(user).isPresent()) {
			ServerVoiceChannel voiceChannel = event.getMessage().getServer().get().getConnectedVoiceChannel(user).get();
		}
	}
}
