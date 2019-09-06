package si.showdown.owainbot;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageSet;
import org.javacord.api.entity.permission.Role;
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

		api.addServerTextChannelChangeTopicListener(event -> {
			try {
				User user = api.getUserById(116331879635877894L).get();
				user.sendMessage("The topic in #" + event.getServerTextChannel().get().getName() + " changed from '" + event.getOldTopic() + "' to '" + event.getNewTopic() + "'");
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		});

		api.addMessageCreateListener(event -> {
			if (event.getMessageContent().equalsIgnoreCase("!help")) {
				event.getMessage().getUserAuthor().get().sendMessage(Constants.HELP);
			} else if (event.getMessageContent().equalsIgnoreCase("!bard")) {
				event.getChannel().sendMessage(Constants.BARD_LINK);
			} else if (event.getMessageContent().equalsIgnoreCase("!eb")) {
				event.getChannel().sendMessage(Constants.EB_LINK);
			} else if (event.getMessageContent().equalsIgnoreCase("!aoa")) {
				event.getChannel().sendMessage(Constants.AOA_LINK);
			} else if (event.getMessageContent().equalsIgnoreCase("!source")) {
				event.getChannel().sendMessage(Constants.SOURCE);
			} else if (event.getMessageContent().equalsIgnoreCase("!roles")) {
				roles(event);
			} else if (event.getMessageContent().equalsIgnoreCase("!attack")) {
				attack(event);
			} else if (event.getMessageContent().startsWith("!poll ")) {
				createPoll(event);
			} else if (event.getMessageContent().startsWith("!roll ")) {
				rollDice(event);
			} else if (event.getMessageContent().startsWith("!play ")) {
				play(event);
			} else if (event.getMessageContent().startsWith("!addrole ")) {
				addRole(event);
			} else if (event.getMessageContent().startsWith("!removerole ")) {
				removeRole(event);
			} else if (event.getMessageContent().equals("!census")) {
				census(event);
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

	private static void roles(MessageCreateEvent event) {
		Server server = event.getMessage().getServer().get();
		Role botRole = null;

		for (Role role : event.getApi().getYourself().getRoles(server)) {
			if (role.isManaged() && !role.isEveryoneRole()) {
				botRole = role;
			}
		}

		String roles = "```";
		for (Role role : server.getRoles()) {
			if (!role.isManaged() && !role.isEveryoneRole()) {
				roles = roles + "\n" + role.getName() + ": " + role.getUsers().size();
			} else if (botRole != null && role == botRole) {
				break;
			}
		}

		event.getChannel().sendMessage(roles + "```");
	}

	private static void attack(MessageCreateEvent event) {
		User user = event.getMessageAuthor().asUser().get();
		Server server = event.getMessage().getServer().get();

		boolean foundRole = false;
		for (Role userRole : user.getRoles(server)) {
			if (userRole.getName().equals("Black Eagles")) {
				event.getChannel().sendMessage("Black Eagle Bash!");
				foundRole = true;
				break;
			} else if (userRole.getName().equals("Blue Lions")) {
				event.getChannel().sendMessage("Blue Lion Lacerate!");
				foundRole = true;
				break;
			} else if (userRole.getName().equals("Golden Deer")) {
				event.getChannel().sendMessage("Golden Deer Gore!");
				foundRole = true;
				break;
			}
		}

		if (!foundRole) {
			event.getChannel().sendMessage("My sword hand twitches, but I must know your allegiance before I can show you my latest attack!");
		}
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
			if (!roll.trim().equals("")) {
				boolean positive = true;
				String[] rollParts = roll.trim().replace(" ", "").split("d");

				if (rollParts[0].startsWith("-") && rollParts.length != 1) {
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
						if (positive) {
							total = total + rollValue;
						} else {
							total = total - rollValue;
						}
					}
					if (!message.equals("( ")) {
						message = message.substring(0, message.length() - 3);
					}
					message = message + " )";
				}
			}
		}

		message = message + " = " + total + "```";

		event.getChannel().sendMessage(message);
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

	private static void addRole(MessageCreateEvent event) {
		User user = event.getMessageAuthor().asUser().get();
		Server server = event.getMessage().getServer().get();

		String role = event.getMessageContent().substring("!addrole ".length());
		Role targetRole = null;

		for (Role serverRole : server.getRoles()) {
			if (serverRole.getName().toLowerCase().equals(role.toLowerCase())) {
				targetRole = serverRole;
				break;
			}
		}

		if (targetRole != null) {
			user.addRole(targetRole);
		}
	}

	private static void removeRole(MessageCreateEvent event) {
		User user = event.getMessageAuthor().asUser().get();
		Server server = event.getMessage().getServer().get();

		String role = event.getMessageContent().substring("!removerole ".length());
		Role targetRole = null;

		for (Role userRole : user.getRoles(server)) {
			if (userRole.getName().toLowerCase().equals(role.toLowerCase())) {
				targetRole = userRole;
				break;
			}
		}

		if (targetRole != null) {
			user.removeRole(targetRole);
		}
	}

	private static void census(MessageCreateEvent event) {
		if (event.getMessage().getUserAuthor().get().getId() == 116331879635877894L) {
			Server server = event.getMessage().getServer().get();

			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.MONTH, -1);

			Predicate<Message> monthCheck = m -> {
				return m.getCreationTimestamp().isBefore(calendar.toInstant());
			};

			Map<String, Integer> messageCounts = new HashMap<>();

			for (ServerTextChannel channel : server.getTextChannels()) {
				try {
					MessageSet messages = channel.getMessagesUntil(monthCheck).get();
					Iterator<Message> iterator = messages.iterator();

					while (iterator.hasNext()) {
						Message message = iterator.next();
						if (message.getAuthor().isRegularUser()) {
							if (messageCounts.containsKey(message.getAuthor().getDisplayName())) {
								messageCounts.put(message.getAuthor().getDisplayName(), messageCounts.get(message.getAuthor().getDisplayName()) + 1);
							} else {
								messageCounts.put(message.getAuthor().getDisplayName(), 1);
							}
						}
					}
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			}

			String result = "```In the last month:";

			Map<String, Integer> sortedMap = sortByValue(messageCounts);
			for (String user : sortedMap.keySet()) {
				result = result + "\n" + user + " posted " + sortedMap.get(user) + " messages";
			}
			result = result + "```";

			event.getChannel().sendMessage(result);
		}
	}

	public static HashMap<String, Integer> sortByValue(Map<String, Integer> hm) {
		List<Map.Entry<String, Integer>> list = new LinkedList<Map.Entry<String, Integer>>(hm.entrySet());

		Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
			public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
				return (o2.getValue()).compareTo(o1.getValue());
			}
		});

		HashMap<String, Integer> temp = new LinkedHashMap<String, Integer>();
		for (Map.Entry<String, Integer> aa : list) {
			temp.put(aa.getKey(), aa.getValue());
		}
		return temp;
	}
}
