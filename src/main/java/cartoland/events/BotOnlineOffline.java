package cartoland.events;

import cartoland.utilities.CommandBlocksHandle;
import cartoland.utilities.FileHandle;
import cartoland.utilities.IDAndEntities;
import cartoland.utilities.QuestionForumHandle;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static cartoland.utilities.IDAndEntities.*;

/**
 * {@code BotOnlineOffline} is a listener that triggers when this bot went online or went offline normally. It won't
 * trigger if this bot was shutdown by accident, such as killed by ^C, server shutdown, etc. This class was
 * registered in {@link cartoland.Cartoland#main}, with the build of JDA. The {@link #onReady} method helps all the entities
 * (except {@link IDAndEntities#jda}) in {@link IDAndEntities} get their instances, and the {@link #onShutdown}
 * method help synchronize JSONObjects and their files.
 *
 * @since 1.0
 * @author Alex Cai
 */
public class BotOnlineOffline extends ListenerAdapter
{
	private final ScheduledExecutorService scheduleExecutor = Executors.newScheduledThreadPool(2);
	private ScheduledFuture<?> threeAMTask;
	private ScheduledFuture<?> twelvePMTask;

	/**
	 * The method that inherited from {@link ListenerAdapter}, triggers when the bot was online. It will initialize
	 * entities in {@link IDAndEntities}, start schedule events and send online message to bot channel.
	 *
	 * @param event The event that carries information.
	 * @since 1.0
	 * @author Alex Cai
	 */
	@Override
	public void onReady(@NotNull ReadyEvent event)
	{
		cartolandServer = jda.getGuildById(CARTOLAND_SERVER_ID); //創聯
		if (cartolandServer == null)
			problemOccurred("Can't find Cartoland Server");

		questionsChannel = cartolandServer.getForumChannelById(QUESTIONS_CHANNEL_ID);
		if (questionsChannel == null)
			problemOccurred("Can't find Questions Channel.");

		voteKickChannel = cartolandServer.getForumChannelById(VOTE_KICK_CHANNEL_ID);
		if (voteKickChannel == null)
			problemOccurred("Can't find Vote Kick Channel.");

		lobbyChannel = cartolandServer.getTextChannelById(LOBBY_CHANNEL_ID); //創聯的大廳頻道
		if (lobbyChannel == null)
			problemOccurred("Can't find Lobby Channel.");

		botChannel = cartolandServer.getTextChannelById(BOT_CHANNEL_ID); //創聯的機器人頻道
		if (botChannel == null)
			problemOccurred("Can't find Bot Channel.");

		undergroundChannel = cartolandServer.getTextChannelById(UNDERGROUND_CHANNEL_ID); //創聯的地下聊天室
		if (undergroundChannel == null)
			problemOccurred("Can't find Underground Channel.");

		resolvedForumTag = questionsChannel.getAvailableTagById(RESOLVED_FORUM_TAG_ID);
		if (resolvedForumTag == null)
			problemOccurred("Can't find Resolved Forum Tag");

		unresolvedForumTag = questionsChannel.getAvailableTagById(UNRESOLVED_FORUM_TAG_ID);
		if (unresolvedForumTag == null)
			problemOccurred("Can't find Unresolved Forum Tag");

		godOfGamblersRole = cartolandServer.getRoleById(GOD_OF_GAMBLERS_ROLE_ID); //賭神身分組
		if (godOfGamblersRole == null)
			problemOccurred("Can't find god Of Gamblers Role.");

		memberRole = cartolandServer.getRoleById(MEMBER_ROLE_ID); //會員身分組
		if (memberRole == null)
			problemOccurred("Can't find Member Role.");

		nsfwRole = cartolandServer.getRoleById(NSFW_ROLE_ID); //地下身分組
		if (nsfwRole == null)
			problemOccurred("Can't find NSFW Role.");

		botItself = jda.getSelfUser(); //機器人自己

		ohBoy3AM(); //好棒 三點了

		idleFormPost12PM(); //中午十二點時處理並提醒未解決的論壇貼文

		CommandBlocksHandle.initial(); //初始化idAndName

		botChannel.sendMessage("Cartoland Bot 已上線。\nCartoland Bot is now online.").queue();
		String logString = "online";
		System.out.println(logString);
		FileHandle.log(logString);
	}

	/**
	 * The method that inherited from {@link ListenerAdapter}. When the bot go offline normally, it will shut
	 * down scheduled events, log "offline" to terminal & log file, serialize idle forum posts and write
	 * JSONObject into users.json & command_blocks.json.
	 *
	 * @param event Information about the shutdown.
	 * @since 1.0
	 * @author Alex Cai
	 */
	@Override
	public void onShutdown(@NotNull ShutdownEvent event)
	{
		//https://stackoverflow.com/questions/34202701
		threeAMTask.cancel(true);
		twelvePMTask.cancel(true);
		scheduleExecutor.shutdown();

		FileHandle.serialize(); //所有有註冊的物件

		String logString = "offline";
		System.out.println(logString);
		FileHandle.log(logString);
		FileHandle.closeLog();
	}

	/**
	 * When an error occurred, an entity is null.
	 *
	 * @param logString The content that will print to standard error stream and log file.
	 * @throws NullPointerException always throw
	 */
	private void problemOccurred(String logString)
	{
		System.err.println(logString);
		FileHandle.log(logString);
		jda.shutdownNow();
		throw new NullPointerException();
	}

	private long secondsUntil(int hour)
	{
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime untilTime = now.withHour(hour).withMinute(0).withSecond(0);

		if (now.compareTo(untilTime) > 0)
			untilTime = untilTime.plusDays(1L);

		return Duration.between(now, untilTime).getSeconds();
	}

	//https://stackoverflow.com/questions/65984126
	private void ohBoy3AM()
	{
		threeAMTask = scheduleExecutor.scheduleAtFixedRate(() ->
		{
			undergroundChannel.sendMessage("https://i.imgur.com/c0HCirP.jpg").queue();
			undergroundChannel.sendMessage("https://i.imgur.com/EGO35hf.jpg").queue();
		}, secondsUntil(3), TimeUnit.DAYS.toSeconds(1), TimeUnit.SECONDS);
	}

	private void idleFormPost12PM()
	{
		twelvePMTask = scheduleExecutor.scheduleAtFixedRate(
			() -> questionsChannel.getThreadChannels().forEach(QuestionForumHandle::tryIdleForumPost),
			secondsUntil(12), TimeUnit.DAYS.toSeconds(1), TimeUnit.SECONDS);
	}
}