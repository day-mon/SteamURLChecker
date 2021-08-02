import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class App
{
      private static final int DEFAULT_CLUSTER_SIZE = 50;

      public static void main(String[] args) throws IOException
      {
            var scanner = new Scanner(System.in);
            var dir = new File(System.getProperty("user.dir"));


            var files = Files.walk(Path.of(dir.getPath()))
                    .filter(path -> path.getFileName().toString().endsWith(".txt"))
                    .filter(file ->
                    {
                          try
                          {
                                return Files.size(file) != 0;
                          }
                          catch (IOException e)
                          {
                                e.printStackTrace();
                          }
                          return false;
                    })
                    .collect(Collectors.toList());

            if (files.isEmpty())
            {
                  System.out.println("You have no non-empty .txt files in the application directory");
                  System.exit(1);
            }

            else if (files.size() == 1)
            {
                  var file = files.get(0);
                  startSearch(file);
                  return;
            }

            for (int i = 0; i < files.size(); i++)
            {
                  System.out.println(i + 1 + " " + files.get(i));
            }

            var validAnswer = false;
            String answer = "";

            while (!validAnswer)
            {
                  System.out.print("Please choose a item from the list using the numbers on the right: ");
                  answer = scanner.nextLine();


                  validAnswer = answer.matches("-?\\d+(\\.\\d+)?") && Integer.parseInt(answer) >= 1 && Integer.parseInt(answer) <= files.size();
            }

            var choice = Integer.parseInt(answer) - 1;
            startSearch(files.get(choice));
      }


      public static void startSearch(Path path)
      {
            var list = readFile(path)
                    .stream()
                    .map(string -> string.replaceAll("\\s+", "_"))
                    .distinct()
                    .collect(Collectors.toList());


            if (list.size() >= 100)
            {
                  System.out.println("Just so you are aware, you may be rate limited due to the amount of ids you are checking");
                  makeThreads(list);
                  return;
            }

            System.out.printf("Found %d unique ids. Checking now. \n", list.size());
            doSearch(list);
      }

      public static List<String> readFile(Path path)
      {
            String line;
            List<String> list = new ArrayList<>();
            try
            {
                  BufferedReader reader = new BufferedReader(new FileReader(String.valueOf(path)));
                  while ((line = reader.readLine()) != null)
                  {
                        list.add(line);
                  }
            }
            catch (Exception e)
            {
                  System.err.println("There was an error while reading your file");
                  e.printStackTrace();
            }
            return list;
      }

      public static void doSearch(List<String> list)
      {
            var defaultUrl = "https://steamcommunity.com/id/";
            List<String> missedIds = new ArrayList<>();

            for (var item : list)
            {
                  Document document;
                  try
                  {
                        document = Jsoup.connect(defaultUrl + item)
                                .get();
                  }
                  catch (IOException e)
                  {
                        e.printStackTrace();
                        missedIds.add(item);
                        return;
                  }

                  var errorElement = document.getElementsByClass("error_ctn").text();


                  System.out.printf("%s is%s taken %s\n", item, (errorElement.length() == 0 ? "" : " not"), defaultUrl + item);
            }
            if (!missedIds.isEmpty())
            {
                  System.out.println("There was " + missedIds + " ids that were not checked \n " + list);
            }
      }


      public static void makeThreads(List<String> list)
      {
            final var listSize = list.size();
            final var threadCount = (int) (Math.ceil(listSize / DEFAULT_CLUSTER_SIZE));
            final ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

            System.out.printf("Found %d unique ids. Checking now with %d threads. \n", listSize, threadCount);

            for (var i = 0; i < list.size() - 1; i += DEFAULT_CLUSTER_SIZE)
            {
                  if (i + DEFAULT_CLUSTER_SIZE > listSize)
                  {
                        var subList = list.subList(i, listSize);
                        executorService.submit(() -> doSearch(subList));
                        break;
                  }
                  var subList = list.subList(i, i + DEFAULT_CLUSTER_SIZE);

                  executorService.submit(() -> doSearch(subList));
            }

      }
}
