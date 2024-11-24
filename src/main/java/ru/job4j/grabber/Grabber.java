package ru.job4j.grabber;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Properties;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public class Grabber implements Grab {
    @Override
    public void init(Parse parse, Store store, Scheduler scheduler) throws SchedulerException, IOException {
        int pageCount = 5;
        int time = Integer.parseInt(cfg().getProperty("time"));
        JobDataMap data = new JobDataMap();
        data.put("store", store);
        data.put("parse", parse);
        data.put("pageCount", pageCount);
        JobDetail job = newJob(GrabJob.class)
                .usingJobData(data)
                .build();
        SimpleScheduleBuilder times = simpleSchedule()
                .withIntervalInSeconds(time)
                .repeatForever();
        Trigger trigger = newTrigger()
                .startNow()
                .withSchedule(times)
                .build();
        scheduler.scheduleJob(job, trigger);
    }

    public static class GrabJob implements Job {
        @Override
        public void execute(JobExecutionContext context) {
            JobDataMap map = context.getJobDetail().getJobDataMap();
            Store store = (Store) map.get("store");
            Parse parse = (Parse) map.get("parse");
            int pages = (int) map.get("pageCount");
            for (int i = 1; i <= pages; i++) {
                List<Post> posts;
                try {
                    posts = parse.list(parse.getSourceLink(i));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                posts.forEach(store::save);
            }
        }
    }

    /*
    Сервер. Ответ от сервера будет в виде списка вакансий.
    С помощью этого метода имеем возможность получить данные через браузер
    от нашего граббера
     */
    public void web(Store store) {
        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(Integer.parseInt(cfg().getProperty("port")))) {
                while (!server.isClosed()) {
                    Socket socket = server.accept();
                    try (OutputStream out = socket.getOutputStream()) {
                        out.write("HTTP/1.1 200 OK\r\n\r\n".getBytes());
                        for (Post post : store.getAll()) {
                            out.write(post.toString().getBytes(Charset.forName("Windows-1251")));
                            out.write(System.lineSeparator().getBytes());
                        }
                    } catch (IOException io) {
                        io.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /*
    метод для получения объекта типа Properties,
    чтобы получать значения из конфиг-файла в используемых методах
     */
    private Properties cfg() throws IOException {
        try (InputStream input = Grabber.class.getClassLoader()
                .getResourceAsStream("app.properties")) {
            Properties config = new Properties();
            config.load(input);
            return config;
        }
    }

    /*
    метод для запуска шедуллера
     */
    private Scheduler scheduler() throws SchedulerException {
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.start();
        return scheduler;
    }

    /*
    метод для получения объекта Store
     */
    private Store store() {
        return new PsqlStore();
    }

    public static void main(String[] args) throws Exception {
        Grabber grab = new Grabber();
        grab.cfg();
        Scheduler scheduler = grab.scheduler();
        Store store = grab.store();
        grab.init(new HabrCareerParse(new HabrCareerDateTimeParser()), store, scheduler);
        grab.web(store);
    }
}