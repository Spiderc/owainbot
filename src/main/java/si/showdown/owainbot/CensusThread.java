package si.showdown.owainbot;

import java.time.Instant;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageSet;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;

public class CensusThread implements Runnable {
	static MessageCreateEvent event;

	public CensusThread(MessageCreateEvent event) {
		CensusThread.event = event;
	}

	public void run() {
		System.out.println("starting run");
		census();
		System.out.println("ending run");
	}

	private static void census() {
		if (event.getMessage().getUserAuthor().get().getId() == 116331879635877894L) {
			Server server = event.getMessage().getServer().get();

			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.MONTH, -1);

			Predicate<Message> monthCheck = m -> {
				return m.getCreationTimestamp().isBefore(calendar.toInstant());
			};

			Map<String, Integer> messageCounts = new HashMap<>();

			for (ServerTextChannel channel : server.getTextChannels()) {
				if (channel.canYouSee()) {
					System.out.println("starting to look at " + channel.getName());
					try {
						Map<String, Instant> lastMessage = new HashMap<>();

						MessageSet messages = channel.getMessagesUntil(monthCheck).get();
						System.out.println("has " + messages.size() + " messages");
						Iterator<Message> iterator = messages.iterator();

						while (iterator.hasNext()) {
							Message message = iterator.next();
							if (message.getAuthor().isRegularUser()) {
								boolean increment = false;
								if (lastMessage.containsKey(message.getAuthor().getDisplayName())) {
									if ((message.getCreationTimestamp().getEpochSecond() - lastMessage.get(message.getAuthor().getDisplayName()).getEpochSecond()) > 3) {
										increment = true;
									}
								} else {
									increment = true;
								}

								lastMessage.put(message.getAuthor().getDisplayName(), message.getCreationTimestamp());

								if (increment) {
									if (messageCounts.containsKey(message.getAuthor().getDisplayName())) {
										messageCounts.put(message.getAuthor().getDisplayName(), messageCounts.get(message.getAuthor().getDisplayName()) + 1);
									} else {
										messageCounts.put(message.getAuthor().getDisplayName(), 1);
									}
								}
							}
						}
					} catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
					}
				}
			}

			String result = "```Message counts in the last month:";

			Map<String, Integer> sortedMap = sortByValue(messageCounts);
			for (String user : sortedMap.keySet()) {
				result = result + "\n" + user + ": " + sortedMap.get(user);
			}
			result = result + "```";

			System.out.println(result);

			event.getChannel().sendMessage(result);
		}
	}

	private static HashMap<String, Integer> sortByValue(Map<String, Integer> hm) {
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
