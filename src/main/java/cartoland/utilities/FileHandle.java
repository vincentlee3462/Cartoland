package cartoland.utilities;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;

public class FileHandle
{
	//將JSON讀入進字串
	static String buildJsonStringFromFile(String fileName)
	{
		try
		{
			return Files.readString(Paths.get(fileName));
		}
		catch (IOException exception)
		{
			exception.printStackTrace(System.err);
			return "{}";
		}
	}

	public static void synchronizeUsersFile()
	{
		try
		{
			FileWriter writer = new FileWriter("users.json"); //同步到檔案裡
			writer.write(JsonHandle.getUsersFileString());
			writer.close();
		}
		catch (IOException exception)
		{
			exception.printStackTrace(System.err);
			System.exit(-1);
		}
	}

	public static void logIntoFile(String output)
	{
		try
		{
			//一定要事先備好logs資料夾
			FileWriter logWriter = new FileWriter("logs/" + LocalDate.now(), true);
			logWriter.write(LocalTime.now() + "\t" + output + "\n"); //時間 內容 換行
			logWriter.close();
		}
		catch (IOException e)
		{
			e.printStackTrace(System.err);
			System.exit(-1);
		}
	}
}
